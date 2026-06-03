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

package org.greenbuttonalliance.espi.common.handoff;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit tests for {@link SignedHandoffCodec} — round-trip, tamper rejection, expiry, wrong
 * direction, malformed payloads, key drift, version mismatch.
 */
class SignedHandoffCodecTest {

	private static final String KEY = "test-handoff-signing-key-must-be-at-least-32-chars";
	private static final String ALT_KEY = "different-key-but-same-length-32+aaaaaaaaaaaaaaaa";
	private static final Instant NOW = Instant.parse("2026-05-31T12:00:00Z");

	private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
	private final SignedHandoffCodec codec = new SignedHandoffCodec(KEY, clock);

	@Test
	void outbound_roundTripsThroughEncodeAndDecode() {
		SignedHandoff.Outbound original = SignedHandoff.Outbound.of(
				"corr-1",
				NOW,
				NOW.plus(Duration.ofMinutes(5)),
				"nonce-1",
				"third_party",
				"FB=4_5_15;IntervalDuration=3600",
				"https://as.example.com/oauth2/authorize/continue?state=xyz");

		String token = codec.encode(original);
		SignedHandoff.Outbound decoded = codec.decodeOutbound(token);

		assertThat(decoded).isEqualTo(original);
	}

	@Test
	void return_roundTripsThroughEncodeAndDecode() {
		SignedHandoff.Return original = SignedHandoff.Return.of(
				"corr-1",
				NOW,
				NOW.plus(Duration.ofMinutes(5)),
				"nonce-2",
				"customer-42",
				List.of(UUID.randomUUID(), UUID.randomUUID()),
				"https://dc.example.com/.../Customer/abc",
				SignedHandoff.Return.CONSENT_ALLOW,
				"FB=4_5_15;IntervalDuration=3600");

		String token = codec.encode(original);
		SignedHandoff.Return decoded = codec.decodeReturn(token);

		assertThat(decoded).isEqualTo(original);
	}

	@Test
	void return_withDenyAndNoCustomerUri_roundTrips() {
		SignedHandoff.Return original = SignedHandoff.Return.of(
				"corr-2",
				NOW,
				NOW.plus(Duration.ofMinutes(5)),
				"nonce-3",
				"customer-42",
				List.of(),
				null,
				SignedHandoff.Return.CONSENT_DENY,
				null);

		String token = codec.encode(original);
		SignedHandoff.Return decoded = codec.decodeReturn(token);

		assertThat(decoded).isEqualTo(original);
	}

	@Test
	void tamperedPayload_isRejected() {
		String token = codec.encode(validOutbound("nonce-1"));
		int dot = token.indexOf('.');
		// Flip a byte in the payload segment.
		char flipped = token.charAt(0) == 'A' ? 'B' : 'A';
		String tampered = flipped + token.substring(1, dot) + token.substring(dot);

		assertThatThrownBy(() -> codec.decodeOutbound(tampered))
				.isInstanceOf(InvalidHandoffException.class)
				.hasMessageContaining("signature mismatch");
	}

	@Test
	void tamperedSignature_isRejected() {
		String token = codec.encode(validOutbound("nonce-1"));
		int dot = token.indexOf('.');
		char flipped = token.charAt(dot + 1) == 'A' ? 'B' : 'A';
		String tampered = token.substring(0, dot + 1) + flipped + token.substring(dot + 2);

		assertThatThrownBy(() -> codec.decodeOutbound(tampered))
				.isInstanceOf(InvalidHandoffException.class)
				.hasMessageContaining("signature mismatch");
	}

	@Test
	void keyDrift_isRejected() {
		String token = codec.encode(validOutbound("nonce-1"));
		SignedHandoffCodec otherSide = new SignedHandoffCodec(ALT_KEY, clock);

		assertThatThrownBy(() -> otherSide.decodeOutbound(token))
				.isInstanceOf(InvalidHandoffException.class)
				.hasMessageContaining("signature mismatch");
	}

	@Test
	void expiredPayload_isRejected() {
		SignedHandoff.Outbound expired = SignedHandoff.Outbound.of(
				"corr-1",
				NOW.minus(Duration.ofMinutes(10)),
				NOW.minus(Duration.ofMinutes(5)),
				"nonce-1",
				"tp", "FB=4_5_15", "https://x");

		String token = codec.encode(expired);

		assertThatThrownBy(() -> codec.decodeOutbound(token))
				.isInstanceOf(InvalidHandoffException.class)
				.hasMessageContaining("expired");
	}

	@Test
	void outboundDecodedAsReturn_isRejectedByDirectionCheck() {
		String outboundToken = codec.encode(validOutbound("nonce-1"));

		// Decoding an outbound payload via decodeReturn: signature passes (same key, same bytes), JSON
		// binds (overlapping fields), but the direction tag fails the contract.
		assertThatThrownBy(() -> codec.decodeReturn(outboundToken))
				.isInstanceOf(InvalidHandoffException.class)
				.hasMessageContaining("wrong direction");
	}

	@Test
	void wrongVersion_isRejected() {
		SignedHandoff.Outbound future = new SignedHandoff.Outbound(
				99, SignedHandoff.DIRECTION_OUTBOUND, "corr-1", NOW, NOW.plus(Duration.ofMinutes(5)),
				"nonce-1", "tp", "FB=4_5_15", "https://x");

		assertThatThrownBy(() -> codec.encode(future))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("version must be " + SignedHandoff.CURRENT_VERSION);
	}

	@Test
	void emptyToken_isRejected() {
		assertThatThrownBy(() -> codec.decodeOutbound(""))
				.isInstanceOf(InvalidHandoffException.class)
				.hasMessageContaining("empty");
		assertThatThrownBy(() -> codec.decodeOutbound(null))
				.isInstanceOf(InvalidHandoffException.class)
				.hasMessageContaining("empty");
	}

	@Test
	void malformedToken_isRejected() {
		assertThatThrownBy(() -> codec.decodeOutbound("no-dot-here"))
				.isInstanceOf(InvalidHandoffException.class)
				.hasMessageContaining("well-formed");
		assertThatThrownBy(() -> codec.decodeOutbound("trailing-dot."))
				.isInstanceOf(InvalidHandoffException.class)
				.hasMessageContaining("well-formed");
	}

	@Test
	void shortSigningKey_isRejectedAtConstruction() {
		assertThatThrownBy(() -> new SignedHandoffCodec("too-short", clock))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("at least 32 characters");
	}

	private static SignedHandoff.Outbound validOutbound(String nonce) {
		return SignedHandoff.Outbound.of(
				"corr-1",
				NOW,
				NOW.plus(Duration.ofMinutes(5)),
				nonce,
				"third_party",
				"FB=4_5_15",
				"https://as.example.com/return");
	}
}
