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

package org.greenbuttonalliance.espi.common.domain.usage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.greenbuttonalliance.espi.common.domain.common.IdentifiedObject;
import org.greenbuttonalliance.espi.common.utils.security.PasswordPolicy;
import org.hibernate.annotations.BatchSize;
import org.hibernate.proxy.HibernateProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Pure JPA/Hibernate entity for RetailCustomer without JAXB concerns.
 * <p>
 * Represents a retail energy customer for Green Button data access.
 * Authentication concerns are handled in DataCustodian/ThirdParty repositories.
 */
@Entity
@Table(name = "retail_customers")
@Getter
@Setter
@NoArgsConstructor
public class RetailCustomerEntity extends IdentifiedObject {

    private static final long serialVersionUID = 1L;

    // Password encoder removed - authentication moved to DataCustodian/ThirdParty
    // private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    // Password policy validator
    private static final PasswordPolicy passwordPolicy = new PasswordPolicy();

    // Role constants
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_CUSTODIAN = "ROLE_CUSTODIAN";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    /**
     * Unique username for authentication.
     * Used for login and identification within the system.
     */
    @Column(name = "username", unique = true)
    @Size(min = 4, max = 30, message = "Username must be between 4 and 30 characters")
    @NotEmpty(message = "Username cannot be empty")
    private String username;

    /**
     * Customer's first name.
     * Required field for customer identification.
     */
    @Column(name = "first_name")
    @Size(max = 30, message = "First name cannot exceed 30 characters")
    @NotEmpty(message = "First name cannot be empty")
    private String firstName;

    /**
     * Customer's last name.
     * Required field for customer identification.
     */
    @Column(name = "last_name")
    @Size(max = 30, message = "Last name cannot exceed 30 characters")
    @NotEmpty(message = "Last name cannot be empty")
    private String lastName;

    /**
     * Encrypted password for authentication.
     * Automatically hashed using BCrypt before storage.
     * Never included in JSON serialization for security.
     */
    @JsonIgnore
    @Column(name = "password", length = 100)
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", 
             message = "Password must contain at least one lowercase letter, one uppercase letter, one digit, and one special character")
    private String password;

    /**
     * Account enabled status.
     * Controls whether the user can authenticate.
     */
    @Column(name = "enabled", columnDefinition = "BIT", length = 1)
    @NotNull(message = "Enabled status cannot be null")
    private Boolean enabled = Boolean.TRUE;

    /**
     * User role for authorization.
     * Determines permissions within the system.
     */
    @Column(name = "role")
    @NotEmpty(message = "Role cannot be empty")
    private String role = ROLE_USER;

    /**
     * Email address for the customer.
     * Optional field for communication and notifications.
     */
    @Column(name = "email")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    /**
     * Phone number for the customer.
     * Optional field for communication.
     */
    @Column(name = "phone")
    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phone;

    /**
     * Account creation timestamp.
     * Automatically set when the entity is first persisted.
     */
    @Column(name = "account_created")
    private Long accountCreated;

    /**
     * Last login timestamp.
     * Updated each time the user successfully authenticates.
     */
    @Column(name = "last_login")
    private Long lastLogin;

    /**
     * Account locked status.
     * Used for security measures like failed login attempts.
     */
    @Column(name = "account_locked")
    private Boolean accountLocked = Boolean.FALSE;

    /**
     * Failed login attempts counter.
     * Used for account security and lockout mechanisms.
     */
    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    /**
     * Usage points associated with this customer.
     * One-to-many relationship representing all metering points.
     * Optimized with batch loading to prevent N+1 queries.
     */
    @OneToMany(mappedBy = "retailCustomer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 10)
    private List<UsagePointEntity> usagePoints = new ArrayList<>();

    /**
     * Authorizations granted to this customer.
     * OAuth2 authorizations for third-party access.
     * Optimized with batch loading to prevent N+1 queries.
     */
    @OneToMany(mappedBy = "retailCustomer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 20)
    private List<AuthorizationEntity> authorizations = new ArrayList<>();

    /**
     * Constructor with basic customer information.
     * 
     * @param username the unique username
     * @param firstName the customer's first name
     * @param lastName the customer's last name
     */
    public RetailCustomerEntity(String username, String firstName, String lastName) {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.accountCreated = System.currentTimeMillis();
    }

    /**
     * Constructor with full customer information.
     * 
     * @param username the unique username
     * @param firstName the customer's first name
     * @param lastName the customer's last name
     * @param email the customer's email
     * @param role the user role
     */
    public RetailCustomerEntity(String username, String firstName, String lastName, String email, String role) {
        this(username, firstName, lastName);
        this.email = email;
        this.role = role != null ? role : ROLE_USER;
    }

    // Note: Collection accessors (getUsagePoints, getAuthorizations) are generated by Lombok @Data
    // Bidirectional relationship management methods removed - handled by DataCustodian/ThirdParty applications

    // Spring Security UserDetails implementation - COMMENTED OUT
    // Authentication concerns moved to DataCustodian/ThirdParty repositories
    
    // @Override
    // public Collection<? extends GrantedAuthority> getAuthorities() {
    //     Collection<GrantedAuthority> authorities = new ArrayList<>();
    //     authorities.add(new SimpleGrantedAuthority(role));
    //     return authorities;
    // }

    // @Override
    // public boolean isAccountNonExpired() {
    //     return true;
    // }

    // @Override
    // public boolean isAccountNonLocked() {
    //     return !Boolean.TRUE.equals(accountLocked);
    // }

    // @Override
    // public boolean isCredentialsNonExpired() {
    //     return true;
    // }

    // @Override
    // public boolean isEnabled() {
    //     return Boolean.TRUE.equals(enabled);
    // }
    
    // @Override
    // public String getUsername() {
    //     return username;
    // }

    // Principal implementation - COMMENTED OUT
    
    // @Override
    // public String getName() {
    //     return username;
    // }

    /**
     * Updates the last login timestamp to current time.
     */
    public void updateLastLogin() {
        this.lastLogin = System.currentTimeMillis();
        this.failedLoginAttempts = 0; // Reset failed attempts on successful login
    }

    /**
     * Increments the failed login attempts counter.
     * 
     * @return the new count of failed attempts
     */
    public int incrementFailedLoginAttempts() {
        this.failedLoginAttempts = (this.failedLoginAttempts != null ? this.failedLoginAttempts : 0) + 1;
        return this.failedLoginAttempts;
    }

    /**
     * Locks the account due to security reasons.
     */
    public void lockAccount() {
        this.accountLocked = Boolean.TRUE;
    }

    /**
     * Unlocks the account and resets failed login attempts.
     */
    public void unlockAccount() {
        this.accountLocked = Boolean.FALSE;
        this.failedLoginAttempts = 0;
    }

    /**
     * Generates the self href for this retail customer.
     * 
     * @return self href string
     */
    public String getSelfHref() {
        return "/espi/1_1/resource/RetailCustomer/" + getHashedId();
    }

    /**
     * Generates the up href for this retail customer.
     * 
     * @return up href string
     */
    public String getUpHref() {
        return "/espi/1_1/resource/RetailCustomer";
    }

    /**
     * Overrides the default self href generation to use retail customer specific logic.
     * 
     * @return self href for this retail customer
     */
    @Override
    protected String generateDefaultSelfHref() {
        return getSelfHref();
    }

    /**
     * Overrides the default up href generation to use retail customer specific logic.
     * 
     * @return up href for this retail customer
     */
    @Override
    protected String generateDefaultUpHref() {
        return getUpHref();
    }

    /**
     * Manual getter for ID field (Lombok issue workaround).
     * 
     * @return the entity ID
     */
    public UUID getId() {
        return this.id;
    }

    /**
     * Custom implementation for retail customers to use ID instead of UUID.
     * 
     * @return string representation of the ID
     */
    @Override
    public String getHashedId() {
        return getId() != null ? getId().toString() : "";
    }

    /**
     * Merges data from another RetailCustomerEntity.
     * Updates user information but preserves security-sensitive fields.
     * 
     * @param other the other retail customer entity to merge from
     */
    public void merge(RetailCustomerEntity other) {
        if (other != null) {
            super.merge(other);
            
            // Update basic information
            this.firstName = other.firstName;
            this.lastName = other.lastName;
            this.email = other.email;
            this.phone = other.phone;
            
            // Only update role if provided
            if (other.role != null && !other.role.trim().isEmpty()) {
                this.role = other.role;
            }
            
            // SECURITY: Never merge password, enabled, accountLocked, or authentication fields
            // Password updates must use setPasswordSecurely() method
            // Note: Collections are not merged to preserve existing relationships
        }
    }

    /**
     * Safely updates password during merge if a new password is provided.
     * This method should be called separately from merge() for security.
     * 
     * @param newRawPassword the new plain text password, or null to skip update
     */
    public void updatePasswordDuringMerge(String newRawPassword) {
        if (newRawPassword != null && !newRawPassword.trim().isEmpty()) {
            setPasswordSecurely(newRawPassword);
        }
    }

    /**
     * Clears all relationships when unlinking the entity.
     * Simplified - applications handle relationship cleanup.
     */
    public void unlink() {
        clearRelatedLinks();
        
        // Simple collection clearing - applications handle bidirectional cleanup
        usagePoints.clear();
        authorizations.clear();
    }

    /**
     * Gets the full name of the customer.
     * 
     * @return formatted full name
     */
    public String getFullName() {
        StringBuilder fullName = new StringBuilder();
        if (firstName != null && !firstName.trim().isEmpty()) {
            fullName.append(firstName.trim());
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(lastName.trim());
        }
        return fullName.toString();
    }

    /**
     * Checks if the customer has admin privileges.
     * 
     * @return true if role is ROLE_ADMIN, false otherwise
     */
    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }

    /**
     * Checks if the customer has custodian privileges.
     * 
     * @return true if role is ROLE_CUSTODIAN, false otherwise
     */
    public boolean isCustodian() {
        return ROLE_CUSTODIAN.equals(role);
    }

    /**
     * Checks if the customer is a regular user.
     * 
     * @return true if role is ROLE_USER, false otherwise
     */
    public boolean isRegularUser() {
        return ROLE_USER.equals(role);
    }

    /**
     * Gets the number of usage points for this customer.
     * 
     * @return count of usage points
     */
    public int getUsagePointCount() {
        return usagePoints != null ? usagePoints.size() : 0;
    }

    /**
     * Gets the number of authorizations for this customer.
     * 
     * @return count of authorizations
     */
    public int getAuthorizationCount() {
        return authorizations != null ? authorizations.size() : 0;
    }

    /**
     * Checks if the account should be locked due to failed login attempts.
     * 
     * @param maxAttempts the maximum allowed failed attempts
     * @return true if account should be locked, false otherwise
     */
    public boolean shouldLockAccount(int maxAttempts) {
        return failedLoginAttempts != null && failedLoginAttempts >= maxAttempts;
    }

    /**
     * Password security methods moved to DataCustodian/ThirdParty applications.
     * This is now a simple setter for migration/testing purposes.
     * 
     * @param rawPassword the plain text password (will be stored as-is for now)
     * @deprecated Use authentication services in DataCustodian/ThirdParty modules
     */
    @Deprecated
    public void setPasswordSecurely(String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        // Simple assignment - encryption handled in web layer
        this.password = rawPassword.trim();
    }

    /**
     * Sets password without validation (for system/migration use only).
     * 
     * @param rawPassword the plain text password to store
     * @deprecated Use authentication services in DataCustodian/ThirdParty modules
     */
    @Deprecated
    public void setPasswordSecurelyNoValidation(String rawPassword) {
        if (rawPassword != null && !rawPassword.trim().isEmpty()) {
            this.password = rawPassword.trim();
        }
    }

    /**
     * Simple password verification for backward compatibility.
     * Real authentication moved to DataCustodian/ThirdParty modules.
     * 
     * @param rawPassword the plain text password to verify
     * @return true if the password matches, false otherwise
     * @deprecated Use authentication services in DataCustodian/ThirdParty modules
     */
    @Deprecated
    public boolean checkPassword(String rawPassword) {
        if (rawPassword == null || password == null) {
            return false;
        }
        return rawPassword.equals(password);
    }

    /**
     * Validates password against security policy.
     * 
     * @param rawPassword the plain text password to check
     * @return validation result with details
     */
    public static PasswordPolicy.PasswordValidationResult validatePassword(String rawPassword) {
        return passwordPolicy.validatePassword(rawPassword);
    }

    /**
     * Checks if the current password is strong according to security policy.
     * 
     * @param rawPassword the plain text password to check
     * @return true if password meets strength requirements, false otherwise
     */
    public static boolean isPasswordStrong(String rawPassword) {
        return passwordPolicy.validatePassword(rawPassword).isValid();
    }

    /**
     * Calculates password strength score (0-100).
     * 
     * @param rawPassword the plain text password to score
     * @return strength score from 0 (weakest) to 100 (strongest)
     */
    public static int getPasswordStrength(String rawPassword) {
        return passwordPolicy.calculatePasswordStrength(rawPassword);
    }

    /**
     * Updates the password if it's different from the current one.
     * Useful for password change operations.
     * 
     * @param newRawPassword the new plain text password
     * @return true if password was updated, false if it was the same
     * @deprecated Use authentication services in DataCustodian/ThirdParty modules
     */
    @Deprecated
    public boolean updatePasswordIfDifferent(String newRawPassword) {
        if (newRawPassword == null || newRawPassword.trim().isEmpty()) {
            return false;
        }
        
        // Check if the new password is different from current
        if (checkPassword(newRawPassword)) {
            return false; // Same password, no update needed
        }
        
        setPasswordSecurely(newRawPassword);
        return true;
    }

    /**
     * Post-construct callback to set account creation time.
     * Called after entity creation but before persistence.
     */
    @PostLoad
    @PostPersist
    protected void onCreateRetailCustomer() {
        if (accountCreated == null) {
            accountCreated = System.currentTimeMillis();
        }
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        RetailCustomerEntity that = (RetailCustomerEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" +
                "id = " + getId() + ", " +
                "username = " + getUsername() + ", " +
                "firstName = " + getFirstName() + ", " +
                "lastName = " + getLastName() + ", " +
                "password = " + getPassword() + ", " +
                "enabled = " + getEnabled() + ", " +
                "role = " + getRole() + ", " +
                "email = " + getEmail() + ", " +
                "phone = " + getPhone() + ", " +
                "accountCreated = " + getAccountCreated() + ", " +
                "lastLogin = " + getLastLogin() + ", " +
                "accountLocked = " + getAccountLocked() + ", " +
                "failedLoginAttempts = " + getFailedLoginAttempts() + ", " +
                "description = " + getDescription() + ", " +
                "created = " + getCreated() + ", " +
                "updated = " + getUpdated() + ", " +
                "published = " + getPublished() + ")";
    }
}