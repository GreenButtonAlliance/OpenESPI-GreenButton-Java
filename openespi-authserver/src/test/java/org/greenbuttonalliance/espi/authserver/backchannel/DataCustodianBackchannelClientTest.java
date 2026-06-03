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
 */
@DisplayName("DataCustodianBackchannelClient")
class DataCustodianBackchannelClientTest {

	private static final UUID UP_1 = UUID.fromString("00000000-0000-5000-8000-000000000001");
	private static final UUID AUTH_ID = UUID.fromString("11111111-1111-5111-8111-111111111111");
	private static final UUID RES_SUB_ID = UUID.fromString("22222222-2222-5222-8222-222222222222");
	private static final UUID CUST_SUB_ID = UUID.fromString("33333333-3333-5333-8333-333333333333");

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
	@DisplayName("provisions and parses the response on 201 happy path")
	void happyPath() {
		server.expect(requestTo("http://dc.example/internal/backchannel/v1/subscriptions"))
				.andExpect(method(org.springframework.http.HttpMethod.POST))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().json("""
						{
						  "correlation_id": "corr-1",
						  "client_id": "tp-1",
						  "granted_scope": "FB_1;FB_4_5",
						  "retail_customer_id": 42,
						  "selected_usage_point_ids": ["%s"]
						}
						""".formatted(UP_1)))
				.andRespond(withStatus(HttpStatus.CREATED)
						.contentType(MediaType.APPLICATION_JSON)
						.body("""
								{
								  "authorization_id": "%s",
								  "resource_subscription_id": "%s",
								  "customer_subscription_id": "%s",
								  "resource_uri": "https://dc.example/Subscription/%s",
								  "authorization_uri": "https://dc.example/Authorization/%s",
								  "customer_resource_uri": "https://dc.example/RetailCustomer/42/Customer/%s"
								}
								""".formatted(AUTH_ID, RES_SUB_ID, CUST_SUB_ID, RES_SUB_ID, AUTH_ID, CUST_SUB_ID)));

		BackchannelResponse response = client.provision(new BackchannelRequest(
				"corr-1", "tp-1", "FB_1;FB_4_5", 42L, List.of(UP_1), null));

		assertThat(response)
				.extracting(BackchannelResponse::authorizationId,
						BackchannelResponse::resourceSubscriptionId,
						BackchannelResponse::customerSubscriptionId,
						BackchannelResponse::resourceUri,
						BackchannelResponse::authorizationUri,
						BackchannelResponse::customerResourceUri)
				.containsExactly(AUTH_ID, RES_SUB_ID, CUST_SUB_ID,
						"https://dc.example/Subscription/" + RES_SUB_ID,
						"https://dc.example/Authorization/" + AUTH_ID,
						"https://dc.example/RetailCustomer/42/Customer/" + CUST_SUB_ID);

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
				new BackchannelRequest("c", "tp", "", 1L, List.of(), null)))
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
				new BackchannelRequest("c", "tp", "FB_1", 1L, List.of(), null)))
				.isInstanceOf(DataCustodianBackchannelException.class)
				.hasMessageContaining("500");
	}

	@Test
	@DisplayName("non-standard status (e.g. 418) still surfaces as exception")
	void unexpectedStatus() {
		server.expect(requestTo("http://dc.example/internal/backchannel/v1/subscriptions"))
				.andRespond(withRawStatus(418).body("no coffee"));

		assertThatThrownBy(() -> client.provision(
				new BackchannelRequest("c", "tp", "FB_1", 1L, List.of(), null)))
				.isInstanceOf(DataCustodianBackchannelException.class)
				.hasMessageContaining("418");
	}
}
