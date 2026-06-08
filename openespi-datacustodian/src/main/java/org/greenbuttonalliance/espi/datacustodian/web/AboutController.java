/*
 *
 *        Copyright (c) 2025 Green Button Alliance, Inc.
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

import org.springframework.boot.SpringBootVersion;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Public "About" page (#175). Replaces the legacy about.jsp build-info table with a small,
 * non-sensitive summary of the running implementation. Public (footer-linked from every page,
 * including the anonymous login page).
 */
@Controller
public class AboutController {

	@GetMapping("/about")
	public String about(Model model) {
		Map<String, String> info = new LinkedHashMap<>();
		String version = AboutController.class.getPackage().getImplementationVersion();
		info.put("Implementation", "OpenESPI Green Button Data Custodian");
		info.put("Version", version != null ? version : "(development build)");
		info.put("Java version", System.getProperty("java.version", "—"));
		info.put("Spring Boot version", SpringBootVersion.getVersion());
		model.addAttribute("info", info);
		return "about";
	}
}
