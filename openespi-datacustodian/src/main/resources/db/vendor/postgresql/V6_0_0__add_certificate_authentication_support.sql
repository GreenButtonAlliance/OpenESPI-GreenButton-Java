-- PostgreSQL: Add certificate authentication support for ESPI 4.0
-- Migration Version: V6.0.0
-- Description: Add tables for client certificate management and authentication
-- Author: Green Button Alliance
-- NAESB ESPI 4.0 Compliance: Certificate-based authentication support

-- Create certificate status enum
CREATE TYPE certificate_status_enum AS ENUM ('active', 'revoked', 'expired');
CREATE TYPE trust_level_enum AS ENUM ('low', 'medium', 'high', 'critical');
CREATE TYPE validation_level_enum AS ENUM ('basic', 'standard', 'strict');

-- Client certificates table
CREATE TABLE oauth2_client_certificates (
    id BIGSERIAL PRIMARY KEY,
    client_id VARCHAR(255) NOT NULL,
    certificate_serial VARCHAR(255) NOT NULL,
    certificate_subject_dn TEXT NOT NULL,
    certificate_issuer_dn TEXT NOT NULL,
    certificate_not_before TIMESTAMP NOT NULL,
    certificate_not_after TIMESTAMP NOT NULL,
    certificate_data BYTEA NOT NULL,
    certificate_fingerprint VARCHAR(512) NOT NULL,
    uploaded_by VARCHAR(100) NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status certificate_status_enum DEFAULT 'active',
    revoked_at TIMESTAMP NULL,
    revoked_by VARCHAR(100) NULL,
    revocation_reason VARCHAR(200) NULL,
    last_validated_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT uk_client_certificates_fingerprint UNIQUE (certificate_fingerprint),
    CONSTRAINT fk_client_certificates_client FOREIGN KEY (client_id) 
        REFERENCES oauth2_registered_client(client_id) ON DELETE CASCADE
);

-- Indexes for client certificates
CREATE INDEX idx_client_certificates_client_id ON oauth2_client_certificates(client_id);
CREATE INDEX idx_client_certificates_fingerprint ON oauth2_client_certificates(certificate_fingerprint);
CREATE INDEX idx_client_certificates_serial ON oauth2_client_certificates(certificate_serial);
CREATE INDEX idx_client_certificates_status ON oauth2_client_certificates(status);
CREATE INDEX idx_client_certificates_expiry ON oauth2_client_certificates(certificate_not_after);

-- Update trigger for oauth2_client_certificates
CREATE OR REPLACE FUNCTION update_oauth2_client_certificates_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_oauth2_client_certificates_updated_at
    BEFORE UPDATE ON oauth2_client_certificates
    FOR EACH ROW
    EXECUTE FUNCTION update_oauth2_client_certificates_updated_at();

-- Certificate validation log table
CREATE TABLE oauth2_certificate_validation_log (
    id BIGSERIAL PRIMARY KEY,
    certificate_subject_dn TEXT NOT NULL,
    certificate_serial VARCHAR(255) NOT NULL,
    client_id VARCHAR(255) NULL,
    validation_success BOOLEAN NOT NULL DEFAULT FALSE,
    error_message TEXT NULL,
    ip_address INET NULL,
    user_agent TEXT NULL,
    validated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for certificate validation log
CREATE INDEX idx_cert_validation_client_id ON oauth2_certificate_validation_log(client_id);
CREATE INDEX idx_cert_validation_success ON oauth2_certificate_validation_log(validation_success);
CREATE INDEX idx_cert_validation_date ON oauth2_certificate_validation_log(validated_at);
CREATE INDEX idx_cert_validation_serial ON oauth2_certificate_validation_log(certificate_serial);

-- Certificate revocation list (CRL) cache table
CREATE TABLE oauth2_certificate_revocation_cache (
    id BIGSERIAL PRIMARY KEY,
    issuer_dn TEXT NOT NULL,
    certificate_serial VARCHAR(255) NOT NULL,
    revocation_date TIMESTAMP NOT NULL,
    revocation_reason VARCHAR(100) NULL,
    crl_url VARCHAR(500) NULL,
    cached_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    
    -- Constraints
    CONSTRAINT uk_cert_revocation_issuer_serial UNIQUE (issuer_dn, certificate_serial)
);

-- Indexes for certificate revocation cache
CREATE INDEX idx_cert_revocation_serial ON oauth2_certificate_revocation_cache(certificate_serial);
CREATE INDEX idx_cert_revocation_issuer ON oauth2_certificate_revocation_cache USING HASH(issuer_dn);
CREATE INDEX idx_cert_revocation_expires ON oauth2_certificate_revocation_cache(expires_at);

-- Certificate authority trust store table
CREATE TABLE oauth2_trusted_certificate_authorities (
    id BIGSERIAL PRIMARY KEY,
    ca_name VARCHAR(200) NOT NULL,
    ca_subject_dn TEXT NOT NULL,
    ca_certificate_data BYTEA NOT NULL,
    ca_certificate_fingerprint VARCHAR(512) NOT NULL,
    ca_key_usage VARCHAR(500) NULL,
    ca_extended_key_usage VARCHAR(500) NULL,
    is_enabled BOOLEAN DEFAULT TRUE,
    is_root_ca BOOLEAN DEFAULT FALSE,
    trust_level trust_level_enum DEFAULT 'medium',
    added_by VARCHAR(100) NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,
    
    -- Constraints
    CONSTRAINT uk_trusted_ca_fingerprint UNIQUE (ca_certificate_fingerprint)
);

-- Indexes for trusted certificate authorities
CREATE INDEX idx_trusted_ca_name ON oauth2_trusted_certificate_authorities(ca_name);
CREATE INDEX idx_trusted_ca_fingerprint ON oauth2_trusted_certificate_authorities(ca_certificate_fingerprint);
CREATE INDEX idx_trusted_ca_enabled ON oauth2_trusted_certificate_authorities(is_enabled);
CREATE INDEX idx_trusted_ca_validity ON oauth2_trusted_certificate_authorities(valid_from, valid_until);

-- Certificate authentication statistics table
CREATE TABLE oauth2_certificate_auth_stats (
    id BIGSERIAL PRIMARY KEY,
    client_id VARCHAR(255) NOT NULL,
    stat_date DATE NOT NULL,
    successful_authentications INTEGER DEFAULT 0,
    failed_authentications INTEGER DEFAULT 0,
    certificate_validations INTEGER DEFAULT 0,
    certificate_validation_failures INTEGER DEFAULT 0,
    last_authentication_at TIMESTAMP NULL,
    last_validation_error VARCHAR(500) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT uk_cert_auth_stats_client_date UNIQUE (client_id, stat_date),
    CONSTRAINT fk_cert_auth_stats_client FOREIGN KEY (client_id) 
        REFERENCES oauth2_registered_client(client_id) ON DELETE CASCADE
);

-- Indexes for certificate auth stats
CREATE INDEX idx_cert_auth_stats_client_date ON oauth2_certificate_auth_stats(client_id, stat_date);
CREATE INDEX idx_cert_auth_stats_date ON oauth2_certificate_auth_stats(stat_date);

-- Update trigger for oauth2_certificate_auth_stats
CREATE OR REPLACE FUNCTION update_oauth2_certificate_auth_stats_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_oauth2_certificate_auth_stats_updated_at
    BEFORE UPDATE ON oauth2_certificate_auth_stats
    FOR EACH ROW
    EXECUTE FUNCTION update_oauth2_certificate_auth_stats_updated_at();

-- Add certificate authentication method to existing client table
ALTER TABLE oauth2_registered_client 
ADD COLUMN certificate_authentication_enabled BOOLEAN DEFAULT FALSE,
ADD COLUMN require_client_certificate BOOLEAN DEFAULT FALSE,
ADD COLUMN certificate_subject_dn_pattern VARCHAR(500) NULL,
ADD COLUMN certificate_validation_level validation_level_enum DEFAULT 'standard';

-- Add indexes for new columns
CREATE INDEX idx_oauth2_client_cert_auth ON oauth2_registered_client(certificate_authentication_enabled);
CREATE INDEX idx_oauth2_client_cert_required ON oauth2_registered_client(require_client_certificate);

-- Insert default trusted certificate authorities (example data)
INSERT INTO oauth2_trusted_certificate_authorities 
(ca_name, ca_subject_dn, ca_certificate_data, ca_certificate_fingerprint, 
 is_enabled, is_root_ca, trust_level, added_by, valid_from, valid_until) 
VALUES 
('ESPI Test Root CA', 'CN=ESPI Test Root CA, O=Green Button Alliance, C=US', 
 E'\\x', 'test_fingerprint_1', TRUE, TRUE, 'high', 'system', 
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '10 years'),
('ESPI Intermediate CA', 'CN=ESPI Intermediate CA, O=Green Button Alliance, C=US', 
 E'\\x', 'test_fingerprint_2', TRUE, FALSE, 'high', 'system', 
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '5 years');

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
        WHEN cc.certificate_not_after < CURRENT_TIMESTAMP + INTERVAL '30 days' THEN 'expiring_soon'
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
  AND validated_at >= CURRENT_TIMESTAMP - INTERVAL '30 days'
GROUP BY client_id;

-- Add table comments
COMMENT ON TABLE oauth2_client_certificates IS 'Stores X.509 client certificates for certificate-based authentication';
COMMENT ON TABLE oauth2_certificate_validation_log IS 'Logs all certificate validation attempts for auditing and monitoring';
COMMENT ON TABLE oauth2_certificate_revocation_cache IS 'Caches certificate revocation status from CRL/OCSP for performance';
COMMENT ON TABLE oauth2_trusted_certificate_authorities IS 'Stores trusted certificate authorities for certificate chain validation';
COMMENT ON TABLE oauth2_certificate_auth_stats IS 'Aggregated statistics for certificate authentication performance monitoring';

-- Add column comments for oauth2_client_certificates
COMMENT ON COLUMN oauth2_client_certificates.certificate_data IS 'DER-encoded X.509 certificate data';
COMMENT ON COLUMN oauth2_client_certificates.certificate_fingerprint IS 'SHA-256 fingerprint of the certificate';
COMMENT ON COLUMN oauth2_client_certificates.status IS 'Current status of the certificate (active, revoked, expired)';

-- Add column comments for oauth2_registered_client
COMMENT ON COLUMN oauth2_registered_client.certificate_authentication_enabled IS 'Whether certificate authentication is enabled for this client';
COMMENT ON COLUMN oauth2_registered_client.require_client_certificate IS 'Whether client certificate is required for authentication';
COMMENT ON COLUMN oauth2_registered_client.certificate_subject_dn_pattern IS 'Regex pattern for validating certificate subject DN';
COMMENT ON COLUMN oauth2_registered_client.certificate_validation_level IS 'Level of certificate validation (basic, standard, strict)';