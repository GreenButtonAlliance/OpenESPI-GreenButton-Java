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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The DC session cookie is SameSite=Lax in every profile (#271): the Authorization Server sends the customer to
 * {@code /oauth/authorize-screen}, a cross-site navigation that must carry the customer's DC sign-in, so Strict
 * would break the customer flow.
 */
@DisplayName("DC session cookie is SameSite=Lax in every profile (#271)")
class SessionCookieSameSiteTest {

	private static final String KEY = "server.servlet.session.cookie.same-site";

	/** This module's own profile files; surefire runs with the module directory as the working directory. */
	private static final Path RESOURCES = Path.of("src", "main", "resources");

	@Test
	@DisplayName("application.yml sets lax, and no profile file sets anything else")
	void laxInEveryProfile() throws IOException {
		Map<String, String> sameSiteByFile;
		try (Stream<Path> files = Files.list(RESOURCES)) {
			sameSiteByFile = files
					.filter(file -> file.getFileName().toString().matches("application.*\\.ya?ml"))
					.collect(Collectors.toMap(file -> file.getFileName().toString(), SessionCookieSameSiteTest::sameSite));
		}

		assertThat(sameSiteByFile)
				.as("same-site per profile file (\"\" = not set)")
				.containsEntry("application.yml", "lax")
				.allSatisfy((file, value) -> assertThat(value).as(file).isIn("", "lax"));
	}

	private static String sameSite(Path file) {
		YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
		yaml.setResources(new FileSystemResource(file));
		Properties properties = yaml.getObject();
		return properties == null ? "" : properties.getProperty(KEY, "").toLowerCase();
	}
}
