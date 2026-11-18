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

package org.greenbuttonalliance.espi.thirdparty.repository.impl;

import org.greenbuttonalliance.espi.common.domain.usage.AuthorizationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.dto.usage.UsagePointDto;
import org.greenbuttonalliance.espi.common.mapper.usage.UsagePointMapper;
import org.greenbuttonalliance.espi.common.service.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The Third Party reads a Data Custodian's UsagePoint feed (#197): each resource inside atom:content, an
 * empty feed, and entries that are not UsagePoints. Replaces the two {@code @Disabled} tests of the former
 * {@code UsagePointRESTRepositoryIntegrationTest}; the WebClient is real, with a stubbed exchange.
 */
class UsagePointRESTRepositoryImplTests {

	private static final String RESOURCE_URI = "https://dc.example/DataCustodian/espi/1_1/resource/Batch/Subscription/1";
	private static final UUID USAGE_POINT_ID = UUID.fromString("07171718-b165-522a-b413-11d9eeb1ca43");

	private static final String FEED = """
			<?xml version="1.0" encoding="UTF-8"?>
			<feed xmlns="http://www.w3.org/2005/Atom" xmlns:espi="http://naesb.org/espi">
			   <id>urn:uuid:3f1c2b2e-9d3a-5b1e-8f0e-000000000001</id>
			   <title>Green Button Usage Feed</title>
			   <updated>2026-01-01T00:00:00Z</updated>
			   <link rel="self" href="https://dc.example/DataCustodian/espi/1_1/resource/Batch/Subscription/1" type="espi-entry/Subscription"/>
			   <entry>
			      <id>urn:uuid:07171718-b165-522a-b413-11d9eeb1ca43</id>
			      <link rel="self" href="https://dc.example/DataCustodian/espi/1_1/resource/Subscription/1/UsagePoint/1" type="espi-entry/UsagePoint"/>
			      <link rel="up" href="https://dc.example/DataCustodian/espi/1_1/resource/Subscription/1/UsagePoint" type="espi-feed/UsagePoint"/>
			      <title>UsagePoint</title>
			      <content>
			         <espi:UsagePoint>
			            <espi:status>1</espi:status>
			         </espi:UsagePoint>
			      </content>
			      <published>2026-01-01T00:00:00Z</published>
			      <updated>2026-01-01T00:00:00Z</updated>
			   </entry>
			   <entry>
			      <id>urn:uuid:41a6bd77-3d03-59c4-9ab2-65f2cb09b1ed</id>
			      <link rel="self" href="https://dc.example/DataCustodian/espi/1_1/resource/Subscription/1/UsagePoint/1/MeterReading/2" type="espi-entry/MeterReading"/>
			      <link rel="up" href="https://dc.example/DataCustodian/espi/1_1/resource/Subscription/1/UsagePoint/1/MeterReading" type="espi-feed/MeterReading"/>
			      <title>MeterReading</title>
			      <content>
			         <espi:MeterReading/>
			      </content>
			      <published>2026-01-01T00:00:00Z</published>
			      <updated>2026-01-01T00:00:00Z</updated>
			   </entry>
			</feed>
			""";

	private static final String EMPTY_FEED = """
			<?xml version="1.0" encoding="UTF-8"?>
			<feed xmlns="http://www.w3.org/2005/Atom">
			   <id>urn:uuid:3f1c2b2e-9d3a-5b1e-8f0e-000000000001</id>
			   <title>Green Button Usage Feed</title>
			   <updated>2026-01-01T00:00:00Z</updated>
			</feed>
			""";

	private final AuthorizationService authorizationService = mock(AuthorizationService.class);
	private final UsagePointMapper usagePointMapper = mock(UsagePointMapper.class);
	private final UsagePointRESTRepositoryImpl repository = new UsagePointRESTRepositoryImpl();
	private final AtomicReference<String> requested = new AtomicReference<>();
	private final AtomicReference<String> authorizationHeader = new AtomicReference<>();

	@BeforeEach
	void setUp() {
		AuthorizationEntity authorization = new AuthorizationEntity();
		authorization.setResourceURI(RESOURCE_URI);
		authorization.setAccessToken("customer-token");
		when(authorizationService.findAllByRetailCustomerId(any(Long.class))).thenReturn(List.of(authorization));
		// Like the real UsagePointMapper: only espi.xsd fields, no id (the id comes from the entry's atom:id).
		when(usagePointMapper.toEntity(any(UsagePointDto.class))).thenAnswer(invocation -> {
			UsagePointEntity entity = new UsagePointEntity();
			entity.setStatus(((UsagePointDto) invocation.getArgument(0)).getStatus());
			return entity;
		});
		repository.setAuthorizationService(authorizationService);
		repository.setUsagePointMapper(usagePointMapper);
	}

	private void dataCustodianAnswers(String body) {
		repository.setWebClient(WebClient.builder().exchangeFunction(request -> {
			requested.set(request.url().toString());
			authorizationHeader.set(request.headers().getFirst(HttpHeaders.AUTHORIZATION));
			return Mono.just(ClientResponse.create(HttpStatus.OK)
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_ATOM_XML_VALUE)
					.body(body)
					.build());
		}).build());
	}

	@Test
	@DisplayName("reads the UsagePoint from atom:content, with the customer's token, and skips other resources")
	void reads_usage_points_from_content() throws Exception {
		dataCustodianAnswers(FEED);

		List<UsagePointEntity> usagePoints = repository.findAllByRetailCustomerId(1L);

		assertThat(requested.get()).isEqualTo(RESOURCE_URI);
		assertThat(authorizationHeader.get()).isEqualTo("Bearer customer-token");
		assertThat(usagePoints).singleElement()
				.extracting(UsagePointEntity::getId, UsagePointEntity::getStatus)
				.containsExactly(USAGE_POINT_ID, (short) 1);
	}

	@Test
	@DisplayName("the entry id gives the UsagePoint id: urn:uuid prefix in any case, or none; anything else is no id")
	void entry_uuid() {
		assertThat(List.of("urn:uuid:" + USAGE_POINT_ID, " URN:UUID:" + USAGE_POINT_ID + " ", USAGE_POINT_ID.toString()))
				.map(UsagePointRESTRepositoryImpl::entryUuid)
				.containsOnly(USAGE_POINT_ID);
		assertThat(java.util.Arrays.asList(null, "urn:uuid:not-a-uuid", "tag:example,2026:1"))
				.map(UsagePointRESTRepositoryImpl::entryUuid)
				.containsOnlyNulls();
	}

	@Test
	@DisplayName("an entry id that is not a urn:uuid gives a UsagePoint with no id, and findByHashedId answers null")
	void entry_without_uuid() throws Exception {
		dataCustodianAnswers(FEED.replace("urn:uuid:" + USAGE_POINT_ID, "tag:example,2026:1"));

		assertThat(repository.findByHashedId(1L, USAGE_POINT_ID.toString())).isNull();
	}

	@Test
	@DisplayName("an empty feed gives no UsagePoints, not a NullPointerException")
	void empty_feed() throws Exception {
		dataCustodianAnswers(EMPTY_FEED);

		assertThat(repository.findAllByRetailCustomerId(1L)).isEmpty();
	}

	@Test
	@DisplayName("finds a UsagePoint of the feed by its id, and answers null for an unknown id")
	void find_by_hashed_id() throws Exception {
		dataCustodianAnswers(FEED);

		assertThat(repository.findByHashedId(1L, USAGE_POINT_ID.toString()))
				.isNotNull()
				.extracting(UsagePointEntity::getId)
				.isEqualTo(USAGE_POINT_ID);
		assertThat(repository.findByHashedId(1L, UUID.randomUUID().toString())).isNull();
	}
}
