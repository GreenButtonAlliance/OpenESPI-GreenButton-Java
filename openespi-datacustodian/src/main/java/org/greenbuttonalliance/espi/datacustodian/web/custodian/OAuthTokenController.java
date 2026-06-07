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

import org.greenbuttonalliance.espi.common.domain.usage.AuthorizationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.service.AuthorizationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Custodian OAuth Token Management page (#173). Read-only view of the Authorization grants the Data
 * Custodian holds. The legacy portal shipped this page as an empty placeholder; this renders a real
 * table over {@link AuthorizationService#findAll()}.
 *
 * <p>Open-Session-In-View is disabled ({@code spring.jpa.open-in-view=false}), so the lazy
 * {@code retailCustomer} relation cannot be touched from the template. The handler is
 * {@link Transactional} and projects each entity into a fully-materialized {@link TokenView} record
 * before returning, so the view renders only flat data.</p>
 */
@Controller
@PreAuthorize("hasRole('ROLE_CUSTODIAN')")
public class OAuthTokenController {

	private final AuthorizationService authorizationService;

	public OAuthTokenController(AuthorizationService authorizationService) {
		this.authorizationService = authorizationService;
	}

	@GetMapping("/custodian/oauth/tokens")
	@Transactional(readOnly = true)
	public String index(Model model) {
		List<TokenView> tokens = authorizationService.findAll().stream()
				.map(OAuthTokenController::toView)
				.toList();
		model.addAttribute("tokens", tokens);
		return "custodian/oauth/tokens";
	}

	private static TokenView toView(AuthorizationEntity a) {
		RetailCustomerEntity customer = a.getRetailCustomer();
		String customerName = customer == null ? "—" : customer.getUsername();

		String status;
		if (a.isRevoked()) {
			status = "REVOKED";
		} else if (a.isExpired()) {
			status = "EXPIRED";
		} else if (a.isActive()) {
			status = "ACTIVE";
		} else {
			status = "PENDING";
		}

		return new TokenView(
				customerName,
				a.getThirdParty(),
				a.getScope(),
				status,
				a.getGrantType() == null ? "—" : a.getGrantType().toString(),
				mask(a.getAccessToken()));
	}

	/** Show only the last 4 characters of a token so the page never leaks a usable credential. */
	private static String mask(String token) {
		if (token == null || token.isBlank()) {
			return "—";
		}
		String trimmed = token.trim();
		return trimmed.length() <= 4 ? "••••" : "••••" + trimmed.substring(trimmed.length() - 4);
	}

	/** Flat, fully-materialized projection safe to render with OSIV disabled. */
	public record TokenView(String customer, String thirdParty, String scope, String status,
							String grantType, String maskedToken) {
	}
}
