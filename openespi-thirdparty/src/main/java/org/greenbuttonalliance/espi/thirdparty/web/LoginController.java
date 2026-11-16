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

import org.greenbuttonalliance.espi.thirdparty.security.ReturnToAuthenticationSuccessHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Renders the Third Party's username/password login form (#249). Spring Security's form login
 * handles the POST to the same path ({@code SecurityConfiguration}). A same-origin
 * {@code return_to} query parameter is carried through the form as a hidden field and honored by
 * {@link ReturnToAuthenticationSuccessHandler}; anything else is dropped here.
 */
@Controller
@RequestMapping("/login")
public class LoginController {

	@GetMapping
	public String index(@RequestParam(value = ReturnToAuthenticationSuccessHandler.RETURN_TO_PARAMETER,
			required = false) String returnTo, Model model) {
		if (ReturnToAuthenticationSuccessHandler.isSafeReturnTo(returnTo)) {
			model.addAttribute("returnTo", returnTo);
		}
		return "login";
	}
}