/*
 * Copyright 2025-2026 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.greenbuttonalliance.espi.handoff;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Repository
public interface HandoffNonceRepository extends JpaRepository<HandoffNonceEntity, String> {

	/**
	 * Delete all nonces whose expiry is strictly before {@code cutoff}, in a single bulk
	 * {@code DELETE} statement, and return the number of rows removed.
	 *
	 * <p>A bulk JPQL delete is used deliberately: a derived {@code deleteBy...} method would load
	 * every matching entity into the persistence context and delete them one by one. Used by
	 * {@link HandoffNonceSweeper}.</p>
	 */
	@Transactional
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from HandoffNonceEntity n where n.expiresAt < :cutoff")
	int deleteByExpiresAtBefore(@Param("cutoff") Instant cutoff);
}
