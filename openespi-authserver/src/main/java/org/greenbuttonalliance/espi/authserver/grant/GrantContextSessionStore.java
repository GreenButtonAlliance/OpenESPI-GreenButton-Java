/*
 * Copyright 2025-2026 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.greenbuttonalliance.espi.authserver.grant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.WebUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Keeps each delegation's {@link GrantContext} in the browser session between
 * {@code /oauth2/authorize/continue} and the replayed {@code /oauth2/authorize} on which Spring Authorization
 * Server mints the code.
 *
 * <p>#305: one context per delegation, keyed by its correlation id, never one slot per session, so two
 * authorizations running at once in one browser (two tabs) cannot take each other's context. The replay names
 * its delegation with {@link #DELEGATION_PARAMETER}; a request without it, or with an unknown or expired one,
 * finds nothing. Each context expires after {@link #TTL}, so a replay the browser never follows leaves nothing
 * a later authorization can use.</p>
 *
 * <p>Being in the session also binds it to the browser that completed the delegation: the id alone, without
 * that session, finds nothing.</p>
 */
@Service
public class GrantContextSessionStore {

	static final String SESSION_KEY = "espi.grant_contexts";

	/** The replayed {@code /oauth2/authorize} parameter naming its delegation (the correlation id). */
	public static final String DELEGATION_PARAMETER = "espi_delegation";

	/** As long as the delegation state ({@code DelegationStateService}). */
	public static final Duration TTL = Duration.ofMinutes(5);

	/** Keep the context for its replay; expired contexts in the session are dropped. */
	public void put(HttpSession session, GrantContext ctx) {
		synchronized (lock(session)) {
			Map<String, GrantContext> contexts = contexts(session);
			Instant now = Instant.now();
			contexts.values().removeIf(c -> c.isExpired(now));
			contexts.put(ctx.correlationId(), ctx);
			// Set again so a session store that persists attributes (not only in memory) sees the change.
			session.setAttribute(SESSION_KEY, contexts);
		}
	}

	/** The unexpired context of this delegation, left in place; null if none. */
	public GrantContext peek(HttpSession session, String correlationId) {
		if (session == null || !StringUtils.hasText(correlationId)) {
			return null;
		}
		synchronized (lock(session)) {
			GrantContext ctx = contexts(session).get(correlationId);
			return ctx == null || ctx.isExpired(Instant.now()) ? null : ctx;
		}
	}

	/** Removes this delegation's context and returns it if it had not expired; null otherwise. Single use. */
	public GrantContext consume(HttpSession session, String correlationId) {
		if (session == null || !StringUtils.hasText(correlationId)) {
			return null;
		}
		synchronized (lock(session)) {
			Map<String, GrantContext> contexts = contexts(session);
			GrantContext ctx = contexts.remove(correlationId);
			session.setAttribute(SESSION_KEY, contexts);
			return ctx == null || ctx.isExpired(Instant.now()) ? null : ctx;
		}
	}

	/** {@link #peek(HttpSession, String)} for the delegation the request names; null if it names none. */
	public GrantContext peek(HttpServletRequest request) {
		return request == null ? null
				: peek(request.getSession(false), request.getParameter(DELEGATION_PARAMETER));
	}

	/** {@link #consume(HttpSession, String)} for the delegation the request names; null if it names none. */
	public GrantContext consume(HttpServletRequest request) {
		return request == null ? null
				: consume(request.getSession(false), request.getParameter(DELEGATION_PARAMETER));
	}

	/** The request being served on this thread, or null outside one. */
	public static HttpServletRequest currentRequest() {
		RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
		return attrs instanceof ServletRequestAttributes sra ? sra.getRequest() : null;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, GrantContext> contexts(HttpSession session) {
		Object value = session.getAttribute(SESSION_KEY);
		return value instanceof Map<?, ?> map ? (Map<String, GrantContext>) map : new HashMap<>();
	}

	private static Object lock(HttpSession session) {
		return WebUtils.getSessionMutex(session);
	}
}
