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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The shared Green Button Alliance theme, logo and favicon ship in the OpenESPI-Common jar
 * (META-INF/resources) and must be served anonymously, and the portal layout must reference them.
 * Guards both the DC resource handlers (@EnableWebMvc disables Boot's default static mappings) and
 * the root-pom resource packaging that once dropped every binary file from the jars.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GbaThemeResourcesTest {

	@Autowired
	private MockMvc mockMvc;

	@ParameterizedTest
	@CsvSource({
			"/css/gba-theme.css,     text/css",
			"/images/gba-logo.png,    image/png",
			"/images/gba-favicon.png, image/png",
			"/images/favicon.png,     image/png"})
	void shared_assets_are_served_anonymously(String path, String contentType) throws Exception {
		byte[] body = mockMvc.perform(get(path))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(contentType))
				.andReturn().getResponse().getContentAsByteArray();

		assertThat(body).isNotEmpty();
	}

	@Test
	void logo_is_packaged_unfiltered() throws Exception {
		byte[] logo = mockMvc.perform(get("/images/gba-logo.png"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsByteArray();

		// PNG signature: resource filtering would have corrupted these bytes
		assertThat(logo).startsWith(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A);
	}

	@Test
	void home_page_uses_the_gba_layout() throws Exception {
		String html = mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(html)
				.contains("/css/gba-theme.css", "/images/gba-logo.png", "/images/gba-favicon.png")
				.contains("gba-navbar", "gba-footer")
				.contains("integrity=\"sha384-");
	}
}
