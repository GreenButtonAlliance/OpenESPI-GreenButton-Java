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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

/**
 * Encodes and verifies {@link SignedHandoff} tokens using HMAC-SHA256.
 *
 * <p>The token wire format is two dot-separated base64URL segments:
 * <pre>
 *     {base64URL(JSON(payload))} . {base64URL(HMAC-SHA256(key, base64URL(JSON(payload))))}
 * </pre>
 *
 * <p>Verification rejects: malformed tokens, signature mismatches (constant-time comparison),
 * payloads with the wrong version, expired payloads, and payloads typed as the wrong direction.</p>
 *
 * <p><strong>This codec does NOT enforce nonce single-use.</strong> Callers must additionally
 * invoke {@link HandoffNonceService#consume} after a successful decode to prevent replay.</p>
 *
 * <p>Identical implementation lives in {@code openespi-authserver} because that module is
 * deliberately independent of {@code openespi-common}. The wire format is the contract;
 * the implementations are duplicated by design.</p>
 */
@Component
public class SignedHandoffCodec {

	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

	private final ObjectMapper objectMapper;
	private final byte[] signingKey;
	private final Clock clock;

	@Autowired
	public SignedHandoffCodec(@Value("${espi.handoff.signing-key:dev-only-handoff-signing-key-change-me}") String signingKey) {
		this(signingKey, Clock.systemUTC());
	}

	/** Test-friendly constructor. */
	SignedHandoffCodec(String signingKey, Clock clock) {
		if (signingKey == null || signingKey.length() < 32) {
			throw new IllegalArgumentException(
					"espi.handoff.signing-key must be at least 32 characters of high-entropy material");
		}
		this.signingKey = signingKey.getBytes(StandardCharsets.UTF_8);
		// Jackson 3.x auto-registers java.time support via its built-in JavaTimeInitializer;
		// no explicit JavaTimeModule registration needed.
		this.objectMapper = JsonMapper.builder().build();
		this.clock = clock;
	}

	/**
	 * Encode and sign an {@link SignedHandoff.Outbound}.
	 *
	 * @throws IllegalArgumentException if the payload version does not match {@link SignedHandoff#CURRENT_VERSION}
	 */
	public String encode(SignedHandoff.Outbound payload) {
		requireCurrentVersion(payload.version());
		return encodeAndSign(payload);
	}

	/**
	 * Encode and sign a {@link SignedHandoff.Return}.
	 *
	 * @throws IllegalArgumentException if the payload version does not match {@link SignedHandoff#CURRENT_VERSION}
	 */
	public String encode(SignedHandoff.Return payload) {
		requireCurrentVersion(payload.version());
		return encodeAndSign(payload);
	}

	/**
	 * Decode and verify an outbound handoff token. Verifies signature, version, direction, and expiry.
	 *
	 * @throws InvalidHandoffException if the token is malformed, tampered, expired, or the wrong direction
	 */
	public SignedHandoff.Outbound decodeOutbound(String token) {
		SignedHandoff.Outbound payload = decodeAndVerify(token, SignedHandoff.Outbound.class);
		verifyDirection(payload.direction(), SignedHandoff.DIRECTION_OUTBOUND);
		verifyNotExpired(payload.expiresAt());
		return payload;
	}

	/**
	 * Decode and verify a return handoff token. Verifies signature, version, direction, and expiry.
	 *
	 * @throws InvalidHandoffException if the token is malformed, tampered, expired, or the wrong direction
	 */
	public SignedHandoff.Return decodeReturn(String token) {
		SignedHandoff.Return payload = decodeAndVerify(token, SignedHandoff.Return.class);
		verifyDirection(payload.direction(), SignedHandoff.DIRECTION_RETURN);
		verifyNotExpired(payload.expiresAt());
		return payload;
	}

	private String encodeAndSign(SignedHandoff payload) {
		try {
			byte[] json = objectMapper.writeValueAsBytes(payload);
			String encodedPayload = URL_ENCODER.encodeToString(json);
			String signature = URL_ENCODER.encodeToString(hmac(encodedPayload.getBytes(StandardCharsets.US_ASCII)));
			return encodedPayload + "." + signature;
		}
		catch (Exception e) {
			throw new IllegalStateException("Failed to encode handoff payload", e);
		}
	}

	private <T extends SignedHandoff> T decodeAndVerify(String token, Class<T> type) {
		if (token == null || token.isBlank()) {
			throw new InvalidHandoffException("token is empty");
		}
		int dot = token.indexOf('.');
		if (dot < 0 || dot == token.length() - 1) {
			throw new InvalidHandoffException("token is not well-formed (missing signature segment)");
		}
		String encodedPayload = token.substring(0, dot);
		String providedSignature = token.substring(dot + 1);

		byte[] expectedSig = hmac(encodedPayload.getBytes(StandardCharsets.US_ASCII));
		byte[] providedSig;
		try {
			providedSig = URL_DECODER.decode(providedSignature);
		}
		catch (IllegalArgumentException e) {
			throw new InvalidHandoffException("signature segment is not valid base64URL", e);
		}
		if (!MessageDigest.isEqual(expectedSig, providedSig)) {
			throw new InvalidHandoffException("signature mismatch (key drift or tampering)");
		}

		T payload;
		try {
			byte[] json = URL_DECODER.decode(encodedPayload);
			payload = objectMapper.readValue(json, type);
		}
		catch (Exception e) {
			throw new InvalidHandoffException("payload is not valid JSON for " + type.getSimpleName(), e);
		}

		if (payload.version() != SignedHandoff.CURRENT_VERSION) {
			throw new InvalidHandoffException(
					"unsupported wire version " + payload.version() + " (expected " + SignedHandoff.CURRENT_VERSION + ")");
		}
		return payload;
	}

	private void verifyDirection(String actual, String expected) {
		if (!expected.equals(actual)) {
			throw new InvalidHandoffException(
					"wrong direction: expected '" + expected + "' but was '" + actual + "'");
		}
	}

	private void verifyNotExpired(Instant expiresAt) {
		if (expiresAt == null) {
			throw new InvalidHandoffException("payload is missing expiresAt");
		}
		if (clock.instant().isAfter(expiresAt)) {
			throw new InvalidHandoffException("payload expired at " + expiresAt);
		}
	}

	private byte[] hmac(byte[] data) {
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(new SecretKeySpec(signingKey, HMAC_ALGORITHM));
			return mac.doFinal(data);
		}
		catch (Exception e) {
			throw new IllegalStateException("HMAC computation failed", e);
		}
	}

	private static void requireCurrentVersion(int version) {
		if (version != SignedHandoff.CURRENT_VERSION) {
			throw new IllegalArgumentException(
					"handoff payload version must be " + SignedHandoff.CURRENT_VERSION + " (was " + version + ")");
		}
	}
}
