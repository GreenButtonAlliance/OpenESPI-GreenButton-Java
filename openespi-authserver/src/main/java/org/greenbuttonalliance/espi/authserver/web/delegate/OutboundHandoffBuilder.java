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

package org.greenbuttonalliance.espi.authserver.web.delegate;

import lombok.RequiredArgsConstructor;
import org.greenbuttonalliance.espi.handoff.HandoffNonceService;
import org.greenbuttonalliance.espi.handoff.SignedHandoff;
import org.greenbuttonalliance.espi.handoff.SignedHandoffCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Constructs a {@link SignedHandoff.Outbound} for the AS&rarr;DC redirect that delegates customer
 * login + Authorization Screen to the Data Custodian.
 *
 * <p>Issued when Spring Authorization Server's filter chain decides the customer needs to
 * authenticate or grant consent and redirects to {@link AuthorizeDelegateController}. The signed
 * outbound carries the third party's {@code client_id}, the requested {@code scope}, and the
 * {@code return_url} the DC redirects the user-agent back to ({@code /oauth2/authorize/continue}
 * on the AS).</p>
 */
@Service
@RequiredArgsConstructor
public class OutboundHandoffBuilder {

	private static final Duration OUTBOUND_TTL = Duration.ofMinutes(5);

	private final SignedHandoffCodec codec;
	private final HandoffNonceService nonceService;
	private final Clock clock = Clock.systemUTC();

	@Value("${spring.security.oauth2.authorizationserver.issuer:http://localhost:9999}")
	private String issuerUri;

	/**
	 * Build a signed outbound handoff token.
	 *
	 * @param clientId      OAuth2 {@code client_id} of the requesting third party
	 * @param scope         the scope string the AS received in the {@code /oauth2/authorize}
	 *                      request (the TP-requested scope, before any customer narrowing)
	 * @param correlationId opaque trace id (re-used here as the wire {@code cid}); pass a fresh
	 *                      UUID per delegation to keep the AS-side and DC-side logs joinable
	 * @return the dot-separated base64URL token; encode this into a URL query param when
	 *         redirecting the user-agent to DC
	 */
	public String buildSignedToken(String clientId, String scope, String correlationId) {
		Instant now = clock.instant();
		String returnUrl = issuerUri + "/oauth2/authorize/continue";
		SignedHandoff.Outbound payload = SignedHandoff.Outbound.of(
				correlationId,
				now,
				now.plus(OUTBOUND_TTL),
				nonceService.generate(),
				clientId,
				scope,
				returnUrl);
		return codec.encode(payload);
	}

	/** Convenience overload that generates a fresh correlation id. */
	public String buildSignedToken(String clientId, String scope) {
		return buildSignedToken(clientId, scope, UUID.randomUUID().toString());
	}
}
