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
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test helper (#271): the DC's Spring profiles, and each profile's configuration resolved the way startup resolves it.
 */
final class ProfileConfig {

	private static final Pattern PROFILE_FILE = Pattern.compile("application-(.+)\\.(ya?ml|properties)");
	private static final Pattern ENDPOINT_KEY = Pattern.compile("management\\.endpoint\\.([a-z0-9-]+)\\..+");

	private ProfileConfig() {
	}

	/**
	 * Every profile with an {@code application-<profile>} file at the classpath root: the DC's own, openespi-common's
	 * and the test resources'. A new profile file is covered without changing the tests.
	 */
	static List<String> profiles() throws IOException {
		Resource[] files = new PathMatchingResourcePatternResolver().getResources("classpath*:application-*");
		return Arrays.stream(files).map(Resource::getFilename).filter(Objects::nonNull)
				.map(PROFILE_FILE::matcher).filter(Matcher::matches).map(matcher -> matcher.group(1))
				.distinct().sorted().toList();
	}

	/**
	 * The profile's configuration as {@code SPRING_PROFILES_ACTIVE=<profile>} resolves it: config data (which refuses
	 * duplicate YAML keys and {@code spring.profiles.*} in a profile-specific file), then every configured actuator
	 * endpoint's access (which refuses {@code enabled} alongside {@code access}). Throws what startup would throw.
	 */
	static StandardEnvironment load(String profile) {
		StandardEnvironment environment = new StandardEnvironment();
		// As SpringApplication does: Boot's conversions (e.g. "read-only" -> Access.READ_ONLY).
		environment.setConversionService(new ApplicationConversionService());
		environment.getPropertySources().addFirst(
				new MapPropertySource("profile under test", Map.of("spring.profiles.active", profile)));

		ConfigDataEnvironmentPostProcessor.applyTo(environment, new DefaultResourceLoader(),
				new DefaultBootstrapContext());
		PropertiesEndpointAccessResolver access = new PropertiesEndpointAccessResolver(environment);
		environment.getPropertySources().stream()
				.filter(EnumerablePropertySource.class::isInstance)
				.flatMap(source -> Arrays.stream(((EnumerablePropertySource<?>) source).getPropertyNames()))
				.map(ENDPOINT_KEY::matcher).filter(Matcher::matches).map(matcher -> matcher.group(1)).distinct()
				.forEach(id -> access.accessFor(EndpointId.of(id), Access.UNRESTRICTED));
		return environment;
	}
}
