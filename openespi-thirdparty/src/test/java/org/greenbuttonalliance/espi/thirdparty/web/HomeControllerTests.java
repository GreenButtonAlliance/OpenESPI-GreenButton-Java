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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ContextConfiguration;

@Disabled //todo - JT, commenting out missing classes
//@RunWith(SpringJUnit4ClassRunner.class)
//@WebAppConfiguration
@ContextConfiguration("/spring/test-context.xml")
public class HomeControllerTests {

	@Autowired
	protected HomeController controller;
//	private RetailCustomer customer;
	private Authentication principal;

	@Before
	public void setup() {
//		customer = new RetailCustomer();
//		customer.setId(99L);
//
//		principal = mock(Authentication.class);
//		when(principal.getPrincipal()).thenReturn(customer);
	}

	@Test
	public void index_whenNotLoggedIn_displaysHomeView() throws Exception {
		//assertEquals("home", controller.index(null));
	}

	@Test
	public void index_whenLoggedIn_redirectsToRetailCustomHome()
			throws Exception {
//		customer.setRole(RetailCustomer.ROLE_USER);
//
//		assertEquals("redirect:/RetailCustomer/" + customer.getId() + "/home",
//				controller.index(principal));
	}

	@Test
	public void home_whenNotLoggedIn_displaysHomeView() throws Exception {
		//assertEquals("home", controller.home(null));
	}

	@Test
	public void home_whenLoggedInAsCustomer_redirectsToRetailCustomerHome()
			throws Exception {
//		customer.setRole(RetailCustomer.ROLE_USER);
//
//		assertEquals("redirect:/RetailCustomer/" + customer.getId() + "/home",
//				controller.home(principal));
	}

	@Test
	public void home_whenLoggedInAsCustodian_redirectsToCustodianHome()
			throws Exception {
//		customer.setRole(RetailCustomer.ROLE_CUSTODIAN);
//
//		assertThat(controller.home(principal), is("redirect:/custodian/home"));
	}

	@Test
	public void termsOfService_displaysTermsOfServiceView() {
//		assertEquals("/TermsOfService", controller.termsOfService());
	}

	@Test
	public void usagePolicy_displaysUsagePolicyView() {
		//assertEquals("/UsagePolicy", controller.usagePolicy());
	}
}
