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

package org.greenbuttonalliance.espi.datacustodian.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.greenbuttonalliance.espi.datacustodian.security.RetailCustomerUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.io.IOException;
import java.net.URI;

/**
 * Security filter chain for the customer-facing login UI and the custodian admin surface.
 *
 * <p>Ordered between {@link BackchannelSecurityConfiguration} (HIGHEST_PRECEDENCE,
 * {@code /internal/**}) and the public OAuth2 resource-server chain in
 * {@link SecurityConfiguration} ({@code @Order(1)} {@code /espi/**}). Matches:</p>
 * <ul>
 *   <li>{@code /login}, {@code /logout} &mdash; customer / admin login form</li>
 *   <li>{@code /custodian/**} &mdash; admin pages protected by
 *       {@code @PreAuthorize("hasRole('ROLE_CUSTODIAN')")}</li>
 *   <li>{@code /oauth/authorize-screen/**} &mdash; the Authorization Screen (built in PR C2b)</li>
 * </ul>
 *
 * <p>Form login with BCrypt, {@code IF_REQUIRED} session, CSRF on via cookie-based token repo.
 * The success handler honors a {@code return_to} form parameter (absolute URL or path);
 * unset &rarr; default landing page. The {@code return_to} carries the AS-issued signed handoff
 * from PR C1 in the AS&rarr;DC delegation flow built in PR C3.</p>
 *
 * <h3>Three-chain architecture</h3>
 * <ul>
 *   <li>{@code @Order(HIGHEST_PRECEDENCE)} &mdash; back-channel HTTP Basic, STATELESS</li>
 *   <li>{@code @Order(0)} &mdash; this chain, formLogin + session, customer + admin UI</li>
 *   <li>{@code @Order(1)} &mdash; OAuth2 opaque-token resource server, STATELESS, public ESPI API</li>
 * </ul>
 *
 * @see BackchannelSecurityConfiguration
 * @see SecurityConfiguration
 * @see RetailCustomerUserDetailsService
 */
@Configuration
public class CustomerLoginSecurityConfiguration {

	private static final String DEFAULT_SUCCESS_URL = "/custodian/home";

	@Bean
	public PasswordEncoder customerPasswordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	@Order(0)
	public SecurityFilterChain customerLoginSecurityFilterChain(
			HttpSecurity http,
			RetailCustomerUserDetailsService userDetailsService,
			PasswordEncoder customerPasswordEncoder) throws Exception {

		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(customerPasswordEncoder);

		PathPatternRequestMatcher.Builder pp = PathPatternRequestMatcher.withDefaults();
		RequestMatcher matcher = new OrRequestMatcher(
				pp.matcher("/login"),
				pp.matcher("/logout"),
				pp.matcher("/custodian/**"),
				pp.matcher("/oauth/authorize-screen/**"));

		return http
				.securityMatcher(matcher)
				.authenticationProvider(provider)
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				// HttpSessionCsrfTokenRepository (Spring Security default) — Thymeleaf's
				// th:action="@{...}" auto-injects a hidden _csrf input that matches the
				// session-stored token. Right shape for vanilla form login.
				.csrf(Customizer.withDefaults())
				.authorizeHttpRequests(authz -> authz
						.requestMatchers(pp.matcher("/login")).permitAll()
						.anyRequest().authenticated())
				.formLogin(form -> form
						.loginPage("/login")
						.loginProcessingUrl("/login")
						.usernameParameter("username")
						.passwordParameter("password")
						.successHandler(returnToSuccessHandler())
						.failureUrl("/login?error")
						.permitAll())
				.logout(logout -> logout
						.logoutUrl("/logout")
						.logoutSuccessUrl("/login?logout")
						.permitAll())
				.build();
	}

	/**
	 * On successful authentication, redirect to the {@code return_to} request parameter if present
	 * (the AS-issued signed handoff destination from PR C1 / C3), otherwise to the default landing
	 * page.
	 */
	private SimpleUrlAuthenticationSuccessHandler returnToSuccessHandler() {
		return new SimpleUrlAuthenticationSuccessHandler() {
			@Override
			public void onAuthenticationSuccess(HttpServletRequest request,
												HttpServletResponse response,
												Authentication authentication) throws IOException {
				String returnTo = request.getParameter("return_to");
				if (isSafeReturnTo(returnTo)) {
					getRedirectStrategy().sendRedirect(request, response, returnTo);
				}
				else {
					getRedirectStrategy().sendRedirect(request, response, DEFAULT_SUCCESS_URL);
				}
			}
		};
	}

	/**
	 * Accept only same-origin paths ({@code /foo}) or absolute URLs whose URI parses cleanly.
	 * Defense in depth against open-redirect &mdash; the AS-issued signed handoff in PR C3 will
	 * carry its own signature verification, but the success handler must reject obvious abuse
	 * (e.g. {@code //evil.example.com}).
	 */
	private static boolean isSafeReturnTo(String returnTo) {
		if (returnTo == null || returnTo.isBlank()) return false;
		if (returnTo.startsWith("//")) return false;
		if (returnTo.startsWith("/")) return true;
		try {
			URI uri = URI.create(returnTo);
			return uri.isAbsolute() && uri.getHost() != null;
		}
		catch (IllegalArgumentException e) {
			return false;
		}
	}
}
