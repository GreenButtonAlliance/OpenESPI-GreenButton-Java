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
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guard tests that lock the {@link FunctionBlock} catalog to NAESB ESPI 4.0 §REQ.21.4.2.1.3.1.
 */
class FunctionBlockTest {

	@Test
	@DisplayName("every FB id is unique and within the spec range 1..70")
	void idsAreUniqueAndInRange() {
		long distinctIds = Arrays.stream(FunctionBlock.values()).map(FunctionBlock::getId).distinct().count();

		assertThat(FunctionBlock.values())
				.allSatisfy(fb -> assertThat(fb.getId()).isBetween(1, 70))
				.hasSize((int) distinctIds);
	}

	@Test
	@DisplayName("deprecated ids never overlap an active FB and never escape the 1..70 range")
	void deprecatedIdsAreDisjointFromActive() {
		var activeIds = Arrays.stream(FunctionBlock.values()).map(FunctionBlock::getId).toList();

		assertThat(FunctionBlock.DEPRECATED_IDS)
				.doesNotContainAnyElementsOf(activeIds)
				.allSatisfy(id -> assertThat(id).isBetween(1, 70));
	}

	@Test
	@DisplayName("every commodity FB except temperature (FB 29) maps to a ServiceKind; FB 29 maps to none")
	void commodityServiceKindMapping() {
		assertThat(FunctionBlock.values())
				.filteredOn(fb -> fb.getCategory() == FunctionBlockCategory.COMMODITY)
				.allSatisfy(fb -> {
					if (fb == FunctionBlock.FB_29) {
						assertThat(fb.getServiceKind()).isEmpty();
					}
					else {
						assertThat(fb.getServiceKind()).isPresent();
					}
				});
	}

	@Test
	@DisplayName("no non-commodity FB carries a ServiceKind")
	void onlyCommodityFbsCarryServiceKind() {
		assertThat(FunctionBlock.values())
				.filteredOn(fb -> fb.getCategory() != FunctionBlockCategory.COMMODITY)
				.allSatisfy(fb -> assertThat(fb.getServiceKind()).isEmpty());
	}

	@Test
	@DisplayName("commodity FBs map to the expected ServiceKind per the #122 mapping")
	void serviceKindOfKnownCommodities() {
		assertThat(FunctionBlock.serviceKindOf(5)).contains(ServiceKind.ELECTRICITY);
		assertThat(FunctionBlock.serviceKindOf(9)).contains(ServiceKind.ELECTRICITY);
		assertThat(FunctionBlock.serviceKindOf(10)).contains(ServiceKind.GAS);
		assertThat(FunctionBlock.serviceKindOf(11)).contains(ServiceKind.WATER);
		assertThat(FunctionBlock.serviceKindOf(29)).isEmpty();
		assertThat(FunctionBlock.serviceKindOf(15)).isEmpty();
		assertThat(FunctionBlock.serviceKindOf(99)).isEmpty();
	}

	@Test
	@DisplayName("categoryOf resolves the load-bearing categories")
	void categoryOfLoadBearing() {
		assertThat(FunctionBlock.categoryOf(4)).isEqualTo(FunctionBlockCategory.BASE);
		assertThat(FunctionBlock.categoryOf(53)).isEqualTo(FunctionBlockCategory.BASE);
		assertThat(FunctionBlock.categoryOf(5)).isEqualTo(FunctionBlockCategory.COMMODITY);
		assertThat(FunctionBlock.categoryOf(29)).isEqualTo(FunctionBlockCategory.COMMODITY);
		assertThat(FunctionBlock.categoryOf(15)).isEqualTo(FunctionBlockCategory.ENERGY_DATA_SHAPE);
		assertThat(FunctionBlock.categoryOf(54)).isEqualTo(FunctionBlockCategory.CUSTOMER_PII);
		assertThat(FunctionBlock.categoryOf(62)).isEqualTo(FunctionBlockCategory.CUSTOMER_PII);
	}

	@ParameterizedTest
	@ValueSource(ints = {14, 18, 19, 32, 33, 34, 36, 38, 46, 47, 48, 49, 50, 66})
	@DisplayName("deprecated ids resolve to DEPRECATED, not UNKNOWN")
	void deprecatedIdsCategorizeAsDeprecated(int id) {
		assertThat(FunctionBlock.categoryOf(id)).isEqualTo(FunctionBlockCategory.DEPRECATED);
		assertThat(FunctionBlock.byId(id)).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(ints = {0, -1, 20, 26, 42, 43, 45, 71, 99})
	@DisplayName("ids absent from the spec table resolve to UNKNOWN")
	void undefinedIdsCategorizeAsUnknown(int id) {
		assertThat(FunctionBlock.categoryOf(id)).isEqualTo(FunctionBlockCategory.UNKNOWN);
		assertThat(FunctionBlock.byId(id)).isEmpty();
	}

	@Test
	@DisplayName("byId returns the catalogued FB for an active id")
	void byIdResolvesActive() {
		assertThat(FunctionBlock.byId(5)).contains(FunctionBlock.FB_05);
		assertThat(FunctionBlock.byId(53)).contains(FunctionBlock.FB_53);
	}

	@Test
	@DisplayName("SFTP bulk FBs are deprecated; REST bulk FBs are active")
	void bulkTransferReflectsSftpDeprecation() {
		assertThat(FunctionBlock.categoryOf(34)).isEqualTo(FunctionBlockCategory.DEPRECATED);
		assertThat(FunctionBlock.categoryOf(66)).isEqualTo(FunctionBlockCategory.DEPRECATED);
		assertThat(FunctionBlock.categoryOf(35)).isEqualTo(FunctionBlockCategory.BULK_TRANSFER);
		assertThat(FunctionBlock.categoryOf(67)).isEqualTo(FunctionBlockCategory.BULK_TRANSFER);
	}
}
