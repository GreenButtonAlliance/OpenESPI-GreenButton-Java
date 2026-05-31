/*
 * Copyright 2025 Green Button Alliance, Inc.
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

package org.greenbuttonalliance.espi.common.service.impl;

import org.greenbuttonalliance.espi.common.domain.usage.ApplicationInformationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.AuthorizationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.ApplicationInformationRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.AuthorizationRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.RetailCustomerRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository;
import org.greenbuttonalliance.espi.common.service.EspiIdGeneratorService;
import org.greenbuttonalliance.espi.common.service.SubscriptionProvisioningService.SubscriptionProvisionCommand;
import org.greenbuttonalliance.espi.common.service.SubscriptionProvisioningService.SubscriptionProvisionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SubscriptionProvisioningServiceImpl}. Verifies aggregate construction,
 * PII vs energy branch logic, and validation rejections against the PR B1 N:1 model.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionProvisioningServiceImplTest {

	private static final String BASE_URI = "https://dc.example.com/DataCustodian/espi/1_1/resource";
	private static final String CLIENT_ID = "test-tp";
	private static final String CORRELATION_ID = "corr-123";
	private static final Long CUSTOMER_ID = 42L;
	private static final String PII_CUSTOMER_URI = BASE_URI + "/RetailCustomer/42/Customer/xyz";

	@Mock private AuthorizationRepository authorizationRepository;
	@Mock private ApplicationInformationRepository applicationInformationRepository;
	@Mock private RetailCustomerRepository retailCustomerRepository;
	@Mock private UsagePointRepository usagePointRepository;
	@Mock private EspiIdGeneratorService idGeneratorService;

	@InjectMocks
	private SubscriptionProvisioningServiceImpl service;

	private RetailCustomerEntity customer;
	private ApplicationInformationEntity application;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(service, "resourceBaseUri", BASE_URI);

		customer = new RetailCustomerEntity();
		customer.setId(CUSTOMER_ID);

		application = new ApplicationInformationEntity();
		application.setId(UUID.randomUUID());
		application.setClientId(CLIENT_ID);

		when(idGeneratorService.generateSubscriptionId(anyString(), anyString()))
				.thenAnswer(invocation -> UUID.randomUUID());
		when(authorizationRepository.save(any(AuthorizationEntity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void energyOnlyGrant_createsResourceSubscriptionAndNoPiiSubscription() {
		UUID upId = stubUsagePoint(customer);
		when(retailCustomerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
		when(applicationInformationRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(application));

		SubscriptionProvisionResult result = service.provisionFromGrant(new SubscriptionProvisionCommand(
				CORRELATION_ID, CLIENT_ID, "FB=4_5_15", CUSTOMER_ID, List.of(upId), null));

		assertThat(result.resourceSubscriptionId()).isNotNull();
		assertThat(result.customerSubscriptionId()).isNull();
		assertThat(result.resourceUri()).startsWith(BASE_URI + "/Subscription/");
		assertThat(result.authorizationUri()).startsWith(BASE_URI + "/Authorization/");
		assertThat(result.customerResourceUri()).isNull();

		AuthorizationEntity saved = captureSavedAuthorization();
		assertThat(saved).extracting(
						AuthorizationEntity::getScope,
						AuthorizationEntity::getThirdParty,
						AuthorizationEntity::getStatus,
						AuthorizationEntity::getResourceURI,
						AuthorizationEntity::getCustomerResourceURI)
				.containsExactly(
						"FB=4_5_15",
						CLIENT_ID,
						AuthorizationEntity.STATUS_ACTIVE,
						result.resourceUri(),
						null);
		assertThat(saved.getSubscriptions()).hasSize(1);
		assertThat(saved.getSubscriptions().get(0).getUsagePoints()).hasSize(1);
	}

	@Test
	void grantWithPiiScope_createsBothSubscriptions() {
		UUID upId = stubUsagePoint(customer);
		when(retailCustomerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
		when(applicationInformationRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(application));

		SubscriptionProvisionResult result = service.provisionFromGrant(new SubscriptionProvisionCommand(
				CORRELATION_ID, CLIENT_ID, "FB=4_5_15_54", CUSTOMER_ID, List.of(upId), PII_CUSTOMER_URI));

		assertThat(result.resourceSubscriptionId()).isNotNull();
		assertThat(result.customerSubscriptionId()).isNotNull();
		assertThat(result.customerResourceUri()).isEqualTo(PII_CUSTOMER_URI);

		AuthorizationEntity saved = captureSavedAuthorization();
		assertThat(saved.getCustomerResourceURI()).isEqualTo(PII_CUSTOMER_URI);
		assertThat(saved.getSubscriptions()).hasSize(2);
	}

	@Test
	void piiOnlyGrant_createsOnlyCustomerSubscription_noResourceUri() {
		when(retailCustomerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
		when(applicationInformationRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(application));

		SubscriptionProvisionResult result = service.provisionFromGrant(new SubscriptionProvisionCommand(
				CORRELATION_ID, CLIENT_ID, "FB=4_54", CUSTOMER_ID, List.of(), PII_CUSTOMER_URI));

		assertThat(result.resourceSubscriptionId()).isNull();
		assertThat(result.resourceUri()).isNull();
		assertThat(result.customerSubscriptionId()).isNotNull();
		assertThat(result.customerResourceUri()).isEqualTo(PII_CUSTOMER_URI);

		AuthorizationEntity saved = captureSavedAuthorization();
		assertThat(saved.getResourceURI()).isNull();
		assertThat(saved.getSubscriptions()).hasSize(1);
	}

	@Test
	void emptyGrant_isRejected() {
		when(retailCustomerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
		when(applicationInformationRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(application));

		assertThatThrownBy(() -> service.provisionFromGrant(new SubscriptionProvisionCommand(
				CORRELATION_ID, CLIENT_ID, "FB=4", CUSTOMER_ID, List.of(), null)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("at least one selected usage point OR a Customer/PII");
	}

	@Test
	void piiScopeWithoutCustomerUri_isRejected() {
		UUID upId = stubUsagePoint(customer);
		when(retailCustomerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
		when(applicationInformationRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(application));

		assertThatThrownBy(() -> service.provisionFromGrant(new SubscriptionProvisionCommand(
				CORRELATION_ID, CLIENT_ID, "FB=4_54", CUSTOMER_ID, List.of(upId), null)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("customer_resource_uri is required");
	}

	@Test
	void customerUriWithoutPiiScope_isRejected() {
		UUID upId = stubUsagePoint(customer);
		when(retailCustomerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
		when(applicationInformationRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(application));

		assertThatThrownBy(() -> service.provisionFromGrant(new SubscriptionProvisionCommand(
				CORRELATION_ID, CLIENT_ID, "FB=4_5_15", CUSTOMER_ID, List.of(upId), PII_CUSTOMER_URI)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("customer_resource_uri must be absent");
	}

	@Test
	void unknownClientId_isRejected() {
		when(applicationInformationRepository.findByClientId("ghost")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.provisionFromGrant(new SubscriptionProvisionCommand(
				CORRELATION_ID, "ghost", "FB=4_5_15", CUSTOMER_ID, List.of(UUID.randomUUID()), null)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Unknown client_id");
	}

	@Test
	void unknownRetailCustomer_isRejected() {
		when(applicationInformationRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(application));
		when(retailCustomerRepository.findById(999L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.provisionFromGrant(new SubscriptionProvisionCommand(
				CORRELATION_ID, CLIENT_ID, "FB=4_5_15", 999L, List.of(UUID.randomUUID()), null)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Unknown retail_customer_id");
	}

	@Test
	void usagePointBelongingToDifferentCustomer_isRejected() {
		UUID upId = UUID.randomUUID();
		RetailCustomerEntity other = new RetailCustomerEntity();
		other.setId(999L);
		UsagePointEntity foreign = new UsagePointEntity();
		foreign.setId(upId);
		foreign.setRetailCustomer(other);

		when(applicationInformationRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(application));
		when(retailCustomerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
		when(usagePointRepository.findById(upId)).thenReturn(Optional.of(foreign));

		assertThatThrownBy(() -> service.provisionFromGrant(new SubscriptionProvisionCommand(
				CORRELATION_ID, CLIENT_ID, "FB=4_5_15", CUSTOMER_ID, List.of(upId), null)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("does not belong to retail_customer_id");
	}

	@Test
	void unknownUsagePoint_isRejected() {
		UUID upId = UUID.randomUUID();
		when(applicationInformationRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(application));
		when(retailCustomerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
		when(usagePointRepository.findById(upId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.provisionFromGrant(new SubscriptionProvisionCommand(
				CORRELATION_ID, CLIENT_ID, "FB=4_5_15", CUSTOMER_ID, List.of(upId), null)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Unknown usage_point_id");
	}

	@Test
	void unparseableScope_isRejected() {
		when(applicationInformationRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(application));
		when(retailCustomerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

		assertThatThrownBy(() -> service.provisionFromGrant(new SubscriptionProvisionCommand(
				CORRELATION_ID, CLIENT_ID, "FB=not_a_number", CUSTOMER_ID, List.of(), null)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void blankRequiredFields_areRejected() {
		assertThatThrownBy(() -> service.provisionFromGrant(new SubscriptionProvisionCommand(
				CORRELATION_ID, "", "FB=4_5_15", CUSTOMER_ID, List.of(), null)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("client_id");
		assertThatThrownBy(() -> service.provisionFromGrant(new SubscriptionProvisionCommand(
				CORRELATION_ID, CLIENT_ID, "  ", CUSTOMER_ID, List.of(), null)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("granted_scope");
		assertThatThrownBy(() -> service.provisionFromGrant(new SubscriptionProvisionCommand(
				CORRELATION_ID, CLIENT_ID, "FB=4_5_15", null, List.of(), null)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("retail_customer_id");
	}

	private UUID stubUsagePoint(RetailCustomerEntity owner) {
		UUID upId = UUID.randomUUID();
		UsagePointEntity up = new UsagePointEntity();
		up.setId(upId);
		up.setRetailCustomer(owner);
		when(usagePointRepository.findById(upId)).thenReturn(Optional.of(up));
		return upId;
	}

	private AuthorizationEntity captureSavedAuthorization() {
		ArgumentCaptor<AuthorizationEntity> captor = ArgumentCaptor.forClass(AuthorizationEntity.class);
		org.mockito.Mockito.verify(authorizationRepository).save(captor.capture());
		return captor.getValue();
	}
}
