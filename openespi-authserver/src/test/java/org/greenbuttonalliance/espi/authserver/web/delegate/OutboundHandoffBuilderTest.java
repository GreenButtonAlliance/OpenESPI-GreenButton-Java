/*
 * Copyright 2025 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package org.greenbuttonalliance.espi.authserver.web.delegate;

import org.greenbuttonalliance.espi.handoff.HandoffNonceService;
import org.greenbuttonalliance.espi.handoff.SignedHandoff;
import org.greenbuttonalliance.espi.handoff.SignedHandoffCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Verifies that the builder produces a token the codec can decode back into the same field values,
 * and that the embedded {@code returnUrl} is derived from the configured issuer URI. Uses the real
 * {@link SignedHandoffCodec} so the round-trip is end-to-end (HMAC + JSON), with only the nonce
 * service mocked so the test does not need a database.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OutboundHandoffBuilder")
class OutboundHandoffBuilderTest {

	private static final String SIGNING_KEY = "test-signing-key-must-be-at-least-32-chars-long";

	@Mock
	private HandoffNonceService nonceService;

	private SignedHandoffCodec codec;
	private OutboundHandoffBuilder builder;

	@BeforeEach
	void setUp() {
		codec = new SignedHandoffCodec(SIGNING_KEY);
		builder = new OutboundHandoffBuilder(codec, nonceService);
		ReflectionTestUtils.setField(builder, "issuerUri", "https://as.example");
	}

	@Test
	@DisplayName("buildSignedToken produces a token the codec decodes with all fields intact")
	void roundTripsViaCodec() {
		when(nonceService.generate()).thenReturn("nonce-aaaa");

		String token = builder.buildSignedToken("tp-client", "FB_1;FB_4_5", "corr-99");

		SignedHandoff.Outbound decoded = codec.decodeOutbound(token);
		assertThat(decoded)
				.extracting(SignedHandoff.Outbound::correlationId,
						SignedHandoff.Outbound::clientId,
						SignedHandoff.Outbound::grantedScope,
						SignedHandoff.Outbound::nonce,
						SignedHandoff.Outbound::returnUrl,
						SignedHandoff.Outbound::direction)
				.containsExactly("corr-99", "tp-client", "FB_1;FB_4_5", "nonce-aaaa",
						"https://as.example/oauth2/authorize/continue", "outbound");
	}

	@Test
	@DisplayName("expiresAt is approximately 5 minutes after issuedAt")
	void expiryWindowIsFiveMinutes() {
		when(nonceService.generate()).thenReturn("nonce-bbbb");

		String token = builder.buildSignedToken("tp-client", "FB_1", "corr-1");
		SignedHandoff.Outbound decoded = codec.decodeOutbound(token);

		long seconds = decoded.expiresAt().getEpochSecond() - decoded.issuedAt().getEpochSecond();
		assertThat(seconds).isEqualTo(300L);
	}

	@Test
	@DisplayName("issuedAt is recent (sanity check that the clock is wired)")
	void issuedAtIsRecent() {
		when(nonceService.generate()).thenReturn("nonce-cccc");

		Instant before = Instant.now().minusSeconds(2);
		String token = builder.buildSignedToken("tp-client", "FB_1", "corr-1");
		Instant after = Instant.now().plusSeconds(2);

		SignedHandoff.Outbound decoded = codec.decodeOutbound(token);
		assertThat(decoded.issuedAt()).isBetween(before, after);
	}

	@Test
	@DisplayName("convenience overload generates a non-null correlation id")
	void convenienceOverloadFillsInCorrelationId() {
		when(nonceService.generate()).thenReturn("nonce-dddd");

		String token = builder.buildSignedToken("tp-client", "FB_1");

		assertThat(codec.decodeOutbound(token).correlationId()).isNotBlank();
	}
}
