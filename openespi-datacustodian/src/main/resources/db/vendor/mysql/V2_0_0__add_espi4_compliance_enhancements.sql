-- NAESB ESPI 4.0 Compliance Enhancements for MySQL
-- Adds advanced security features, performance optimizations, and ESPI 4.0 requirements

-- Add ESPI 4.0 specific columns to existing tables
ALTER TABLE oauth2_registered_client 
ADD COLUMN espi_version VARCHAR(10) DEFAULT '4.0' COMMENT 'NAESB ESPI version compliance',
ADD COLUMN security_level ENUM('standard', 'high', 'maximum') DEFAULT 'standard' COMMENT 'Security classification level',
ADD COLUMN certificate_subject_dn VARCHAR(1000) DEFAULT NULL COMMENT 'Client certificate subject DN for mTLS',
ADD COLUMN certificate_serial_number VARCHAR(100) DEFAULT NULL COMMENT 'Client certificate serial number',
ADD COLUMN certificate_thumbprint VARCHAR(128) DEFAULT NULL COMMENT 'SHA-256 thumbprint of client certificate',
ADD COLUMN rate_limit_per_minute INT DEFAULT 100 COMMENT 'API rate limit per minute',
ADD COLUMN max_concurrent_sessions INT DEFAULT 5 COMMENT 'Maximum concurrent sessions allowed',
ADD COLUMN client_status ENUM('active', 'suspended', 'revoked') DEFAULT 'active' COMMENT 'Client status',
ADD COLUMN created_by VARCHAR(100) DEFAULT 'system' COMMENT 'Created by user/system',
ADD COLUMN updated_by VARCHAR(100) DEFAULT 'system' COMMENT 'Last updated by user/system',
ADD COLUMN last_used_at TIMESTAMP NULL DEFAULT NULL COMMENT 'Last successful authentication timestamp',
ADD COLUMN failure_count INT DEFAULT 0 COMMENT 'Consecutive authentication failures',
ADD COLUMN locked_until TIMESTAMP NULL DEFAULT NULL COMMENT 'Account lock expiration timestamp';

-- Add ESPI 4.0 compliance audit columns to oauth2_authorization
ALTER TABLE oauth2_authorization
ADD COLUMN espi_compliance_level VARCHAR(20) DEFAULT 'ESPI_4_0' COMMENT 'ESPI compliance level',
ADD COLUMN client_ip_address VARCHAR(45) DEFAULT NULL COMMENT 'Client IP address for audit',
ADD COLUMN user_agent VARCHAR(500) DEFAULT NULL COMMENT 'Client user agent string',
ADD COLUMN geo_location VARCHAR(100) DEFAULT NULL COMMENT 'Geographic location if available',
ADD COLUMN session_id VARCHAR(128) DEFAULT NULL COMMENT 'Session identifier for tracking',
ADD COLUMN risk_score DECIMAL(3,2) DEFAULT 0.00 COMMENT 'Calculated risk score (0.00-1.00)',
ADD COLUMN fraud_indicators JSON DEFAULT NULL COMMENT 'JSON array of fraud indicators',
ADD COLUMN consent_version VARCHAR(20) DEFAULT '1.0' COMMENT 'Consent form version used';

-- Create audit trail table for ESPI 4.0 compliance
CREATE TABLE oauth2_audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_type VARCHAR(50) NOT NULL COMMENT 'Type of event (login, token_issued, etc.)',
    client_id VARCHAR(100) NOT NULL COMMENT 'OAuth2 client identifier',
    principal_name VARCHAR(200) DEFAULT NULL COMMENT 'User principal name',
    event_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT 'When the event occurred',
    ip_address VARCHAR(45) DEFAULT NULL COMMENT 'Client IP address',
    user_agent VARCHAR(500) DEFAULT NULL COMMENT 'Client user agent',
    session_id VARCHAR(128) DEFAULT NULL COMMENT 'Session identifier',
    request_id VARCHAR(128) DEFAULT NULL COMMENT 'Unique request identifier',
    success BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Whether the operation succeeded',
    error_code VARCHAR(50) DEFAULT NULL COMMENT 'Error code if failed',
    error_description VARCHAR(500) DEFAULT NULL COMMENT 'Error description if failed',
    scopes_requested VARCHAR(1000) DEFAULT NULL COMMENT 'Scopes requested in operation',
    scopes_granted VARCHAR(1000) DEFAULT NULL COMMENT 'Scopes actually granted',
    grant_type VARCHAR(50) DEFAULT NULL COMMENT 'OAuth2 grant type used',
    espi_version VARCHAR(10) DEFAULT '4.0' COMMENT 'ESPI version in use',
    compliance_flags JSON DEFAULT NULL COMMENT 'JSON object with compliance flags',
    additional_data JSON DEFAULT NULL COMMENT 'Additional event-specific data',
    PRIMARY KEY (id),
    INDEX idx_audit_client_timestamp (client_id, event_timestamp),
    INDEX idx_audit_principal_timestamp (principal_name, event_timestamp),
    INDEX idx_audit_event_type (event_type),
    INDEX idx_audit_ip_address (ip_address),
    INDEX idx_audit_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuth2 audit log for ESPI 4.0 compliance';

-- Create token usage tracking table
CREATE TABLE oauth2_token_usage (
    id BIGINT NOT NULL AUTO_INCREMENT,
    token_id VARCHAR(128) NOT NULL COMMENT 'Token identifier (hash)',
    client_id VARCHAR(100) NOT NULL COMMENT 'OAuth2 client identifier',
    principal_name VARCHAR(200) DEFAULT NULL COMMENT 'User principal name',
    token_type ENUM('access_token', 'refresh_token', 'id_token') NOT NULL COMMENT 'Type of token',
    usage_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT 'When token was used',
    endpoint VARCHAR(200) DEFAULT NULL COMMENT 'Endpoint where token was used',
    ip_address VARCHAR(45) DEFAULT NULL COMMENT 'Client IP address',
    user_agent VARCHAR(500) DEFAULT NULL COMMENT 'Client user agent',
    success BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Whether the request succeeded',
    response_code INT DEFAULT NULL COMMENT 'HTTP response code',
    bytes_transferred BIGINT DEFAULT 0 COMMENT 'Bytes transferred in response',
    response_time_ms INT DEFAULT NULL COMMENT 'Response time in milliseconds',
    espi_resource_type VARCHAR(50) DEFAULT NULL COMMENT 'Type of ESPI resource accessed',
    PRIMARY KEY (id),
    INDEX idx_token_usage_token_timestamp (token_id, usage_timestamp),
    INDEX idx_token_usage_client_timestamp (client_id, usage_timestamp),
    INDEX idx_token_usage_principal (principal_name),
    INDEX idx_token_usage_endpoint (endpoint)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Token usage tracking for analytics and compliance';

-- Create enhanced consent management table
CREATE TABLE oauth2_consent_details (
    id BIGINT NOT NULL AUTO_INCREMENT,
    consent_id VARCHAR(128) NOT NULL COMMENT 'Unique consent identifier',
    registered_client_id VARCHAR(100) NOT NULL COMMENT 'OAuth2 client identifier',
    principal_name VARCHAR(200) NOT NULL COMMENT 'User principal name',
    consent_version VARCHAR(20) NOT NULL DEFAULT '1.0' COMMENT 'Consent form version',
    consent_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT 'When consent was given',
    expiration_timestamp TIMESTAMP DEFAULT NULL COMMENT 'When consent expires',
    scopes_consented VARCHAR(1000) NOT NULL COMMENT 'Scopes user consented to',
    espi_data_categories JSON DEFAULT NULL COMMENT 'ESPI data categories consented',
    data_retention_period VARCHAR(50) DEFAULT NULL COMMENT 'How long data can be retained',
    sharing_restrictions JSON DEFAULT NULL COMMENT 'Any restrictions on data sharing',
    withdrawal_timestamp TIMESTAMP DEFAULT NULL COMMENT 'When consent was withdrawn',
    withdrawal_reason VARCHAR(200) DEFAULT NULL COMMENT 'Reason for consent withdrawal',
    ip_address VARCHAR(45) DEFAULT NULL COMMENT 'IP address where consent given',
    user_agent VARCHAR(500) DEFAULT NULL COMMENT 'User agent when consent given',
    consent_method ENUM('web', 'mobile', 'api', 'phone', 'in_person') DEFAULT 'web' COMMENT 'How consent was obtained',
    consent_evidence TEXT DEFAULT NULL COMMENT 'Evidence of consent (e.g., recording ID)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_consent_client_principal (consent_id),
    INDEX idx_consent_client_principal (registered_client_id, principal_name),
    INDEX idx_consent_timestamp (consent_timestamp),
    INDEX idx_consent_expiration (expiration_timestamp),
    CONSTRAINT fk_consent_client FOREIGN KEY (registered_client_id) REFERENCES oauth2_registered_client (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Detailed consent management for ESPI compliance';

-- Create client metrics summary table
CREATE TABLE oauth2_client_metrics (
    id BIGINT NOT NULL AUTO_INCREMENT,
    client_id VARCHAR(100) NOT NULL COMMENT 'OAuth2 client identifier',
    metric_date DATE NOT NULL COMMENT 'Date for metrics (daily aggregation)',
    total_requests BIGINT DEFAULT 0 COMMENT 'Total API requests',
    successful_requests BIGINT DEFAULT 0 COMMENT 'Successful API requests',
    failed_requests BIGINT DEFAULT 0 COMMENT 'Failed API requests',
    total_tokens_issued BIGINT DEFAULT 0 COMMENT 'Total tokens issued',
    total_tokens_revoked BIGINT DEFAULT 0 COMMENT 'Total tokens revoked',
    total_bytes_transferred BIGINT DEFAULT 0 COMMENT 'Total bytes transferred',
    avg_response_time_ms DECIMAL(10,2) DEFAULT 0.00 COMMENT 'Average response time',
    peak_concurrent_sessions INT DEFAULT 0 COMMENT 'Peak concurrent sessions',
    unique_users_served INT DEFAULT 0 COMMENT 'Unique users served',
    espi_data_requests BIGINT DEFAULT 0 COMMENT 'ESPI-specific data requests',
    consent_grants INT DEFAULT 0 COMMENT 'New consent grants',
    consent_withdrawals INT DEFAULT 0 COMMENT 'Consent withdrawals',
    security_incidents INT DEFAULT 0 COMMENT 'Security incidents detected',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_client_metric_date (client_id, metric_date),
    INDEX idx_client_metrics_date (metric_date),
    CONSTRAINT fk_metrics_client FOREIGN KEY (client_id) REFERENCES oauth2_registered_client (client_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Daily client metrics for monitoring and reporting';

-- Enhanced ESPI application info with NAESB ESPI 4.0 fields
ALTER TABLE espi_application_info
ADD COLUMN espi_version VARCHAR(10) DEFAULT '4.0' COMMENT 'NAESB ESPI version compliance',
ADD COLUMN security_classification ENUM('public', 'internal', 'confidential', 'restricted') DEFAULT 'internal' COMMENT 'Data security classification',
ADD COLUMN data_retention_policy VARCHAR(200) DEFAULT NULL COMMENT 'Data retention policy statement',
ADD COLUMN privacy_policy_uri VARCHAR(500) DEFAULT NULL COMMENT 'Privacy policy URI',
ADD COLUMN security_policy_uri VARCHAR(500) DEFAULT NULL COMMENT 'Security policy URI',
ADD COLUMN incident_response_contact VARCHAR(200) DEFAULT NULL COMMENT 'Security incident response contact',
ADD COLUMN compliance_attestation JSON DEFAULT NULL COMMENT 'JSON object with compliance attestations',
ADD COLUMN certification_status ENUM('self_certified', 'third_party_certified', 'gba_certified') DEFAULT 'self_certified' COMMENT 'Certification status',
ADD COLUMN certification_date DATE DEFAULT NULL COMMENT 'Date of certification',
ADD COLUMN certification_expiry_date DATE DEFAULT NULL COMMENT 'Certification expiry date',
ADD COLUMN penetration_test_date DATE DEFAULT NULL COMMENT 'Last penetration test date',
ADD COLUMN vulnerability_scan_date DATE DEFAULT NULL COMMENT 'Last vulnerability scan date',
ADD COLUMN business_category VARCHAR(100) DEFAULT NULL COMMENT 'Business category (utility, aggregator, etc.)',
ADD COLUMN service_territory VARCHAR(200) DEFAULT NULL COMMENT 'Geographic service territory',
ADD COLUMN customer_count_range VARCHAR(50) DEFAULT NULL COMMENT 'Range of customers served',
ADD COLUMN annual_data_volume_range VARCHAR(50) DEFAULT NULL COMMENT 'Annual data volume range';

-- Create performance optimization indexes
CREATE INDEX idx_oauth2_authorization_expires_at ON oauth2_authorization (access_token_expires_at);
CREATE INDEX idx_oauth2_authorization_issued_at ON oauth2_authorization (access_token_issued_at);
CREATE INDEX idx_oauth2_authorization_grant_type ON oauth2_authorization (authorization_grant_type);
CREATE INDEX idx_oauth2_registered_client_status ON oauth2_registered_client (client_status);
CREATE INDEX idx_oauth2_registered_client_last_used ON oauth2_registered_client (last_used_at);
CREATE INDEX idx_oauth2_registered_client_created ON oauth2_registered_client (client_id_issued_at);

-- Add constraint checks for ESPI 4.0 compliance
ALTER TABLE oauth2_registered_client
ADD CONSTRAINT chk_rate_limit_positive CHECK (rate_limit_per_minute > 0),
ADD CONSTRAINT chk_max_sessions_positive CHECK (max_concurrent_sessions > 0),
ADD CONSTRAINT chk_failure_count_non_negative CHECK (failure_count >= 0);

-- Create stored procedure for client metrics calculation
DELIMITER //
CREATE PROCEDURE CalculateClientMetrics(IN target_date DATE)
BEGIN
    -- Calculate daily metrics for all clients
    INSERT INTO oauth2_client_metrics (
        client_id, metric_date, total_requests, successful_requests, failed_requests,
        total_tokens_issued, avg_response_time_ms, unique_users_served
    )
    SELECT 
        au.client_id,
        target_date,
        COUNT(*) as total_requests,
        SUM(CASE WHEN au.success = TRUE THEN 1 ELSE 0 END) as successful_requests,
        SUM(CASE WHEN au.success = FALSE THEN 1 ELSE 0 END) as failed_requests,
        (SELECT COUNT(*) FROM oauth2_authorization oa WHERE oa.registered_client_id = au.client_id 
         AND DATE(oa.access_token_issued_at) = target_date) as total_tokens_issued,
        AVG(tu.response_time_ms) as avg_response_time_ms,
        COUNT(DISTINCT au.principal_name) as unique_users_served
    FROM oauth2_audit_log au
    LEFT JOIN oauth2_token_usage tu ON au.client_id = tu.client_id AND DATE(au.event_timestamp) = DATE(tu.usage_timestamp)
    WHERE DATE(au.event_timestamp) = target_date
    GROUP BY au.client_id
    ON DUPLICATE KEY UPDATE
        total_requests = VALUES(total_requests),
        successful_requests = VALUES(successful_requests),
        failed_requests = VALUES(failed_requests),
        total_tokens_issued = VALUES(total_tokens_issued),
        avg_response_time_ms = VALUES(avg_response_time_ms),
        unique_users_served = VALUES(unique_users_served);
END //
DELIMITER ;

-- Create event to run daily metrics calculation
CREATE EVENT IF NOT EXISTS daily_metrics_calculation
ON SCHEDULE EVERY 1 DAY
STARTS TIMESTAMP(CURRENT_DATE + INTERVAL 1 DAY, '01:00:00')
DO CALL CalculateClientMetrics(CURRENT_DATE - INTERVAL 1 DAY);

-- Set event scheduler to ON (may require SUPER privilege)
-- SET GLOBAL event_scheduler = ON;

-- Add ESPI 4.0 compliance validation
INSERT INTO oauth2_audit_log (event_type, client_id, principal_name, success, additional_data)
VALUES ('schema_upgrade', 'system', 'migration', TRUE, 
        JSON_OBJECT('version', 'V2_0_0', 'espi_version', '4.0', 'upgrade_timestamp', NOW()));

-- Update existing clients to ESPI 4.0 compliance
UPDATE oauth2_registered_client 
SET espi_version = '4.0', 
    security_level = 'high',
    updated_by = 'migration_v2_0_0'
WHERE espi_version IS NULL;

UPDATE espi_application_info 
SET espi_version = '4.0',
    security_classification = 'internal',
    certification_status = 'self_certified'
WHERE espi_version IS NULL;