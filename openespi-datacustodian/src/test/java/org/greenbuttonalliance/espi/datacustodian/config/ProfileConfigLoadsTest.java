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

package org.greenbuttonalliance.espi.datacustodian.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every DC profile's configuration loads the way Spring Boot loads it at startup (#271): no duplicate YAML key, no
 * {@code spring.profiles.active} or {@code include} in a profile-specific file, and no actuator endpoint with both
 * the removed {@code enabled} key and the {@code access} key. Each stops the DC before it serves a request. The DC
 * has no {@code prod}, {@code docker} or {@code aws-sandbox} file of its own; those come from openespi-common, which
 * used to set {@code spring.profiles.active} in them.
 */
@DisplayName("Every DC profile's configuration loads (#271)")
class ProfileConfigLoadsTest {

	static List<String> profiles() throws IOException {
		List<String> profiles = ProfileConfig.profiles();
		assertThat(profiles).as("profile files on the classpath, openespi-common's included")
				.contains("dev-mysql", "local", "prod", "docker", "aws-sandbox");
		return profiles;
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("profiles")
	@DisplayName("the profile loads as SPRING_PROFILES_ACTIVE would load it, actuator endpoint access included")
	void profileLoads(String profile) {
		assertThat(ProfileConfig.load(profile).getActiveProfiles()).containsExactly(profile);
	}
}
