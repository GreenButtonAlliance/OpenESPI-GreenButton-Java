/*
 * Copyright 2025 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package org.greenbuttonalliance.espi.authserver.backchannel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withRawStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * MockRestServiceServer-driven coverage of {@link DataCustodianBackchannelClient}. Verifies the
 * wire request shape (URL, method, JSON body), happy-path response parsing, and the four failure
 * modes the client translates into {@link DataCustodianBackchannelException}.
 *
 * <p>The happy path is the AS <em>consumer</em> side of the AS↔DC back-channel wire contract
 * (Contract 1 of #150): it asserts the client emits exactly the shared
 * {@code contracts/backchannel-subscription-request.json} and correctly parses
 * {@code contracts/backchannel-subscription-response.json}. The DC provider test
 * ({@code BackchannelWireContractTest}) binds the same fixtures, so neither side can drift.</p>
 */
@DisplayName("DataCustodianBackchannelClient")
class DataCustodianBackchannelClientTest {

	private static final UUID UP_1 = UUID.fromString("00000000-0000-5000-8000-000000000001");
	private static final UUID AUTH_ID = UUID.fromString("11111111-1111-5111-8111-111111111111");
	private static final UUID RES_SUB_ID = UUID.fromString("22222222-2222-5222-8222-222222222222");
	private static final UUID CUST_SUB_ID = UUID.fromString("33333333-3333-5333-8333-333333333333");
	private static final String RESOURCE_BASE =
			"https://utilityapi.com/DataCustodian/espi/1_1/resource";

	private RestClient.Builder builder;
	private MockRestServiceServer server;
	private DataCustodianBackchannelClient client;

	@BeforeEach
	void setUp() {
		builder = RestClient.builder().baseUrl("http://dc.example");
		server = MockRestServiceServer.bindTo(builder).build();
		client = new DataCustodianBackchannelClient(builder.build());
	}

	@Test
	@DisplayName("provisions and parses the response on 201 happy path (shared contract fixtures)")
	void happyPath() throws Exception {
		server.expect(requestTo("http://dc.example/internal/backchannel/v1/subscriptions"))
				.andExpect(method(org.springframework.http.HttpMethod.POST))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				// AS consumer must emit exactly the shared request contract.
				.andExpect(content().json(readContract("backchannel-subscription-request.json")))
				.andRespond(withStatus(HttpStatus.CREATED)
						.contentType(MediaType.APPLICATION_JSON)
						// ...and correctly parse the shared response contract.
						.body(readContract("backchannel-subscription-response.json")));

		BackchannelResponse response = client.provision(new BackchannelRequest(
				"corr-7f3a1c20", "third_party",
				"FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13",
				42L, List.of(UP_1)));

		assertThat(response)
				.extracting(BackchannelResponse::authorizationId,
						BackchannelResponse::resourceSubscriptionId,
						BackchannelResponse::customerSubscriptionId,
						BackchannelResponse::resourceUri,
						BackchannelResponse::authorizationUri,
						BackchannelResponse::customerResourceUri)
				.containsExactly(AUTH_ID, RES_SUB_ID, CUST_SUB_ID,
						RESOURCE_BASE + "/Batch/Subscription/" + RES_SUB_ID,
						RESOURCE_BASE + "/Authorization/" + AUTH_ID,
						RESOURCE_BASE + "/Batch/RetailCustomer/42");

		server.verify();
	}

	@Test
	@DisplayName("4xx response throws DataCustodianBackchannelException with response body in message")
	void clientError() {
		server.expect(requestTo("http://dc.example/internal/backchannel/v1/subscriptions"))
				.andRespond(withStatus(HttpStatus.BAD_REQUEST)
						.contentType(MediaType.APPLICATION_JSON)
						.body("""
								{"error":"invalid_request","error_description":"granted_scope is missing"}
								"""));

		assertThatThrownBy(() -> client.provision(
				new BackchannelRequest("c", "tp", "", 1L, List.of())))
				.isInstanceOf(DataCustodianBackchannelException.class)
				.hasMessageContaining("400")
				.hasMessageContaining("granted_scope is missing");
	}

	@Test
	@DisplayName("5xx response throws DataCustodianBackchannelException")
	void serverError() {
		server.expect(requestTo("http://dc.example/internal/backchannel/v1/subscriptions"))
				.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
						.contentType(MediaType.APPLICATION_JSON)
						.body("{\"error\":\"internal\"}"));

		assertThatThrownBy(() -> client.provision(
				new BackchannelRequest("c", "tp", "FB_1", 1L, List.of())))
				.isInstanceOf(DataCustodianBackchannelException.class)
				.hasMessageContaining("500");
	}

	@Test
	@DisplayName("non-standard status (e.g. 418) still surfaces as exception")
	void unexpectedStatus() {
		server.expect(requestTo("http://dc.example/internal/backchannel/v1/subscriptions"))
				.andRespond(withRawStatus(418).body("no coffee"));

		assertThatThrownBy(() -> client.provision(
				new BackchannelRequest("c", "tp", "FB_1", 1L, List.of())))
				.isInstanceOf(DataCustodianBackchannelException.class)
				.hasMessageContaining("418");
	}

	/** Reads a shared wire-contract fixture from the repo-root {@code contracts/} directory. */
	private static String readContract(String name) throws Exception {
		for (Path candidate : new Path[] { Path.of("..", "contracts"), Path.of("contracts") }) {
			if (Files.isDirectory(candidate)) {
				return Files.readString(candidate.resolve(name));
			}
		}
		throw new IllegalStateException(
				"contracts/ directory not found from " + Path.of("").toAbsolutePath());
	}
}
