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

package org.greenbuttonalliance.espi.handoff;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Tamper-evident value object carried as a URL parameter between the GBA Authorization Server and a
 * Data Custodian sandbox during the customer-facing OAuth2 flow.
 *
 * <h2>Why not a shared session?</h2>
 * AS and DC are independently deployed Spring Boot applications. A shared JDBC/Redis Spring Session
 * would force cookie-domain coupling, SameSite=None in dev, and an ambiguous "whose principal lives
 * in the session" answer. The signed-handoff approach borrows the same idea OAuth2 itself uses for
 * the {@code state} parameter: encode the cross-app state in the URL, sign it with HMAC, let each
 * side keep its own session. Replay is prevented by a single-use nonce table on the receiver.
 *
 * <h2>Wire format</h2>
 * Two dot-separated base64URL segments:
 * <pre>
 *     {base64URL(JSON(payload))} . {base64URL(HMAC-SHA256(key, base64URL(JSON(payload))))}
 * </pre>
 * Identical wire format on both sides; the codec is duplicated by design because {@code
 * openespi-authserver} is independent of {@code openespi-common}. The JSON payload structure below
 * is the contract.
 *
 * <h2>Directions</h2>
 * <ul>
 *   <li>{@link Outbound} &mdash; AS to DC ("log this customer in and have them consent to this scope")</li>
 *   <li>{@link Return}   &mdash; DC to AS ("customer logged in and made these choices")</li>
 * </ul>
 *
 * <h2>Lifetime</h2>
 * Issued with a short expiry (default 5 minutes) and a 128-bit random nonce. The receiver verifies
 * the signature, rejects expired payloads, and consumes the nonce single-use via
 * {@link HandoffNonceService}.
 *
 * @see SignedHandoffCodec
 * @see HandoffNonceService
 */
public sealed interface SignedHandoff permits SignedHandoff.Outbound, SignedHandoff.Return {

	/** Wire-format version. Increment when changing the JSON shape in a non-backwards-compatible way. */
	int CURRENT_VERSION = 1;

	/** Wire-format direction tag for {@link Outbound} payloads (AS &rarr; DC). */
	String DIRECTION_OUTBOUND = "outbound";

	/** Wire-format direction tag for {@link Return} payloads (DC &rarr; AS). */
	String DIRECTION_RETURN = "return";

	int version();
	String direction();
	String correlationId();
	Instant issuedAt();
	Instant expiresAt();
	String nonce();

	/**
	 * AS &rarr; DC handoff. Encodes the granted scope, the client_id, and the URL the DC should
	 * redirect the user back to after login + Authorization Screen.
	 *
	 * @param version       wire-format version, must equal {@link #CURRENT_VERSION}
	 * @param correlationId opaque AS-supplied trace id, echoed on the return handoff
	 * @param issuedAt      issuance timestamp (epoch seconds)
	 * @param expiresAt     expiry timestamp (epoch seconds); receiver rejects past this
	 * @param nonce         base64URL-encoded 128 bits; receiver tracks single-use
	 * @param clientId      OAuth2 {@code client_id} of the requesting third party
	 * @param grantedScope  ESPI scope string the customer is being asked to approve
	 * @param returnUrl     absolute URL the DC redirects the user-agent to on completion
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	record Outbound(
			@JsonProperty("v") int version,
			@JsonProperty("dir") String direction,
			@JsonProperty("cid") String correlationId,
			@JsonProperty("iat") Instant issuedAt,
			@JsonProperty("exp") Instant expiresAt,
			@JsonProperty("nonce") String nonce,
			@JsonProperty("client_id") String clientId,
			@JsonProperty("scope") String grantedScope,
			@JsonProperty("return_url") String returnUrl
	) implements SignedHandoff {

		/** Convenience factory that fills in {@code version} and {@code direction}. */
		public static Outbound of(String correlationId, Instant issuedAt, Instant expiresAt, String nonce,
								  String clientId, String grantedScope, String returnUrl) {
			return new Outbound(CURRENT_VERSION, DIRECTION_OUTBOUND, correlationId, issuedAt, expiresAt,
					nonce, clientId, grantedScope, returnUrl);
		}
	}

	/**
	 * DC &rarr; AS handoff. Encodes the authenticated principal and the customer's selections from
	 * the Authorization Screen (or a deny decision).
	 *
	 * @param version                 wire-format version, must equal {@link #CURRENT_VERSION}
	 * @param correlationId           the AS-supplied trace id from the corresponding outbound
	 * @param issuedAt                issuance timestamp
	 * @param expiresAt               expiry timestamp
	 * @param nonce                   base64URL-encoded 128 bits; AS tracks single-use
	 * @param principal               DC's authenticated retail-customer identifier
	 * @param selectedUsagePointIds   usage points the customer chose to share; empty list for a
	 *                                PII-only or denied grant
	 * @param customerResourceUri     absolute URI of the customer/PII resource the customer
	 *                                approved sharing, or {@code null} if not granted
	 * @param consent                 {@code "allow"} if the customer granted; {@code "deny"} if not
	 * @param approvedScope           the effective scope after the customer's checkbox decisions
	 *                                &mdash; subset of the originally-requested scope. The AS
	 *                                mints the access token with <em>this</em> scope, not the
	 *                                original. {@code null} on {@code consent="deny"}.
	 *                                <p>Additive field introduced in PR C2b. Codec version remains
	 *                                at 1 because old readers parsing this payload simply see
	 *                                {@code approvedScope=null}; they had no use for it anyway
	 *                                because the customer-checkbox UI (and the effective scope)
	 *                                did not exist when C1 shipped.</p>
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	record Return(
			@JsonProperty("v") int version,
			@JsonProperty("dir") String direction,
			@JsonProperty("cid") String correlationId,
			@JsonProperty("iat") Instant issuedAt,
			@JsonProperty("exp") Instant expiresAt,
			@JsonProperty("nonce") String nonce,
			@JsonProperty("sub") String principal,
			@JsonProperty("up") List<UUID> selectedUsagePointIds,
			@JsonProperty("cust_uri") String customerResourceUri,
			@JsonProperty("consent") String consent,
			@JsonProperty("approved_scope") String approvedScope
	) implements SignedHandoff {

		public static final String CONSENT_ALLOW = "allow";
		public static final String CONSENT_DENY = "deny";

		/** Convenience factory that fills in {@code version} and {@code direction}. */
		public static Return of(String correlationId, Instant issuedAt, Instant expiresAt, String nonce,
								String principal, List<UUID> selectedUsagePointIds,
								String customerResourceUri, String consent, String approvedScope) {
			return new Return(CURRENT_VERSION, DIRECTION_RETURN, correlationId, issuedAt, expiresAt,
					nonce, principal, selectedUsagePointIds, customerResourceUri, consent, approvedScope);
		}
	}
}
