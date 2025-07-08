# Field Encryption for OpenESPI

This package provides transparent field-level encryption for sensitive data in OpenESPI entities using JPA AttributeConverter.

## Encrypted Fields

### ApplicationInformationEntity
- `clientSecret` - OAuth2 client secret for third-party applications
- `registrationAccessToken` - Dynamic client registration access token

### AuthorizationEntity  
- `accessToken` - OAuth2 access token for API access
- `refreshToken` - OAuth2 refresh token for token renewal
- `code` - OAuth2 authorization code (temporary)

## Security Features

- **AES-256-GCM encryption** - Industry standard with authenticated encryption
- **Random IV per field** - Each field value gets unique initialization vector
- **Base64 encoding** - Safe storage in database text columns
- **Automatic conversion** - Transparent to application code via JPA converters

## Setup Instructions

### 1. Generate Encryption Key

```bash
# Generate a 256-bit (32 byte) AES key
openssl rand -base64 32
```

### 2. Set Environment Variable

```bash
# In production environment
export ESPI_FIELD_ENCRYPTION_KEY="your-base64-encoded-key-here"

# Or in application.yml
espi:
  field:
    encryption:
      key: ${ESPI_FIELD_ENCRYPTION_KEY}
```

### 3. Key Management Best Practices

**Production Deployment:**
- Store encryption key in secure key management system (AWS KMS, HashiCorp Vault, etc.)
- Never commit keys to version control
- Use different keys for different environments
- Implement key rotation procedures
- Backup encrypted data before key changes

**Development/Testing:**
- Use test keys only
- Generate fresh keys for each environment
- Document key locations for team access

### 4. Database Migration

When enabling encryption on existing data:

```sql
-- Before: Plaintext data
SELECT client_secret FROM application_information WHERE id = 1;
-- Returns: "plain-secret-123"

-- After: Encrypted data  
SELECT client_secret FROM application_information WHERE id = 1;
-- Returns: "AQIDBAUGBwgJCgsMDQ4PEBESExQV...encrypted-base64-data..."
```

**Migration Steps:**
1. Deploy application with encryption enabled
2. Run data migration script to encrypt existing plaintext values
3. Verify all sensitive data is properly encrypted
4. Test decryption by reading entities through JPA

## Error Handling

### Missing Key
- **Development:** Logs warning, stores plaintext (not recommended)
- **Production:** Should throw exception to prevent data exposure

### Decryption Failure  
- Logs error with field information
- Returns encrypted value as fallback
- May indicate key rotation needed or data corruption

### Key Rotation
1. Deploy new key alongside old key
2. Migrate data using both keys (decrypt with old, encrypt with new)
3. Remove old key after verification
4. Monitor logs for decryption errors

## Monitoring

Monitor application logs for:
- `"Field encryption initialized"` - Successful startup
- `"Encryption key not available"` - Missing key configuration
- `"Failed to encrypt/decrypt field"` - Runtime encryption errors

## Performance Impact

- **Encryption overhead:** ~1-2ms per field
- **Storage overhead:** ~33% increase in field size (Base64 + IV + auth tag)
- **Memory impact:** Minimal - encryption is on-demand
- **Database impact:** Larger column sizes for encrypted fields

## Security Considerations

- Encrypted fields cannot be used in SQL WHERE clauses for searching
- Database backups contain encrypted data but require encryption key
- Application logs should never contain decrypted sensitive values
- Consider implementing field-level access controls in application logic

## Testing

```java
@Test
public void testFieldEncryption() {
    ApplicationInformationEntity app = new ApplicationInformationEntity();
    app.setClientSecret("test-secret-123");
    
    // Save to database - triggers encryption
    applicationRepository.save(app);
    
    // Read from database - triggers decryption  
    ApplicationInformationEntity retrieved = applicationRepository.findById(app.getId()).get();
    assertEquals("test-secret-123", retrieved.getClientSecret());
}
```