-- DataCustodian Integration Support for MySQL
-- Adds tables and functions for integrating with OpenESPI-DataCustodian-java

-- Create table for storing integration mappings between Authorization Server and DataCustodian
CREATE TABLE datacustodian_integration_mapping (
    id BIGINT NOT NULL AUTO_INCREMENT,
    authorization_id VARCHAR(100) NOT NULL COMMENT 'OAuth2 authorization ID from Authorization Server',
    grant_id VARCHAR(100) NOT NULL COMMENT 'Grant ID from DataCustodian',
    customer_id VARCHAR(100) NOT NULL COMMENT 'Retail customer ID',
    usage_point_ids JSON DEFAULT NULL COMMENT 'JSON array of authorized usage point IDs',
    data_custodian_url VARCHAR(500) DEFAULT NULL COMMENT 'DataCustodian base URL for this mapping',
    integration_status ENUM('active', 'suspended', 'error') DEFAULT 'active' COMMENT 'Status of integration',
    last_sync_at TIMESTAMP NULL DEFAULT NULL COMMENT 'Last successful synchronization timestamp',
    sync_error_count INT DEFAULT 0 COMMENT 'Number of consecutive sync errors',
    last_sync_error TEXT DEFAULT NULL COMMENT 'Last synchronization error message',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_authorization_id (authorization_id),
    INDEX idx_datacustodian_mapping_grant (grant_id),
    INDEX idx_datacustodian_mapping_customer (customer_id),
    INDEX idx_datacustodian_mapping_status (integration_status),
    INDEX idx_datacustodian_mapping_sync_at (last_sync_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Mapping table for Authorization Server to DataCustodian integration';

-- Create table for DataCustodian health monitoring
CREATE TABLE datacustodian_health_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    data_custodian_url VARCHAR(500) NOT NULL COMMENT 'DataCustodian base URL',
    status VARCHAR(20) NOT NULL COMMENT 'Health status (UP, DOWN, DEGRADED)',
    version VARCHAR(50) DEFAULT NULL,
    uptime BIGINT DEFAULT NULL,
    database_connected BOOLEAN DEFAULT NULL,
    active_connections INT DEFAULT NULL,
    total_customers INT DEFAULT NULL,
    total_usage_points INT DEFAULT NULL,
    response_time_ms INT DEFAULT NULL COMMENT 'Health check response time in milliseconds',
    error_message TEXT DEFAULT NULL,
    checked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_datacustodian_health_url (data_custodian_url),
    INDEX idx_datacustodian_health_status (status),
    INDEX idx_datacustodian_health_checked_at (checked_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Health monitoring log for DataCustodian instances';

-- Create table for tracking DataCustodian API calls
CREATE TABLE datacustodian_api_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    integration_mapping_id BIGINT DEFAULT NULL COMMENT 'Reference to integration mapping',
    api_endpoint VARCHAR(200) NOT NULL COMMENT 'DataCustodian API endpoint called',
    http_method VARCHAR(10) NOT NULL,
    request_data JSON DEFAULT NULL COMMENT 'Request payload sent to DataCustodian',
    response_status INT DEFAULT NULL,
    response_data JSON DEFAULT NULL COMMENT 'Response data received from DataCustodian',
    response_time_ms INT DEFAULT NULL,
    error_message TEXT DEFAULT NULL,
    called_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_datacustodian_api_mapping (integration_mapping_id),
    INDEX idx_datacustodian_api_endpoint (api_endpoint),
    INDEX idx_datacustodian_api_status (response_status),
    INDEX idx_datacustodian_api_called_at (called_at),
    CONSTRAINT fk_api_log_mapping FOREIGN KEY (integration_mapping_id) 
        REFERENCES datacustodian_integration_mapping(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Log of API calls to DataCustodian for debugging and monitoring';

-- Add DataCustodian integration columns to oauth2_authorization table
ALTER TABLE oauth2_authorization
ADD COLUMN datacustodian_grant_id VARCHAR(100) DEFAULT NULL COMMENT 'DataCustodian grant ID for this authorization',
ADD COLUMN datacustodian_customer_id VARCHAR(100) DEFAULT NULL COMMENT 'DataCustodian customer ID',
ADD COLUMN datacustodian_sync_status VARCHAR(20) DEFAULT NULL COMMENT 'Synchronization status with DataCustodian',
ADD COLUMN datacustodian_last_sync TIMESTAMP NULL DEFAULT NULL COMMENT 'Last successful sync with DataCustodian';

-- Create indexes for new authorization columns
CREATE INDEX idx_oauth2_authorization_dc_grant ON oauth2_authorization (datacustodian_grant_id);
CREATE INDEX idx_oauth2_authorization_dc_customer ON oauth2_authorization (datacustodian_customer_id);
CREATE INDEX idx_oauth2_authorization_dc_sync_status ON oauth2_authorization (datacustodian_sync_status);

-- Create stored procedure to sync authorization with DataCustodian
DELIMITER //
CREATE PROCEDURE SyncAuthorizationWithDataCustodian(
    IN auth_id VARCHAR(100),
    IN customer_id VARCHAR(100),
    IN usage_point_ids JSON
)
BEGIN
    DECLARE mapping_exists INT DEFAULT 0;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;
    
    START TRANSACTION;
    
    -- Check if mapping already exists
    SELECT COUNT(*) INTO mapping_exists
    FROM datacustodian_integration_mapping 
    WHERE authorization_id = auth_id;
    
    IF mapping_exists > 0 THEN
        -- Update existing mapping
        UPDATE datacustodian_integration_mapping 
        SET customer_id = customer_id,
            usage_point_ids = usage_point_ids,
            updated_at = CURRENT_TIMESTAMP
        WHERE authorization_id = auth_id;
    ELSE
        -- Create new mapping (grant_id will be set when DataCustodian responds)
        INSERT INTO datacustodian_integration_mapping 
        (authorization_id, grant_id, customer_id, usage_point_ids)
        VALUES (auth_id, '', customer_id, usage_point_ids);
    END IF;
    
    COMMIT;
END //
DELIMITER ;

-- Create stored procedure to get DataCustodian integration status
DELIMITER //
CREATE PROCEDURE GetDataCustodianIntegrationStatus()
BEGIN
    SELECT 
        COUNT(*) as total_mappings,
        SUM(CASE WHEN integration_status = 'active' THEN 1 ELSE 0 END) as active_mappings,
        SUM(CASE WHEN integration_status = 'error' THEN 1 ELSE 0 END) as error_mappings,
        SUM(CASE WHEN sync_error_count > 0 THEN 1 ELSE 0 END) as last_sync_errors,
        AVG(TIMESTAMPDIFF(MINUTE, created_at, updated_at)) as avg_sync_time_minutes
    FROM datacustodian_integration_mapping;
END //
DELIMITER ;

-- Create stored procedure to cleanup old health logs
DELIMITER //
CREATE PROCEDURE CleanupDataCustodianHealthLogs(IN retention_days INT)
BEGIN
    DELETE FROM datacustodian_health_log 
    WHERE checked_at < DATE_SUB(CURDATE(), INTERVAL retention_days DAY);
    
    SELECT ROW_COUNT() as deleted_count;
END //
DELIMITER ;

-- Create stored procedure to cleanup old API logs
DELIMITER //
CREATE PROCEDURE CleanupDataCustodianApiLogs(IN retention_days INT)
BEGIN
    DELETE FROM datacustodian_api_log 
    WHERE called_at < DATE_SUB(CURDATE(), INTERVAL retention_days DAY);
    
    SELECT ROW_COUNT() as deleted_count;
END //
DELIMITER ;

-- Create view for integration monitoring
CREATE VIEW v_datacustodian_integration_summary AS
SELECT 
    dim.authorization_id,
    dim.customer_id,
    dim.integration_status,
    dim.last_sync_at,
    dim.sync_error_count,
    oa.principal_name,
    oa.authorization_grant_type,
    oa.authorized_scopes,
    orc.client_id,
    orc.client_name
FROM datacustodian_integration_mapping dim
LEFT JOIN oauth2_authorization oa ON dim.authorization_id = oa.id
LEFT JOIN oauth2_registered_client orc ON oa.registered_client_id = orc.id;

-- Create event to cleanup old logs (requires event scheduler to be enabled)
CREATE EVENT IF NOT EXISTS cleanup_datacustodian_logs
ON SCHEDULE EVERY 1 DAY
STARTS TIMESTAMP(CURRENT_DATE + INTERVAL 1 DAY, '02:00:00')
DO 
BEGIN
    CALL CleanupDataCustodianHealthLogs(30);
    CALL CleanupDataCustodianApiLogs(7);
END;

-- Add DataCustodian integration audit log entry
INSERT INTO oauth2_audit_log (event_type, client_id, principal_name, success, additional_data)
VALUES ('schema_upgrade', 'system', 'migration', TRUE, 
        JSON_OBJECT('version', 'V4_0_0', 'feature', 'datacustodian_integration', 
                   'description', 'Added DataCustodian integration support', 
                   'migration_timestamp', NOW()));

-- Create default DataCustodian configuration entry
INSERT INTO datacustodian_integration_mapping 
(authorization_id, grant_id, customer_id, usage_point_ids, integration_status)
VALUES 
('default-config', 'default', 'system', JSON_ARRAY(), 'active')
ON DUPLICATE KEY UPDATE 
    grant_id = VALUES(grant_id),
    integration_status = VALUES(integration_status);