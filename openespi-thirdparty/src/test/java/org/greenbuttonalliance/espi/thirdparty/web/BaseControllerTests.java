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

//import org.greenbuttonalliance.espi.common.domain.RetailCustomer;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.security.core.Authentication;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@Disabled //todo - JT commenting out missing classes
public class BaseControllerTests {

	//private RetailCustomer retailCustomer;
	private Authentication principal;
	private BaseController controller;

	@Before
	public void setUp() throws Exception {
//		retailCustomer = new RetailCustomer();
//		principal = mock(Authentication.class);
//		when(principal.getPrincipal()).thenReturn(retailCustomer);

		controller = new BaseController();
	}

	@Test
	public void currentCustomer() throws Exception {
		//assertEquals(retailCustomer, controller.currentCustomer(principal));
	}

	@Test
	public void currentCustomer_withoutAPrincipal() throws Exception {
		//assertThat(controller.currentCustomer(null), is(nullValue()));
	}

	@Test
	public void isUserCustodian() {
		//retailCustomer.setRole(RetailCustomer.ROLE_CUSTODIAN);

	//	assertThat(controller.isUserCustodian(principal), is(true));
	}

	@Test
	public void isUserCustodian_withDefaultRole() {
	//	assertThat(controller.isUserCustodian(principal), is(false));
	//	assertThat(controller.isUserUserRole(principal), is(true));
	}

	@Test
	public void isUserUserRole() {
		//retailCustomer.setRole(RetailCustomer.ROLE_USER);

		//assertThat(controller.isUserUserRole(principal), is(true));
	}

	@Test
	public void isUserUserRole_givenNullPrincipal() {
		assertThat(controller.isUserUserRole(null), is(false));
	}
}
