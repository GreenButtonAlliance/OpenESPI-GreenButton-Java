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

import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.service.RetailCustomerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * Self-service password change for any signed-in account (#180). One controller serves both portals
 * — the customer page ({@code /customer/password}) and the custodian/admin page
 * ({@code /custodian/password}) — sharing a single change routine (DRY); only the rendered view
 * (which navbar) differs. Accounts are unified {@link RetailCustomerEntity} rows distinguished by
 * role, so the logic is identical.
 *
 * <p>A user may change only their own password: the account is resolved from the authenticated
 * principal and the current password must be verified before the new one is accepted.</p>
 */
@Controller
@PreAuthorize("isAuthenticated()")
public class PasswordController {

	private final RetailCustomerService retailCustomerService;
	private final PasswordEncoder customerPasswordEncoder;

	public PasswordController(RetailCustomerService retailCustomerService,
							  PasswordEncoder customerPasswordEncoder) {
		this.retailCustomerService = retailCustomerService;
		this.customerPasswordEncoder = customerPasswordEncoder;
	}

	@GetMapping("/customer/password")
	public String customerForm() {
		return "customer/password";
	}

	@GetMapping("/custodian/password")
	public String custodianForm() {
		return "custodian/password";
	}

	@PostMapping("/customer/password")
	public String changeCustomer(Principal principal,
								 @RequestParam(required = false) String currentPassword,
								 @RequestParam(required = false) String newPassword,
								 @RequestParam(required = false) String confirmPassword,
								 Model model, RedirectAttributes redirectAttributes) {
		return change("customer/password", principal, currentPassword, newPassword, confirmPassword,
				model, redirectAttributes);
	}

	@PostMapping("/custodian/password")
	public String changeCustodian(Principal principal,
								  @RequestParam(required = false) String currentPassword,
								  @RequestParam(required = false) String newPassword,
								  @RequestParam(required = false) String confirmPassword,
								  Model model, RedirectAttributes redirectAttributes) {
		return change("custodian/password", principal, currentPassword, newPassword, confirmPassword,
				model, redirectAttributes);
	}

	/**
	 * Shared change routine. {@code view} is the portal template, which (sans leading slash) also
	 * equals the request path, so the post/redirect/get target is {@code "redirect:/" + view}.
	 */
	private String change(String view, Principal principal, String currentPassword,
						  String newPassword, String confirmPassword,
						  Model model, RedirectAttributes redirectAttributes) {
		RetailCustomerEntity account = retailCustomerService.findByUsername(principal.getName());
		if (account == null) {
			model.addAttribute("error", "Account not found.");
			return view;
		}
		if (currentPassword == null
				|| !customerPasswordEncoder.matches(currentPassword, account.getPassword())) {
			model.addAttribute("error", "Your current password is incorrect.");
			return view;
		}
		if (newPassword == null || newPassword.isBlank()) {
			model.addAttribute("error", "The new password must not be blank.");
			return view;
		}
		if (!newPassword.equals(confirmPassword)) {
			model.addAttribute("error", "The new password and confirmation do not match.");
			return view;
		}

		account.setPassword(customerPasswordEncoder.encode(newPassword));
		retailCustomerService.save(account);
		redirectAttributes.addFlashAttribute("message", "Your password has been changed.");
		return "redirect:/" + view;
	}
}
