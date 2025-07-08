/*
 *
 *        Copyright (c) 2025 Green Button Alliance, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
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

package org.greenbuttonalliance.espi.common.utils.encryption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * JPA AttributeConverter for encrypting sensitive string fields at rest.
 * Uses AES-256-GCM encryption for strong security with authentication.
 * 
 * Environment Variables Required:
 * - ESPI_FIELD_ENCRYPTION_KEY: Base64-encoded 256-bit encryption key
 * 
 * Key Generation (for initial setup):
 * KeyGenerator keyGen = KeyGenerator.getInstance("AES");
 * keyGen.init(256);
 * SecretKey key = keyGen.generateKey();
 * String encodedKey = Base64.getEncoder().encodeToString(key.getEncoded());
 */
@Component
@Converter
public class FieldEncryptionConverter implements AttributeConverter<String, String> {

    private static final Logger logger = LoggerFactory.getLogger(FieldEncryptionConverter.class);
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    
    @Value("${espi.field.encryption.key:}")
    private String encryptionKeyBase64;
    
    private SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Initializes the encryption key from environment variable.
     * Called during Spring bean initialization.
     */
    private void initializeKey() {
        if (secretKey != null) {
            return; // Already initialized
        }
        
        if (encryptionKeyBase64 == null || encryptionKeyBase64.trim().isEmpty()) {
            logger.error("ESPI_FIELD_ENCRYPTION_KEY environment variable not set. Field encryption disabled.");
            return;
        }
        
        try {
            byte[] decodedKey = Base64.getDecoder().decode(encryptionKeyBase64.trim());
            if (decodedKey.length != 32) { // 256 bits
                logger.error("Invalid encryption key length: {} bytes. Expected 32 bytes for AES-256.", decodedKey.length);
                return;
            }
            secretKey = new SecretKeySpec(decodedKey, ALGORITHM);
            logger.info("Field encryption initialized with AES-256-GCM");
        } catch (Exception e) {
            logger.error("Failed to initialize encryption key: {}", e.getMessage());
        }
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }
        
        initializeKey();
        if (secretKey == null) {
            logger.warn("Encryption key not available. Storing field value in plaintext (SECURITY RISK)");
            return attribute;
        }
        
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            
            // Generate random IV for each encryption
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);  
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
            
            byte[] encryptedData = cipher.doFinal(attribute.getBytes("UTF-8"));
            
            // Combine IV + encrypted data for storage
            byte[] encryptedWithIv = new byte[iv.length + encryptedData.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, iv.length);
            System.arraycopy(encryptedData, 0, encryptedWithIv, iv.length, encryptedData.length);
            
            return Base64.getEncoder().encodeToString(encryptedWithIv);
            
        } catch (Exception e) {
            logger.error("Failed to encrypt field: {}", e.getMessage());
            // In production environments, consider throwing exception instead
            return attribute; // Fallback to plaintext (not recommended for production)
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }
        
        initializeKey();
        if (secretKey == null) {
            logger.warn("Encryption key not available. Reading field value as plaintext");
            return dbData;
        }
        
        try {
            byte[] encryptedWithIv = Base64.getDecoder().decode(dbData);
            
            // Extract IV and encrypted data
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encryptedWithIv, 0, iv, 0, iv.length);
            
            byte[] encryptedData = new byte[encryptedWithIv.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedWithIv, GCM_IV_LENGTH, encryptedData, 0, encryptedData.length);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
            
            byte[] decryptedData = cipher.doFinal(encryptedData);
            return new String(decryptedData, "UTF-8");
            
        } catch (Exception e) {
            logger.error("Failed to decrypt field: {}", e.getMessage());
            // In production, this might indicate data corruption or wrong key
            return dbData; // Fallback to returning encrypted data as-is
        }
    }
}