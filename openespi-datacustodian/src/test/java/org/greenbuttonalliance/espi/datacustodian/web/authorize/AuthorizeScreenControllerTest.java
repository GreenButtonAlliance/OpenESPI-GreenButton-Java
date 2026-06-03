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

package org.greenbuttonalliance.espi.datacustodian.web.authorize;

import org.greenbuttonalliance.espi.common.domain.common.ServiceCategory;
import org.greenbuttonalliance.espi.common.domain.usage.ApplicationInformationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.handoff.SignedHandoff;
import org.greenbuttonalliance.espi.handoff.SignedHandoffCodec;
import org.greenbuttonalliance.espi.common.repositories.usage.ApplicationInformationRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.RetailCustomerRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository;
import org.greenbuttonalliance.espi.common.scope.EspiScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc tests for {@link AuthorizeScreenController} — the customer-facing Authorization Screen.
 *
 * <p>Covers the security boundary (invalid-handoff rejection variants, all surfacing as a uniform
 * 400) and the happy-path GET / POST round trip (handoff verification, nonce consumption,
 * effective-scope computation, signed return handoff back to the AS).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorizeScreenControllerTest {

	private static final String TP_CLIENT_ID = "tp-screen-test";
	private static final String CUSTOMER_USERNAME = "screen-test-customer";
	private static final String CUSTOMER_PASSWORD = "secret123";

	@Autowired private MockMvc mockMvc;
	@Autowired private SignedHandoffCodec codec;
	@Autowired private ApplicationInformationRepository applicationInformationRepository;
	@Autowired private RetailCustomerRepository retailCustomerRepository;
	@Autowired private UsagePointRepository usagePointRepository;
	@Autowired private PasswordEncoder customerPasswordEncoder;

	private RetailCustomerEntity customer;
	private ApplicationInformationEntity application;
	private UUID electricityUpId;
	private UUID gasUpId;

	@BeforeEach
	void seedFixtures() {
		application = applicationInformationRepository.findByClientId(TP_CLIENT_ID)
				.orElseGet(() -> {
					ApplicationInformationEntity app = new ApplicationInformationEntity();
					app.setId(UUID.randomUUID());
					app.setClientId(TP_CLIENT_ID);
					app.setThirdPartyApplicationDescription("Test Third Party");
					app.setScope(new HashSet<>(List.of("FB=4_5_10_15_54_58")));
					return applicationInformationRepository.save(app);
				});

		customer = retailCustomerRepository.findByUsername(CUSTOMER_USERNAME)
				.orElseGet(() -> {
					RetailCustomerEntity c = new RetailCustomerEntity(CUSTOMER_USERNAME, "Screen", "Tester");
					c.setPassword(customerPasswordEncoder.encode(CUSTOMER_PASSWORD));
					c.setRole(RetailCustomerEntity.ROLE_USER);
					c.setEnabled(Boolean.TRUE);
					c.setAccountLocked(Boolean.FALSE);
					return retailCustomerRepository.save(c);
				});

		if (usagePointRepository.findAllByRetailCustomerId(customer.getId()).isEmpty()) {
			UsagePointEntity elec = new UsagePointEntity();
			elec.setId(UUID.randomUUID());
			elec.setServiceCategory(ServiceCategory.ELECTRICITY);
			elec.setRetailCustomer(customer);
			electricityUpId = usagePointRepository.save(elec).getId();

			UsagePointEntity gas = new UsagePointEntity();
			gas.setId(UUID.randomUUID());
			gas.setServiceCategory(ServiceCategory.GAS);
			gas.setRetailCustomer(customer);
			gasUpId = usagePointRepository.save(gas).getId();
		}
		else {
			List<UsagePointEntity> ups = usagePointRepository.findAllByRetailCustomerId(customer.getId());
			electricityUpId = ups.stream()
					.filter(u -> u.getServiceCategory() == ServiceCategory.ELECTRICITY)
					.findFirst().orElseThrow().getId();
			gasUpId = ups.stream()
					.filter(u -> u.getServiceCategory() == ServiceCategory.GAS)
					.findFirst().orElseThrow().getId();
		}
	}

	@Test
	void invalid_handoff_returns_400_uniform_page() throws Exception {
		mockMvc.perform(get("/oauth/authorize-screen")
						.param("handoff", "not-a-valid-handoff")
						.with(user(CUSTOMER_USERNAME).roles("USER")))
				.andExpect(status().isBadRequest())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Bad Request")));
	}

	@Test
	void unknown_client_id_returns_400() throws Exception {
		String token = codec.encode(SignedHandoff.Outbound.of(
				"corr-1", Instant.now(), Instant.now().plusSeconds(120), UUID.randomUUID().toString(),
				"ghost-client", "FB=4_5_15", "https://as.example.com/continue"));

		mockMvc.perform(get("/oauth/authorize-screen")
						.param("handoff", token)
						.with(user(CUSTOMER_USERNAME).roles("USER")))
				.andExpect(status().isBadRequest());
	}

	@Test
	void scope_escalation_beyond_registered_returns_400() throws Exception {
		// TP registered FB=4_5_10_15_54_58; request a non-registered FB (e.g. 16)
		String token = codec.encode(SignedHandoff.Outbound.of(
				"corr-2", Instant.now(), Instant.now().plusSeconds(120), UUID.randomUUID().toString(),
				TP_CLIENT_ID, "FB=4_5_16", "https://as.example.com/continue"));

		mockMvc.perform(get("/oauth/authorize-screen")
						.param("handoff", token)
						.with(user(CUSTOMER_USERNAME).roles("USER")))
				.andExpect(status().isBadRequest());
	}

	@Test
	void replayed_nonce_returns_400() throws Exception {
		String reusedNonce = UUID.randomUUID().toString();
		String token = codec.encode(SignedHandoff.Outbound.of(
				"corr-3", Instant.now(), Instant.now().plusSeconds(120), reusedNonce,
				TP_CLIENT_ID, "FB=4_5_15", "https://as.example.com/continue"));

		// First request consumes the nonce.
		mockMvc.perform(get("/oauth/authorize-screen")
						.param("handoff", token)
						.with(user(CUSTOMER_USERNAME).roles("USER")))
				.andExpect(status().isOk());

		// Second request with the same nonce must be rejected.
		mockMvc.perform(get("/oauth/authorize-screen")
						.param("handoff", token)
						.with(user(CUSTOMER_USERNAME).roles("USER")))
				.andExpect(status().isBadRequest());
	}

	@Test
	void happy_GET_renders_form_with_tp_name_and_csrf() throws Exception {
		String token = codec.encode(SignedHandoff.Outbound.of(
				"corr-4", Instant.now(), Instant.now().plusSeconds(120), UUID.randomUUID().toString(),
				TP_CLIENT_ID, "FB=4_5_15", "https://as.example.com/continue"));

		mockMvc.perform(get("/oauth/authorize-screen")
						.param("handoff", token)
						.with(user(CUSTOMER_USERNAME).roles("USER")))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Test Third Party")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"handoff\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("selected_usage_point_ids")));
	}

	@Test
	void POST_allow_redirects_to_AS_with_signed_return_handoff_carrying_approved_scope() throws Exception {
		String token = codec.encode(SignedHandoff.Outbound.of(
				"corr-5", Instant.now(), Instant.now().plusSeconds(120), UUID.randomUUID().toString(),
				TP_CLIENT_ID, "FB=4_5_10_15_54_58", "https://as.example.com/continue?state=abc"));

		MvcResult result = mockMvc.perform(post("/oauth/authorize-screen")
						.with(csrf())
						.with(user(CUSTOMER_USERNAME).roles("USER"))
						.param("handoff", token)
						.param("decision", "allow")
						.param("selected_usage_point_ids", electricityUpId.toString())
						.param("approved_pii_fbs", "54"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("https://as.example.com/continue?state=abc&handoff=*"))
				.andReturn();

		String location = result.getResponse().getHeader("Location");
		String returnToken = location.substring(location.indexOf("handoff=") + "handoff=".length());
		SignedHandoff.Return ret = codec.decodeReturn(returnToken);

		assertThat(ret.consent()).isEqualTo(SignedHandoff.Return.CONSENT_ALLOW);
		assertThat(ret.correlationId()).isEqualTo("corr-5");
		assertThat(ret.selectedUsagePointIds()).containsExactly(electricityUpId);

		// Effective approved scope: customer kept electricity (5 + implicit 1+4), data-shape 15, PII 54 (+ implicit 51).
		// Gas (10) dropped, FB 58 dropped (customer didn't approve).
		EspiScope approved = EspiScope.parse(ret.approvedScope());
		assertThat(approved.functionBlocks()).containsExactly(1, 4, 5, 15, 51, 54);
	}

	@Test
	void POST_deny_redirects_to_AS_with_deny_handoff_no_approved_scope() throws Exception {
		String token = codec.encode(SignedHandoff.Outbound.of(
				"corr-6", Instant.now(), Instant.now().plusSeconds(120), UUID.randomUUID().toString(),
				TP_CLIENT_ID, "FB=4_5_15", "https://as.example.com/continue"));

		MvcResult result = mockMvc.perform(post("/oauth/authorize-screen")
						.with(csrf())
						.with(user(CUSTOMER_USERNAME).roles("USER"))
						.param("handoff", token)
						.param("decision", "deny"))
				.andExpect(status().is3xxRedirection())
				.andReturn();

		String location = result.getResponse().getHeader("Location");
		String returnToken = location.substring(location.indexOf("handoff=") + "handoff=".length());
		SignedHandoff.Return ret = codec.decodeReturn(returnToken);

		assertThat(ret.consent()).isEqualTo(SignedHandoff.Return.CONSENT_DENY);
		assertThat(ret.approvedScope()).isNull();
		assertThat(ret.selectedUsagePointIds()).isEmpty();
	}

	@Test
	void POST_allow_with_no_selections_falls_back_to_deny() throws Exception {
		String token = codec.encode(SignedHandoff.Outbound.of(
				"corr-7", Instant.now(), Instant.now().plusSeconds(120), UUID.randomUUID().toString(),
				TP_CLIENT_ID, "FB=4_5_15_54", "https://as.example.com/continue"));

		MvcResult result = mockMvc.perform(post("/oauth/authorize-screen")
						.with(csrf())
						.with(user(CUSTOMER_USERNAME).roles("USER"))
						.param("handoff", token)
						.param("decision", "allow"))
				.andExpect(status().is3xxRedirection())
				.andReturn();

		String location = result.getResponse().getHeader("Location");
		String returnToken = location.substring(location.indexOf("handoff=") + "handoff=".length());
		SignedHandoff.Return ret = codec.decodeReturn(returnToken);

		// Customer clicked Allow but checked nothing → effectively a deny.
		assertThat(ret.consent()).isEqualTo(SignedHandoff.Return.CONSENT_DENY);
		assertThat(ret.approvedScope()).isNull();
	}
}
