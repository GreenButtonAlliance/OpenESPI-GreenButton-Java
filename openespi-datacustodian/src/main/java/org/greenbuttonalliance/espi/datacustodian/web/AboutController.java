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

package org.greenbuttonalliance.espi.datacustodian.web;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Public "About" page (#175), footer-linked from every page including the anonymous login page.
 *
 * <p>Shows only what identifies the product: name, version, build time, the ESPI standard it
 * implements and its release status. It deliberately does NOT show the Java or Spring Boot version:
 * exact framework versions help attackers pick known CVEs, so they stay behind the admin-only
 * {@code /actuator/info} (#240). The version and build time come from {@link BuildProperties}
 * ({@code spring-boot-maven-plugin:build-info}), which, unlike the jar manifest, is also present
 * under {@code spring-boot:run}.</p>
 */
@Controller
public class AboutController {

	static final String IMPLEMENTATION = "OpenESPI Green Button Data Custodian";
	static final String ESPI_STANDARD = "NAESB REQ.21 ESPI, version 4.1";

	private static final DateTimeFormatter BUILD_TIME =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

	private final ObjectProvider<BuildProperties> buildProperties;

	public AboutController(ObjectProvider<BuildProperties> buildProperties) {
		this.buildProperties = buildProperties;
	}

	@GetMapping("/about")
	public String about(Model model) {
		BuildProperties build = buildProperties.getIfAvailable();
		String version = build != null ? build.getVersion() : null;

		Map<String, String> info = new LinkedHashMap<>();
		info.put("Implementation", IMPLEMENTATION);
		info.put("Version", version != null ? version : "(development build)");
		if (build != null && build.getTime() != null) {
			info.put("Build time", BUILD_TIME.format(build.getTime()));
		}
		info.put("Standard", ESPI_STANDARD);
		info.put("Status", releaseStatus(version));
		model.addAttribute("info", info);
		return "about";
	}

	/** A version without a release (null or a SNAPSHOT) is pre-release and not for production use. */
	static String releaseStatus(String version) {
		return version == null || version.endsWith("-SNAPSHOT")
				? "Pre-release: not for production use"
				: "Release";
	}
}
