/*
 * Copyright (c) 2026 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.greenbuttonalliance.espi.datacustodian.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The About page is public, identifies the product and its release status, and does not disclose
 * the Java or Spring Boot version to anonymous visitors.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AboutControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectProvider<BuildProperties> buildProperties;

	@Test
	void about_is_public_and_shows_product_identity_without_framework_versions() throws Exception {
		String html = mockMvc.perform(get("/about"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(html)
				.contains(AboutController.IMPLEMENTATION, AboutController.ESPI_STANDARD, "Status", "SECURITY.md")
				.doesNotContain("Java version")
				// The framework is not named at all (#240), not only its version: "Java" cannot be checked this
				// way, since the repository URL (OpenESPI-GreenButton-Java) contains it.
				.doesNotContainIgnoringCase("Spring Boot", "spring-boot", "springboot")
				.doesNotContainPattern(standalone(System.getProperty("java.version")))
				.doesNotContainPattern(standalone(SpringBootVersion.getVersion()));
	}

	/**
	 * {@code version} as a number of its own anywhere in the page, markup and comments included (#277), so that a
	 * {@code java.version} of exactly {@code 25} matches neither a class such as Bootstrap's {@code w-25} nor the
	 * build time ({@code 2026-11-25 10:25 UTC}) nor the product's own version ({@code 4.2.0-SNAPSHOT} when Spring
	 * Boot is {@code 4.2.0}), while {@code Java 25.}, {@code Java:25}, {@code Java25} or {@code v4.2.0} still match.
	 * Not before it: a digit, a {@code -} after a letter or digit, a {@code .} or {@code :} after a digit; not after
	 * it: a letter or digit, a {@code .} or {@code :} before a digit, or a {@code -} before a letter or digit. A
	 * hyphenated {@code Java-25} is therefore not caught. The same helper is in openespi-thirdparty's
	 * {@code AboutControllerTest}: change both.
	 */
	private static Pattern standalone(String version) {
		return Pattern.compile("(?<!\\d|\\w-|\\d[.:])" + Pattern.quote(version) + "(?!\\w|[.:]\\d|-\\w)");
	}

	@ParameterizedTest(name = "\"{0}\" shows version {1}: {2}")
	@CsvSource(delimiter = '|', value = {
			"<th class=\"w-25\">Key</th>             | 25    | false",
			"2026-11-25 10:25 UTC                    | 25    | false",
			"Version 4.0.0-SNAPSHOT 125 25.0.1       | 25    | false",
			"Running on Java 25.                     | 25    | true",
			"Java:25                                 | 25    | true",
			"Java25 v25                              | 25    | true",
			"Spring Boot v4.2.0                      | 4.2.0 | true",
			"Version 4.2.0-SNAPSHOT 4.2.0-RC1        | 4.2.0 | false",
			"<!-- built with Java 25 -->             | 25    | true",
			"<meta content=\"Spring Boot 4.2.0\">    | 4.2.0 | true",
			"Spring Boot 4.2.0.                      | 4.2.0 | true",
			"Spring Boot 4.2.0.1                     | 4.2.0 | false"})
	@DisplayName("#277: a version is found as a number of its own, in text, markup and comments")
	void standaloneVersionPattern(String page, String version, boolean found) {
		assertThat(standalone(version).matcher(page).find()).isEqualTo(found);
	}

	@Test
	void about_shows_the_build_version_when_build_info_is_present() throws Exception {
		BuildProperties build = buildProperties.getIfAvailable();
		String html = mockMvc.perform(get("/about"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		// build-info.properties is generated by spring-boot-maven-plugin:build-info in a Maven build
		assertThat(build).as("BuildProperties (spring-boot-maven-plugin:build-info)").isNotNull();
		assertThat(html).contains(build.getVersion(), AboutController.releaseStatus(build.getVersion()), "Build time");
	}

	@Test
	void release_status_follows_the_version() {
		assertThat(AboutController.releaseStatus("4.0.0-SNAPSHOT")).startsWith("Pre-release");
		assertThat(AboutController.releaseStatus(null)).startsWith("Pre-release");
		assertThat(AboutController.releaseStatus("4.0.0")).isEqualTo("Release");
	}
}
