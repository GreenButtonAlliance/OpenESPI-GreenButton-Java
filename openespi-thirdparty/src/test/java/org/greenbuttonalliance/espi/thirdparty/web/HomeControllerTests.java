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

package org.greenbuttonalliance.espi.thirdparty.web;

import org.greenbuttonalliance.espi.common.repositories.usage.AuthorizationRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.RetailCustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.ui.ExtendedModelMap;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HomeControllerTests {

	private final RetailCustomerRepository retailCustomerRepository = mock(RetailCustomerRepository.class);

	private final HomeController controller = new HomeController(retailCustomerRepository,
			mock(AuthorizationRepository.class), mock(ClientRegistrationRepository.class));

	private MockHttpServletRequest request;

	private ExtendedModelMap model;

	@BeforeEach
	void setup() {
		request = new MockHttpServletRequest();
		model = new ExtendedModelMap();
	}

	@Test
	void index_whenCustodian_redirectsToCustodianHome() {
		request.addUserRole("ROLE_CUSTODIAN");

		assertThat(controller.index(request, model)).isEqualTo("redirect:/custodian/home");
	}

	@Test
	void index_whenCustomer_displaysHomeViewWithTheAuthorizationCards() {
		request.addUserRole("ROLE_USER");
		request.setUserPrincipal(new TestingAuthenticationToken("customer", null, "ROLE_USER"));
		when(retailCustomerRepository.findByUsername("customer")).thenReturn(Optional.empty());

		assertThat(controller.index(request, model)).isEqualTo("home");
		assertThat(model).containsKey("authorizations");
	}

	@Test
	void index_whenAnonymous_displaysHomeView() {
		assertThat(controller.index(request, model)).isEqualTo("home");
		assertThat(model).doesNotContainKey("authorizations");
	}

	@Test
	void home_delegatesToIndex() {
		request.addUserRole("ROLE_CUSTODIAN");

		assertThat(controller.home(request, model)).isEqualTo("redirect:/custodian/home");
	}

	@Test
	void termsOfService_displaysTermsOfServiceView() {
		assertThat(controller.termsOfService()).isEqualTo("/TermsOfService");
	}

	@Test
	void usagePolicy_displaysUsagePolicyView() {
		assertThat(controller.usagePolicy()).isEqualTo("/UsagePolicy");
	}
}
