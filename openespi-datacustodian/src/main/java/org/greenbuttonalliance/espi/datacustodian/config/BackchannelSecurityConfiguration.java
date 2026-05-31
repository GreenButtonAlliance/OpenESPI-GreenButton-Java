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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security filter chain for the AS&harr;DC back-channel under {@code /internal/**}.
 *
 * <p>Separate from the public ESPI resource-server chain in {@link SecurityConfiguration}. This
 * chain authenticates the GBA Authorization Server (the only legitimate caller) with HTTP Basic
 * against a dedicated, single-purpose credential &mdash; <strong>not</strong> the OAuth2 token
 * introspection used for the public API. Network-level isolation (binding {@code /internal/**} to
 * an internal interface or gating it at ingress) is expected on top of this in production; mTLS is
 * a future enhancement tracked separately.</p>
 *
 * <p>Ordered at {@link Ordered#HIGHEST_PRECEDENCE} so the {@code securityMatcher("/internal/**")}
 * is evaluated first &mdash; back-channel requests never fall through to the public chain. The
 * back-channel {@link UserDetailsService} and {@link PasswordEncoder} are wired into this chain
 * via a private {@link DaoAuthenticationProvider} and are NOT exposed as top-level beans, so the
 * public OAuth2 resource-server chain remains unaffected.</p>
 *
 * @see SecurityConfiguration
 */
@Configuration
public class BackchannelSecurityConfiguration {

	private static final String BACKCHANNEL_ROLE = "BACKCHANNEL";

	@Value("${espi.backchannel.client-id:as-backchannel}")
	private String backchannelClientId;

	@Value("${espi.backchannel.client-secret:change-me-in-production}")
	private String backchannelClientSecret;

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	public SecurityFilterChain backchannelSecurityFilterChain(HttpSecurity http) throws Exception {
		PasswordEncoder encoder = new BCryptPasswordEncoder();
		UserDetailsService uds = new InMemoryUserDetailsManager(
				User.withUsername(backchannelClientId)
						.password(encoder.encode(backchannelClientSecret))
						.roles(BACKCHANNEL_ROLE)
						.build());
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(uds);
		provider.setPasswordEncoder(encoder);

		return http
				.securityMatcher("/internal/**")
				.csrf(AbstractHttpConfigurer::disable)
				.cors(AbstractHttpConfigurer::disable)
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authz -> authz
						.anyRequest().hasRole(BACKCHANNEL_ROLE))
				.authenticationProvider(provider)
				.httpBasic(httpBasic -> {})
				.build();
	}
}
