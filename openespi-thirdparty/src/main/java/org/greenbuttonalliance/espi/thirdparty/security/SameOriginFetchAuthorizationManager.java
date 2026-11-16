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

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.function.Supplier;

/**
 * Fetch Metadata resource-isolation check: allows a request only when the browser reports that it
 * came from this origin ({@code Sec-Fetch-Site: same-origin}) or from the user directly, by typing
 * the URL or using a bookmark ({@code none}).
 *
 * <p>Used for the H2 console, which is exempt from CSRF tokens because its own forms carry none.
 * Session cookies alone do not protect it: {@code SameSite=Lax} still sends them on cross-site
 * top-level GET navigations, and every {@code localhost} port counts as the same site, so a page
 * served by another local application could otherwise drive the console with the developer's
 * session. Rejected: {@code cross-site}, {@code same-site} and a missing header (non-browser clients
 * and browsers too old to send it).</p>
 */
public final class SameOriginFetchAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

	static final String SEC_FETCH_SITE = "Sec-Fetch-Site";

	@Override
	public AuthorizationResult authorize(Supplier<? extends Authentication> authentication,
										 RequestAuthorizationContext context) {
		String site = context.getRequest().getHeader(SEC_FETCH_SITE);
		return new AuthorizationDecision("same-origin".equals(site) || "none".equals(site));
	}
}
