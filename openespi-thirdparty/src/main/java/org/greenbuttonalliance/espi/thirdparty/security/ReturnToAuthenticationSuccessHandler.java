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

package org.greenbuttonalliance.espi.thirdparty.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

import java.io.IOException;
import java.net.URI;

/**
 * Form-login success handler for the Third Party (#249). Redirects to, in order:
 * <ol>
 *   <li>the {@code return_to} form parameter, when it is a same-origin path;</li>
 *   <li>the request the login page interrupted;</li>
 *   <li>the role landing page: {@code /custodian/home} for a TP administrator
 *       ({@code ROLE_CUSTODIAN}), otherwise the customer home {@code /}, which shows the
 *       Data Access Authorization cards.</li>
 * </ol>
 */
public class ReturnToAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	public static final String RETURN_TO_PARAMETER = "return_to";
	static final String CUSTODIAN_LANDING = "/custodian/home";
	static final String CUSTOMER_LANDING = "/";

	private final RequestCache requestCache = new HttpSessionRequestCache();

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
										Authentication authentication) throws IOException {
		String returnTo = request.getParameter(RETURN_TO_PARAMETER);
		SavedRequest savedRequest = requestCache.getRequest(request, response);
		String target;
		if (isSafeReturnTo(returnTo)) {
			target = returnTo;
		}
		else if (savedRequest != null) {
			target = savedRequest.getRedirectUrl();
		}
		else {
			target = landingFor(authentication);
		}
		requestCache.removeRequest(request, response);
		clearAuthenticationAttributes(request);
		getRedirectStrategy().sendRedirect(request, response, target);
	}

	static String landingFor(Authentication authentication) {
		boolean custodian = authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.anyMatch(RetailCustomerEntity.ROLE_CUSTODIAN::equals);
		return custodian ? CUSTODIAN_LANDING : CUSTOMER_LANDING;
	}

	/**
	 * Accept only same-origin paths ({@code /foo}): no scheme, no host, and none of the forms a
	 * browser reads as protocol-relative ({@code //host}, {@code /\host}).
	 */
	public static boolean isSafeReturnTo(String returnTo) {
		if (returnTo == null || returnTo.isBlank()) return false;
		if (!returnTo.startsWith("/") || returnTo.startsWith("//") || returnTo.indexOf('\\') >= 0) return false;
		try {
			URI uri = URI.create(returnTo);
			return uri.getScheme() == null && uri.getRawAuthority() == null;
		}
		catch (IllegalArgumentException e) {
			return false;
		}
	}
}
