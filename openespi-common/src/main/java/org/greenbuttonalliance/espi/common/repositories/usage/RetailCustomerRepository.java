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

/**
 * Modern Spring Data JPA repository for RetailCustomer entities.
 * RetailCustomer is an application-specific correlation table (not part of ESPI standard).
 * Replaces the legacy RetailCustomerRepositoryImpl with modern Spring Data patterns.
 */
@Repository
public interface RetailCustomerRepository extends JpaRepository<RetailCustomerEntity, Long> {

    // JpaRepository provides: save(), findById(), findAll(), deleteById(), etc.

    /**
     * Find retail customer by username (indexed).
     */
    Optional<RetailCustomerEntity> findByUsername(String username);

    /**
     * Find retail customers by role.
     */
    List<RetailCustomerEntity> findByRole(String role);

    /**
     * Find enabled retail customers.
     */
    List<RetailCustomerEntity> findByEnabledTrue();

    /**
     * Find retail customers by email.
     */
    Optional<RetailCustomerEntity> findByEmail(String email);

    /**
     * Find retail customers by first and last name.
     */
    List<RetailCustomerEntity> findByFirstNameAndLastName(String firstName, String lastName);

    /**
     * Check if username exists.
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists.
     */
    boolean existsByEmail(String email);

    /**
     * Find all retail customer IDs (performance optimized - selects only IDs).
     */
    @Query("SELECT rc.id FROM RetailCustomerEntity rc")
    List<Long> findAllIds();

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
    List<RetailCustomerEntity> findByAccountLockedTrue();
}