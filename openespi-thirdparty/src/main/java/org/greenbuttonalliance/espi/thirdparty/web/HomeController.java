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

import jakarta.servlet.http.HttpServletRequest;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.AuthorizationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * The Third Party home page. A TP administrator ({@code ROLE_CUSTODIAN}) is sent to the admin page;
 * everyone else gets {@code home}, which for a signed-in TP customer ({@code ROLE_USER}) shows the
 * Data Access Authorization cards (#249) and the customer's stored authorizations (#146).
 */
@Controller
public class HomeController {

	private final AuthorizationRepository authorizationRepository;
	private final ClientRegistrationRepository clientRegistrationRepository;

	public HomeController(AuthorizationRepository authorizationRepository,
						  ClientRegistrationRepository clientRegistrationRepository) {
		this.authorizationRepository = authorizationRepository;
		this.clientRegistrationRepository = clientRegistrationRepository;
	}

	@GetMapping("/")
	public String index(HttpServletRequest request, Model model) {
		if (request.isUserInRole(RetailCustomerEntity.ROLE_CUSTODIAN)) {
			return "redirect:/custodian/home";
		}
		if (request.isUserInRole(RetailCustomerEntity.ROLE_USER) && request.getUserPrincipal() != null) {
			model.addAttribute("authorizations", authorizationsOf(request.getUserPrincipal().getName()));
		}
		return "home";
	}

	@GetMapping("/home")
	public String home(HttpServletRequest request, Model model) {
		return index(request, model);
	}

	@GetMapping("/TermsOfService")
	public String termsOfService() {
		return "/TermsOfService";
	}

	@GetMapping("/UsagePolicy")
	public String usagePolicy() {
		return "/UsagePolicy";
	}

	/** The customer's Data Custodian authorizations obtained by this TP, newest first. */
	private List<AuthorizationView> authorizationsOf(String username) {
		return authorizationRepository
				.findAllByRetailCustomer_UsernameAndClientRegistrationIdIsNotNullOrderByUpdatedDesc(username).stream()
				.map(authorization -> AuthorizationView.of(authorization, clientRegistrationRepository))
				.toList();
	}
}
