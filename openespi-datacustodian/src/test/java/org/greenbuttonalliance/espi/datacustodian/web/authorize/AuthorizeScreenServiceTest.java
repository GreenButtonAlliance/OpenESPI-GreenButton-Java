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
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.handoff.InvalidHandoffException;
import org.greenbuttonalliance.espi.common.repositories.usage.ApplicationInformationRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository;
import org.greenbuttonalliance.espi.common.scope.EspiScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthorizeScreenService}: client-and-scope validation (defense-in-depth FB
 * subset check), view-model construction (commodity grouping), and scope-narrowing from customer
 * checkbox decisions.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthorizeScreenServiceTest {

	private static final Locale LOCALE = Locale.ENGLISH;
	private static final String CLIENT_ID = "test-tp";

	@Mock private ApplicationInformationRepository applicationInformationRepository;
	@Mock private UsagePointRepository usagePointRepository;

	private MessageSource messages;
	private AuthorizeScreenService service;

	@BeforeEach
	void setUp() {
		messages = staticMessages();
		service = new AuthorizeScreenService(applicationInformationRepository, usagePointRepository, messages);
	}

	@Test
	void validateClientAndScope_passes_when_scope_is_subset_of_registered() {
		ApplicationInformationEntity app = appWithRegisteredScopes(
				"FB=4_5_15;IntervalDuration=3600",
				"FB=4_5_54");
		when(applicationInformationRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(app));

		assertThat(service.validateClientAndScope(CLIENT_ID, "FB=4_5")).isSameAs(app);
		assertThat(service.validateClientAndScope(CLIENT_ID, "FB=4_5_15")).isSameAs(app);
		assertThat(service.validateClientAndScope(CLIENT_ID, "FB=4_54")).isSameAs(app);
	}

	@Test
	void validateClientAndScope_rejects_unknown_client() {
		when(applicationInformationRepository.findByClientId("ghost")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.validateClientAndScope("ghost", "FB=4_5_15"))
				.isInstanceOf(InvalidHandoffException.class)
				.hasMessageContaining("unknown client_id");
	}

	@Test
	void validateClientAndScope_rejects_scope_escalation() {
		ApplicationInformationEntity app = appWithRegisteredScopes("FB=4_5_15");
		when(applicationInformationRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(app));

		assertThatThrownBy(() -> service.validateClientAndScope(CLIENT_ID, "FB=4_5_15_54"))
				.isInstanceOf(InvalidHandoffException.class)
				.hasMessageContaining("not registered");
	}

	@Test
	void buildViewModel_groups_usage_points_by_requested_commodity_only() {
		ApplicationInformationEntity app = appWithRegisteredScopes("FB=4_5_15");
		UUID elec1 = UUID.randomUUID();
		UUID elec2 = UUID.randomUUID();
		UUID gas1 = UUID.randomUUID();
		when(usagePointRepository.findAllByRetailCustomerId(42L)).thenReturn(List.of(
				usagePoint(elec1, ServiceCategory.ELECTRICITY),
				usagePoint(elec2, ServiceCategory.ELECTRICITY),
				usagePoint(gas1, ServiceCategory.GAS)));

		AuthorizeScreenViewModel vm = service.buildViewModel(app, "FB=4_5_15", 42L, "tok", LOCALE);

		// TP only requested electricity → only one commodity section
		assertThat(vm.commoditySections()).hasSize(1);
		AuthorizeScreenViewModel.CommoditySection sec = vm.commoditySections().get(0);
		assertThat(sec.usagePoints()).extracting(AuthorizeScreenViewModel.UsagePointChoice::id)
				.containsExactlyInAnyOrder(elec1, elec2);
		// Gas usage point is not exposed.
	}

	@Test
	void buildViewModel_emits_section_per_requested_commodity() {
		ApplicationInformationEntity app = appWithRegisteredScopes("FB=4_5_10_15");
		when(usagePointRepository.findAllByRetailCustomerId(42L)).thenReturn(List.of(
				usagePoint(UUID.randomUUID(), ServiceCategory.ELECTRICITY),
				usagePoint(UUID.randomUUID(), ServiceCategory.GAS)));

		AuthorizeScreenViewModel vm = service.buildViewModel(app, "FB=4_5_10_15", 42L, "tok", LOCALE);

		assertThat(vm.commoditySections())
				.extracting(AuthorizeScreenViewModel.CommoditySection::kind)
				.containsExactlyInAnyOrder(
						org.greenbuttonalliance.espi.common.domain.usage.enums.ServiceKind.ELECTRICITY,
						org.greenbuttonalliance.espi.common.domain.usage.enums.ServiceKind.GAS);
	}

	@Test
	void buildViewModel_emits_pii_option_per_requested_pii_fb() {
		ApplicationInformationEntity app = appWithRegisteredScopes("FB=4_5_54_55_58");

		AuthorizeScreenViewModel vm = service.buildViewModel(app, "FB=4_5_54_55_58", 42L, "tok", LOCALE);

		assertThat(vm.piiOptions())
				.extracting(AuthorizeScreenViewModel.PiiOption::fbId)
				.containsExactly(54, 55, 58);
	}

	@Test
	void buildViewModel_omits_pii_section_when_no_pii_scope() {
		ApplicationInformationEntity app = appWithRegisteredScopes("FB=4_5_15");

		AuthorizeScreenViewModel vm = service.buildViewModel(app, "FB=4_5_15", 42L, "tok", LOCALE);

		assertThat(vm.piiOptions()).isEmpty();
	}

	@Test
	void buildViewModel_PII_only_grant_emits_no_commodity_sections() {
		ApplicationInformationEntity app = appWithRegisteredScopes("FB=4_54");

		AuthorizeScreenViewModel vm = service.buildViewModel(app, "FB=4_54", 42L, "tok", LOCALE);

		assertThat(vm.commoditySections()).isEmpty();
		assertThat(vm.piiOptions()).extracting(AuthorizeScreenViewModel.PiiOption::fbId).containsExactly(54);
	}

	@Test
	void computeApprovedScope_narrows_commodity_to_selected_kinds() {
		UUID elec = UUID.randomUUID();
		UUID gas = UUID.randomUUID();
		List<UsagePointEntity> ups = List.of(
				usagePoint(elec, ServiceCategory.ELECTRICITY),
				usagePoint(gas, ServiceCategory.GAS));

		// Original scope requested electricity + gas; customer kept electricity only.
		String approved = service.computeApprovedScope(
				"FB=4_5_10_15;IntervalDuration=3600",
				Set.of(elec),                // only electricity selected
				Set.of(),                    // no PII
				ups);

		EspiScope parsed = EspiScope.parse(approved);
		// FB 1 + 4 auto-added (implicit base for any energy grant); FB 5 kept; FB 10 dropped
		// (gas not selected); FB 15 kept (data-shape modifier valid when commodity remains).
		assertThat(parsed.functionBlocks()).containsExactly(1, 4, 5, 15);
		assertThat(parsed.intervalDuration()).isEqualTo(3600);
	}

	@Test
	void computeApprovedScope_PII_only_grant_keeps_only_approved_PII_fbs() {
		String approved = service.computeApprovedScope(
				"FB=4_54_55_58",
				Set.of(),
				Set.of(54, 58),              // customer approved 54 + 58, denied 55
				List.of());

		EspiScope parsed = EspiScope.parse(approved);
		assertThat(parsed.functionBlocks()).containsExactly(51, 54, 58); // FB 51 (customer base) added
		// FB 55 dropped (denied); FB 4 dropped (no energy in approved scope)
	}

	@Test
	void computeApprovedScope_adds_implicit_energy_bases_when_any_commodity_remains() {
		UUID elec = UUID.randomUUID();
		String approved = service.computeApprovedScope(
				"FB=5", Set.of(elec), Set.of(),                                 // request omits FB 1 + 4
				List.of(usagePoint(elec, ServiceCategory.ELECTRICITY)));

		// Implementation auto-adds 1 + 4 (required for any energy response to be well-formed).
		assertThat(EspiScope.parse(approved).functionBlocks()).contains(1, 4);
	}

	@Test
	void computeApprovedScope_drops_energy_bases_for_PII_only_outcome() {
		String approved = service.computeApprovedScope(
				"FB=4_5_54", Set.of(), Set.of(54), List.of());

		assertThat(EspiScope.parse(approved).functionBlocks()).doesNotContain(1, 4, 5);
		assertThat(EspiScope.parse(approved).functionBlocks()).contains(54, 51);
	}

	@Test
	void computeApprovedScope_returns_null_when_customer_grants_nothing() {
		assertThat(service.computeApprovedScope("FB=4_5_15_54", Set.of(), Set.of(), List.of())).isNull();
	}

	// --- helpers --------------------------------------------------------------------------

	private static ApplicationInformationEntity appWithRegisteredScopes(String... scopes) {
		ApplicationInformationEntity app = new ApplicationInformationEntity();
		app.setClientId(CLIENT_ID);
		app.setThirdPartyApplicationDescription("Test Third Party");
		app.setScope(new java.util.HashSet<>(java.util.Arrays.asList(scopes)));
		return app;
	}

	private static UsagePointEntity usagePoint(UUID id, ServiceCategory category) {
		UsagePointEntity up = new UsagePointEntity();
		up.setId(id);
		up.setServiceCategory(category);
		return up;
	}

	private static MessageSource staticMessages() {
		StaticMessageSource src = new StaticMessageSource();
		src.setUseCodeAsDefaultMessage(true);
		return src;
	}
}
