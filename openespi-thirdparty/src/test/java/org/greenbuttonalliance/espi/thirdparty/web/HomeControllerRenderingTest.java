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

import org.greenbuttonalliance.espi.common.domain.usage.AuthorizationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.AuthorizationRepository;
import org.greenbuttonalliance.espi.thirdparty.oauth.DataCustodianRegistrations;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The home page rendered through MockMvc with the customer's authorizations mocked, to check the per-row
 * links of "Your authorizations" (#89). The controller's own logic is in {@link HomeControllerTests}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HomeControllerRenderingTest {

	private static final UUID ACTIVE_ID = UUID.fromString("11111111-2222-5333-8444-555555555555");
	private static final UUID REVOKED_ID = UUID.fromString("66666666-7777-5888-9999-000000000000");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthorizationRepository authorizationRepository;

	private static AuthorizationEntity authorization(UUID id, String registrationId, boolean active) {
		RetailCustomerEntity customer = new RetailCustomerEntity();
		customer.setUsername("customer");
		AuthorizationEntity authorization = new AuthorizationEntity();
		authorization.setId(id);
		authorization.setRetailCustomer(customer);
		authorization.setClientRegistrationId(registrationId);
		authorization.setScope("FB=4_5_15;HistoryLength=604800");
		authorization.setResourceURI("http://localhost:8081/DataCustodian/espi/1_1/resource/Batch/Subscription/" + id);
		if (active) {
			authorization.activate();
		} else {
			authorization.revoke();
		}
		return authorization;
	}

	private String homeWith(AuthorizationEntity... authorizations) throws Exception {
		when(authorizationRepository
				.findAllByRetailCustomer_UsernameAndClientRegistrationIdIsNotNullOrderByUpdatedDesc("customer"))
				.thenReturn(List.of(authorizations));
		String html = mockMvc.perform(get("/").with(user("customer").roles("USER")))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		// only the "Your authorizations" table: the authorize cards above it link to /oauth2/authorize/... too
		int table = html.indexOf("id=\"authorizations\"");
		assertThat(table).as("authorizations table rendered").isNotNegative();
		return html.substring(table, html.indexOf("</table>", table));
	}

	@Test
	void activeRowWithResourceUriLinksToItsSubscriptionPage() throws Exception {
		assertThat(homeWith(authorization(ACTIVE_ID, DataCustodianRegistrations.DAILY, true)))
				.contains("href=\"/subscription/" + ACTIVE_ID + "\"", "View data")
				.doesNotContain("Authorize again");
	}

	@Test
	void revokedRowOffersAuthorizeAgainAndNoLink() throws Exception {
		assertThat(homeWith(authorization(REVOKED_ID, DataCustodianRegistrations.DAILY, false)))
				.contains("Authorize again", "href=\"/oauth2/authorize/daily\"")
				.doesNotContain("/subscription/" + REVOKED_ID, "View data");
	}

	@Test
	void activeRowWithoutResourceUriHasNoLink() throws Exception {
		AuthorizationEntity noUri = authorization(ACTIVE_ID, DataCustodianRegistrations.MONTHLY, true);
		noUri.setResourceURI(null);

		assertThat(homeWith(noUri)).doesNotContain("/subscription/", "View data");
	}
}
