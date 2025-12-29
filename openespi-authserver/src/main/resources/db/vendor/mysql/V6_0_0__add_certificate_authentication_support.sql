-- MySQL: Add certificate authentication support for ESPI 4.0
-- Migration Version: V6.0.0
-- Description: Add tables for client certificate management and authentication
-- Author: Green Button Alliance
-- NAESB ESPI 4.0 Compliance: Certificate-based authentication support

-- Client certificates table
CREATE TABLE oauth2_client_certificates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id VARCHAR(255) NOT NULL,
    certificate_serial VARCHAR(255) NOT NULL,
    certificate_subject_dn TEXT NOT NULL,
    certificate_issuer_dn TEXT NOT NULL,
    certificate_not_before TIMESTAMP NOT NULL,
    certificate_not_after TIMESTAMP NOT NULL,
    certificate_data LONGBLOB NOT NULL,
    certificate_fingerprint VARCHAR(512) NOT NULL,
    uploaded_by VARCHAR(100) NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('active', 'revoked', 'expired') DEFAULT 'active',
    revoked_at TIMESTAMP NULL,
    revoked_by VARCHAR(100) NULL,
    revocation_reason VARCHAR(200) NULL,
    last_validated_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Indexes
    INDEX idx_client_certificates_client_id (client_id),
    INDEX idx_client_certificates_fingerprint (certificate_fingerprint),
    INDEX idx_client_certificates_serial (certificate_serial),
    INDEX idx_client_certificates_status (status),
    INDEX idx_client_certificates_expiry (certificate_not_after),
    
    -- Constraints
    UNIQUE KEY uk_client_certificates_fingerprint (certificate_fingerprint),
    CONSTRAINT fk_client_certificates_client FOREIGN KEY (client_id) 
        REFERENCES oauth2_registered_client(client_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Certificate validation log table
CREATE TABLE oauth2_certificate_validation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    certificate_subject_dn TEXT NOT NULL,
    certificate_serial VARCHAR(255) NOT NULL,
    client_id VARCHAR(255) NULL,
    validation_success BOOLEAN NOT NULL DEFAULT FALSE,
    error_message TEXT NULL,
    ip_address VARCHAR(45) NULL,
    user_agent TEXT NULL,
    validated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Indexes
    INDEX idx_cert_validation_client_id (client_id),
    INDEX idx_cert_validation_success (validation_success),
    INDEX idx_cert_validation_date (validated_at),
    INDEX idx_cert_validation_serial (certificate_serial)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Certificate revocation list (CRL) cache table
CREATE TABLE oauth2_certificate_revocation_cache (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    issuer_dn TEXT NOT NULL,
    certificate_serial VARCHAR(255) NOT NULL,
    revocation_date TIMESTAMP NOT NULL,
    revocation_reason VARCHAR(100) NULL,
    crl_url VARCHAR(500) NULL,
    cached_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    
    -- Indexes
    INDEX idx_cert_revocation_serial (certificate_serial),
    INDEX idx_cert_revocation_issuer (issuer_dn(255)),
    INDEX idx_cert_revocation_expires (expires_at),
    
    -- Constraints
    UNIQUE KEY uk_cert_revocation_issuer_serial (issuer_dn(255), certificate_serial)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Certificate authority trust store table
CREATE TABLE oauth2_trusted_certificate_authorities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ca_name VARCHAR(200) NOT NULL,
    ca_subject_dn TEXT NOT NULL,
    ca_certificate_data LONGBLOB NOT NULL,
    ca_certificate_fingerprint VARCHAR(512) NOT NULL,
    ca_key_usage VARCHAR(500) NULL,
    ca_extended_key_usage VARCHAR(500) NULL,
    is_enabled BOOLEAN DEFAULT TRUE,
    is_root_ca BOOLEAN DEFAULT FALSE,
    trust_level ENUM('low', 'medium', 'high', 'critical') DEFAULT 'medium',
    added_by VARCHAR(100) NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,
    
    -- Indexes
    INDEX idx_trusted_ca_name (ca_name),
    INDEX idx_trusted_ca_fingerprint (ca_certificate_fingerprint),
    INDEX idx_trusted_ca_enabled (is_enabled),
    INDEX idx_trusted_ca_validity (valid_from, valid_until),
    
    -- Constraints
    UNIQUE KEY uk_trusted_ca_fingerprint (ca_certificate_fingerprint)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Certificate authentication statistics table
CREATE TABLE oauth2_certificate_auth_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id VARCHAR(255) NOT NULL,
    stat_date DATE NOT NULL,
    successful_authentications INT DEFAULT 0,
    failed_authentications INT DEFAULT 0,
    certificate_validations INT DEFAULT 0,
    certificate_validation_failures INT DEFAULT 0,
    last_authentication_at TIMESTAMP NULL,
    last_validation_error VARCHAR(500) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Indexes
    INDEX idx_cert_auth_stats_client_date (client_id, stat_date),
    INDEX idx_cert_auth_stats_date (stat_date),
    
    -- Constraints
    UNIQUE KEY uk_cert_auth_stats_client_date (client_id, stat_date),
    CONSTRAINT fk_cert_auth_stats_client FOREIGN KEY (client_id) 
        REFERENCES oauth2_registered_client(client_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add certificate authentication method to existing client table
ALTER TABLE oauth2_registered_client 
ADD COLUMN certificate_authentication_enabled BOOLEAN DEFAULT FALSE,
ADD COLUMN require_client_certificate BOOLEAN DEFAULT FALSE,
ADD COLUMN certificate_subject_dn_pattern VARCHAR(500) NULL,
ADD COLUMN certificate_validation_level ENUM('basic', 'standard', 'strict') DEFAULT 'standard';

-- Add indexes for new columns
CREATE INDEX idx_oauth2_client_cert_auth ON oauth2_registered_client(certificate_authentication_enabled);
CREATE INDEX idx_oauth2_client_cert_required ON oauth2_registered_client(require_client_certificate);

-- Insert default trusted certificate authorities (example data)
INSERT INTO oauth2_trusted_certificate_authorities 
(ca_name, ca_subject_dn, ca_certificate_data, ca_certificate_fingerprint, 
 is_enabled, is_root_ca, trust_level, added_by, valid_from, valid_until) 
VALUES 
('ESPI Test Root CA', 'CN=ESPI Test Root CA, O=Green Button Alliance, C=US', 
 '', 'test_fingerprint_1', TRUE, TRUE, 'high', 'system', 
 CURRENT_TIMESTAMP, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 10 YEAR)),
('ESPI Intermediate CA', 'CN=ESPI Intermediate CA, O=Green Button Alliance, C=US', 
 '', 'test_fingerprint_2', TRUE, FALSE, 'high', 'system', 
 CURRENT_TIMESTAMP, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 5 YEAR));

-- Create view for active client certificates
CREATE OR REPLACE VIEW v_active_client_certificates AS
SELECT 
    cc.client_id,
    cc.certificate_serial,
    cc.certificate_subject_dn,
    cc.certificate_issuer_dn,
    cc.certificate_not_before,
    cc.certificate_not_after,
    cc.certificate_fingerprint,
    cc.uploaded_by,
    cc.uploaded_at,
    cc.last_validated_at,
    rc.client_name,
    rc.client_status,
    rc.certificate_authentication_enabled,
    CASE 
        WHEN cc.certificate_not_after < CURRENT_TIMESTAMP THEN 'expired'
        WHEN cc.certificate_not_after < DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY) THEN 'expiring_soon'
        ELSE 'valid'
    END as certificate_status
FROM oauth2_client_certificates cc
JOIN oauth2_registered_client rc ON cc.client_id = rc.client_id
WHERE cc.status = 'active'
  AND rc.client_status = 'active';

-- Create view for certificate validation statistics
CREATE OR REPLACE VIEW v_certificate_validation_stats AS
SELECT 
    client_id,
    COUNT(*) as total_validations,
    SUM(CASE WHEN validation_success = TRUE THEN 1 ELSE 0 END) as successful_validations,
    SUM(CASE WHEN validation_success = FALSE THEN 1 ELSE 0 END) as failed_validations,
    ROUND(
        (SUM(CASE WHEN validation_success = TRUE THEN 1 ELSE 0 END) * 100.0) / COUNT(*), 
        2
    ) as success_rate,
    MAX(validated_at) as last_validation_attempt
FROM oauth2_certificate_validation_log
WHERE client_id IS NOT NULL
  AND validated_at >= DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
GROUP BY client_id;

-- Add comments to tables
ALTER TABLE oauth2_client_certificates 
COMMENT = 'Stores X.509 client certificates for certificate-based authentication';

ALTER TABLE oauth2_certificate_validation_log 
COMMENT = 'Logs all certificate validation attempts for auditing and monitoring';

ALTER TABLE oauth2_certificate_revocation_cache 
COMMENT = 'Caches certificate revocation status from CRL/OCSP for performance';

ALTER TABLE oauth2_trusted_certificate_authorities 
COMMENT = 'Stores trusted certificate authorities for certificate chain validation';

ALTER TABLE oauth2_certificate_auth_stats 
COMMENT = 'Aggregated statistics for certificate authentication performance monitoring';