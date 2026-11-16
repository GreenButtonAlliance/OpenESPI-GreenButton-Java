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

package org.greenbuttonalliance.espi.thirdparty.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/** Normalisation of {@code spring.h2.console.path} shared by both security chains. */
class H2ConsoleBasePathTest {

	@ParameterizedTest
	@CsvSource({"/h2-console, /h2-console", "/h2-console/, /h2-console", "/h2-console//, /h2-console",
			"' /db/ ', /db", "/a/b/, /a/b"})
	void trailing_slashes_are_removed(String configured, String expected) {
		assertThat(H2ConsoleSecurityConfiguration.basePath(configured)).isEqualTo(expected);
	}

	@ParameterizedTest
	@ValueSource(strings = {"/", "//", "", "h2-console"})
	void paths_not_below_root_are_rejected(String configured) {
		assertThatIllegalStateException().isThrownBy(() -> H2ConsoleSecurityConfiguration.basePath(configured));
	}

	@Test
	void null_is_rejected() {
		assertThatIllegalStateException().isThrownBy(() -> H2ConsoleSecurityConfiguration.basePath(null));
	}
}
