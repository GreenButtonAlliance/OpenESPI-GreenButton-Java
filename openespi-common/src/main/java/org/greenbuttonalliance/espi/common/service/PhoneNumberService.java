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

import org.greenbuttonalliance.espi.common.domain.customer.entity.PhoneNumberEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing PhoneNumberEntity relationships across different parent entities.
 * 
 * Provides utilities for creating, updating, and managing phone numbers as separate entities
 * while maintaining the logical association with parent entities (Customer, ServiceSupplier, etc.).
 */
@Service
public class PhoneNumberService {

    /**
     * Creates phone number entities for a given parent entity.
     * 
     * @param parentEntityUuid UUID of the parent entity
     * @param parentEntityType Type of the parent entity (e.g., "CustomerEntity", "ServiceSupplierEntity")
     * @param phone1AreaCode Primary phone area code
     * @param phone1CityCode Primary phone city code
     * @param phone1LocalNumber Primary phone local number
     * @param phone1Extension Primary phone extension
     * @param phone2AreaCode Secondary phone area code (optional)
     * @param phone2CityCode Secondary phone city code (optional)
     * @param phone2LocalNumber Secondary phone local number (optional)
     * @param phone2Extension Secondary phone extension (optional)
     * @return List of PhoneNumberEntity objects
     */
    public List<PhoneNumberEntity> createPhoneNumbers(
            String parentEntityUuid,
            String parentEntityType,
            String phone1AreaCode,
            String phone1CityCode,
            String phone1LocalNumber,
            String phone1Extension,
            String phone2AreaCode,
            String phone2CityCode,
            String phone2LocalNumber,
            String phone2Extension) {
        
        List<PhoneNumberEntity> phoneNumbers = new ArrayList<>();
        
        // Create primary phone if any field is provided
        if (hasPhoneData(phone1AreaCode, phone1CityCode, phone1LocalNumber, phone1Extension)) {
            PhoneNumberEntity primaryPhone = new PhoneNumberEntity();
            primaryPhone.setId(UUID.randomUUID());
            primaryPhone.setParentEntityUuid(parentEntityUuid);
            primaryPhone.setParentEntityType(parentEntityType);
            primaryPhone.setPhoneType(PhoneNumberEntity.PhoneType.PRIMARY);
            primaryPhone.setAreaCode(phone1AreaCode);
            primaryPhone.setCityCode(phone1CityCode);
            primaryPhone.setLocalNumber(phone1LocalNumber);
            primaryPhone.setExtension(phone1Extension);
            phoneNumbers.add(primaryPhone);
        }
        
        // Create secondary phone if any field is provided
        if (hasPhoneData(phone2AreaCode, phone2CityCode, phone2LocalNumber, phone2Extension)) {
            PhoneNumberEntity secondaryPhone = new PhoneNumberEntity();
            secondaryPhone.setId(UUID.randomUUID());
            secondaryPhone.setParentEntityUuid(parentEntityUuid);
            secondaryPhone.setParentEntityType(parentEntityType);
            secondaryPhone.setPhoneType(PhoneNumberEntity.PhoneType.SECONDARY);
            secondaryPhone.setAreaCode(phone2AreaCode);
            secondaryPhone.setCityCode(phone2CityCode);
            secondaryPhone.setLocalNumber(phone2LocalNumber);
            secondaryPhone.setExtension(phone2Extension);
            phoneNumbers.add(secondaryPhone);
        }
        
        return phoneNumbers;
    }

    /**
     * Updates existing phone number entities for a parent entity.
     * Removes existing phone numbers and creates new ones based on provided data.
     * 
     * @param existingPhoneNumbers Current phone number entities
     * @param parentEntityUuid UUID of the parent entity
     * @param parentEntityType Type of the parent entity
     * @param phone1AreaCode Primary phone area code
     * @param phone1CityCode Primary phone city code
     * @param phone1LocalNumber Primary phone local number
     * @param phone1Extension Primary phone extension
     * @param phone2AreaCode Secondary phone area code (optional)
     * @param phone2CityCode Secondary phone city code (optional)
     * @param phone2LocalNumber Secondary phone local number (optional)
     * @param phone2Extension Secondary phone extension (optional)
     * @return Updated list of PhoneNumberEntity objects
     */
    public List<PhoneNumberEntity> updatePhoneNumbers(
            List<PhoneNumberEntity> existingPhoneNumbers,
            String parentEntityUuid,
            String parentEntityType,
            String phone1AreaCode,
            String phone1CityCode,
            String phone1LocalNumber,
            String phone1Extension,
            String phone2AreaCode,
            String phone2CityCode,
            String phone2LocalNumber,
            String phone2Extension) {
        
        // Clear existing phone numbers
        if (existingPhoneNumbers != null) {
            existingPhoneNumbers.clear();
        }
        
        // Create new phone numbers
        return createPhoneNumbers(
            parentEntityUuid, parentEntityType,
            phone1AreaCode, phone1CityCode, phone1LocalNumber, phone1Extension,
            phone2AreaCode, phone2CityCode, phone2LocalNumber, phone2Extension
        );
    }

    /**
     * Extracts a phone number by type from a list of phone number entities.
     * 
     * @param phoneNumbers List of phone number entities
     * @param phoneType Type of phone to extract
     * @return PhoneNumberEntity of the specified type, or null if not found
     */
    public PhoneNumberEntity getPhoneByType(List<PhoneNumberEntity> phoneNumbers, PhoneNumberEntity.PhoneType phoneType) {
        if (phoneNumbers == null) {
            return null;
        }
        
        return phoneNumbers.stream()
            .filter(phone -> phone.getPhoneType() == phoneType)
            .findFirst()
            .orElse(null);
    }

    /**
     * Gets the primary phone number from a list of phone number entities.
     * 
     * @param phoneNumbers List of phone number entities
     * @return Primary PhoneNumberEntity, or null if not found
     */
    public PhoneNumberEntity getPrimaryPhone(List<PhoneNumberEntity> phoneNumbers) {
        return getPhoneByType(phoneNumbers, PhoneNumberEntity.PhoneType.PRIMARY);
    }

    /**
     * Gets the secondary phone number from a list of phone number entities.
     * 
     * @param phoneNumbers List of phone number entities
     * @return Secondary PhoneNumberEntity, or null if not found
     */
    public PhoneNumberEntity getSecondaryPhone(List<PhoneNumberEntity> phoneNumbers) {
        return getPhoneByType(phoneNumbers, PhoneNumberEntity.PhoneType.SECONDARY);
    }

    /**
     * Checks if any phone data is provided.
     * 
     * @param areaCode Area code
     * @param cityCode City code
     * @param localNumber Local number
     * @param extension Extension
     * @return true if any field has data, false otherwise
     */
    private boolean hasPhoneData(String areaCode, String cityCode, String localNumber, String extension) {
        return (areaCode != null && !areaCode.trim().isEmpty()) ||
               (cityCode != null && !cityCode.trim().isEmpty()) ||
               (localNumber != null && !localNumber.trim().isEmpty()) ||
               (extension != null && !extension.trim().isEmpty());
    }
}