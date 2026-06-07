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

package org.greenbuttonalliance.espi.datacustodian.web.customer;

import org.greenbuttonalliance.espi.common.domain.usage.AuthorizationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.service.AuthorizationService;
import org.greenbuttonalliance.espi.common.service.RetailCustomerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * Customer self-service portal (#173). A signed-in retail customer (ROLE_USER) sees the third-party
 * authorizations granted against their data and can revoke any of them. This is the post-login
 * landing for non-custodian users.
 *
 * <p>The authenticated principal carries only the username, so the customer is resolved via
 * {@link RetailCustomerService#findByUsername(String)}. Open-Session-In-View is disabled, so the
 * read handler is {@link Transactional} and projects entities into flat {@link AuthorizationView}
 * records before the template renders. Revoke is authorization-checked: a customer may only revoke
 * an authorization that belongs to them.</p>
 */
@Controller
@PreAuthorize("isAuthenticated()")
public class CustomerAuthorizationController {

	private final RetailCustomerService retailCustomerService;
	private final AuthorizationService authorizationService;

	public CustomerAuthorizationController(RetailCustomerService retailCustomerService,
										   AuthorizationService authorizationService) {
		this.retailCustomerService = retailCustomerService;
		this.authorizationService = authorizationService;
	}

	@GetMapping({"/customer", "/customer/home", "/customer/authorizations"})
	@Transactional(readOnly = true)
	public String authorizations(Principal principal, Model model) {
		RetailCustomerEntity customer = retailCustomerService.findByUsername(principal.getName());
		List<AuthorizationView> authorizations = customer == null ? List.of()
				: authorizationService.findAllByRetailCustomerId(customer.getId()).stream()
						.map(CustomerAuthorizationController::toView)
						.toList();
		model.addAttribute("authorizations", authorizations);
		return "customer/authorizations";
	}

	@PostMapping("/customer/authorizations/{authorizationId}/revoke")
	@Transactional
	public String revoke(@PathVariable UUID authorizationId, Principal principal,
						 RedirectAttributes redirectAttributes) {
		RetailCustomerEntity customer = retailCustomerService.findByUsername(principal.getName());
		AuthorizationEntity authorization = authorizationService.findById(authorizationId);

		if (customer == null || authorization == null
				|| authorization.getRetailCustomer() == null
				|| !customer.getId().equals(authorization.getRetailCustomer().getId())) {
			// Never let a customer act on an authorization that is not theirs.
			redirectAttributes.addFlashAttribute("message", "Authorization not found.");
			return "redirect:/customer/authorizations";
		}

		authorization.setStatus(AuthorizationEntity.STATUS_REVOKED);
		authorizationService.save(authorization);
		redirectAttributes.addFlashAttribute("message", "Access revoked.");
		return "redirect:/customer/authorizations";
	}

	private static AuthorizationView toView(AuthorizationEntity a) {
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
		return new AuthorizationView(
				a.getId() == null ? null : a.getId().toString(),
				a.getThirdParty(),
				a.getScope(),
				status,
				a.isRevoked() || a.isExpired());
	}

	/** Flat projection safe to render with OSIV disabled. {@code terminal} = cannot be revoked. */
	public record AuthorizationView(String id, String thirdParty, String scope, String status,
									boolean terminal) {
	}
}
