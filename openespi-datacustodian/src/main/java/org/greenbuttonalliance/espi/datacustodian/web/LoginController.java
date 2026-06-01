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

import org.greenbuttonalliance.espi.datacustodian.web.constants.Routes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Renders the customer / custodian login form.
 *
 * <p>Authentication itself is handled by Spring Security's {@code UsernamePasswordAuthentication
 * Filter} on the same {@code /login} path, configured in {@code CustomerLoginSecurityConfiguration}.
 * This controller only serves the GET that renders {@code login.html}.</p>
 *
 * <p>An optional {@code return_to} query parameter is preserved as a hidden form input so it
 * round-trips through the POST to the success handler. The success handler honors it as the
 * post-authentication redirect destination &mdash; the path used by the AS&rarr;DC delegation flow
 * (PR C3) to bring the customer back to the AS once they've authenticated.</p>
 */
@Controller
@RequestMapping(Routes.LOGIN)
public class LoginController {

	@GetMapping
	public String index(@RequestParam(value = "return_to", required = false) String returnTo,
						Model model) {
		if (returnTo != null && !returnTo.isBlank()) {
			model.addAttribute("returnTo", returnTo);
		}
		return "login";
	}
}
