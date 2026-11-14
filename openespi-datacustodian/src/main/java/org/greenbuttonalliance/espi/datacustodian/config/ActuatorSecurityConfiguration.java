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

import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security filter chain for the Spring Boot Actuator endpoints.
 *
 * <p>Only {@code health} (status only, including the {@code liveness} and {@code readiness} probe
 * groups) is anonymous, so load balancers and orchestrators can check that the application is
 * operating normally. Every other exposed endpoint ({@code info}, {@code metrics},
 * {@code prometheus} and the links page) requires an opaque access token carrying the
 * {@code DataCustodian_Admin_Access} scope, validated by the same introspector as the ESPI API.
 * Endpoint access is read-only ({@code management.endpoint.*.access} in application.yml).</p>
 *
 * <p>Ordered just after the back-channel chain so actuator requests never reach the ESPI
 * resource-server chain or the customer-login chain.</p>
 */
@Configuration
public class ActuatorSecurityConfiguration {

	static final String ADMIN_AUTHORITY = "SCOPE_DataCustodian_Admin_Access";

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE + 1)
	public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http,
			OpaqueTokenIntrospector introspector) throws Exception {
		return http
				.securityMatcher(EndpointRequest.toAnyEndpoint())
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authz -> authz
						.requestMatchers(EndpointRequest.to("health")).permitAll()
						.anyRequest().hasAuthority(ADMIN_AUTHORITY))
				.oauth2ResourceServer(oauth2 -> oauth2
						.opaqueToken(opaque -> opaque.introspector(introspector)))
				.build();
	}
}
