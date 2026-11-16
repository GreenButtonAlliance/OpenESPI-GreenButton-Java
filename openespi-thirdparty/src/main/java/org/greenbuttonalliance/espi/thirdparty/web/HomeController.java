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

package org.greenbuttonalliance.espi.thirdparty.web;

import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The Third Party home page. A TP administrator ({@code ROLE_CUSTODIAN}) is sent to the admin page;
 * everyone else gets {@code home}, which for a signed-in TP customer ({@code ROLE_USER}) shows the
 * Data Access Authorization cards used to authorize the TP at a Data Custodian (#249).
 */
@Controller
public class HomeController {

	@GetMapping("/")
	public String index(HttpServletRequest request) {
		if (request.isUserInRole(RetailCustomerEntity.ROLE_CUSTODIAN)) {
			return "redirect:/custodian/home";
		}
		return "home";
	}

	@GetMapping("/home")
	public String home(HttpServletRequest request) {
		return index(request);
	}

	@GetMapping("/TermsOfService")
	public String termsOfService() {
		return "/TermsOfService";
	}

	@GetMapping("/UsagePolicy")
	public String usagePolicy() {
		return "/UsagePolicy";
	}

}