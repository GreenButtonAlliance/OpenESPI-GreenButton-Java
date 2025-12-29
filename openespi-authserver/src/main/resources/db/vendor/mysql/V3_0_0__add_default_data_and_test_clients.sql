-- Default Data and Test Clients for ESPI 4.0 Authorization Server
-- MySQL Implementation

-- Insert additional default ESPI-compliant clients with proper JSON serialization
INSERT INTO oauth2_registered_client 
(id, client_id, client_name, client_authentication_methods, authorization_grant_types, 
 redirect_uris, post_logout_redirect_uris, scopes, client_settings, token_settings,
 espi_version, security_level, client_status, created_by)
VALUES 

-- Green Button Connect My Data Client
('green-button-connect-client',
 'green_button_connect_mydata',
 'Green Button Connect My Data',
 'client_secret_basic',
 'authorization_code,refresh_token',
 'https://gbconnect.example.com/oauth/callback,https://gbconnect.example.com/oauth/callback2',
 'https://gbconnect.example.com/logout',
 'openid,profile,FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13,FB=4_5_16;IntervalDuration=900;BlockDuration=daily;HistoryLength=7',
 '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":true,"settings.client.require-authorization-consent":true}',
 '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":false,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",21600.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"reference"},"settings.token.refresh-token-time-to-live":["java.time.Duration",216000.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000]}',
 '4.0', 'high', 'active', 'migration_v3_0_0'),

-- Utility Admin Client for DataCustodian operations
('utility-admin-client',
 'utility_admin_portal',
 'Utility Admin Portal',
 'client_secret_basic',
 'client_credentials',
 '',
 '',
 'DataCustodian_Admin_Access,Upload_Admin_Access',
 '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":false,"settings.client.require-authorization-consent":false}',
 '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":true,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",3600.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"reference"},"settings.token.refresh-token-time-to-live":["java.time.Duration",7200.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000]}',
 '4.0', 'maximum', 'active', 'migration_v3_0_0'),

-- Third Party Administrator Client
('third-party-admin-client',
 'third_party_admin_portal',
 'Third Party Administrator Portal',
 'client_secret_basic',
 'client_credentials,authorization_code,refresh_token',
 'https://admin.thirdparty.example.com/oauth/callback',
 'https://admin.thirdparty.example.com/logout',
 'ThirdParty_Admin_Access,openid,profile',
 '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":false,"settings.client.require-authorization-consent":true}',
 '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":true,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",21600.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"reference"},"settings.token.refresh-token-time-to-live":["java.time.Duration",216000.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000]}',
 '4.0', 'high', 'active', 'migration_v3_0_0'),

-- Mobile Customer Application
('mobile-customer-app',
 'mobile_espi_customer_app',
 'Mobile ESPI Customer App',
 'none',
 'authorization_code,refresh_token',
 'com.example.espi://oauth/callback,https://mobile.example.com/oauth/callback',
 'com.example.espi://logout,https://mobile.example.com/logout',
 'openid,profile,FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13',
 '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":true,"settings.client.require-authorization-consent":true}',
 '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":false,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",3600.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"reference"},"settings.token.refresh-token-time-to-live":["java.time.Duration",86400.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000]}',
 '4.0', 'high', 'active', 'migration_v3_0_0'),

-- Testing Client for Development/QA
('test-development-client',
 'test_development_client',
 'Test Development Client',
 'client_secret_basic',
 'authorization_code,client_credentials,refresh_token',
 'http://localhost:3000/oauth/callback,https://test.example.com/oauth/callback',
 'http://localhost:3000/logout,https://test.example.com/logout',
 'openid,profile,FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13,DataCustodian_Admin_Access',
 '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":false,"settings.client.require-authorization-consent":false}',
 '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":true,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",3600.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"reference"},"settings.token.refresh-token-time-to-live":["java.time.Duration",7200.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000]}',
 '4.0', 'standard', 'active', 'migration_v3_0_0');

-- Insert corresponding ESPI application information
INSERT INTO espi_application_info 
(client_id, client_name, client_description, client_uri, logo_uri, contact_name, contact_email, 
 tos_uri, policy_uri, software_id, software_version, scope, grant_types, response_types, 
 token_endpoint_auth_method, third_party_application_type, espi_version, security_classification,
 certification_status, business_category, service_territory)
VALUES 

-- Green Button Connect My Data
('green_button_connect_mydata',
 'Green Button Connect My Data',
 'Official Green Button Connect My Data application for secure energy data sharing',
 'https://gbconnect.greenbuttonalliance.org',
 'https://gbconnect.greenbuttonalliance.org/assets/logo.png',
 'GBA Support Team',
 'support@greenbuttonalliance.org',
 'https://gbconnect.greenbuttonalliance.org/terms',
 'https://gbconnect.greenbuttonalliance.org/privacy',
 'GB_CONNECT_2024',
 '2.1.0',
 'openid,profile,FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13,FB=4_5_16;IntervalDuration=900;BlockDuration=daily;HistoryLength=7',
 'authorization_code,refresh_token',
 'code',
 'client_secret_basic',
 'WEB',
 '4.0',
 'internal',
 'gba_certified',
 'Energy Data Platform',
 'North America'),

-- Utility Admin Portal
('utility_admin_portal',
 'Utility Admin Portal',
 'Administrative portal for utility DataCustodian operations and customer management',
 'https://admin.utility.example.com',
 'https://admin.utility.example.com/assets/utility-logo.png',
 'IT Operations Team',
 'itops@utility.example.com',
 'https://utility.example.com/terms',
 'https://utility.example.com/privacy',
 'UTILITY_ADMIN_2024',
 '1.5.2',
 'DataCustodian_Admin_Access,Upload_Admin_Access',
 'client_credentials',
 '',
 'client_secret_basic',
 'ADMIN',
 '4.0',
 'confidential',
 'self_certified',
 'Electric Utility',
 'State of California'),

-- Third Party Admin Portal
('third_party_admin_portal',
 'Third Party Administrator Portal',
 'Administrative interface for third-party energy service providers',
 'https://admin.thirdparty.example.com',
 'https://admin.thirdparty.example.com/assets/tp-logo.png',
 'Admin Support',
 'admin@thirdparty.example.com',
 'https://thirdparty.example.com/terms',
 'https://thirdparty.example.com/privacy',
 'TP_ADMIN_2024',
 '3.0.1',
 'ThirdParty_Admin_Access,openid,profile',
 'client_credentials,authorization_code,refresh_token',
 'code',
 'client_secret_basic',
 'WEB',
 '4.0',
 'internal',
 'third_party_certified',
 'Energy Service Provider',
 'Multi-State'),

-- Mobile Customer App
('mobile_espi_customer_app',
 'Mobile ESPI Customer App',
 'Native mobile application for customers to access their energy data',
 'https://mobile.example.com',
 'https://mobile.example.com/assets/mobile-logo.png',
 'Mobile Support Team',
 'mobile-support@example.com',
 'https://mobile.example.com/terms',
 'https://mobile.example.com/privacy',
 'MOBILE_ESPI_2024',
 '4.2.1',
 'openid,profile,FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13',
 'authorization_code,refresh_token',
 'code',
 'none',
 'NATIVE',
 '4.0',
 'internal',
 'self_certified',
 'Consumer Mobile App',
 'National'),

-- Test Development Client
('test_development_client',
 'Test Development Client',
 'Development and testing client for ESPI functionality validation',
 'https://test.example.com',
 'https://test.example.com/assets/test-logo.png',
 'QA Team',
 'qa@example.com',
 'https://test.example.com/terms',
 'https://test.example.com/privacy',
 'TEST_CLIENT_2024',
 '1.0.0',
 'openid,profile,FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13,DataCustodian_Admin_Access',
 'authorization_code,client_credentials,refresh_token',
 'code',
 'client_secret_basic',
 'WEB',
 '4.0',
 'internal',
 'self_certified',
 'Testing/Development',
 'Development Environment');

-- Update client secrets for the default clients (in production, these should be properly hashed)
UPDATE oauth2_registered_client SET client_secret = '{noop}gbconnect_secret_2024' WHERE client_id = 'green_button_connect_mydata';
UPDATE oauth2_registered_client SET client_secret = '{noop}utility_admin_secret_secure' WHERE client_id = 'utility_admin_portal';
UPDATE oauth2_registered_client SET client_secret = '{noop}tp_admin_secret_secure' WHERE client_id = 'third_party_admin_portal';
UPDATE oauth2_registered_client SET client_secret = NULL WHERE client_id = 'mobile_espi_customer_app'; -- Public client
UPDATE oauth2_registered_client SET client_secret = '{noop}test_secret_dev_only' WHERE client_id = 'test_development_client';

-- Set rate limits based on client type
UPDATE oauth2_registered_client SET rate_limit_per_minute = 1000 WHERE client_id IN ('utility_admin_portal', 'third_party_admin_portal');
UPDATE oauth2_registered_client SET rate_limit_per_minute = 200 WHERE client_id = 'green_button_connect_mydata';
UPDATE oauth2_registered_client SET rate_limit_per_minute = 50 WHERE client_id = 'mobile_espi_customer_app';
UPDATE oauth2_registered_client SET rate_limit_per_minute = 500 WHERE client_id = 'test_development_client';

-- Set max concurrent sessions
UPDATE oauth2_registered_client SET max_concurrent_sessions = 50 WHERE client_id IN ('utility_admin_portal', 'third_party_admin_portal');
UPDATE oauth2_registered_client SET max_concurrent_sessions = 10 WHERE client_id = 'green_button_connect_mydata';
UPDATE oauth2_registered_client SET max_concurrent_sessions = 3 WHERE client_id = 'mobile_espi_customer_app';
UPDATE oauth2_registered_client SET max_concurrent_sessions = 20 WHERE client_id = 'test_development_client';

-- Insert sample consent details for testing
INSERT INTO oauth2_consent_details 
(consent_id, registered_client_id, principal_name, consent_version, scopes_consented, 
 espi_data_categories, data_retention_period, consent_method, ip_address)
VALUES 
('consent_001_gb_connect',
 'green-button-connect-client',
 'customer@example.com',
 '1.0',
 'openid,profile,FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13',
 '["usage_data", "billing_data", "meter_readings"]',
 '2 years',
 'web',
 '192.168.1.100'),

('consent_002_mobile_app',
 'mobile-customer-app',
 'mobile.user@example.com',
 '1.0',
 'openid,profile,FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13',
 '["usage_data", "meter_readings"]',
 '1 year',
 'mobile',
 '10.0.2.15');

-- Insert sample audit log entries
INSERT INTO oauth2_audit_log 
(event_type, client_id, principal_name, success, scopes_requested, scopes_granted, 
 grant_type, ip_address, additional_data)
VALUES 
('client_registration', 'green_button_connect_mydata', 'system', TRUE, 
 'openid,profile,FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13',
 'openid,profile,FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13',
 'registration', '127.0.0.1',
 '{"client_type": "web", "registration_method": "manual", "created_by": "migration"}'),

('client_registration', 'utility_admin_portal', 'system', TRUE,
 'DataCustodian_Admin_Access,Upload_Admin_Access',
 'DataCustodian_Admin_Access,Upload_Admin_Access',
 'registration', '127.0.0.1',
 '{"client_type": "admin", "registration_method": "manual", "created_by": "migration"}'),

('client_registration', 'mobile_espi_customer_app', 'system', TRUE,
 'openid,profile,FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13',
 'openid,profile,FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13',
 'registration', '127.0.0.1',
 '{"client_type": "native", "registration_method": "manual", "created_by": "migration", "pkce_required": true}');

-- Insert initial client metrics (sample data for testing)
INSERT INTO oauth2_client_metrics 
(client_id, metric_date, total_requests, successful_requests, failed_requests, 
 total_tokens_issued, avg_response_time_ms, unique_users_served)
VALUES 
('green_button_connect_mydata', CURDATE() - INTERVAL 1 DAY, 150, 145, 5, 25, 85.5, 12),
('utility_admin_portal', CURDATE() - INTERVAL 1 DAY, 300, 295, 5, 50, 45.2, 8),
('mobile_espi_customer_app', CURDATE() - INTERVAL 1 DAY, 75, 70, 5, 15, 120.8, 15),
('third_party_admin_portal', CURDATE() - INTERVAL 1 DAY, 200, 190, 10, 35, 65.3, 6),
('test_development_client', CURDATE() - INTERVAL 1 DAY, 500, 450, 50, 100, 95.1, 20);

-- Create views for common reporting queries
CREATE VIEW v_active_clients AS
SELECT 
    c.client_id,
    c.client_name,
    c.client_status,
    c.security_level,
    c.espi_version,
    c.last_used_at,
    c.rate_limit_per_minute,
    c.max_concurrent_sessions,
    e.business_category,
    e.certification_status,
    e.service_territory
FROM oauth2_registered_client c
LEFT JOIN espi_application_info e ON c.client_id = e.client_id
WHERE c.client_status = 'active';

CREATE VIEW v_client_usage_summary AS
SELECT 
    m.client_id,
    c.client_name,
    AVG(m.total_requests) as avg_daily_requests,
    AVG(m.successful_requests) as avg_successful_requests,
    AVG(m.failed_requests) as avg_failed_requests,
    AVG(m.avg_response_time_ms) as avg_response_time,
    SUM(m.unique_users_served) as total_unique_users,
    MAX(m.metric_date) as last_metric_date
FROM oauth2_client_metrics m
JOIN oauth2_registered_client c ON m.client_id = c.client_id
WHERE m.metric_date >= CURDATE() - INTERVAL 30 DAY
GROUP BY m.client_id, c.client_name;

-- Add final audit entry for this migration
INSERT INTO oauth2_audit_log (event_type, client_id, principal_name, success, additional_data)
VALUES ('schema_migration', 'system', 'migration', TRUE, 
        JSON_OBJECT('version', 'V3_0_0', 'description', 'Added default clients and test data', 
                   'clients_added', 5, 'migration_timestamp', NOW()));