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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.util.Assert;

import java.net.URI;
import java.util.function.Supplier;

/**
 * Request-origin check for a tool, such as the H2 console, that is exempt from CSRF tokens. Allows:
 * <ul>
 *   <li>the tool's <em>entry page</em> ({@code <basePath>} or {@code <basePath>/}, a {@code GET}
 *       with no parameters) when the browser reports it as same-origin or user-initiated
 *       ({@code Sec-Fetch-Site: same-origin} or {@code none}: typed URL, bookmark, or a link
 *       clicked in another application). The entry page performs no action.</li>
 *   <li>any other request only when it is same-origin <em>and</em> its {@code Referer} is a page of
 *       the tool itself. The tool's own frames, forms and scripts always send one.</li>
 * </ul>
 *
 * <p>{@code Sec-Fetch-Site} alone is not enough: a same-origin redirect also arrives as
 * {@code same-origin}. Without the {@code Referer} rule, an action URL from an attacker
 * ({@code login.do?url=...;INIT=RUNSCRIPT ...}) could be laundered through the TP's sign-in:
 * saved as the interrupted request, or passed as {@code return_to}, and then replayed by the
 * redirect after the login form's POST. Such a redirect carries the login page's {@code Referer},
 * so it is refused.</p>
 *
 * <p>Also rejected: {@code cross-site}; {@code same-site}, which is how a page on another
 * {@code localhost} port arrives, and which {@code SameSite=Lax} cookies do not stop; and a missing
 * header (non-browser clients, and browsers that are too old or strip the {@code Referer}).</p>
 */
public final class SameOriginFetchAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

	static final String SEC_FETCH_SITE = "Sec-Fetch-Site";

	private final String basePath;

	/** @param basePath the tool's path within the application, e.g. {@code /h2-console} (no trailing slash) */
	public SameOriginFetchAuthorizationManager(String basePath) {
		Assert.hasText(basePath, "basePath must not be empty");
		Assert.isTrue(basePath.startsWith("/") && !basePath.endsWith("/"),
				"basePath must start with '/' and must not end with '/': " + basePath);
		this.basePath = basePath;
	}

	@Override
	public AuthorizationResult authorize(Supplier<? extends Authentication> authentication,
										 RequestAuthorizationContext context) {
		HttpServletRequest request = context.getRequest();
		String site = request.getHeader(SEC_FETCH_SITE);
		boolean allowed;
		if (isEntryPage(request)) {
			allowed = "same-origin".equals(site) || "none".equals(site);
		}
		else {
			allowed = "same-origin".equals(site) && refererIsInsideTool(request);
		}
		return new AuthorizationDecision(allowed);
	}

	/** A plain {@code GET} of {@code <basePath>} or {@code <basePath>/} with no parameters: no action. */
	public boolean isEntryPage(HttpServletRequest request) {
		if (!HttpMethod.GET.matches(request.getMethod()) || request.getQueryString() != null
				|| !request.getParameterMap().isEmpty()) {
			return false;
		}
		String path = request.getRequestURI().substring(request.getContextPath().length());
		return path.equals(basePath) || path.equals(basePath + "/");
	}

	private boolean refererIsInsideTool(HttpServletRequest request) {
		String referer = request.getHeader(HttpHeaders.REFERER);
		if (referer == null) {
			return false;
		}
		try {
			String path = URI.create(referer).normalize().getRawPath();
			String tool = request.getContextPath() + basePath;
			return path != null && (path.equals(tool) || path.startsWith(tool + "/"));
		}
		catch (IllegalArgumentException e) {
			return false;
		}
	}
}
