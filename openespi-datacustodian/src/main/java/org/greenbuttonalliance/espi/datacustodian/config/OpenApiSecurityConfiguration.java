/*
 *
 *    Copyright (c) 2018-2026 Green Button Alliance, Inc.
 *
 *    Portions (c) 2013-2018 EnergyOS.org
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

package org.greenbuttonalliance.espi.datacustodian.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * The security scheme named {@code oauth2} that every ESPI resource controller requires
 * ({@code @SecurityRequirement(name = "oauth2")}). Without it the API docs referred to an undefined scheme, so
 * Swagger UI showed the padlocks but no Authorize button, and "Try it out" sent no token (#312).
 *
 * <p>The DC validates opaque access tokens by introspection at the Authorization Server, so the scheme is a
 * bearer token: the token is obtained from the AS (e.g. client_credentials, see {@code docs/LOCAL_TESTING.md})
 * and pasted into Authorize; Swagger UI then sends it as {@code Authorization: Bearer ...}. An OAuth 2.0 flow
 * run from Swagger UI itself would need a client registered at the AS for Swagger UI and its redirect URI.</p>
 */
@Configuration
@SecurityScheme(
		name = "oauth2",
		type = SecuritySchemeType.HTTP,
		scheme = "bearer",
		bearerFormat = "opaque",
		description = "An OAuth 2.0 access token from the Authorization Server, e.g. the DataCustodian admin's"
				+ " client_credentials token (DataCustodian_Admin_Access) or a customer's token. Paste the token"
				+ " value only; it is sent as Authorization: Bearer <token>.")
public class OpenApiSecurityConfiguration {
}
