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

package org.greenbuttonalliance.espi.common.scope;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Truth-table tests for {@link ResourceFbPolicy}. The positive/negative cases mirror exactly the
 * {@code @WithMockUser(authorities = ...)} sets exercised by the Data Custodian's resource-controller
 * MockMvc tests, so the extracted policy provably reproduces the endpoint behaviour it replaced
 * ({@code FB_4+FB_5} usage data, {@code FB_15} usage summary, {@code FB_17} power quality,
 * {@code FB_53+FB_56} CustomerAccount, {@code FB_53+FB_54} Customer; {@code FB_53} or {@code FB_54}
 * alone denied).
 */
class ResourceFbPolicyTest {

	private final ResourceFbPolicy policy = new ResourceFbPolicy();

	// --- Energy interval data: FB_4 AND a commodity FB -----------------------------------------

	@Test
	void energyIntervalData_requires_fb4_and_a_commodity() {
		// {FB_4, FB_5} — the canonical usage-data grant the controller tests assert as 200.
		for (EspiResource r : Set.of(EspiResource.USAGE_POINT, EspiResource.METER_READING,
				EspiResource.READING_TYPE, EspiResource.INTERVAL_BLOCK, EspiResource.LOCAL_TIME_PARAMETERS)) {
			assertThat(policy.isAuthorized(r, Set.of(4, 5))).as("%s with {4,5}", r).isTrue();
			assertThat(policy.isAuthorized(r, Set.of(4))).as("%s with {4} only", r).isFalse();
			assertThat(policy.isAuthorized(r, Set.of(5))).as("%s with commodity only", r).isFalse();
			assertThat(policy.isAuthorized(r, Set.of())).as("%s with no FBs", r).isFalse();
		}
	}

	@Test
	void energyIntervalBase_accepts_any_commodity_not_just_electricity() {
		assertThat(policy.requiresEnergyIntervalBase(Set.of(4, 10))).as("gas").isTrue();   // FB_10 gas
		assertThat(policy.requiresEnergyIntervalBase(Set.of(4, 11))).as("water").isTrue(); // FB_11 water
		assertThat(policy.requiresEnergyIntervalBase(Set.of(4))).as("base only").isFalse();
		assertThat(policy.requiresEnergyIntervalBase(Set.of(5))).as("commodity only").isFalse();
		assertThat(policy.requiresEnergyIntervalBase(null)).as("null").isFalse();
	}

	// --- Data-shape summaries ------------------------------------------------------------------

	@Test
	void usageSummary_requires_a_data_shape_fb() {
		assertThat(policy.isAuthorized(EspiResource.USAGE_SUMMARY, Set.of(15))).isTrue();
		assertThat(policy.isAuthorized(EspiResource.USAGE_SUMMARY, Set.of(16))).isTrue();
		assertThat(policy.isAuthorized(EspiResource.USAGE_SUMMARY, Set.of(27))).isTrue();
		assertThat(policy.isAuthorized(EspiResource.USAGE_SUMMARY, Set.of(28))).isTrue();
		assertThat(policy.isAuthorized(EspiResource.USAGE_SUMMARY, Set.of(4, 5))).isFalse();
		assertThat(policy.isAuthorized(EspiResource.USAGE_SUMMARY, Set.of())).isFalse();
	}

	@Test
	void powerQuality_requires_fb17() {
		assertThat(policy.isAuthorized(EspiResource.ELECTRIC_POWER_QUALITY_SUMMARY, Set.of(17))).isTrue();
		assertThat(policy.isAuthorized(EspiResource.ELECTRIC_POWER_QUALITY_SUMMARY, Set.of(15))).isFalse();
		assertThat(policy.isAuthorized(EspiResource.ELECTRIC_POWER_QUALITY_SUMMARY, Set.of(4, 5))).isFalse();
	}

	// --- Customer / PII: FB_53 base AND the resource-specific FB --------------------------------

	@Test
	void customer_requires_base_and_specific_fb() {
		assertThat(policy.isAuthorized(EspiResource.CUSTOMER, Set.of(53, 54))).isTrue();
		assertThat(policy.isAuthorized(EspiResource.CUSTOMER_ACCOUNT, Set.of(53, 56))).isTrue();
		assertThat(policy.isAuthorized(EspiResource.CUSTOMER_AGREEMENT, Set.of(53, 57))).isTrue();
		assertThat(policy.isAuthorized(EspiResource.SERVICE_LOCATION, Set.of(53, 58))).isTrue();
		assertThat(policy.isAuthorized(EspiResource.SERVICE_SUPPLIER, Set.of(53, 59))).isTrue();
		assertThat(policy.isAuthorized(EspiResource.METER, Set.of(53, 60))).isTrue();
		assertThat(policy.isAuthorized(EspiResource.END_DEVICE, Set.of(53, 61))).isTrue();
		assertThat(policy.isAuthorized(EspiResource.PROGRAM_DATE_ID_MAPPINGS, Set.of(53, 62))).isTrue();
	}

	@Test
	void customer_base_alone_or_specific_alone_is_denied() {
		assertThat(policy.isAuthorized(EspiResource.CUSTOMER, Set.of(53))).as("base only").isFalse();
		assertThat(policy.isAuthorized(EspiResource.CUSTOMER, Set.of(54))).as("specific only").isFalse();
		assertThat(policy.isAuthorized(EspiResource.CUSTOMER_ACCOUNT, Set.of(56))).as("acct, no base").isFalse();
		// A customer FB must not unlock a different customer resource.
		assertThat(policy.isAuthorized(EspiResource.CUSTOMER_ACCOUNT, Set.of(53, 54))).as("wrong specific").isFalse();
	}

	// --- Deferred / null safety ----------------------------------------------------------------

	@Test
	void energyProgramIdMappings_is_fail_closed_until_168() {
		assertThat(policy.isAuthorized(EspiResource.PROGRAM_ID_MAPPINGS, Set.of(4, 5))).isFalse();
	}

	@Test
	void null_arguments_are_denied() {
		assertThat(policy.isAuthorized(null, Set.of(4, 5))).isFalse();
		assertThat(policy.isAuthorized(EspiResource.USAGE_POINT, null)).isFalse();
	}
}
