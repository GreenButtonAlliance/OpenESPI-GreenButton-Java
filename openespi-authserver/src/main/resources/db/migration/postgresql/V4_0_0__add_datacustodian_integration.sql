-- DataCustodian Integration Support for PostgreSQL
-- Adds tables and functions for integrating with OpenESPI-DataCustodian-java

-- Create table for storing integration mappings between Authorization Server and DataCustodian
CREATE TABLE datacustodian_integration_mapping (
    id BIGSERIAL PRIMARY KEY,
    authorization_id VARCHAR(100) NOT NULL UNIQUE,
    grant_id VARCHAR(100) NOT NULL,
    customer_id VARCHAR(100) NOT NULL,
    usage_point_ids JSONB DEFAULT '[]'::jsonb,
    data_custodian_url VARCHAR(500) DEFAULT NULL,
    integration_status VARCHAR(20) DEFAULT 'active',
    last_sync_at TIMESTAMP DEFAULT NULL,
    sync_error_count INTEGER DEFAULT 0,
    last_sync_error TEXT DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Add comments
COMMENT ON TABLE datacustodian_integration_mapping IS 'Mapping table for Authorization Server to DataCustodian integration';
COMMENT ON COLUMN datacustodian_integration_mapping.authorization_id IS 'OAuth2 authorization ID from Authorization Server';
COMMENT ON COLUMN datacustodian_integration_mapping.grant_id IS 'Grant ID from DataCustodian';
COMMENT ON COLUMN datacustodian_integration_mapping.customer_id IS 'Retail customer ID';
COMMENT ON COLUMN datacustodian_integration_mapping.usage_point_ids IS 'JSON array of authorized usage point IDs';
COMMENT ON COLUMN datacustodian_integration_mapping.data_custodian_url IS 'DataCustodian base URL for this mapping';
COMMENT ON COLUMN datacustodian_integration_mapping.integration_status IS 'Status of integration (active, suspended, error)';
COMMENT ON COLUMN datacustodian_integration_mapping.last_sync_at IS 'Last successful synchronization timestamp';
COMMENT ON COLUMN datacustodian_integration_mapping.sync_error_count IS 'Number of consecutive sync errors';
COMMENT ON COLUMN datacustodian_integration_mapping.last_sync_error IS 'Last synchronization error message';

-- Create indexes
CREATE INDEX idx_datacustodian_mapping_authorization ON datacustodian_integration_mapping (authorization_id);
CREATE INDEX idx_datacustodian_mapping_grant ON datacustodian_integration_mapping (grant_id);
CREATE INDEX idx_datacustodian_mapping_customer ON datacustodian_integration_mapping (customer_id);
CREATE INDEX idx_datacustodian_mapping_status ON datacustodian_integration_mapping (integration_status);
CREATE INDEX idx_datacustodian_mapping_sync_at ON datacustodian_integration_mapping (last_sync_at);

-- Create trigger to update updated_at timestamp
CREATE TRIGGER update_datacustodian_mapping_updated_at 
    BEFORE UPDATE ON datacustodian_integration_mapping 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Create table for DataCustodian health monitoring
CREATE TABLE datacustodian_health_log (
    id BIGSERIAL PRIMARY KEY,
    data_custodian_url VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    version VARCHAR(50) DEFAULT NULL,
    uptime BIGINT DEFAULT NULL,
    database_connected BOOLEAN DEFAULT NULL,
    active_connections INTEGER DEFAULT NULL,
    total_customers INTEGER DEFAULT NULL,
    total_usage_points INTEGER DEFAULT NULL,
    response_time_ms INTEGER DEFAULT NULL,
    error_message TEXT DEFAULT NULL,
    checked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Add comments for health log
COMMENT ON TABLE datacustodian_health_log IS 'Health monitoring log for DataCustodian instances';
COMMENT ON COLUMN datacustodian_health_log.data_custodian_url IS 'DataCustodian base URL';
COMMENT ON COLUMN datacustodian_health_log.status IS 'Health status (UP, DOWN, DEGRADED)';
COMMENT ON COLUMN datacustodian_health_log.response_time_ms IS 'Health check response time in milliseconds';

-- Create indexes for health log
CREATE INDEX idx_datacustodian_health_url ON datacustodian_health_log (data_custodian_url);
CREATE INDEX idx_datacustodian_health_status ON datacustodian_health_log (status);
CREATE INDEX idx_datacustodian_health_checked_at ON datacustodian_health_log (checked_at);

-- Create table for tracking DataCustodian API calls
CREATE TABLE datacustodian_api_log (
    id BIGSERIAL PRIMARY KEY,
    integration_mapping_id BIGINT REFERENCES datacustodian_integration_mapping(id) ON DELETE CASCADE,
    api_endpoint VARCHAR(200) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    request_data JSONB DEFAULT NULL,
    response_status INTEGER DEFAULT NULL,
    response_data JSONB DEFAULT NULL,
    response_time_ms INTEGER DEFAULT NULL,
    error_message TEXT DEFAULT NULL,
    called_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Add comments for API log
COMMENT ON TABLE datacustodian_api_log IS 'Log of API calls to DataCustodian for debugging and monitoring';
COMMENT ON COLUMN datacustodian_api_log.integration_mapping_id IS 'Reference to integration mapping';
COMMENT ON COLUMN datacustodian_api_log.api_endpoint IS 'DataCustodian API endpoint called';
COMMENT ON COLUMN datacustodian_api_log.request_data IS 'Request payload sent to DataCustodian';
COMMENT ON COLUMN datacustodian_api_log.response_data IS 'Response data received from DataCustodian';

-- Create indexes for API log
CREATE INDEX idx_datacustodian_api_mapping ON datacustodian_api_log (integration_mapping_id);
CREATE INDEX idx_datacustodian_api_endpoint ON datacustodian_api_log (api_endpoint);
CREATE INDEX idx_datacustodian_api_status ON datacustodian_api_log (response_status);
CREATE INDEX idx_datacustodian_api_called_at ON datacustodian_api_log (called_at);

-- Add DataCustodian integration columns to oauth2_authorization table
ALTER TABLE oauth2_authorization
ADD COLUMN datacustodian_grant_id VARCHAR(100) DEFAULT NULL,
ADD COLUMN datacustodian_customer_id VARCHAR(100) DEFAULT NULL,
ADD COLUMN datacustodian_sync_status VARCHAR(20) DEFAULT NULL,
ADD COLUMN datacustodian_last_sync TIMESTAMP DEFAULT NULL;

-- Add comments for new authorization columns
COMMENT ON COLUMN oauth2_authorization.datacustodian_grant_id IS 'DataCustodian grant ID for this authorization';
COMMENT ON COLUMN oauth2_authorization.datacustodian_customer_id IS 'DataCustodian customer ID';
COMMENT ON COLUMN oauth2_authorization.datacustodian_sync_status IS 'Synchronization status with DataCustodian';
COMMENT ON COLUMN oauth2_authorization.datacustodian_last_sync IS 'Last successful sync with DataCustodian';

-- Create indexes for new authorization columns
CREATE INDEX idx_oauth2_authorization_dc_grant ON oauth2_authorization (datacustodian_grant_id);
CREATE INDEX idx_oauth2_authorization_dc_customer ON oauth2_authorization (datacustodian_customer_id);
CREATE INDEX idx_oauth2_authorization_dc_sync_status ON oauth2_authorization (datacustodian_sync_status);

-- Create function to sync authorization with DataCustodian
CREATE OR REPLACE FUNCTION sync_authorization_with_datacustodian(
    auth_id VARCHAR(100),
    customer_id VARCHAR(100),
    usage_point_ids TEXT[]
) RETURNS BOOLEAN AS $$
DECLARE
    mapping_exists BOOLEAN := FALSE;
    usage_point_json JSONB;
BEGIN
    -- Convert usage point IDs array to JSON
    SELECT to_jsonb(usage_point_ids) INTO usage_point_json;
    
    -- Check if mapping already exists
    SELECT EXISTS(
        SELECT 1 FROM datacustodian_integration_mapping 
        WHERE authorization_id = auth_id
    ) INTO mapping_exists;
    
    IF mapping_exists THEN
        -- Update existing mapping
        UPDATE datacustodian_integration_mapping 
        SET customer_id = sync_authorization_with_datacustodian.customer_id,
            usage_point_ids = usage_point_json,
            updated_at = CURRENT_TIMESTAMP
        WHERE authorization_id = auth_id;
    ELSE
        -- Create new mapping (grant_id will be set when DataCustodian responds)
        INSERT INTO datacustodian_integration_mapping 
        (authorization_id, grant_id, customer_id, usage_point_ids)
        VALUES (auth_id, '', sync_authorization_with_datacustodian.customer_id, usage_point_json);
    END IF;
    
    RETURN TRUE;
EXCEPTION
    WHEN OTHERS THEN
        -- Log error and return false
        RAISE NOTICE 'Error syncing authorization %: %', auth_id, SQLERRM;
        RETURN FALSE;
END;
$$ LANGUAGE plpgsql;

-- Create function to get DataCustodian integration status
CREATE OR REPLACE FUNCTION get_datacustodian_integration_status()
RETURNS TABLE (
    total_mappings BIGINT,
    active_mappings BIGINT,
    error_mappings BIGINT,
    last_sync_errors BIGINT,
    avg_sync_time_minutes NUMERIC
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(*) as total_mappings,
        COUNT(*) FILTER (WHERE integration_status = 'active') as active_mappings,
        COUNT(*) FILTER (WHERE integration_status = 'error') as error_mappings,
        COUNT(*) FILTER (WHERE sync_error_count > 0) as last_sync_errors,
        AVG(EXTRACT(EPOCH FROM (updated_at - created_at)) / 60) as avg_sync_time_minutes
    FROM datacustodian_integration_mapping;
END;
$$ LANGUAGE plpgsql;

-- Create function to cleanup old health logs
CREATE OR REPLACE FUNCTION cleanup_datacustodian_health_logs(retention_days INTEGER DEFAULT 30)
RETURNS BIGINT AS $$
DECLARE
    deleted_count BIGINT;
BEGIN
    DELETE FROM datacustodian_health_log 
    WHERE checked_at < CURRENT_DATE - INTERVAL '1 day' * retention_days;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Create function to cleanup old API logs
CREATE OR REPLACE FUNCTION cleanup_datacustodian_api_logs(retention_days INTEGER DEFAULT 7)
RETURNS BIGINT AS $$
DECLARE
    deleted_count BIGINT;
BEGIN
    DELETE FROM datacustodian_api_log 
    WHERE called_at < CURRENT_DATE - INTERVAL '1 day' * retention_days;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

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

-- Add DataCustodian integration audit log entry
INSERT INTO oauth2_audit_log (event_type, client_id, principal_name, success, additional_data)
VALUES ('schema_upgrade', 'system', 'migration', TRUE, 
        jsonb_build_object('version', 'V4_0_0', 'feature', 'datacustodian_integration', 
                          'description', 'Added DataCustodian integration support', 
                          'migration_timestamp', NOW()));

-- Create default DataCustodian configuration entry
INSERT INTO datacustodian_integration_mapping 
(authorization_id, grant_id, customer_id, usage_point_ids, integration_status)
VALUES 
('default-config', 'default', 'system', '[]'::jsonb, 'active')
ON CONFLICT (authorization_id) DO NOTHING;