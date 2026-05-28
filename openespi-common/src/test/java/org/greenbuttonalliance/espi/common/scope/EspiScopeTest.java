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

import org.greenbuttonalliance.espi.common.domain.usage.enums.ServiceKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Unit tests for {@link EspiScope} parsing and the category-aware views it derives from
 * {@link FunctionBlock}.
 */
class EspiScopeTest {

	@Test
	@DisplayName("parses the full ESPI scope grammar")
	void parsesFullGrammar() {
		EspiScope scope = EspiScope.parse("FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13");

		assertThat(scope)
				.satisfies(s -> assertThat(s.raw()).isEqualTo("FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13"))
				.extracting(
						EspiScope::functionBlocks,
						EspiScope::intervalDuration,
						EspiScope::blockDuration,
						EspiScope::historyLength,
						EspiScope::additionalParameters)
				.containsExactly(
						java.util.Set.of(4, 5, 15),
						3600,
						"monthly",
						13,
						java.util.Map.of());
	}

	@Test
	@DisplayName("FB ids are sorted ascending regardless of input order")
	void functionBlocksAreSorted() {
		EspiScope scope = EspiScope.parse("FB=15_4_29_5");

		assertThat(scope.functionBlocks()).containsExactly(4, 5, 15, 29);
	}

	@Test
	@DisplayName("unrecognized terms and bare tokens are preserved in additionalParameters")
	void preservesUnknownTerms() {
		EspiScope scope = EspiScope.parse("FB=4_5;ServiceKindFilter=ELECTRIC;DataCustodian_Admin_Access");

		assertThat(scope.additionalParameters())
				.containsEntry("ServiceKindFilter", "ELECTRIC")
				.containsEntry("DataCustodian_Admin_Access", "");
	}

	@Test
	@DisplayName("recognized term keys are matched case-insensitively")
	void keysAreCaseInsensitive() {
		EspiScope scope = EspiScope.parse("fb=4_5;intervalduration=900;BLOCKDURATION=daily;HistoryLength=7");

		assertThat(scope)
				.extracting(EspiScope::functionBlocks, EspiScope::intervalDuration, EspiScope::blockDuration, EspiScope::historyLength)
				.containsExactly(java.util.Set.of(4, 5), 900, "daily", 7);
		assertThat(scope.additionalParameters()).isEmpty();
	}

	@Test
	@DisplayName("tolerant of surrounding whitespace and stray/empty segments")
	void tolerantOfWhitespaceAndEmptySegments() {
		EspiScope scope = EspiScope.parse("  ;; FB = 4_5 ; ; IntervalDuration = 3600 ;");

		assertThat(scope)
				.extracting(EspiScope::functionBlocks, EspiScope::intervalDuration)
				.containsExactly(java.util.Set.of(4, 5), 3600);
	}

	@Test
	@DisplayName("a scope with no FB term yields an empty functionBlocks set")
	void scopeWithoutFbTerm() {
		EspiScope scope = EspiScope.parse("IntervalDuration=3600");

		assertThat(scope.functionBlocks()).isEmpty();
		assertThat(scope.intervalDuration()).isEqualTo(3600);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"   ", "\t"})
	@DisplayName("null or blank scope is rejected")
	void rejectsNullOrBlank(String scope) {
		assertThatExceptionOfType(IllegalArgumentException.class)
				.isThrownBy(() -> EspiScope.parse(scope))
				.withMessageContaining("must not be null or blank");
	}

	@Test
	@DisplayName("a non-integer FB token is rejected")
	void rejectsNonIntegerFb() {
		assertThatExceptionOfType(IllegalArgumentException.class)
				.isThrownBy(() -> EspiScope.parse("FB=4_x_15"))
				.withMessageContaining("FB");
	}

	@Test
	@DisplayName("a non-integer IntervalDuration is rejected")
	void rejectsNonIntegerIntervalDuration() {
		assertThatExceptionOfType(IllegalArgumentException.class)
				.isThrownBy(() -> EspiScope.parse("FB=4;IntervalDuration=hourly"))
				.withMessageContaining("IntervalDuration");
	}

	@Test
	@DisplayName("containsFunctionBlock reflects membership")
	void containsFunctionBlock() {
		EspiScope scope = EspiScope.parse("FB=4_5_15");

		assertThat(scope.containsFunctionBlock(5)).isTrue();
		assertThat(scope.containsFunctionBlock(16)).isFalse();
	}

	@Test
	@DisplayName("functionBlocksIn filters by category")
	void functionBlocksInByCategory() {
		EspiScope scope = EspiScope.parse("FB=4_5_10_15_54_36_99");

		assertThat(scope.functionBlocksIn(FunctionBlockCategory.BASE)).containsExactly(4);
		assertThat(scope.functionBlocksIn(FunctionBlockCategory.COMMODITY)).containsExactly(5, 10);
		assertThat(scope.functionBlocksIn(FunctionBlockCategory.ENERGY_DATA_SHAPE)).containsExactly(15);
		assertThat(scope.functionBlocksIn(FunctionBlockCategory.CUSTOMER_PII)).containsExactly(54);
		assertThat(scope.functionBlocksIn(FunctionBlockCategory.DEPRECATED)).containsExactly(36);
		assertThat(scope.functionBlocksIn(FunctionBlockCategory.UNKNOWN)).containsExactly(99);
	}

	@Test
	@DisplayName("commodityServiceKinds aggregates kinds; temperature (FB 29) contributes none")
	void commodityServiceKinds() {
		EspiScope scope = EspiScope.parse("FB=4_5_10_11_29_15");

		assertThat(scope.commodityServiceKinds())
				.containsExactlyInAnyOrder(ServiceKind.ELECTRICITY, ServiceKind.GAS, ServiceKind.WATER);
	}

	@Test
	@DisplayName("FB 29 alone yields no commodity ServiceKind")
	void temperatureAloneYieldsNoServiceKind() {
		assertThat(EspiScope.parse("FB=4_29").commodityServiceKinds()).isEmpty();
	}

	@Test
	@DisplayName("includesCustomerPii and includesEnergyData reflect the granted FBs")
	void domainPredicates() {
		assertThat(EspiScope.parse("FB=53_54").includesCustomerPii()).isTrue();
		assertThat(EspiScope.parse("FB=4_5_15").includesCustomerPii()).isFalse();
		assertThat(EspiScope.parse("FB=4_5_15").includesEnergyData()).isTrue();
		assertThat(EspiScope.parse("FB=53_54").includesEnergyData()).isFalse();
	}

	@Test
	@DisplayName("unrecognized and deprecated FB ids parse without error and are carried")
	void carriesUnknownAndDeprecatedFbs() {
		assertThatNoException().isThrownBy(() -> EspiScope.parse("FB=99_36"));

		EspiScope scope = EspiScope.parse("FB=99_36");
		assertThat(scope.functionBlocks()).containsExactly(36, 99);
	}

	@Test
	@DisplayName("the functionBlocks set is immutable")
	void functionBlocksImmutable() {
		EspiScope scope = EspiScope.parse("FB=4_5");

		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> scope.functionBlocks().add(99));
	}
}
