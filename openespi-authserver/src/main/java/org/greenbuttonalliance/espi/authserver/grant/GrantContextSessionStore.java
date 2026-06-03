/*
 * Copyright 2025 Green Button Alliance, Inc.
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

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper over {@link HttpSession} for stashing the {@link GrantContext} between
 * {@code /oauth2/authorize/continue} and the subsequent {@code /oauth2/authorize} call where
 * Spring Authorization Server creates the {@code OAuth2Authorization} aggregate.
 *
 * <p>The browser session is the natural carrier here: both {@code /continue} and {@code /authorize}
 * are GET requests made by the same user-agent in the same session, and the
 * {@link GrantContextEnrichingAuthorizationService} runs inside the {@code /authorize} request
 * processing so it has access to the same session.</p>
 *
 * <p>Single-use: {@link #consume} reads then removes. The session itself only carries this
 * attribute for the brief window between the two GETs.</p>
 */
@Service
public class GrantContextSessionStore {

	static final String SESSION_KEY = "espi.grant_context";

	/** Stash the context for the upcoming Spring AS authorization flow. */
	public void put(HttpSession session, GrantContext ctx) {
		session.setAttribute(SESSION_KEY, ctx);
	}

	/**
	 * Read and remove the context. Single-use: a second consume returns {@code null}. Returns
	 * {@code null} when the session is missing the attribute (e.g. M2M token-exchange requests
	 * that arrive on a fresh session without going through {@code /continue}).
	 */
	public GrantContext consume(HttpSession session) {
		GrantContext ctx = (GrantContext) session.getAttribute(SESSION_KEY);
		if (ctx != null) {
			session.removeAttribute(SESSION_KEY);
		}
		return ctx;
	}

	/** Read without remove — for inspection / debugging. */
	public GrantContext peek(HttpSession session) {
		return (GrantContext) session.getAttribute(SESSION_KEY);
	}
}
