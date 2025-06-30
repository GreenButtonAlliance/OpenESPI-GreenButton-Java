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

package org.greenbuttonalliance.espi.authserver.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.greenbuttonalliance.espi.authserver.service.ClientCertificateService;
import org.greenbuttonalliance.espi.authserver.service.ClientCertificateService.ClientCertificateInfo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for certificate management operations
 * 
 * Provides API endpoints for:
 * - Certificate upload and storage
 * - Certificate validation and verification
 * - Certificate information retrieval
 * - Certificate revocation
 * - Certificate renewal monitoring
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
@RestController
@RequestMapping("/api/v1/certificates")
@PreAuthorize("hasRole('ADMIN') or hasRole('DC_ADMIN')")
@Validated
public class CertificateManagementController {

    private static final Logger logger = LoggerFactory.getLogger(CertificateManagementController.class);

    private final ClientCertificateService certificateService;

    @Autowired
    public CertificateManagementController(ClientCertificateService certificateService) {
        this.certificateService = certificateService;
    }

    /**
     * Upload client certificate
     * 
     * POST /api/v1/certificates/clients/{clientId}/upload
     */
    @PostMapping("/clients/{clientId}/upload")
    public ResponseEntity<?> uploadCertificate(
            @PathVariable @NotBlank String clientId,
            @RequestParam("certificate") MultipartFile certificateFile,
            @RequestParam(value = "uploaded_by", required = false) String uploadedBy) {
        
        logger.info("Uploading certificate for client: {}", clientId);

        try {
            if (certificateFile.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("invalid_file", "Certificate file is empty"));
            }

            // Parse certificate
            X509Certificate certificate = parseCertificate(certificateFile.getBytes());
            if (certificate == null) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("invalid_certificate", "Unable to parse certificate"));
            }

            // Validate certificate
            ClientCertificateService.CertificateValidationResult validation = 
                certificateService.validateClientCertificate(certificate);
            
            if (!validation.isValid()) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("certificate_validation_failed", validation.getErrorMessage()));
            }

            // Store certificate
            String uploader = uploadedBy != null ? uploadedBy : "system";
            certificateService.storeClientCertificate(clientId, certificate, uploader);

            // Build response
            CertificateUploadResponse response = new CertificateUploadResponse();
            response.setClientId(clientId);
            response.setSerialNumber(certificate.getSerialNumber().toString());
            response.setSubjectDn(certificate.getSubjectDN().toString());
            response.setIssuerDn(certificate.getIssuerDN().toString());
            response.setNotBefore(certificate.getNotBefore().toInstant());
            response.setNotAfter(certificate.getNotAfter().toInstant());
            response.setUploadedAt(Instant.now());
            response.setStatus("active");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.error("Error uploading certificate for client: {}", clientId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("upload_failed", "Certificate upload failed"));
        }
    }

    /**
     * Get client certificate information
     * 
     * GET /api/v1/certificates/clients/{clientId}
     */
    @GetMapping("/clients/{clientId}")
    public ResponseEntity<?> getCertificate(@PathVariable @NotBlank String clientId) {
        logger.debug("Getting certificate info for client: {}", clientId);

        try {
            ClientCertificateInfo certificateInfo = certificateService.getClientCertificateInfo(clientId);
            
            if (certificateInfo == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(certificateInfo);

        } catch (Exception e) {
            logger.error("Error getting certificate for client: {}", clientId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("retrieval_failed", "Failed to retrieve certificate"));
        }
    }

    /**
     * Revoke client certificate
     * 
     * DELETE /api/v1/certificates/clients/{clientId}
     */
    @DeleteMapping("/clients/{clientId}")
    public ResponseEntity<?> revokeCertificate(
            @PathVariable @NotBlank String clientId,
            @Valid @RequestBody RevokeCertificateRequest request) {
        
        logger.info("Revoking certificate for client: {}, reason: {}", clientId, request.getReason());

        try {
            certificateService.revokeCertificate(clientId, request.getReason(), request.getRevokedBy());

            Map<String, String> response = new HashMap<>();
            response.put("status", "revoked");
            response.put("client_id", clientId);
            response.put("message", "Certificate revoked successfully");
            response.put("revoked_at", Instant.now().toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error revoking certificate for client: {}", clientId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("revocation_failed", "Failed to revoke certificate"));
        }
    }

    /**
     * Get certificates expiring soon
     * 
     * GET /api/v1/certificates/expiring
     */
    @GetMapping("/expiring")
    public ResponseEntity<?> getExpiringCertificates() {
        logger.debug("Getting certificates expiring soon");

        try {
            List<ClientCertificateInfo> expiringCertificates = 
                certificateService.getCertificatesExpiringSoon();

            ExpiringCertificatesResponse response = new ExpiringCertificatesResponse();
            response.setCertificates(expiringCertificates);
            response.setCount(expiringCertificates.size());
            response.setRetrievedAt(Instant.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting expiring certificates", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("retrieval_failed", "Failed to retrieve expiring certificates"));
        }
    }

    /**
     * Validate certificate format and content
     * 
     * POST /api/v1/certificates/validate
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateCertificate(@RequestParam("certificate") MultipartFile certificateFile) {
        logger.debug("Validating certificate format");

        try {
            if (certificateFile.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("invalid_file", "Certificate file is empty"));
            }

            // Parse certificate
            X509Certificate certificate = parseCertificate(certificateFile.getBytes());
            if (certificate == null) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("invalid_certificate", "Unable to parse certificate"));
            }

            // Validate certificate
            ClientCertificateService.CertificateValidationResult validation = 
                certificateService.validateClientCertificate(certificate);

            CertificateValidationResponse response = new CertificateValidationResponse();
            response.setValid(validation.isValid());
            response.setSerialNumber(certificate.getSerialNumber().toString());
            response.setSubjectDn(certificate.getSubjectDN().toString());
            response.setIssuerDn(certificate.getIssuerDN().toString());
            response.setNotBefore(certificate.getNotBefore().toInstant());
            response.setNotAfter(certificate.getNotAfter().toInstant());
            response.setValidatedAt(Instant.now());
            
            if (!validation.isValid()) {
                response.setErrorMessage(validation.getErrorMessage());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error validating certificate", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("validation_failed", "Certificate validation failed"));
        }
    }

    // Helper methods

    private X509Certificate parseCertificate(byte[] certificateData) {
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certificateData));
        } catch (Exception e) {
            logger.error("Error parsing certificate", e);
            return null;
        }
    }

    // DTO Classes

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RevokeCertificateRequest {
        @NotBlank(message = "Reason is required")
        @Size(max = 200, message = "Reason must not exceed 200 characters")
        @JsonProperty("reason")
        private String reason;

        @JsonProperty("revoked_by")
        @Size(max = 100, message = "Revoked by must not exceed 100 characters")
        private String revokedBy;

        // Getters and setters
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        public String getRevokedBy() { return revokedBy; }
        public void setRevokedBy(String revokedBy) { this.revokedBy = revokedBy; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CertificateUploadResponse {
        @JsonProperty("client_id")
        private String clientId;

        @JsonProperty("serial_number")
        private String serialNumber;

        @JsonProperty("subject_dn")
        private String subjectDn;

        @JsonProperty("issuer_dn")
        private String issuerDn;

        @JsonProperty("not_before")
        private Instant notBefore;

        @JsonProperty("not_after")
        private Instant notAfter;

        @JsonProperty("uploaded_at")
        private Instant uploadedAt;

        @JsonProperty("status")
        private String status;

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

        public Instant getUploadedAt() { return uploadedAt; }
        public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CertificateValidationResponse {
        @JsonProperty("valid")
        private boolean valid;

        @JsonProperty("serial_number")
        private String serialNumber;

        @JsonProperty("subject_dn")
        private String subjectDn;

        @JsonProperty("issuer_dn")
        private String issuerDn;

        @JsonProperty("not_before")
        private Instant notBefore;

        @JsonProperty("not_after")
        private Instant notAfter;

        @JsonProperty("validated_at")
        private Instant validatedAt;

        @JsonProperty("error_message")
        private String errorMessage;

        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }

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

        public Instant getValidatedAt() { return validatedAt; }
        public void setValidatedAt(Instant validatedAt) { this.validatedAt = validatedAt; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ExpiringCertificatesResponse {
        @JsonProperty("certificates")
        private List<ClientCertificateInfo> certificates;

        @JsonProperty("count")
        private int count;

        @JsonProperty("retrieved_at")
        private Instant retrievedAt;

        // Getters and setters
        public List<ClientCertificateInfo> getCertificates() { return certificates; }
        public void setCertificates(List<ClientCertificateInfo> certificates) { this.certificates = certificates; }

        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }

        public Instant getRetrievedAt() { return retrievedAt; }
        public void setRetrievedAt(Instant retrievedAt) { this.retrievedAt = retrievedAt; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorResponse {
        @JsonProperty("status")
        private String status = "error";

        @JsonProperty("error")
        private String error;

        @JsonProperty("message")
        private String message;

        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }

        // Getters and setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}