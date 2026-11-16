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

package org.greenbuttonalliance.espi.thirdparty.config;

import org.greenbuttonalliance.espi.thirdparty.security.SameOriginFetchAuthorizationManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Security for the H2 web console, a development-only diagnostic tool. Loaded only where the
 * console is enabled ({@code spring.h2.console.enabled=true}: the {@code local}, {@code dev} and
 * {@code h2} profiles). Everywhere else this configuration does not exist and the main chain in
 * {@link SecurityConfiguration} denies {@code /h2-console/**}.
 *
 * <p>The console runs arbitrary SQL, and H2's {@code CREATE ALIAS} runs Java code. Its own forms
 * carry no CSRF token, so CSRF tokens are off for this chain and it is guarded instead by:</p>
 * <ul>
 *   <li>H2 itself answering localhost only ({@code web-allow-others: false} on every profile that
 *       enables the console);</li>
 *   <li>a signed-in TP administrator ({@code ROLE_CUSTODIAN}; the session from the TP's form login
 *       applies here too);</li>
 *   <li>{@link SameOriginFetchAuthorizationManager}: the browser must report the request as
 *       same-origin; a user-initiated request ({@code Sec-Fetch-Site: none}, e.g. a bookmark or a
 *       link clicked in another application) may only open the console's entry page. This stops
 *       cross-site pages, pages on other {@code localhost} ports (which {@code SameSite=Lax} cookies
 *       do not stop) and links carrying a console action from driving it with the developer's
 *       session.</li>
 * </ul>
 */
@Configuration
@ConditionalOnProperty(name = "spring.h2.console.enabled", havingValue = "true")
public class H2ConsoleSecurityConfiguration {

	/**
	 * Ahead of the main chain, which has no request matcher and would otherwise take every request.
	 * The console's path comes from {@code spring.h2.console.path} (Spring Boot's default
	 * {@code /h2-console}), the same property the console itself is served from, so the rules follow
	 * the console if the path changes.
	 */
	@Bean
	@Order(0)
	public SecurityFilterChain h2ConsoleSecurityFilterChain(HttpSecurity http,
			@Value("${spring.h2.console.path:/h2-console}") String configuredPath) throws Exception {
		String path = basePath(configuredPath);
		SameOriginFetchAuthorizationManager sameOrigin = new SameOriginFetchAuthorizationManager(path);

		// Not signed in: go to the TP login page. Only the console's entry page is saved for the
		// redirect back after sign-in. A saved action URL (e.g. an attacker's login.do?url=...) would
		// otherwise be replayed by that redirect, which the browser reports as same-origin.
		HttpSessionRequestCache entryPageOnly = new HttpSessionRequestCache();
		entryPageOnly.setRequestMatcher(sameOrigin::isEntryPage);
		// No "?continue" marker on the replay: it must stay a parameterless GET of the entry page.
		entryPageOnly.setMatchingRequestParameterName(null);

		return http
				.securityMatcher(PathPatternRequestMatcher.withDefaults().matcher(path + "/**"))
				.authorizeHttpRequests(authz -> authz
						.anyRequest().access(AuthorizationManagers.allOf(
								AuthorityAuthorizationManager.hasRole("CUSTODIAN"),
								sameOrigin)))
				.requestCache(cache -> cache.requestCache(entryPageOnly))
				.exceptionHandling(e -> e.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
				.csrf(AbstractHttpConfigurer::disable)
				// The console is built from frames on its own origin.
				.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
				.build();
	}

	/**
	 * {@code spring.h2.console.path} without trailing slashes. Spring Boot accepts {@code /h2-console/};
	 * appending {@code /**} to that as configured would give {@code /h2-console//**}, which matches
	 * nothing, so the console would fall through to the main chain's {@code authenticated()}. Used by
	 * both this chain and {@link SecurityConfiguration}'s rule that closes the console.
	 */
	static String basePath(String configuredPath) {
		String path = configuredPath == null ? "" : configuredPath.trim();
		while (path.length() > 1 && path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		if (path.isEmpty() || path.equals("/") || !path.startsWith("/")) {
			throw new IllegalStateException("spring.h2.console.path must be a path below '/': '" + configuredPath + "'");
		}
		return path;
	}
}
