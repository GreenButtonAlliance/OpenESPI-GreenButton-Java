-- OIDC UserInfo Support for MySQL
-- Adds tables and functions for storing user information for OIDC UserInfo endpoint

-- Create table for storing user information
CREATE TABLE oauth2_user_info (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(200) NOT NULL COMMENT 'Unique username (subject identifier)',
    first_name VARCHAR(100) DEFAULT NULL COMMENT 'Given name',
    last_name VARCHAR(100) DEFAULT NULL COMMENT 'Family name',
    email VARCHAR(300) DEFAULT NULL COMMENT 'Email address',
    email_verified BOOLEAN DEFAULT FALSE COMMENT 'Whether email has been verified',
    locale VARCHAR(10) DEFAULT NULL COMMENT 'Locale preference (e.g., en-US)',
    zone_info VARCHAR(50) DEFAULT NULL COMMENT 'Time zone (e.g., America/Los_Angeles)',
    customer_id VARCHAR(100) DEFAULT NULL COMMENT 'DataCustodian customer identifier',
    customer_type VARCHAR(50) DEFAULT NULL COMMENT 'Type of customer (RESIDENTIAL, COMMERCIAL, etc.)',
    account_number VARCHAR(100) DEFAULT NULL COMMENT 'Utility account number',
    service_territory VARCHAR(200) DEFAULT NULL COMMENT 'Geographic service territory',
    phone_number VARCHAR(50) DEFAULT NULL,
    phone_number_verified BOOLEAN DEFAULT FALSE,
    address JSON DEFAULT NULL COMMENT 'JSON object containing address information',
    birthdate DATE DEFAULT NULL,
    gender VARCHAR(20) DEFAULT NULL,
    picture_url VARCHAR(500) DEFAULT NULL,
    website_url VARCHAR(500) DEFAULT NULL,
    profile_url VARCHAR(500) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    INDEX idx_oauth2_user_info_email (email),
    INDEX idx_oauth2_user_info_customer_id (customer_id),
    INDEX idx_oauth2_user_info_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User information for OIDC UserInfo endpoint';

-- Create table for storing OIDC claims mapping
CREATE TABLE oidc_claims_mapping (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scope VARCHAR(100) NOT NULL COMMENT 'OAuth2 scope that enables this claim',
    claim_name VARCHAR(100) NOT NULL COMMENT 'Name of the claim in UserInfo response',
    claim_description VARCHAR(500) DEFAULT NULL,
    essential BOOLEAN DEFAULT FALSE COMMENT 'Whether this claim is essential',
    data_source VARCHAR(50) DEFAULT 'user_info' COMMENT 'Source of claim data (user_info, datacustodian, etc.)',
    espi_specific BOOLEAN DEFAULT FALSE COMMENT 'Whether this is an ESPI-specific claim',
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_scope_claim (scope, claim_name),
    INDEX idx_oidc_claims_scope (scope),
    INDEX idx_oidc_claims_enabled (enabled),
    INDEX idx_oidc_claims_espi (espi_specific)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Configuration for OIDC claims returned by UserInfo endpoint';

-- Insert standard OIDC claims mapping
INSERT INTO oidc_claims_mapping (scope, claim_name, claim_description, essential, data_source, espi_specific) VALUES
-- Profile scope claims
('profile', 'name', 'Full name', false, 'user_info', false),
('profile', 'given_name', 'Given name', false, 'user_info', false),
('profile', 'family_name', 'Family name', false, 'user_info', false),
('profile', 'preferred_username', 'Preferred username', false, 'user_info', false),
('profile', 'locale', 'Locale preference', false, 'user_info', false),
('profile', 'zoneinfo', 'Time zone', false, 'user_info', false),
('profile', 'updated_at', 'Last update time', false, 'user_info', false),
('profile', 'picture', 'Profile picture URL', false, 'user_info', false),
('profile', 'website', 'Website URL', false, 'user_info', false),
('profile', 'profile', 'Profile page URL', false, 'user_info', false),
('profile', 'birthdate', 'Birth date', false, 'user_info', false),
('profile', 'gender', 'Gender', false, 'user_info', false),

-- Email scope claims
('email', 'email', 'Email address', false, 'user_info', false),
('email', 'email_verified', 'Email verification status', false, 'user_info', false),

-- Phone scope claims
('phone', 'phone_number', 'Phone number', false, 'user_info', false),
('phone', 'phone_number_verified', 'Phone verification status', false, 'user_info', false),

-- Address scope claims
('address', 'address', 'Physical mailing address', false, 'user_info', false),

-- ESPI-specific claims
('FB=4_5_15', 'customer_id', 'DataCustodian customer identifier', true, 'datacustodian', true),
('FB=4_5_15', 'customer_type', 'Type of customer', false, 'datacustodian', true),
('FB=4_5_15', 'account_number', 'Utility account number', false, 'datacustodian', true),
('FB=4_5_15', 'service_territory', 'Geographic service territory', false, 'datacustodian', true),
('FB=4_5_15', 'espi_scopes', 'ESPI-specific scopes granted', true, 'computed', true),
('FB=4_5_15', 'authorized_usage_points', 'Authorized usage point IDs', true, 'datacustodian', true),
('FB=4_5_15', 'usage_point_details', 'Detailed usage point information', false, 'datacustodian', true),
('FB=4_5_15', 'datacustodian_grant_id', 'DataCustodian grant identifier', false, 'integration', true),

-- Green Button Alliance extension claims
('openid', 'gba_version', 'Green Button Alliance version', false, 'static', true),
('openid', 'espi_version', 'NAESB ESPI version', false, 'static', true),
('openid', 'data_rights', 'Granted data access rights', false, 'computed', true);

-- Create stored procedure to get claims for scope
DELIMITER //
CREATE PROCEDURE GetClaimsForScope(IN scope_name VARCHAR(100))
BEGIN
    SELECT 
        claim_name,
        essential,
        data_source,
        espi_specific
    FROM oidc_claims_mapping
    WHERE scope = scope_name 
    AND enabled = TRUE
    ORDER BY essential DESC, claim_name;
END //
DELIMITER ;

-- Create function to validate ESPI scope
DELIMITER //
CREATE FUNCTION ValidateESPIScope(scope_value TEXT) 
RETURNS BOOLEAN
READS SQL DATA
DETERMINISTIC
BEGIN
    -- Validate ESPI FB scope format: FB=x_y_z;IntervalDuration=nnn;BlockDuration=xxx;HistoryLength=nn
    RETURN scope_value REGEXP '^FB=[0-9]+_[0-9]+_[0-9]+;IntervalDuration=[0-9]+;BlockDuration=(monthly|daily|hourly);HistoryLength=[0-9]+$';
END //
DELIMITER ;

-- Create function to extract ESPI scope parameters
DELIMITER //
CREATE FUNCTION ExtractESPIScopeParams(scope_value TEXT) 
RETURNS JSON
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE fb_version VARCHAR(20);
    DECLARE interval_duration INT;
    DECLARE block_duration VARCHAR(20);
    DECLARE history_length INT;
    
    IF NOT ValidateESPIScope(scope_value) THEN
        RETURN JSON_OBJECT();
    END IF;
    
    -- Extract FB version (simplified for MySQL)
    SET fb_version = SUBSTRING_INDEX(SUBSTRING_INDEX(scope_value, 'FB=', -1), ';', 1);
    
    -- Extract interval duration
    SET interval_duration = CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(scope_value, 'IntervalDuration=', -1), ';', 1) AS UNSIGNED);
    
    -- Extract block duration
    SET block_duration = SUBSTRING_INDEX(SUBSTRING_INDEX(scope_value, 'BlockDuration=', -1), ';', 1);
    
    -- Extract history length
    SET history_length = CAST(SUBSTRING_INDEX(scope_value, 'HistoryLength=', -1) AS UNSIGNED);
    
    RETURN JSON_OBJECT(
        'fb_version', fb_version,
        'interval_duration', interval_duration,
        'block_duration', block_duration,
        'history_length', history_length
    );
END //
DELIMITER ;

-- Create view for user information with computed fields
CREATE VIEW v_oauth2_user_info_complete AS
SELECT 
    oui.*,
    CASE 
        WHEN oui.first_name IS NOT NULL AND oui.last_name IS NOT NULL 
        THEN CONCAT(oui.first_name, ' ', oui.last_name)
        WHEN oui.first_name IS NOT NULL 
        THEN oui.first_name
        WHEN oui.last_name IS NOT NULL 
        THEN oui.last_name
        ELSE oui.username
    END as full_name,
    CASE 
        WHEN oui.customer_id IS NOT NULL THEN 'customer'
        ELSE 'user'
    END as user_type,
    dim.grant_id as datacustodian_grant_id,
    dim.usage_point_ids as authorized_usage_points,
    dim.integration_status as datacustodian_status
FROM oauth2_user_info oui
LEFT JOIN datacustodian_integration_mapping dim ON oui.customer_id = dim.customer_id;

-- Create stored procedure to cleanup old user info cache
DELIMITER //
CREATE PROCEDURE CleanupUserInfoCache()
BEGIN
    DECLARE updated_count INT DEFAULT 0;
    
    -- Update users that haven't been updated in 24 hours from DataCustodian
    UPDATE oauth2_user_info 
    SET updated_at = CURRENT_TIMESTAMP 
    WHERE customer_id IS NOT NULL 
    AND updated_at < DATE_SUB(NOW(), INTERVAL 24 HOUR);
    
    SET updated_count = ROW_COUNT();
    
    SELECT updated_count as updated_users;
END //
DELIMITER ;

-- Insert sample user data for testing
INSERT INTO oauth2_user_info 
(username, first_name, last_name, email, email_verified, locale, zone_info,
 customer_id, customer_type, account_number, service_territory) 
VALUES 
('customer@example.com', 'John', 'Doe', 'customer@example.com', true, 'en-US', 'America/Los_Angeles',
 'customer-123', 'RESIDENTIAL', 'ACC-789456', 'Northern California'),
 
('admin@example.com', 'Admin', 'User', 'admin@example.com', true, 'en-US', 'America/New_York',
 NULL, NULL, NULL, NULL),
 
('mobile.user@example.com', 'Jane', 'Smith', 'mobile.user@example.com', true, 'en-US', 'America/Chicago',
 'customer-456', 'RESIDENTIAL', 'ACC-123789', 'Illinois');

-- Create event to cleanup user info cache
CREATE EVENT IF NOT EXISTS cleanup_userinfo_cache
ON SCHEDULE EVERY 1 DAY
STARTS TIMESTAMP(CURRENT_DATE + INTERVAL 1 DAY, '03:00:00')
DO CALL CleanupUserInfoCache();

-- Add UserInfo audit log entry
INSERT INTO oauth2_audit_log (event_type, client_id, principal_name, success, additional_data)
VALUES ('schema_upgrade', 'system', 'migration', TRUE, 
        JSON_OBJECT('version', 'V5_0_0', 'feature', 'oidc_userinfo', 
                   'description', 'Added OIDC UserInfo endpoint support', 
                   'migration_timestamp', NOW()));