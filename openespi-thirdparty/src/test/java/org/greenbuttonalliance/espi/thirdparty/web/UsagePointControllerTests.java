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

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link UsagePointController}.
 * <p>
 * Only {@code index} is covered. {@code show} still takes Long ids and builds its
 * display bag from a placeholder entity until the Long-to-UUID conversion is done.
 * Its tests will be written when the Third Party UI is recreated.
 */
class UsagePointControllerTests {

	@Test
	void index_displaysIndexView() {
		UsagePointController controller = new UsagePointController();
		ModelMap model = new ModelMap();

		assertThat(controller.index(model, mock(Authentication.class))).isEqualTo("/customer/usagepoints/index");
		assertThat(model).isEmpty();
	}
}
