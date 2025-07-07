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

import org.greenbuttonalliance.espi.common.domain.usage.ServiceDeliveryPointEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceDeliveryPointRepository extends JpaRepository<ServiceDeliveryPointEntity, UUID> {

	// JpaRepository provides: save(), findById(), findAll(), deleteById(), etc.

	@Modifying
	@Transactional
	@Query("DELETE FROM ServiceDeliveryPointEntity s WHERE s.id = :id")
	void deleteById(@Param("id") UUID id);

	@Query("SELECT s.id FROM ServiceDeliveryPointEntity s")
	List<UUID> findAllIds();

	@Query("SELECT s FROM ServiceDeliveryPointEntity s WHERE s.name = :name")
	List<ServiceDeliveryPointEntity> findByName(@Param("name") String name);

	@Query("SELECT s FROM ServiceDeliveryPointEntity s WHERE s.tariffProfile = :tariffProfile")
	List<ServiceDeliveryPointEntity> findByTariffProfile(@Param("tariffProfile") String tariffProfile);

	@Query("SELECT s FROM ServiceDeliveryPointEntity s WHERE s.customerAgreement = :customerAgreement")
	List<ServiceDeliveryPointEntity> findByCustomerAgreement(@Param("customerAgreement") String customerAgreement);

	@Query("SELECT s FROM ServiceDeliveryPointEntity s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :searchText, '%'))")
	List<ServiceDeliveryPointEntity> findByNameContaining(@Param("searchText") String searchText);

	// Additional utility methods
	@Query("SELECT COUNT(s) FROM ServiceDeliveryPointEntity s WHERE s.tariffProfile = :tariffProfile")
	Long countByTariffProfile(@Param("tariffProfile") String tariffProfile);

}