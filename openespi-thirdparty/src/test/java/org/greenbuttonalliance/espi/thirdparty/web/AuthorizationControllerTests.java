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

//import org.greenbuttonalliance.espi.common.domain.AccessToken;
//import org.greenbuttonalliance.espi.common.domain.ApplicationInformation;
//import org.greenbuttonalliance.espi.common.domain.Authorization;
//import org.greenbuttonalliance.espi.common.domain.RetailCustomer;

//missing a bunch of classes, commenting out for now
public class AuthorizationControllerTests {
	private final String CODE = "code";

//	private AuthorizationController controller;
//	private ClientRestTemplate restTemplate;
//	private Authentication principal;
//	private AuthorizationService service;
//	private RetailCustomer retailCustomer;
//	private ApplicationInformation applicationInformation;
//	private Authorization authorization;
//
//	@BeforeEach
//	public void before() {
//		controller = new AuthorizationController();
//
//		service = mock(AuthorizationService.class);
//		controller.setAuthorizationService(service);
//
//		restTemplate = mock(ClientRestTemplate.class);
//		ClientRestTemplateFactory factory = mock(ClientRestTemplateFactory.class);
//		when(factory.newClientRestTemplate(anyString(), anyString()))
//				.thenReturn(restTemplate);
//		controller.setClientRestTemplateFactory(factory);
//
//		retailCustomer = EspiFactory.newRetailCustomer();
//		principal = mock(Authentication.class);
//		when(principal.getPrincipal()).thenReturn(retailCustomer);
//
//		applicationInformation = EspiFactory.newApplicationInformation();
//		authorization = EspiFactory.newAuthorization(retailCustomer,
//				applicationInformation);
//		when(service.findByState(authorization.getState())).thenReturn(
//				authorization);
//		when(restTemplate.getForObject(anyString(), eq(AccessToken.class)))
//				.thenReturn(new AccessToken());
//	}
//
//	@Test
//	@Disabled
//	public void authorization_fetchesToken() throws Exception {
//		String url = String.format(
//				"%s?redirect_uri=%s&code=%s&grant_type=authorization_code",
//				applicationInformation.getAuthorizationServerTokenEndpoint(),
//				applicationInformation.getRedirectUri(), CODE);
//
//		controller.authorization(CODE, authorization.getState(),
//				new ModelMap(), principal, url, url, url);
//
//		verify(restTemplate).getForObject(eq(url), eq(AccessToken.class));
//	}
//
//	@Test
//	@Disabled
//	public void authorization_updatesAuthorization() throws Exception {
//		controller.authorization(CODE, authorization.getState(),
//				new ModelMap(), principal, CODE, CODE, CODE);
//
//		verify(service).merge(any(Authorization.class));
//	}
//
//	@Test
//	@Disabled
//	public void authorization_returnsAuthorizationList() throws Exception {
//		List<Authorization> authorizations = new ArrayList<>();
//		authorizations.add(new Authorization());
//		when(service.findAllByRetailCustomerId(anyLong())).thenReturn(
//				authorizations);
//		ModelMap model = new ModelMap();
//
//		controller.authorization(CODE, authorization.getState(), model,
//				principal, CODE, CODE, CODE);
//
//		assertEquals(authorizations, model.get("authorizationList"));
//	}
//
//	@Test
//	public void index_returnsAuthorizationList() {
//		List<Authorization> authorizations = new ArrayList<>();
//		authorizations.add(new Authorization());
//		when(service.findAllByRetailCustomerId(anyLong())).thenReturn(
//				authorizations);
//		ModelMap model = new ModelMap();
//
//		controller.index(model, principal);
//
//		assertEquals(authorizations, model.get("authorizationList"));
//	}
//
//	@Test
//	public void index_displaysIndexView() throws Exception {
//		assertEquals("/RetailCustomer/AuthorizationList/index",
//				controller.index(new ModelMap(), principal));
//	}
}
