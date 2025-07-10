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

package org.greenbuttonalliance.espi.thirdparty.web;

// UserRoles constants replaced with direct strings
import org.greenbuttonalliance.espi.common.service.RetailCustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;

@Controller
public class DefaultController {

	@Autowired
	private RetailCustomerService retailCustomerService;

	@GetMapping("/default")
	public String defaultAfterLogin(HttpServletRequest request, Principal principal) {
		if (request.isUserInRole("ROLE_CUSTODIAN")) {
			return "redirect:/custodian/home";
		} else if (request.isUserInRole("ROLE_USER")) {
			if (principal instanceof Authentication auth) {
				var customer = retailCustomerService.findByUsername(auth.getName());
				if (customer != null) {
					return "redirect:/RetailCustomer/" + customer.getId() + "/home";
				}
			}
		}
		return "redirect:/home";
	}
}
