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

package org.greenbuttonalliance.espi.datacustodian.web.custodian;

import org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository;
import org.greenbuttonalliance.espi.common.service.RetailCustomerService;
import org.springframework.boot.SpringBootVersion;
import org.springframework.core.env.Environment;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Custodian System Settings page (#173). Deliberately <strong>read-only</strong>: it surfaces the
 * runtime configuration an operator needs to see (active profiles, service endpoints, datasource,
 * versions, entity counts). It exposes NO mutating actions — the legacy "reset / initialize
 * sandbox" DB-management commands are intentionally not reproduced here, and CRUD writes remain
 * deferred per #166.
 */
@Controller
@PreAuthorize("hasRole('ROLE_CUSTODIAN')")
public class SettingsController {

	private final Environment environment;
	private final RetailCustomerService retailCustomerService;
	private final UsagePointRepository usagePointRepository;

	public SettingsController(Environment environment,
							  RetailCustomerService retailCustomerService,
							  UsagePointRepository usagePointRepository) {
		this.environment = environment;
		this.retailCustomerService = retailCustomerService;
		this.usagePointRepository = usagePointRepository;
	}

	@GetMapping("/custodian/settings")
	public String index(Model model) {
		Map<String, String> info = new LinkedHashMap<>();

		String[] profiles = environment.getActiveProfiles();
		info.put("Active profiles", profiles.length == 0 ? "(default)" : String.join(", ", profiles));
		info.put("Data Custodian base URL",
				environment.getProperty("espi.datacustodian.base-url", "—"));
		info.put("Authorization Server issuer",
				environment.getProperty("espi.authorization-server.issuer-uri", "—"));
		info.put("Token introspection endpoint",
				environment.getProperty("espi.authorization-server.introspection-endpoint", "—"));
		info.put("Datasource URL",
				environment.getProperty("spring.datasource.url", "—"));
		info.put("Open Session In View",
				environment.getProperty("spring.jpa.open-in-view", "true"));
		info.put("Java version", System.getProperty("java.version", "—"));
		info.put("Spring Boot version", SpringBootVersion.getVersion());

		Map<String, Long> counts = new LinkedHashMap<>();
		counts.put("Retail customers", (long) retailCustomerService.findAll().size());
		counts.put("Usage points", usagePointRepository.count());

		model.addAttribute("info", info);
		model.addAttribute("counts", counts);
		return "custodian/settings";
	}
}
