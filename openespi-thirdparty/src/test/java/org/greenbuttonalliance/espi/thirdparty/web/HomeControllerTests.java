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

import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.service.RetailCustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeControllerTests {

	@Mock
	private RetailCustomerService retailCustomerService;

	@InjectMocks
	private HomeController controller;

	private MockHttpServletRequest request;
	private Authentication authentication;

	@BeforeEach
	void setup() {
		request = new MockHttpServletRequest();
		authentication = new UsernamePasswordAuthenticationToken("alice", "n/a");
	}

	@Test
	void index_whenCustodian_redirectsToCustodianHome() {
		request.addUserRole("ROLE_CUSTODIAN");

		assertThat(controller.index(request, authentication)).isEqualTo("redirect:/custodian/home");
		verifyNoInteractions(retailCustomerService);
	}

	@Test
	void index_whenUserWithKnownCustomer_redirectsToRetailCustomerHome() {
		request.addUserRole("ROLE_USER");
		RetailCustomerEntity customer = new RetailCustomerEntity();
		customer.setId(42L);
		when(retailCustomerService.findByUsername("alice")).thenReturn(customer);

		assertThat(controller.index(request, authentication)).isEqualTo("redirect:/RetailCustomer/42/home");
	}

	@Test
	void index_whenUserWithUnknownCustomer_displaysHomeView() {
		request.addUserRole("ROLE_USER");
		when(retailCustomerService.findByUsername("alice")).thenReturn(null);

		assertThat(controller.index(request, authentication)).isEqualTo("home");
	}

	@Test
	void index_whenUserPrincipalIsNotAnAuthentication_displaysHomeView() {
		request.addUserRole("ROLE_USER");
		Principal plainPrincipal = () -> "alice";

		assertThat(controller.index(request, plainPrincipal)).isEqualTo("home");
		verify(retailCustomerService, never()).findByUsername(anyString());
	}

	@Test
	void index_whenAnonymous_displaysHomeView() {
		assertThat(controller.index(request, null)).isEqualTo("home");
		verifyNoInteractions(retailCustomerService);
	}

	@Test
	void home_delegatesToIndex() {
		request.addUserRole("ROLE_CUSTODIAN");

		assertThat(controller.home(request, authentication)).isEqualTo("redirect:/custodian/home");
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
