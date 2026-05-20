/*
 *
 *        Copyright (c) 2025 Green Button Alliance, Inc.
 *
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

package org.greenbuttonalliance.espi.common.service;

import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Service for generating NAESB ESPI compliant UUID type 5 identifiers.
 * <p>
 * This service generates deterministic UUID5 identifiers based on href URLs
 * to ensure ESPI compliance and consistency across the system.
 */
@Service
public class EspiIdGeneratorService {

    /**
     * ESPI namespace UUID for generating consistent UUID5 identifiers.
     * This namespace is specifically for Green Button ESPI resources.
     */
    private static final UUID ESPI_NAMESPACE = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");

    /**
     * Generates a NAESB ESPI compliant UUID5 based on the provided href URL.
     * <p>
     * UUID5 uses SHA-1 hashing to create deterministic identifiers, ensuring
     * that the same href will always generate the same UUID.
     * 
     * @param href the resource href URL to base the UUID on
     * @return a deterministic UUID5 identifier
     * @throws IllegalArgumentException if href is null, empty, or not a valid absolute URL
     */
    public UUID generateEspiId(String href) {
        if (href == null || href.trim().isEmpty()) {
            throw new IllegalArgumentException("href cannot be null or empty");
        }

        // Validate that href is a proper absolute URL
        try {
            URL url = new URL(href);
            
            // Ensure it's an absolute URL with protocol and host
            if (url.getProtocol() == null || url.getHost() == null || url.getHost().isEmpty()) {
                throw new IllegalArgumentException("href must be a valid absolute URL with protocol and host: " + href);
            }
            
            // Ensure it's not a relative URL (relative URLs would fail URL constructor anyway, but being explicit)
            if (href.startsWith("/") || href.startsWith("./") || href.startsWith("../")) {
                throw new IllegalArgumentException("href cannot be a relative URL: " + href);
            }
            
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("href is not a valid URL: " + href, e);
        }

        try {
            return generateUUID5(ESPI_NAMESPACE, href);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not available", e);
        }
    }

    /**
     * Generates a UUID5 using the specified namespace and name.
     * 
     * @param namespace the namespace UUID
     * @param name the name to hash
     * @return the generated UUID5
     * @throws NoSuchAlgorithmException if SHA-1 is not available
     */
    private UUID generateUUID5(UUID namespace, String name) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        
        // Convert namespace UUID to bytes
        byte[] namespaceBytes = uuidToBytes(namespace);
        
        // Hash namespace + name
        sha1.update(namespaceBytes);
        sha1.update(name.getBytes(StandardCharsets.UTF_8));
        
        byte[] hash = sha1.digest();
        
        // Set version (5) and variant bits according to RFC 4122
        hash[6] &= 0x0f;  // clear version
        hash[6] |= 0x50;  // set to version 5
        hash[8] &= 0x3f;  // clear variant
        hash[8] |= 0x80;  // set to IETF variant
        
        return bytesToUUID(hash);
    }

    /**
     * Converts a UUID to byte array in big-endian format.
     */
    private byte[] uuidToBytes(UUID uuid) {
        byte[] bytes = new byte[16];
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (msb >>> ((7 - i) * 8));
            bytes[8 + i] = (byte) (lsb >>> ((7 - i) * 8));
        }
        
        return bytes;
    }

    /**
     * Converts byte array to UUID.
     */
    private UUID bytesToUUID(byte[] bytes) {
        long msb = 0;
        long lsb = 0;

        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (bytes[i] & 0xff);
            lsb = (lsb << 8) | (bytes[8 + i] & 0xff);
        }

        return new UUID(msb, lsb);
    }

    /**
     * Generates a deterministic NAESB ESPI compliant UUID5 for any entity type.
     *
     * This method creates a deterministic UUID5 based on the entity type and natural key.
     * The same entityType and naturalKey will always produce the same UUID, ensuring
     * idempotency for entity creation and updates.
     *
     * @param entityType the type of entity (e.g., "CustomerAccount", "CustomerAgreement")
     * @param naturalKey the natural key or business identifier (e.g., accountId, agreementId)
     * @return a deterministic UUID5 identifier for the entity
     * @throws IllegalArgumentException if entityType or naturalKey is null or empty
     */
    public UUID generateEntityId(String entityType, String naturalKey) {
        if (entityType == null || entityType.trim().isEmpty()) {
            throw new IllegalArgumentException("entityType cannot be null or empty");
        }
        if (naturalKey == null || naturalKey.trim().isEmpty()) {
            throw new IllegalArgumentException("naturalKey cannot be null or empty");
        }

        // Build the deterministic name string (no timestamps - purely deterministic)
        String name = entityType.trim() + ":" + naturalKey.trim();

        try {
            return generateUUID5(ESPI_NAMESPACE, name);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not available", e);
        }
    }

    /**
     * Generates a NAESB ESPI compliant UUID5 for a Subscription entity.
     *
     * The UUID5 is generated from clientId + username + current timestamp to ensure
     * uniqueness even when the same client/user creates multiple subscriptions.
     *
     * @param clientId the OAuth2 client ID
     * @param username the retail customer username
     * @return a unique UUID5 identifier for the subscription
     * @throws IllegalArgumentException if both clientId and username are null or empty
     */
    public UUID generateSubscriptionId(String clientId, String username) {
        if ((clientId == null || clientId.trim().isEmpty()) &&
            (username == null || username.trim().isEmpty())) {
            throw new IllegalArgumentException("At least one of clientId or username must be provided");
        }

        // Build the name string with timestamp for non-repeatability
        StringBuilder nameBuilder = new StringBuilder();
        nameBuilder.append("Subscription:");
        if (clientId != null && !clientId.trim().isEmpty()) {
            nameBuilder.append(clientId.trim());
        }
        nameBuilder.append(":");
        if (username != null && !username.trim().isEmpty()) {
            nameBuilder.append(username.trim());
        }
        nameBuilder.append(":");
        nameBuilder.append(System.currentTimeMillis());
        nameBuilder.append(":");
        nameBuilder.append(System.nanoTime()); // Additional entropy

        try {
            return generateUUID5(ESPI_NAMESPACE, nameBuilder.toString());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not available", e);
        }
    }
}