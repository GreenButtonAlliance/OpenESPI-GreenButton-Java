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

package org.greenbuttonalliance.espi.authserver.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.actuate.autoconfigure.endpoint.PropertiesEndpointAccessResolver;
import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.bootstrap.DefaultBootstrapContext;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every AS profile's configuration loads the way Spring Boot loads it at startup (#271). Each of these stops the AS
 * before it serves a request:
 * <ul>
 *   <li>a duplicate YAML key;</li>
 *   <li>{@code spring.profiles.active} or {@code include} in a profile-specific file;</li>
 *   <li>an actuator endpoint with both the removed {@code enabled} key and the {@code access} key.</li>
 * </ul>
 * The profiles are every {@code application-<profile>} file in this module's {@code src/main/resources} and the
 * Docker image's {@code docker/config} (surefire runs in the module directory), so a new profile file is covered
 * without changing this test. The test resources are not read.
 */
@DisplayName("Every AS profile's configuration loads (#271)")
class ProfileConfigLoadsTest {

	private static final Path RESOURCES = Path.of("src", "main", "resources");
	private static final Path DOCKER_CONFIG = Path.of("docker", "config");
	private static final Pattern PROFILE_FILE = Pattern.compile("application-(.+)\\.(ya?ml|properties)");
	private static final Pattern ENDPOINT_KEY = Pattern.compile("management\\.endpoint\\.([a-z0-9-]+)\\..+");

	static List<String> profiles() throws IOException {
		List<String> profiles = Stream.concat(profilesIn(RESOURCES).stream(), profilesIn(DOCKER_CONFIG).stream())
				.distinct().sorted().toList();
		assertThat(profiles).as("profile files found").contains("prod", "docker");
		return profiles;
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("profiles")
	@DisplayName("the profile loads as SPRING_PROFILES_ACTIVE would load it, actuator endpoint access included")
	void profileLoads(String profile) {
		StandardEnvironment environment = new StandardEnvironment();
		// As SpringApplication does: Boot's conversions (e.g. "read-only" -> Access.READ_ONLY).
		environment.setConversionService(new ApplicationConversionService());
		environment.getPropertySources().addFirst(new MapPropertySource("profile under test", Map.of(
				"spring.config.location", "file:src/main/resources/,file:docker/config/",
				"spring.profiles.active", profile)));

		ConfigDataEnvironmentPostProcessor.applyTo(environment, new DefaultResourceLoader(),
				new DefaultBootstrapContext());
		PropertiesEndpointAccessResolver access = new PropertiesEndpointAccessResolver(environment);
		configuredEndpoints(environment).forEach(id -> access.accessFor(EndpointId.of(id), Access.UNRESTRICTED));

		assertThat(environment.getActiveProfiles()).containsExactly(profile);
	}

	private static List<String> profilesIn(Path directory) throws IOException {
		try (Stream<Path> files = Files.list(directory)) {
			return files.map(file -> PROFILE_FILE.matcher(file.getFileName().toString()))
					.filter(Matcher::matches).map(matcher -> matcher.group(1)).toList();
		}
	}

	/** The endpoint ids that have any {@code management.endpoint.<id>.*} key: the only ones a conflict can come from. */
	private static Stream<String> configuredEndpoints(StandardEnvironment environment) {
		return environment.getPropertySources().stream()
				.filter(EnumerablePropertySource.class::isInstance)
				.flatMap(source -> Arrays.stream(((EnumerablePropertySource<?>) source).getPropertyNames()))
				.map(ENDPOINT_KEY::matcher).filter(Matcher::matches).map(matcher -> matcher.group(1)).distinct();
	}
}
