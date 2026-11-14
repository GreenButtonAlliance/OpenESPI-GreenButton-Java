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

package org.greenbuttonalliance.espi.thirdparty.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.SpringOpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security filter chain for the Spring Boot Actuator endpoints.
 *
 * <p>Only {@code health} (status only, including the {@code liveness} and {@code readiness} probe
 * groups) is anonymous, so load balancers and orchestrators can check that the application is
 * operating normally. Every other exposed endpoint ({@code info}, {@code metrics},
 * {@code prometheus} and the links page) requires an opaque admin access token. The Third Party is
 * otherwise only an OAuth client, so the token is introspected at the Authorization Server with the
 * Third Party's client credentials and must carry {@code espi.actuator.admin-scope}
 * ({@code ThirdParty_Admin_Access} by default). Endpoint access is read-only
 * ({@code management.endpoint.*.access} in application.yml).</p>
 *
 * <p>Ordered first so actuator requests never reach the {@code oauth2Login} chain in
 * {@link SecurityConfiguration}, which would redirect them to the login page.</p>
 */
@Configuration
public class ActuatorSecurityConfiguration {

	@Value("${espi.authorization-server.introspection-endpoint:http://localhost:9999/oauth2/introspect}")
	private String introspectionUri;

	@Value("${espi.authorization-server.client-id:third_party}")
	private String clientId;

	@Value("${espi.authorization-server.client-secret:secret}")
	private String clientSecret;

	@Value("${espi.actuator.admin-scope:ThirdParty_Admin_Access}")
	private String adminScope;

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
		OpaqueTokenIntrospector introspector = SpringOpaqueTokenIntrospector
				.withIntrospectionUri(introspectionUri)
				.clientId(clientId)
				.clientSecret(clientSecret)
				.build();

		return http
				.securityMatcher(EndpointRequest.toAnyEndpoint())
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authz -> authz
						.requestMatchers(EndpointRequest.to("health")).permitAll()
						.anyRequest().hasAuthority("SCOPE_" + adminScope))
				.oauth2ResourceServer(oauth2 -> oauth2
						.opaqueToken(opaque -> opaque.introspector(introspector)))
				.build();
	}
}
