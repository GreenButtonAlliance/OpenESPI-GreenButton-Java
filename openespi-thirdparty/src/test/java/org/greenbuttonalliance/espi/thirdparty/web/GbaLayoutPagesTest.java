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

package org.greenbuttonalliance.espi.thirdparty.web;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
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
 * Third Party pages render through fragments/layout (shared GBA theme, logo, context-relative
 * links) and the public pages linked from the navbar are reachable without logging in.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GbaLayoutPagesTest {

	@Autowired
	private MockMvc mockMvc;

	/** The production context path; rendering under it is what makes the #235 check meaningful. */
	private static final String CONTEXT_PATH = "/ThirdParty";

	@ParameterizedTest
	@ValueSource(strings = {"/", "/login", "/about", "/TermsOfService", "/UsagePolicy"})
	void public_pages_render_anonymously_with_the_gba_layout(String path) throws Exception {
		// Render under the real context path. With MockMvc's default empty context path, a correct
		// th:href="@{/about}" and a broken href="/about" produce the same HTML, so #235 could not
		// be detected.
		String html = mockMvc.perform(get(CONTEXT_PATH + path).contextPath(CONTEXT_PATH))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(html)
				.contains(CONTEXT_PATH + "/css/gba-theme.css", CONTEXT_PATH + "/images/gba-logo.png",
						CONTEXT_PATH + "/images/gba-favicon.png")
				.contains("gba-navbar", "gba-footer", "data-gba-app=\"thirdparty\"")
				.contains("integrity=\"sha384-")
				// #235: every internal link must carry the context path. Allowed: /ThirdParty...,
				// protocol-relative //host and absolute https:// links.
				.doesNotContainPattern("(href|src)=\"/(?!ThirdParty[/\"]|/)");
	}

	@ParameterizedTest
	@CsvSource({
			"/css/gba-theme.css,     text/css",
			"/images/gba-logo.png,    image/png",
			"/images/gba-favicon.png, image/png"})
	void shared_assets_are_served_anonymously(String path, String contentType) throws Exception {
		mockMvc.perform(get(path))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(contentType));
	}
}
