/*
 *
 *        Copyright (c) 2025 Green Button Alliance, Inc.
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BaseControllerTests {

	private RetailCustomerEntity retailCustomer;
	private Authentication principal;
	private BaseController controller;

	@BeforeEach
	void setUp() {
		retailCustomer = new RetailCustomerEntity();
		principal = mock(Authentication.class);
		when(principal.getPrincipal()).thenReturn(retailCustomer);

		controller = new BaseController();
	}

	@Test
	void currentCustomer_returnsAuthenticationPrincipal() {
		assertThat(controller.currentCustomer(principal)).isSameAs(retailCustomer);
	}

	@Test
	void currentCustomer_withoutAPrincipal_returnsNull() {
		assertThat(controller.currentCustomer(null)).isNull();
	}

	@Test
	void currentCustomer_whenPrincipalIsNotARetailCustomer_returnsNull() {
		when(principal.getPrincipal()).thenReturn("alice");

		assertThat(controller.currentCustomer(principal)).isNull();
	}

	@Test
	void currentCustomer_whenPrincipalIsNotAnAuthentication_returnsNull() {
		Principal plainPrincipal = () -> "alice";

		assertThat(controller.currentCustomer(plainPrincipal)).isNull();
	}

	@Test
	void custodianRole_isCustodianAndNotUser() {
		retailCustomer.setRole(RetailCustomerEntity.ROLE_CUSTODIAN);

		assertThat(new boolean[] { controller.isUserCustodian(principal), controller.isUserUserRole(principal) })
				.containsExactly(true, false);
	}

	@Test
	void defaultRole_isUserAndNotCustodian() {
		assertThat(new boolean[] { controller.isUserCustodian(principal), controller.isUserUserRole(principal) })
				.containsExactly(false, true);
	}

	@Test
	void adminRole_isNeitherCustodianNorUser() {
		retailCustomer.setRole(RetailCustomerEntity.ROLE_ADMIN);

		assertThat(new boolean[] { controller.isUserCustodian(principal), controller.isUserUserRole(principal) })
				.containsExactly(false, false);
	}

	@Test
	void nullPrincipal_hasNoRoles() {
		assertThat(new boolean[] { controller.isUserCustodian(null), controller.isUserUserRole(null) })
				.containsExactly(false, false);
	}
}
