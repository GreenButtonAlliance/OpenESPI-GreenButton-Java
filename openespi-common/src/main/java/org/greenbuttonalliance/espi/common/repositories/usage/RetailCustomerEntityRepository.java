/*
 *
 *         Copyright (c) 2025 Green Button Alliance, Inc.
 *
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package org.greenbuttonalliance.espi.common.repositories.usage;

import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Modern Spring Data JPA repository for RetailCustomer entities.
 * Replaces the legacy RetailCustomerRepositoryImpl with modern Spring Data patterns.
 */
@Repository
public interface RetailCustomerEntityRepository extends JpaRepository<RetailCustomerEntity, UUID> {

    // JpaRepository provides: save(), findById(), findAll(), deleteById(), etc.

    /**
     * Find retail customer by username.
     */
    Optional<RetailCustomerEntity> findByUsername(String username);

    /**
     * Find retail customers by role.
     */
    @Query("SELECT rc FROM RetailCustomerEntity rc WHERE rc.role = :role")
    List<RetailCustomerEntity> findByRole(@Param("role") String role);

    /**
     * Find enabled retail customers.
     */
    @Query("SELECT rc FROM RetailCustomerEntity rc WHERE rc.enabled = true")
    List<RetailCustomerEntity> findByEnabledTrue();

    /**
     * Find retail customers by email.
     */
    Optional<RetailCustomerEntity> findByEmail(String email);

    /**
     * Find retail customers by first and last name.
     */
    @Query("SELECT rc FROM RetailCustomerEntity rc WHERE rc.firstName = :firstName AND rc.lastName = :lastName")
    List<RetailCustomerEntity> findByFirstNameAndLastName(@Param("firstName") String firstName, @Param("lastName") String lastName);

    /**
     * Check if username exists.
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists.
     */
    boolean existsByEmail(String email);

    /**
     * Find all retail customer IDs.
     */
    @Query("SELECT rc.id FROM RetailCustomerEntity rc")
    List<UUID> findAllIds();

    /**
     * Find retail customers created after timestamp.
     */
    @Query("SELECT rc FROM RetailCustomerEntity rc WHERE rc.accountCreated > :timestamp")
    List<RetailCustomerEntity> findByAccountCreatedAfter(@Param("timestamp") Long timestamp);

    /**
     * Find retail customers with last login after timestamp.
     */
    @Query("SELECT rc FROM RetailCustomerEntity rc WHERE rc.lastLogin > :timestamp")
    List<RetailCustomerEntity> findByLastLoginAfter(@Param("timestamp") Long timestamp);

    /**
     * Find locked accounts.
     */
    @Query("SELECT rc FROM RetailCustomerEntity rc WHERE rc.accountLocked = true")
    List<RetailCustomerEntity> findLockedAccounts();
}