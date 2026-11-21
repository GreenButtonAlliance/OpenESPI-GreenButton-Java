/*
 *
 *        Copyright (c) 2025-2026 Green Button Alliance, Inc.
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


package org.greenbuttonalliance.espi.thirdparty.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Saved {@code Batch/Subscription} fetches (#89), always read through their authorization. */
public interface SubscriptionFeedFetchRepository extends JpaRepository<SubscriptionFeedFetchEntity, UUID> {

	/** A fetch without its body (which can be tens of MiB): what the page's list of earlier fetches needs. */
	record FetchSummary(UUID id, Instant fetchedAt, int httpStatus, Instant publishedMin, Instant publishedMax) {
	}

	/** Newest first; loads no body. Load the one to show with {@link #findByIdAndAuthorizationId}. */
	List<FetchSummary> findAllByAuthorizationIdOrderByFetchedAtDesc(UUID authorizationId);

	Optional<SubscriptionFeedFetchEntity> findByIdAndAuthorizationId(UUID id, UUID authorizationId);
}
