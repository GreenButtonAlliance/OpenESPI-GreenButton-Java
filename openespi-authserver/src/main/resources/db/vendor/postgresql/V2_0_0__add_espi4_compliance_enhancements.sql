-- NAESB ESPI 4.0 Compliance Enhancements for PostgreSQL
-- Adds advanced security features, performance optimizations, and ESPI 4.0 requirements

-- Create custom types for better data integrity
CREATE TYPE client_status_type AS ENUM ('active', 'suspended', 'revoked');
CREATE TYPE security_level_type AS ENUM ('standard', 'high', 'maximum');
CREATE TYPE token_type_enum AS ENUM ('access_token', 'refresh_token', 'id_token');
CREATE TYPE consent_method_type AS ENUM ('web', 'mobile', 'api', 'phone', 'in_person');
CREATE TYPE security_classification_type AS ENUM ('public', 'internal', 'confidential', 'restricted');
CREATE TYPE certification_status_type AS ENUM ('self_certified', 'third_party_certified', 'gba_certified');

-- Add ESPI 4.0 specific columns to existing tables
ALTER TABLE oauth2_registered_client 
ADD COLUMN espi_version VARCHAR(10) DEFAULT '4.0',
ADD COLUMN security_level security_level_type DEFAULT 'standard',
ADD COLUMN certificate_subject_dn VARCHAR(1000) DEFAULT NULL,
ADD COLUMN certificate_serial_number VARCHAR(100) DEFAULT NULL,
ADD COLUMN certificate_thumbprint VARCHAR(128) DEFAULT NULL,
ADD COLUMN rate_limit_per_minute INTEGER DEFAULT 100,
ADD COLUMN max_concurrent_sessions INTEGER DEFAULT 5,
ADD COLUMN client_status client_status_type DEFAULT 'active',
ADD COLUMN created_by VARCHAR(100) DEFAULT 'system',
ADD COLUMN updated_by VARCHAR(100) DEFAULT 'system',
ADD COLUMN last_used_at TIMESTAMP DEFAULT NULL,
ADD COLUMN failure_count INTEGER DEFAULT 0,
ADD COLUMN locked_until TIMESTAMP DEFAULT NULL;

-- Add comments to new columns
COMMENT ON COLUMN oauth2_registered_client.espi_version IS 'NAESB ESPI version compliance';
COMMENT ON COLUMN oauth2_registered_client.security_level IS 'Security classification level';
COMMENT ON COLUMN oauth2_registered_client.certificate_subject_dn IS 'Client certificate subject DN for mTLS';
COMMENT ON COLUMN oauth2_registered_client.certificate_serial_number IS 'Client certificate serial number';
COMMENT ON COLUMN oauth2_registered_client.certificate_thumbprint IS 'SHA-256 thumbprint of client certificate';
COMMENT ON COLUMN oauth2_registered_client.rate_limit_per_minute IS 'API rate limit per minute';
COMMENT ON COLUMN oauth2_registered_client.max_concurrent_sessions IS 'Maximum concurrent sessions allowed';
COMMENT ON COLUMN oauth2_registered_client.client_status IS 'Client status';
COMMENT ON COLUMN oauth2_registered_client.created_by IS 'Created by user/system';
COMMENT ON COLUMN oauth2_registered_client.updated_by IS 'Last updated by user/system';
COMMENT ON COLUMN oauth2_registered_client.last_used_at IS 'Last successful authentication timestamp';
COMMENT ON COLUMN oauth2_registered_client.failure_count IS 'Consecutive authentication failures';
COMMENT ON COLUMN oauth2_registered_client.locked_until IS 'Account lock expiration timestamp';

-- Add ESPI 4.0 compliance audit columns to oauth2_authorization
ALTER TABLE oauth2_authorization
ADD COLUMN espi_compliance_level VARCHAR(20) DEFAULT 'ESPI_4_0',
ADD COLUMN client_ip_address INET DEFAULT NULL,
ADD COLUMN user_agent VARCHAR(500) DEFAULT NULL,
ADD COLUMN geo_location VARCHAR(100) DEFAULT NULL,
ADD COLUMN session_id VARCHAR(128) DEFAULT NULL,
ADD COLUMN risk_score DECIMAL(3,2) DEFAULT 0.00,
ADD COLUMN fraud_indicators JSONB DEFAULT NULL,
ADD COLUMN consent_version VARCHAR(20) DEFAULT '1.0';

-- Add comments to oauth2_authorization columns
COMMENT ON COLUMN oauth2_authorization.espi_compliance_level IS 'ESPI compliance level';
COMMENT ON COLUMN oauth2_authorization.client_ip_address IS 'Client IP address for audit';
COMMENT ON COLUMN oauth2_authorization.user_agent IS 'Client user agent string';
COMMENT ON COLUMN oauth2_authorization.geo_location IS 'Geographic location if available';
COMMENT ON COLUMN oauth2_authorization.session_id IS 'Session identifier for tracking';
COMMENT ON COLUMN oauth2_authorization.risk_score IS 'Calculated risk score (0.00-1.00)';
COMMENT ON COLUMN oauth2_authorization.fraud_indicators IS 'JSON array of fraud indicators';
COMMENT ON COLUMN oauth2_authorization.consent_version IS 'Consent form version used';

-- Create audit trail table for ESPI 4.0 compliance
CREATE TABLE oauth2_audit_log (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    client_id VARCHAR(100) NOT NULL,
    principal_name VARCHAR(200) DEFAULT NULL,
    event_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ip_address INET DEFAULT NULL,
    user_agent VARCHAR(500) DEFAULT NULL,
    session_id VARCHAR(128) DEFAULT NULL,
    request_id VARCHAR(128) DEFAULT NULL,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    error_code VARCHAR(50) DEFAULT NULL,
    error_description VARCHAR(500) DEFAULT NULL,
    scopes_requested VARCHAR(1000) DEFAULT NULL,
    scopes_granted VARCHAR(1000) DEFAULT NULL,
    grant_type VARCHAR(50) DEFAULT NULL,
    espi_version VARCHAR(10) DEFAULT '4.0',
    compliance_flags JSONB DEFAULT NULL,
    additional_data JSONB DEFAULT NULL
);

-- Add comments and indexes for audit log
COMMENT ON TABLE oauth2_audit_log IS 'OAuth2 audit log for ESPI 4.0 compliance';
COMMENT ON COLUMN oauth2_audit_log.event_type IS 'Type of event (login, token_issued, etc.)';
COMMENT ON COLUMN oauth2_audit_log.client_id IS 'OAuth2 client identifier';
COMMENT ON COLUMN oauth2_audit_log.principal_name IS 'User principal name';
COMMENT ON COLUMN oauth2_audit_log.event_timestamp IS 'When the event occurred';
COMMENT ON COLUMN oauth2_audit_log.ip_address IS 'Client IP address';
COMMENT ON COLUMN oauth2_audit_log.user_agent IS 'Client user agent';
COMMENT ON COLUMN oauth2_audit_log.session_id IS 'Session identifier';
COMMENT ON COLUMN oauth2_audit_log.request_id IS 'Unique request identifier';
COMMENT ON COLUMN oauth2_audit_log.success IS 'Whether the operation succeeded';
COMMENT ON COLUMN oauth2_audit_log.error_code IS 'Error code if failed';
COMMENT ON COLUMN oauth2_audit_log.error_description IS 'Error description if failed';
COMMENT ON COLUMN oauth2_audit_log.scopes_requested IS 'Scopes requested in operation';
COMMENT ON COLUMN oauth2_audit_log.scopes_granted IS 'Scopes actually granted';
COMMENT ON COLUMN oauth2_audit_log.grant_type IS 'OAuth2 grant type used';
COMMENT ON COLUMN oauth2_audit_log.espi_version IS 'ESPI version in use';
COMMENT ON COLUMN oauth2_audit_log.compliance_flags IS 'JSON object with compliance flags';
COMMENT ON COLUMN oauth2_audit_log.additional_data IS 'Additional event-specific data';

-- Create indexes for audit log
CREATE INDEX idx_audit_client_timestamp ON oauth2_audit_log (client_id, event_timestamp);
CREATE INDEX idx_audit_principal_timestamp ON oauth2_audit_log (principal_name, event_timestamp);
CREATE INDEX idx_audit_event_type ON oauth2_audit_log (event_type);
CREATE INDEX idx_audit_ip_address ON oauth2_audit_log (ip_address);
CREATE INDEX idx_audit_session_id ON oauth2_audit_log (session_id);
CREATE INDEX idx_audit_request_id ON oauth2_audit_log (request_id);

-- Create token usage tracking table
CREATE TABLE oauth2_token_usage (
    id BIGSERIAL PRIMARY KEY,
    token_id VARCHAR(128) NOT NULL,
    client_id VARCHAR(100) NOT NULL,
    principal_name VARCHAR(200) DEFAULT NULL,
    token_type token_type_enum NOT NULL,
    usage_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    endpoint VARCHAR(200) DEFAULT NULL,
    ip_address INET DEFAULT NULL,
    user_agent VARCHAR(500) DEFAULT NULL,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    response_code INTEGER DEFAULT NULL,
    bytes_transferred BIGINT DEFAULT 0,
    response_time_ms INTEGER DEFAULT NULL,
    espi_resource_type VARCHAR(50) DEFAULT NULL
);

COMMENT ON TABLE oauth2_token_usage IS 'Token usage tracking for analytics and compliance';
COMMENT ON COLUMN oauth2_token_usage.token_id IS 'Token identifier (hash)';
COMMENT ON COLUMN oauth2_token_usage.client_id IS 'OAuth2 client identifier';
COMMENT ON COLUMN oauth2_token_usage.principal_name IS 'User principal name';
COMMENT ON COLUMN oauth2_token_usage.token_type IS 'Type of token';
COMMENT ON COLUMN oauth2_token_usage.usage_timestamp IS 'When token was used';
COMMENT ON COLUMN oauth2_token_usage.endpoint IS 'Endpoint where token was used';
COMMENT ON COLUMN oauth2_token_usage.ip_address IS 'Client IP address';
COMMENT ON COLUMN oauth2_token_usage.user_agent IS 'Client user agent';
COMMENT ON COLUMN oauth2_token_usage.success IS 'Whether the request succeeded';
COMMENT ON COLUMN oauth2_token_usage.response_code IS 'HTTP response code';
COMMENT ON COLUMN oauth2_token_usage.bytes_transferred IS 'Bytes transferred in response';
COMMENT ON COLUMN oauth2_token_usage.response_time_ms IS 'Response time in milliseconds';
COMMENT ON COLUMN oauth2_token_usage.espi_resource_type IS 'Type of ESPI resource accessed';

-- Create indexes for token usage
CREATE INDEX idx_token_usage_token_timestamp ON oauth2_token_usage (token_id, usage_timestamp);
CREATE INDEX idx_token_usage_client_timestamp ON oauth2_token_usage (client_id, usage_timestamp);
CREATE INDEX idx_token_usage_principal ON oauth2_token_usage (principal_name);
CREATE INDEX idx_token_usage_endpoint ON oauth2_token_usage (endpoint);

-- Create enhanced consent management table
CREATE TABLE oauth2_consent_details (
    id BIGSERIAL PRIMARY KEY,
    consent_id VARCHAR(128) NOT NULL UNIQUE,
    registered_client_id VARCHAR(100) NOT NULL,
    principal_name VARCHAR(200) NOT NULL,
    consent_version VARCHAR(20) NOT NULL DEFAULT '1.0',
    consent_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expiration_timestamp TIMESTAMP DEFAULT NULL,
    scopes_consented VARCHAR(1000) NOT NULL,
    espi_data_categories JSONB DEFAULT NULL,
    data_retention_period VARCHAR(50) DEFAULT NULL,
    sharing_restrictions JSONB DEFAULT NULL,
    withdrawal_timestamp TIMESTAMP DEFAULT NULL,
    withdrawal_reason VARCHAR(200) DEFAULT NULL,
    ip_address INET DEFAULT NULL,
    user_agent VARCHAR(500) DEFAULT NULL,
    consent_method consent_method_type DEFAULT 'web',
    consent_evidence TEXT DEFAULT NULL
);

COMMENT ON TABLE oauth2_consent_details IS 'Detailed consent management for ESPI compliance';
COMMENT ON COLUMN oauth2_consent_details.consent_id IS 'Unique consent identifier';
COMMENT ON COLUMN oauth2_consent_details.registered_client_id IS 'OAuth2 client identifier';
COMMENT ON COLUMN oauth2_consent_details.principal_name IS 'User principal name';
COMMENT ON COLUMN oauth2_consent_details.consent_version IS 'Consent form version';
COMMENT ON COLUMN oauth2_consent_details.consent_timestamp IS 'When consent was given';
COMMENT ON COLUMN oauth2_consent_details.expiration_timestamp IS 'When consent expires';
COMMENT ON COLUMN oauth2_consent_details.scopes_consented IS 'Scopes user consented to';
COMMENT ON COLUMN oauth2_consent_details.espi_data_categories IS 'ESPI data categories consented';
COMMENT ON COLUMN oauth2_consent_details.data_retention_period IS 'How long data can be retained';
COMMENT ON COLUMN oauth2_consent_details.sharing_restrictions IS 'Any restrictions on data sharing';
COMMENT ON COLUMN oauth2_consent_details.withdrawal_timestamp IS 'When consent was withdrawn';
COMMENT ON COLUMN oauth2_consent_details.withdrawal_reason IS 'Reason for consent withdrawal';
COMMENT ON COLUMN oauth2_consent_details.ip_address IS 'IP address where consent given';
COMMENT ON COLUMN oauth2_consent_details.user_agent IS 'User agent when consent given';
COMMENT ON COLUMN oauth2_consent_details.consent_method IS 'How consent was obtained';
COMMENT ON COLUMN oauth2_consent_details.consent_evidence IS 'Evidence of consent (e.g., recording ID)';

-- Create indexes for consent details
CREATE INDEX idx_consent_client_principal ON oauth2_consent_details (registered_client_id, principal_name);
CREATE INDEX idx_consent_timestamp ON oauth2_consent_details (consent_timestamp);
CREATE INDEX idx_consent_expiration ON oauth2_consent_details (expiration_timestamp);

-- Add foreign key constraint
ALTER TABLE oauth2_consent_details
ADD CONSTRAINT fk_consent_client FOREIGN KEY (registered_client_id) REFERENCES oauth2_registered_client (id);

-- Create client metrics summary table
CREATE TABLE oauth2_client_metrics (
    id BIGSERIAL PRIMARY KEY,
    client_id VARCHAR(100) NOT NULL,
    metric_date DATE NOT NULL,
    total_requests BIGINT DEFAULT 0,
    successful_requests BIGINT DEFAULT 0,
    failed_requests BIGINT DEFAULT 0,
    total_tokens_issued BIGINT DEFAULT 0,
    total_tokens_revoked BIGINT DEFAULT 0,
    total_bytes_transferred BIGINT DEFAULT 0,
    avg_response_time_ms DECIMAL(10,2) DEFAULT 0.00,
    peak_concurrent_sessions INTEGER DEFAULT 0,
    unique_users_served INTEGER DEFAULT 0,
    espi_data_requests BIGINT DEFAULT 0,
    consent_grants INTEGER DEFAULT 0,
    consent_withdrawals INTEGER DEFAULT 0,
    security_incidents INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (client_id, metric_date)
);

COMMENT ON TABLE oauth2_client_metrics IS 'Daily client metrics for monitoring and reporting';
COMMENT ON COLUMN oauth2_client_metrics.client_id IS 'OAuth2 client identifier';
COMMENT ON COLUMN oauth2_client_metrics.metric_date IS 'Date for metrics (daily aggregation)';
COMMENT ON COLUMN oauth2_client_metrics.total_requests IS 'Total API requests';
COMMENT ON COLUMN oauth2_client_metrics.successful_requests IS 'Successful API requests';
COMMENT ON COLUMN oauth2_client_metrics.failed_requests IS 'Failed API requests';
COMMENT ON COLUMN oauth2_client_metrics.total_tokens_issued IS 'Total tokens issued';
COMMENT ON COLUMN oauth2_client_metrics.total_tokens_revoked IS 'Total tokens revoked';
COMMENT ON COLUMN oauth2_client_metrics.total_bytes_transferred IS 'Total bytes transferred';
COMMENT ON COLUMN oauth2_client_metrics.avg_response_time_ms IS 'Average response time';
COMMENT ON COLUMN oauth2_client_metrics.peak_concurrent_sessions IS 'Peak concurrent sessions';
COMMENT ON COLUMN oauth2_client_metrics.unique_users_served IS 'Unique users served';
COMMENT ON COLUMN oauth2_client_metrics.espi_data_requests IS 'ESPI-specific data requests';
COMMENT ON COLUMN oauth2_client_metrics.consent_grants IS 'New consent grants';
COMMENT ON COLUMN oauth2_client_metrics.consent_withdrawals IS 'Consent withdrawals';
COMMENT ON COLUMN oauth2_client_metrics.security_incidents IS 'Security incidents detected';

-- Create indexes for client metrics
CREATE INDEX idx_client_metrics_date ON oauth2_client_metrics (metric_date);
CREATE INDEX idx_client_metrics_client ON oauth2_client_metrics (client_id);

-- Add foreign key constraint
ALTER TABLE oauth2_client_metrics
ADD CONSTRAINT fk_metrics_client FOREIGN KEY (client_id) REFERENCES oauth2_registered_client (client_id);

-- Enhanced ESPI application info with NAESB ESPI 4.0 fields
ALTER TABLE espi_application_info
ADD COLUMN espi_version VARCHAR(10) DEFAULT '4.0',
ADD COLUMN security_classification security_classification_type DEFAULT 'internal',
ADD COLUMN data_retention_policy VARCHAR(200) DEFAULT NULL,
ADD COLUMN privacy_policy_uri VARCHAR(500) DEFAULT NULL,
ADD COLUMN security_policy_uri VARCHAR(500) DEFAULT NULL,
ADD COLUMN incident_response_contact VARCHAR(200) DEFAULT NULL,
ADD COLUMN compliance_attestation JSONB DEFAULT NULL,
ADD COLUMN certification_status certification_status_type DEFAULT 'self_certified',
ADD COLUMN certification_date DATE DEFAULT NULL,
ADD COLUMN certification_expiry_date DATE DEFAULT NULL,
ADD COLUMN penetration_test_date DATE DEFAULT NULL,
ADD COLUMN vulnerability_scan_date DATE DEFAULT NULL,
ADD COLUMN business_category VARCHAR(100) DEFAULT NULL,
ADD COLUMN service_territory VARCHAR(200) DEFAULT NULL,
ADD COLUMN customer_count_range VARCHAR(50) DEFAULT NULL,
ADD COLUMN annual_data_volume_range VARCHAR(50) DEFAULT NULL;

-- Add comments to espi_application_info columns
COMMENT ON COLUMN espi_application_info.espi_version IS 'NAESB ESPI version compliance';
COMMENT ON COLUMN espi_application_info.security_classification IS 'Data security classification';
COMMENT ON COLUMN espi_application_info.data_retention_policy IS 'Data retention policy statement';
COMMENT ON COLUMN espi_application_info.privacy_policy_uri IS 'Privacy policy URI';
COMMENT ON COLUMN espi_application_info.security_policy_uri IS 'Security policy URI';
COMMENT ON COLUMN espi_application_info.incident_response_contact IS 'Security incident response contact';
COMMENT ON COLUMN espi_application_info.compliance_attestation IS 'JSON object with compliance attestations';
COMMENT ON COLUMN espi_application_info.certification_status IS 'Certification status';
COMMENT ON COLUMN espi_application_info.certification_date IS 'Date of certification';
COMMENT ON COLUMN espi_application_info.certification_expiry_date IS 'Certification expiry date';
COMMENT ON COLUMN espi_application_info.penetration_test_date IS 'Last penetration test date';
COMMENT ON COLUMN espi_application_info.vulnerability_scan_date IS 'Last vulnerability scan date';
COMMENT ON COLUMN espi_application_info.business_category IS 'Business category (utility, aggregator, etc.)';
COMMENT ON COLUMN espi_application_info.service_territory IS 'Geographic service territory';
COMMENT ON COLUMN espi_application_info.customer_count_range IS 'Range of customers served';
COMMENT ON COLUMN espi_application_info.annual_data_volume_range IS 'Annual data volume range';

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
ADD CONSTRAINT chk_failure_count_non_negative CHECK (failure_count >= 0),
ADD CONSTRAINT chk_risk_score_range CHECK (risk_score >= 0.00 AND risk_score <= 1.00);

-- Create function for calculating client metrics
CREATE OR REPLACE FUNCTION calculate_client_metrics(target_date DATE)
RETURNS VOID AS $$
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
    ON CONFLICT (client_id, metric_date) DO UPDATE SET
        total_requests = EXCLUDED.total_requests,
        successful_requests = EXCLUDED.successful_requests,
        failed_requests = EXCLUDED.failed_requests,
        total_tokens_issued = EXCLUDED.total_tokens_issued,
        avg_response_time_ms = EXCLUDED.avg_response_time_ms,
        unique_users_served = EXCLUDED.unique_users_served;
END;
$$ LANGUAGE plpgsql;

-- Create function to update updated_at timestamp for all tables
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for espi_application_info
CREATE TRIGGER update_espi_application_info_updated_at 
    BEFORE UPDATE ON espi_application_info 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Add ESPI 4.0 compliance validation
INSERT INTO oauth2_audit_log (event_type, client_id, principal_name, success, additional_data)
VALUES ('schema_upgrade', 'system', 'migration', TRUE, 
        jsonb_build_object('version', 'V2_0_0', 'espi_version', '4.0', 'upgrade_timestamp', NOW()));

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

-- Create materialized view for client performance monitoring
CREATE MATERIALIZED VIEW mv_client_performance_summary AS
SELECT 
    c.client_id,
    c.client_name,
    c.client_status,
    c.security_level,
    c.espi_version,
    c.last_used_at,
    COUNT(DISTINCT a.id) as total_authorizations,
    COUNT(DISTINCT a.principal_name) as unique_users,
    AVG(m.avg_response_time_ms) as avg_response_time,
    SUM(m.total_requests) as total_requests,
    SUM(m.successful_requests) as successful_requests,
    SUM(m.failed_requests) as failed_requests,
    CASE 
        WHEN SUM(m.total_requests) > 0 THEN 
            (SUM(m.successful_requests)::DECIMAL / SUM(m.total_requests) * 100)
        ELSE 0 
    END as success_rate_percent
FROM oauth2_registered_client c
LEFT JOIN oauth2_authorization a ON c.id = a.registered_client_id
LEFT JOIN oauth2_client_metrics m ON c.client_id = m.client_id
WHERE m.metric_date >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY c.client_id, c.client_name, c.client_status, c.security_level, c.espi_version, c.last_used_at;

-- Create unique index on materialized view
CREATE UNIQUE INDEX idx_mv_client_performance_client_id ON mv_client_performance_summary (client_id);

-- Refresh the materialized view
REFRESH MATERIALIZED VIEW mv_client_performance_summary;