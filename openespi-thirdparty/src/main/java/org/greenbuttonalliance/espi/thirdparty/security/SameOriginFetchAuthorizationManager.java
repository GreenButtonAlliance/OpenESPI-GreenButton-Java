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
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.util.Assert;

import java.util.function.Supplier;

/**
 * Fetch Metadata resource-isolation check for a tool, such as the H2 console, that is exempt from
 * CSRF tokens. Allows:
 * <ul>
 *   <li>any request the browser reports as coming from this origin
 *       ({@code Sec-Fetch-Site: same-origin}): the tool's own frames, links and forms;</li>
 *   <li>a request the user started outside any page ({@code none}: typed URL, bookmark, or a link
 *       clicked in another application) only when it is a {@code GET} of the tool's entry page
 *       ({@code <basePath>} or {@code <basePath>/}). A link from an e-mail, chat or PDF can open the
 *       tool but cannot carry an action, such as {@code login.do?url=...;INIT=RUNSCRIPT ...}.</li>
 * </ul>
 *
 * <p>Rejected: {@code cross-site}; {@code same-site}, which is how a page on another
 * {@code localhost} port arrives, and which {@code SameSite=Lax} cookies do not stop; and a missing
 * header (non-browser clients and browsers too old to send it).</p>
 */
public final class SameOriginFetchAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

	static final String SEC_FETCH_SITE = "Sec-Fetch-Site";

	private final String basePath;

	/** @param basePath the tool's path within the application, e.g. {@code /h2-console} */
	public SameOriginFetchAuthorizationManager(String basePath) {
		Assert.hasText(basePath, "basePath must not be empty");
		this.basePath = basePath.endsWith("/") ? basePath.substring(0, basePath.length() - 1) : basePath;
	}

	@Override
	public AuthorizationResult authorize(Supplier<? extends Authentication> authentication,
										 RequestAuthorizationContext context) {
		HttpServletRequest request = context.getRequest();
		String site = request.getHeader(SEC_FETCH_SITE);
		boolean allowed = "same-origin".equals(site) || ("none".equals(site) && isEntryPageGet(request));
		return new AuthorizationDecision(allowed);
	}

	private boolean isEntryPageGet(HttpServletRequest request) {
		// A plain GET with no parameters: the entry page itself, never an action on it.
		if (!HttpMethod.GET.matches(request.getMethod()) || request.getQueryString() != null
				|| !request.getParameterMap().isEmpty()) {
			return false;
		}
		String path = request.getRequestURI().substring(request.getContextPath().length());
		return path.equals(basePath) || path.equals(basePath + "/");
	}
}
