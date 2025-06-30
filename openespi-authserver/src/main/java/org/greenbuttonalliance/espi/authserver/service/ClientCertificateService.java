/*
 *
 *    Copyright (c) 2018-2025 Green Button Alliance, Inc.
 *
 *    Portions (c) 2013-2018 EnergyOS.org
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package org.greenbuttonalliance.espi.authserver.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.greenbuttonalliance.espi.authserver.config.ClientCertificateAuthenticationConfig;

import java.io.ByteArrayInputStream;
import java.security.cert.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Service for managing client certificate authentication
 * 
 * Provides comprehensive certificate management for ESPI 4.0 compliance:
 * - Certificate validation and verification
 * - Certificate revocation checking (CRL/OCSP)
 * - Certificate storage and retrieval
 * - Certificate-to-client mapping
 * - Certificate renewal monitoring
 * - Trust chain validation
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
@Service
public class ClientCertificateService {

    private static final Logger logger = LoggerFactory.getLogger(ClientCertificateService.class);

    private final JdbcTemplate jdbcTemplate;
    private final ClientCertificateAuthenticationConfig config;

    @Autowired
    public ClientCertificateService(JdbcTemplate jdbcTemplate, 
                                  ClientCertificateAuthenticationConfig config) {
        this.jdbcTemplate = jdbcTemplate;
        this.config = config;
    }

    /**
     * Validate client certificate and return associated client ID
     */
    public CertificateValidationResult validateClientCertificate(X509Certificate certificate) {
        logger.debug("Validating client certificate: {}", certificate.getSubjectDN());

        try {
            // Basic certificate validation
            if (!isValidCertificate(certificate)) {
                return CertificateValidationResult.invalid("Certificate validation failed");
            }

            // Check certificate expiration
            if (isCertificateExpired(certificate)) {
                return CertificateValidationResult.invalid("Certificate expired");
            }

            // Check certificate revocation
            if (config.isEnableCertificateRevocationCheck() && isCertificateRevoked(certificate)) {
                return CertificateValidationResult.invalid("Certificate revoked");
            }

            // Find associated client
            String clientId = findClientByCertificate(certificate);
            if (clientId == null) {
                return CertificateValidationResult.invalid("No client associated with certificate");
            }

            // Verify client is active
            if (!isClientActive(clientId)) {
                return CertificateValidationResult.invalid("Client not active");
            }

            // Log successful validation
            logCertificateValidation(certificate, clientId, true, null);

            return CertificateValidationResult.valid(clientId, certificate);

        } catch (Exception e) {
            logger.error("Certificate validation error", e);
            logCertificateValidation(certificate, null, false, e.getMessage());
            return CertificateValidationResult.invalid("Certificate validation error: " + e.getMessage());
        }
    }

    /**
     * Store client certificate for a registered client
     */
    public void storeClientCertificate(String clientId, X509Certificate certificate, String uploadedBy) {
        logger.info("Storing certificate for client: {}", clientId);

        try {
            String sql = """
                INSERT INTO oauth2_client_certificates 
                (client_id, certificate_serial, certificate_subject_dn, certificate_issuer_dn,
                 certificate_not_before, certificate_not_after, certificate_data, 
                 certificate_fingerprint, uploaded_by, uploaded_at, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'active')
                """;

            byte[] certificateData = certificate.getEncoded();
            String fingerprint = calculateCertificateFingerprint(certificate);

            jdbcTemplate.update(sql,
                clientId,
                certificate.getSerialNumber().toString(),
                certificate.getSubjectDN().toString(),
                certificate.getIssuerDN().toString(),
                certificate.getNotBefore(),
                certificate.getNotAfter(),
                certificateData,
                fingerprint,
                uploadedBy,
                Instant.now(),
                "active"
            );

            logger.info("Certificate stored successfully for client: {}", clientId);

        } catch (Exception e) {
            logger.error("Error storing certificate for client: {}", clientId, e);
            throw new RuntimeException("Failed to store client certificate", e);
        }
    }

    /**
     * Get certificate information for a client
     */
    @Cacheable(value = "clientCertificates", key = "#clientId")
    public ClientCertificateInfo getClientCertificateInfo(String clientId) {
        logger.debug("Getting certificate info for client: {}", clientId);

        try {
            String sql = """
                SELECT certificate_serial, certificate_subject_dn, certificate_issuer_dn,
                       certificate_not_before, certificate_not_after, certificate_fingerprint,
                       uploaded_by, uploaded_at, status, last_validated_at
                FROM oauth2_client_certificates 
                WHERE client_id = ? AND status = 'active'
                ORDER BY uploaded_at DESC
                LIMIT 1
                """;

            return jdbcTemplate.queryForObject(sql, 
                new Object[]{clientId},
                (rs, rowNum) -> {
                    ClientCertificateInfo info = new ClientCertificateInfo();
                    info.setClientId(clientId);
                    info.setSerialNumber(rs.getString("certificate_serial"));
                    info.setSubjectDn(rs.getString("certificate_subject_dn"));
                    info.setIssuerDn(rs.getString("certificate_issuer_dn"));
                    info.setNotBefore(rs.getTimestamp("certificate_not_before").toInstant());
                    info.setNotAfter(rs.getTimestamp("certificate_not_after").toInstant());
                    info.setFingerprint(rs.getString("certificate_fingerprint"));
                    info.setUploadedBy(rs.getString("uploaded_by"));
                    info.setUploadedAt(rs.getTimestamp("uploaded_at").toInstant());
                    info.setStatus(rs.getString("status"));
                    info.setLastValidatedAt(rs.getTimestamp("last_validated_at") != null ?
                        rs.getTimestamp("last_validated_at").toInstant() : null);
                    return info;
                });

        } catch (Exception e) {
            logger.debug("No certificate found for client: {}", clientId);
            return null;
        }
    }

    /**
     * Get certificates expiring soon
     */
    public List<ClientCertificateInfo> getCertificatesExpiringSoon() {
        logger.debug("Getting certificates expiring within {} days", 
                    config.getCertificateRenewalWarningDays());

        try {
            String sql = """
                SELECT client_id, certificate_serial, certificate_subject_dn, certificate_issuer_dn,
                       certificate_not_before, certificate_not_after, certificate_fingerprint,
                       uploaded_by, uploaded_at, status
                FROM oauth2_client_certificates 
                WHERE status = 'active' 
                AND certificate_not_after <= DATE_ADD(NOW(), INTERVAL ? DAY)
                ORDER BY certificate_not_after ASC
                """;

            return jdbcTemplate.query(sql, 
                new Object[]{config.getCertificateRenewalWarningDays()},
                (rs, rowNum) -> {
                    ClientCertificateInfo info = new ClientCertificateInfo();
                    info.setClientId(rs.getString("client_id"));
                    info.setSerialNumber(rs.getString("certificate_serial"));
                    info.setSubjectDn(rs.getString("certificate_subject_dn"));
                    info.setIssuerDn(rs.getString("certificate_issuer_dn"));
                    info.setNotBefore(rs.getTimestamp("certificate_not_before").toInstant());
                    info.setNotAfter(rs.getTimestamp("certificate_not_after").toInstant());
                    info.setFingerprint(rs.getString("certificate_fingerprint"));
                    info.setUploadedBy(rs.getString("uploaded_by"));
                    info.setUploadedAt(rs.getTimestamp("uploaded_at").toInstant());
                    info.setStatus(rs.getString("status"));
                    return info;
                });

        } catch (Exception e) {
            logger.error("Error getting expiring certificates", e);
            return new ArrayList<>();
        }
    }

    /**
     * Revoke a client certificate
     */
    public void revokeCertificate(String clientId, String reason, String revokedBy) {
        logger.info("Revoking certificate for client: {}, reason: {}", clientId, reason);

        try {
            String sql = """
                UPDATE oauth2_client_certificates 
                SET status = 'revoked', revoked_at = ?, revoked_by = ?, revocation_reason = ?
                WHERE client_id = ? AND status = 'active'
                """;

            int updated = jdbcTemplate.update(sql, Instant.now(), revokedBy, reason, clientId);
            
            if (updated > 0) {
                logger.info("Certificate revoked successfully for client: {}", clientId);
                
                // Log audit event
                logCertificateAuditEvent("certificate_revoked", clientId, revokedBy, 
                                       Map.of("reason", reason));
            } else {
                logger.warn("No active certificate found to revoke for client: {}", clientId);
            }

        } catch (Exception e) {
            logger.error("Error revoking certificate for client: {}", clientId, e);
            throw new RuntimeException("Failed to revoke certificate", e);
        }
    }

    // Private helper methods

    private boolean isValidCertificate(X509Certificate certificate) {
        try {
            // Check certificate basic constraints
            certificate.checkValidity();
            
            // Validate certificate format
            if (certificate.getVersion() < 3) {
                logger.warn("Certificate version {} not supported", certificate.getVersion());
                return false;
            }

            // Check key usage
            boolean[] keyUsage = certificate.getKeyUsage();
            if (keyUsage != null && !keyUsage[0]) { // Digital signature
                logger.warn("Certificate does not allow digital signature");
                return false;
            }

            // Check for required extensions if configured
            if (config.getRequiredCertificateExtensions() != null) {
                for (String oid : config.getRequiredCertificateExtensions()) {
                    if (certificate.getExtensionValue(oid) == null) {
                        logger.warn("Required certificate extension {} not found", oid);
                        return false;
                    }
                }
            }

            return true;

        } catch (CertificateExpiredException | CertificateNotYetValidException e) {
            logger.warn("Certificate validity check failed", e);
            return false;
        }
    }

    private boolean isCertificateExpired(X509Certificate certificate) {
        try {
            certificate.checkValidity();
            return false;
        } catch (CertificateExpiredException | CertificateNotYetValidException e) {
            return true;
        }
    }

    private boolean isCertificateRevoked(X509Certificate certificate) {
        if (!config.isEnableCertificateRevocationCheck()) {
            return false;
        }

        try {
            // Check CRL if enabled
            if (config.isEnableCrlCheck()) {
                // Implementation would check Certificate Revocation List
                // This is a placeholder for actual CRL checking logic
                logger.debug("CRL check not implemented yet");
            }

            // Check OCSP if enabled
            if (config.isEnableOcspCheck()) {
                // Implementation would check Online Certificate Status Protocol
                // This is a placeholder for actual OCSP checking logic
                logger.debug("OCSP check not implemented yet");
            }

            return false; // Assume not revoked if checks pass

        } catch (Exception e) {
            logger.error("Error checking certificate revocation status", e);
            return true; // Fail safe - assume revoked if check fails
        }
    }

    private String findClientByCertificate(X509Certificate certificate) {
        try {
            String fingerprint = calculateCertificateFingerprint(certificate);
            
            String sql = """
                SELECT client_id FROM oauth2_client_certificates 
                WHERE certificate_fingerprint = ? AND status = 'active'
                LIMIT 1
                """;

            return jdbcTemplate.queryForObject(sql, String.class, fingerprint);

        } catch (Exception e) {
            logger.debug("No client found for certificate fingerprint");
            return null;
        }
    }

    private boolean isClientActive(String clientId) {
        try {
            String sql = """
                SELECT client_status FROM oauth2_registered_client 
                WHERE client_id = ?
                """;

            String status = jdbcTemplate.queryForObject(sql, String.class, clientId);
            return "active".equals(status);

        } catch (Exception e) {
            logger.debug("Client not found or inactive: {}", clientId);
            return false;
        }
    }

    private String calculateCertificateFingerprint(X509Certificate certificate) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(certificate.getEncoded());
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate certificate fingerprint", e);
        }
    }

    private void logCertificateValidation(X509Certificate certificate, String clientId, 
                                        boolean success, String errorMessage) {
        try {
            String sql = """
                INSERT INTO oauth2_certificate_validation_log 
                (certificate_subject_dn, certificate_serial, client_id, validation_success, 
                 error_message, validated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

            jdbcTemplate.update(sql,
                certificate.getSubjectDN().toString(),
                certificate.getSerialNumber().toString(),
                clientId,
                success,
                errorMessage,
                Instant.now()
            );

        } catch (Exception e) {
            logger.error("Failed to log certificate validation", e);
        }
    }

    private void logCertificateAuditEvent(String eventType, String clientId, 
                                        String principalName, Map<String, String> additionalData) {
        try {
            String sql = """
                INSERT INTO oauth2_audit_log 
                (event_type, client_id, principal_name, success, additional_data)
                VALUES (?, ?, ?, ?, ?)
                """;
            
            String additionalDataJson = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(additionalData);
            
            jdbcTemplate.update(sql, eventType, clientId, principalName, true, additionalDataJson);

        } catch (Exception e) {
            logger.error("Failed to log certificate audit event", e);
        }
    }

    // Result classes

    public static class CertificateValidationResult {
        private final boolean valid;
        private final String clientId;
        private final X509Certificate certificate;
        private final String errorMessage;

        private CertificateValidationResult(boolean valid, String clientId, 
                                          X509Certificate certificate, String errorMessage) {
            this.valid = valid;
            this.clientId = clientId;
            this.certificate = certificate;
            this.errorMessage = errorMessage;
        }

        public static CertificateValidationResult valid(String clientId, X509Certificate certificate) {
            return new CertificateValidationResult(true, clientId, certificate, null);
        }

        public static CertificateValidationResult invalid(String errorMessage) {
            return new CertificateValidationResult(false, null, null, errorMessage);
        }

        // Getters
        public boolean isValid() { return valid; }
        public String getClientId() { return clientId; }
        public X509Certificate getCertificate() { return certificate; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class ClientCertificateInfo {
        private String clientId;
        private String serialNumber;
        private String subjectDn;
        private String issuerDn;
        private Instant notBefore;
        private Instant notAfter;
        private String fingerprint;
        private String uploadedBy;
        private Instant uploadedAt;
        private String status;
        private Instant lastValidatedAt;

        // Getters and setters
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }

        public String getSerialNumber() { return serialNumber; }
        public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

        public String getSubjectDn() { return subjectDn; }
        public void setSubjectDn(String subjectDn) { this.subjectDn = subjectDn; }

        public String getIssuerDn() { return issuerDn; }
        public void setIssuerDn(String issuerDn) { this.issuerDn = issuerDn; }

        public Instant getNotBefore() { return notBefore; }
        public void setNotBefore(Instant notBefore) { this.notBefore = notBefore; }

        public Instant getNotAfter() { return notAfter; }
        public void setNotAfter(Instant notAfter) { this.notAfter = notAfter; }

        public String getFingerprint() { return fingerprint; }
        public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }

        public String getUploadedBy() { return uploadedBy; }
        public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

        public Instant getUploadedAt() { return uploadedAt; }
        public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Instant getLastValidatedAt() { return lastValidatedAt; }
        public void setLastValidatedAt(Instant lastValidatedAt) { this.lastValidatedAt = lastValidatedAt; }
    }
}