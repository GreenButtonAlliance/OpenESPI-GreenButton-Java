/*
 * Copyright 2026 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package org.greenbuttonalliance.espi.authserver.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.greenbuttonalliance.espi.authserver.dto.ClientManagementDTOs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * #332: {@code docs/openapi.yaml} describes the client-management requests and responses with the JSON names the
 * server reads and writes, so a client that follows it is not silently ignored.
 */
@DisplayName("docs/openapi.yaml client management schemas match the DTOs (#332)")
class ClientManagementOpenApiTest {

	private static Map<String, Object> spec() throws IOException {
		try (Reader reader = Files.newBufferedReader(Path.of("docs", "openapi.yaml"))) {
			return new Yaml().load(reader);
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> child(Map<String, Object> node, String... path) {
		Map<String, Object> current = node;
		for (String key : path) {
			current = (Map<String, Object>) current.get(key);
			assertThat(current).as(String.join(".", path)).isNotNull();
		}
		return current;
	}

	private static List<String> jsonNames(Class<?> dto) {
		return Arrays.stream(dto.getDeclaredFields())
				.map(field -> field.getAnnotation(JsonProperty.class))
				.filter(java.util.Objects::nonNull)
				.map(JsonProperty::value)
				.toList();
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource({"CreateClientRequest", "UpdateClientRequest", "CreateClientResponse"})
	@DisplayName("each schema has exactly the DTO's JSON properties")
	void schemaHasTheDtoProperties(String name) throws Exception {
		Class<?> dto = Class.forName(ClientManagementDTOs.class.getName() + "$" + name);

		assertThat(child(spec(), "components", "schemas", name, "properties").keySet())
				.containsExactlyInAnyOrderElementsOf(jsonNames(dto));
	}

	@Test
	@DisplayName("authentication_methods lists exactly the methods the server accepts")
	void authenticationMethodsMatchTheServer() throws Exception {
		assertThat(child(spec(), "components", "schemas", "CreateClientRequest", "properties",
				"authentication_methods", "items").get("enum"))
				.asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.list(String.class))
				.containsExactlyInAnyOrderElementsOf(OAuth2ClientManagementController.SUPPORTED_AUTHENTICATION_METHODS);
	}

	@Test
	@DisplayName("POST answers 201 with CreateClientResponse and PUT 200 with SuccessResponse, as the server does")
	void responsesMatchTheServer() throws Exception {
		Map<String, Object> paths = child(spec(), "paths");

		assertThat(List.of(
				child(paths, "/api/v1/oauth2/clients", "post", "responses", "201", "content", "application/json",
						"schema").get("$ref"),
				child(paths, "/api/v1/oauth2/clients/{clientId}", "put", "responses", "200", "content",
						"application/json", "schema").get("$ref")))
				.containsExactly("#/components/schemas/CreateClientResponse", "#/components/schemas/SuccessResponse");
	}

	@Test
	@DisplayName("SuccessResponse has exactly the DTO's JSON properties")
	void successResponseMatches() throws Exception {
		assertThat(child(spec(), "components", "schemas", "SuccessResponse", "properties").keySet())
				.containsExactlyInAnyOrderElementsOf(jsonNames(ClientManagementDTOs.SuccessResponse.class));
	}
}
