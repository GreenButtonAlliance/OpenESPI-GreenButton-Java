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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The home page controller called directly, without a Spring context. The rendering of the per-row links of
 * "Your authorizations" (#89) is in {@link HomeControllerRenderingTest}.
 */
class HomeControllerTests {

	private final AuthorizationRepository authorizationRepository = mock(AuthorizationRepository.class);

	private final HomeController controller = new HomeController(authorizationRepository,
			mock(ClientRegistrationRepository.class));

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
		when(authorizationRepository
				.findAllByRetailCustomer_UsernameAndClientRegistrationIdIsNotNullOrderByUpdatedDesc("customer"))
				.thenReturn(List.of());

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
