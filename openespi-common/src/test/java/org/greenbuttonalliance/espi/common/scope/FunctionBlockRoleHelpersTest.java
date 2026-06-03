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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the Authorization Screen role-classifier predicates added in PR C2b. Each FB has
 * exactly one screen role (implicit base / commodity profile / data-shape modifier / PII-selectable
 * / none-of-the-above) and the predicates are checked for correctness against representative ids.
 */
class FunctionBlockRoleHelpersTest {

	@ParameterizedTest
	@ValueSource(ints = {1, 4, 51})
	void implicitBase_ids_are_recognized(int fbId) {
		assertThat(FunctionBlock.isImplicitBase(fbId)).isTrue();
		assertThat(FunctionBlock.byId(fbId)).hasValueSatisfying(fb -> {
			assertThat(fb.isImplicitBase()).isTrue();
			assertThat(fb.isCommodityProfile()).isFalse();
			assertThat(fb.isDataShapeModifier()).isFalse();
			assertThat(fb.isPiiSelectable()).isFalse();
		});
	}

	@ParameterizedTest
	@ValueSource(ints = {5, 6, 7, 8, 9, 10, 11, 29})
	void commodityProfile_ids_are_recognized(int fbId) {
		assertThat(FunctionBlock.isCommodityProfile(fbId)).isTrue();
		assertThat(FunctionBlock.byId(fbId)).hasValueSatisfying(fb -> {
			assertThat(fb.isCommodityProfile()).isTrue();
			assertThat(fb.isImplicitBase()).isFalse();
			assertThat(fb.isDataShapeModifier()).isFalse();
			assertThat(fb.isPiiSelectable()).isFalse();
		});
	}

	@ParameterizedTest
	@ValueSource(ints = {12, 15, 16, 17, 27, 28})
	void dataShapeModifier_ids_are_recognized(int fbId) {
		assertThat(FunctionBlock.isDataShapeModifier(fbId)).isTrue();
		assertThat(FunctionBlock.byId(fbId)).hasValueSatisfying(fb -> {
			assertThat(fb.isDataShapeModifier()).isTrue();
			assertThat(fb.isImplicitBase()).isFalse();
			assertThat(fb.isCommodityProfile()).isFalse();
			assertThat(fb.isPiiSelectable()).isFalse();
		});
	}

	@ParameterizedTest
	@ValueSource(ints = {54, 55, 56, 57, 58, 59, 60, 61, 62})
	void piiSelectable_ids_are_recognized(int fbId) {
		assertThat(FunctionBlock.isPiiSelectable(fbId)).isTrue();
		assertThat(FunctionBlock.byId(fbId)).hasValueSatisfying(fb -> {
			assertThat(fb.isPiiSelectable()).isTrue();
			assertThat(fb.isImplicitBase()).isFalse();
			assertThat(fb.isCommodityProfile()).isFalse();
			assertThat(fb.isDataShapeModifier()).isFalse();
		});
	}

	@ParameterizedTest
	@ValueSource(ints = {2, 3, 13, 30, 31, 53, 63, 14 /* deprecated */, 999 /* unknown */})
	void other_ids_are_none_of_the_screen_roles(int fbId) {
		assertThat(FunctionBlock.isImplicitBase(fbId)).isFalse();
		assertThat(FunctionBlock.isCommodityProfile(fbId)).isFalse();
		assertThat(FunctionBlock.isDataShapeModifier(fbId)).isFalse();
		assertThat(FunctionBlock.isPiiSelectable(fbId)).isFalse();
	}
}
