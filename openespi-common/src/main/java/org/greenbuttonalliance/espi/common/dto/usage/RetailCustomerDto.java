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

package org.greenbuttonalliance.espi.common.dto.usage;

import jakarta.xml.bind.annotation.*;
import java.util.List;

/**
 * RetailCustomer DTO record for JAXB XML marshalling/unmarshalling.
 * 
 * Represents a retail energy customer for Green Button data access.
 * Security-sensitive fields like password are excluded from the DTO.
 */
@XmlRootElement(name = "RetailCustomer", namespace = "http://naesb.org/espi")
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlType(name = "RetailCustomer", namespace = "http://naesb.org/espi", propOrder = {
    "username", "firstName", "lastName", "email", "phone", "role", 
    "enabled", "accountCreated", "lastLogin", "accountLocked", 
    "failedLoginAttempts", "usagePoints", "authorizations"
})
public record RetailCustomerDto(
    
    String uuid,
    String username,
    String firstName,
    String lastName,
    String email,
    String phone,
    String role,
    Boolean enabled,
    Long accountCreated,
    Long lastLogin,
    Boolean accountLocked,
    Integer failedLoginAttempts,
    List<UsagePointDto> usagePoints,
    List<AuthorizationDto> authorizations
    
) {

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
        return "ROLE_ADMIN".equals(role);
    }

    /**
     * Checks if the customer has custodian privileges.
     * 
     * @return true if role is ROLE_CUSTODIAN, false otherwise
     */
    public boolean isCustodian() {
        return "ROLE_CUSTODIAN".equals(role);
    }

    /**
     * Checks if the customer is a regular user.
     * 
     * @return true if role is ROLE_USER, false otherwise
     */
    public boolean isRegularUser() {
        return "ROLE_USER".equals(role);
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
}