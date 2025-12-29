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

import jakarta.xml.bind.JAXBException;
import org.greenbuttonalliance.espi.common.service.UsagePointService;
import org.greenbuttonalliance.espi.common.service.impl.UsagePointServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ModelMap;

import static org.mockito.Mockito.mock;

public class UsagePointControllerTests {

	private UsagePointController controller;
	private UsagePointService service;
//	private ResourceServiceImpl resourceService;
	private Authentication authentication;
//	private RetailCustomer retailCustomer;

	@BeforeEach
	public void setup() {
		controller = new UsagePointController();
		service = mock(UsagePointServiceImpl.class);
//		resourceService = mock(ResourceServiceImpl.class);
//		controller.setResourceService(resourceService);
//		authentication = mock(Authentication.class);
//		retailCustomer = EspiFactory.newRetailCustomer();
//		when(authentication.getPrincipal()).thenReturn(retailCustomer);
	}

	@Test
	@Disabled
	public void index_displaysIndexView() throws Exception {
//		when(resourceService.findAllIds(UsagePoint.class)).thenReturn(
//				new ArrayList<Long>());
//		assertEquals("/usagepoints/index",
//				controller.index(mock(ModelMap.class), authentication));
	}

	@Test
	@Disabled
	public void index_findsUsagePointsForLoggedInCustomer()
			throws JAXBException {
		controller.index(mock(ModelMap.class), authentication);
//		verify(resourceService).findAllIdsByXPath(1L, UsagePoint.class).equals(
//				null);

	}

	@Test
	@Disabled
	public void show_displaysShowView() throws Exception {
//		when(resourceService.findById(anyLong(), UsagePoint.class)).thenReturn(
//				EspiFactory.newUsagePoint());
//		assertEquals("/usagepoints/show",
//				controller.show(1L, 1L, mock(ModelMap.class)));
	}

	@Test
	@Disabled
	public void show_findsTheUsagePointByUUID() throws Exception {
//		UsagePoint usagePoint = Factory.newUsagePoint();
//		String hashedId = "hashedId";
//		when(service.findByHashedId(hashedId)).thenReturn(usagePoint);
//
//		controller.show(1L, 1L, mock(ModelMap.class));
//		verify(resourceService).findById(retailCustomer.getId(),
//				UsagePoint.class);
	}
}
