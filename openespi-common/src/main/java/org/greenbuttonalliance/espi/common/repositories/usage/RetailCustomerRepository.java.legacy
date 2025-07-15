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
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RetailCustomerRepository extends JpaRepository<RetailCustomerEntity, UUID> {

	// JpaRepository provides: save(), findAll(), findById(), deleteById(), etc.

	@Modifying
	@Transactional
	@Query("DELETE FROM RetailCustomerEntity r WHERE r.id = :id")
	void deleteById(@Param("id") UUID retailCustomerId);

	@Query("SELECT r FROM RetailCustomerEntity r WHERE r.id = :id")
	Optional<RetailCustomerEntity> findByIdString(@Param("id") String retailCustomerId);

	@Query("SELECT r FROM RetailCustomerEntity r WHERE r.username = :username")
	Optional<UserDetails> findByUsername(@Param("username") String userName);

	@Query("SELECT r.id FROM RetailCustomerEntity r")
	List<UUID> findAllIds();

	@Query("SELECT DISTINCT r.id FROM RetailCustomerEntity r")
	List<UUID> findAllIdsByXpath0();

	@Query("SELECT DISTINCT r.id FROM RetailCustomerEntity r WHERE r.id = :o1Id")
	Optional<UUID> findIdsByXpath(@Param("o1Id") UUID o1Id);

}
