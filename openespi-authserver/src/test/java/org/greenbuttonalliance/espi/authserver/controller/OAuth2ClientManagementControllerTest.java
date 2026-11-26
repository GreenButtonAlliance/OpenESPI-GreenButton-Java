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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.assertj.core.api.InstanceOfAssertFactories.collection;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * #332: the client-management API accepts only the authentication methods a client created through it can use,
 * gives a public client no secret, and refuses fields it does not have, or values it would not apply, instead of
 * dropping them.
 */
@DisplayName("OAuth2ClientManagementController (#332)")
class OAuth2ClientManagementControllerTest {

	private static final String CLIENTS = "/api/v1/oauth2/clients";

	private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

	private final RegisteredClientRepository clients = mock(RegisteredClientRepository.class);
	private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
	private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
	private MockMvc mockMvc;

	/** A response's status and body. */
	private record Result(int status, String body) {
	}

	private final ListAppender<ILoggingEvent> log = new ListAppender<>();
	private final Logger controllerLogger =
			(Logger) LoggerFactory.getLogger(OAuth2ClientManagementController.class);

	@BeforeEach
	void setUp() {
		when(passwordEncoder.encode(anyString())).thenAnswer(call -> "{encoded}" + call.getArgument(0));
		mockMvc = MockMvcBuilders.standaloneSetup(
				new OAuth2ClientManagementController(clients, jdbcTemplate, passwordEncoder, VALIDATOR)).build();
		log.start();
		controllerLogger.addAppender(log);
	}

	@AfterEach
	void detachLog() {
		controllerLogger.detachAppender(log);
	}

	/** What the controller logged, one entry per event. */
	private List<String> logged() {
		return log.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
	}

	private Result perform(MockHttpServletRequestBuilder request, String json) throws Exception {
		MockHttpServletResponse response = mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(json))
				.andReturn().getResponse();
		return new Result(response.getStatus(), response.getContentAsString());
	}

	private Result create(String json) throws Exception {
		return perform(post(CLIENTS), json);
	}

	private Result update(String json) throws Exception {
		when(clients.findByClientId("tp")).thenReturn(client());
		return perform(put(CLIENTS + "/tp"), json);
	}

	private RegisteredClient saved() {
		ArgumentCaptor<RegisteredClient> client = ArgumentCaptor.forClass(RegisteredClient.class);
		verify(clients).save(client.capture());
		return client.getValue();
	}

	@Test
	@DisplayName("a public client (authentication method none only) gets no secret and requires PKCE")
	void publicClientGetsNoSecret() throws Exception {
		assertThat(create("""
				{"client_name":"Public TP","authentication_methods":["none"],
				 "redirect_uris":["https://tp.example/cb"]}
				""")).returns(201, Result::status)
				.extracting(Result::body, STRING).contains("\"client_id\"").doesNotContain("client_secret");
		assertThat(saved()).satisfies(c -> assertThat(c.getClientSecret()).isNull())
				.satisfies(c -> assertThat(c.getClientAuthenticationMethods())
						.containsExactly(ClientAuthenticationMethod.NONE))
				.extracting(RegisteredClient::getClientSettings).extracting(ClientSettings::isRequireProofKey)
				.isEqualTo(true);
		verify(passwordEncoder, never()).encode(anyString());
		// token_endpoint_auth_method, the 7th value of espi_application_info's INSERT, records the public client's none
		verify(jdbcTemplate).update(contains("INSERT INTO espi_application_info"), any(), any(), any(), any(), any(),
				any(), eq("none"), any(), any(), any(), any(), any(), any());
	}

	@Test
	@DisplayName("a confidential client gets a secret, returned once and stored encoded")
	void confidentialClientGetsASecret() throws Exception {
		assertThat(create("""
				{"client_name":"Confidential TP","authentication_methods":["client_secret_post"],
				 "redirect_uris":["https://tp.example/cb"]}
				""")).returns(201, Result::status)
				.extracting(Result::body, STRING).contains("\"client_secret\"");
		assertThat(saved()).satisfies(c -> assertThat(c.getClientSecret()).startsWith("{encoded}"))
				.extracting(RegisteredClient::getClientAuthenticationMethods, collection(ClientAuthenticationMethod.class))
				.containsExactly(ClientAuthenticationMethod.CLIENT_SECRET_POST);
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"client_secret_basik", "tls_client_auth", "self_signed_tls_client_auth",
			"private_key_jwt", "client_secret_jwt", ""})
	@DisplayName("an authentication method a client created here cannot use is refused, and nothing is saved")
	void unsupportedAuthenticationMethodIsRefused(String method) throws Exception {
		assertThat(create("""
				{"client_name":"TP","authentication_methods":["client_secret_basic","%s"]}
				""".formatted(method))).returns(400, Result::status)
				.extracting(Result::body, STRING).contains("invalid_request").contains("authentication_methods");
		verify(clients, never()).save(any());
	}

	@Test
	@DisplayName("a public client cannot have the client_credentials grant: it has nothing to authenticate with")
	void publicClientCannotUseClientCredentials() throws Exception {
		assertThat(create("""
				{"client_name":"TP","authentication_methods":["none"],"grant_types":["client_credentials"]}
				""")).returns(400, Result::status)
				.extracting(Result::body, STRING).contains("client_credentials");
		verify(clients, never()).save(any());
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', value = {
			"none, client_secret_basic        | client_secret_basic",
			"client_secret_post, none         | client_secret_post",
			"client_secret_post, client_secret_basic | client_secret_basic",
			"none                             | none"})
	@DisplayName("#332 review: the recorded token_endpoint_auth_method is the secret method when the client has one")
	void recordedAuthenticationMethodPrefersTheSecretMethod(String methods, String expected) {
		RegisteredClient.Builder builder = RegisteredClient.withId("id").clientId("c")
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE).redirectUri("https://tp.example/cb");
		Arrays.stream(methods.split(",")).map(String::trim)
				.forEach(m -> builder.clientAuthenticationMethod(new ClientAuthenticationMethod(m)));

		assertThat(OAuth2ClientManagementController.recordedAuthenticationMethod(builder.build())).isEqualTo(expected);
	}

	@Test
	@DisplayName("#332 review: a refused grant_type is quoted in the response and the log without its line breaks")
	void refusedGrantTypeIsNotEchoedRaw() throws Exception {
		assertThat(create("""
				{"client_name":"TP","grant_types":["x\\r\\nINFO forged"]}
				""")).returns(400, Result::status)
				.extracting(Result::body, STRING).contains("x??INFO forged").doesNotContain("\\r", "\\n");
		assertThat(logged()).anyMatch(line -> line.contains("x??INFO forged")).noneMatch(OAuth2ClientManagementControllerTest::breaksLines);
		verify(clients, never()).save(any());
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"POST", "PUT"})
	@DisplayName("#332 review (round 2): a scope Spring refuses is quoted in the response and the log without its line breaks")
	void refusedScopeIsNotEchoedRaw(String method) throws Exception {
		String json = """
				{"client_name":"TP","redirect_uris":["https://tp.example/cb"],"scopes":["x\\r\\nINFO forged"]}
				""";
		Result result = "POST".equals(method) ? create(json) : update(json);

		assertThat(result).returns(400, Result::status)
				.extracting(Result::body, STRING).contains("invalid_request").contains("x??INFO forged")
				.doesNotContain("\\r", "\\n");
		// The handler's own line, not create's earlier non-ESPI scope warning (#332 review, round 3)
		String handlerLine = "POST".equals(method) ? "Client creation validation failed:" : "Client update validation failed:";
		assertThat(logged()).anyMatch(line -> line.startsWith(handlerLine) && line.contains("x??INFO forged"))
				.noneMatch(OAuth2ClientManagementControllerTest::breaksLines);
		verify(clients, never()).save(any());
	}

	@Test
	@DisplayName("#332 review (round 3): a client_name_prefix outside letters, digits, '_' and '-' is refused")
	void unsafeClientIdPrefixIsRefused() throws Exception {
		assertThat(create("""
				{"client_name":"TP","client_name_prefix":"tp\\r\\n2026-11-25 INFO admin granted FB_51",
				 "redirect_uris":["https://tp.example/cb"]}
				""")).returns(400, Result::status)
				.extracting(Result::body, STRING).contains("invalid_request").contains("client_name_prefix")
				.doesNotContain("\\r", "\\n");
		// The handler's own line, so the check fails if nothing is logged (#332 review, round 4)
		assertThat(logged())
				.anyMatch(line -> line.startsWith("Client creation validation failed:")
						&& line.contains("tp??2026-11-25 INFO admin granted FB_51"))
				.noneMatch(OAuth2ClientManagementControllerTest::breaksLines);
		verify(clients, never()).save(any());
	}

	@ParameterizedTest(name = "\"{0}\"")
	@ValueSource(strings = {"", " ", "\\t", "ab1234567890123456789012345678901234567890"})
	@DisplayName("#332 review (round 4): an empty, blank or 42-character client_name_prefix is refused, not defaulted")
	void emptyOrTooLongClientIdPrefixIsRefused(String prefix) throws Exception {
		assertThat(create("""
				{"client_name":"TP","client_name_prefix":"%s","redirect_uris":["https://tp.example/cb"]}
				""".formatted(prefix))).returns(400, Result::status)
				.extracting(Result::body, STRING).contains("invalid_request").contains("client_name_prefix must be 1-41");
		verify(clients, never()).save(any());
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"gb_Connect-2", "a1234567890123456789012345678901234567890"})
	@DisplayName("#332 review (round 4): a client_name_prefix of 1-41 letters, digits, '_' and '-' starts a client_id of at most 64")
	void safeClientIdPrefixStartsTheClientId(String prefix) throws Exception {
		assertThat(create("""
				{"client_name":"TP","client_name_prefix":"%s","redirect_uris":["https://tp.example/cb"]}
				""".formatted(prefix))).returns(201, Result::status);
		assertThat(saved().getClientId())
				.matches(java.util.regex.Pattern.quote(prefix) + "_\\d{13}_[0-9a-f]{8}")
				.hasSizeLessThanOrEqualTo(64);
	}

	@Test
	@DisplayName("#332 review (round 5): an absent or null client_name_prefix gives the espi_client default")
	void nullClientIdPrefixGetsTheDefault() throws Exception {
		assertThat(create("""
				{"client_name":"TP","client_name_prefix":null,"redirect_uris":["https://tp.example/cb"]}
				""")).returns(201, Result::status);
		assertThat(saved().getClientId()).matches("espi_client_\\d{13}_[0-9a-f]{8}");
	}

	@ParameterizedTest(name = "millis {0}, random {1}")
	@CsvSource({"0, 0", "1764115200000, -1", "9999999999999, 2147483647", "1764115200000, 1"})
	@DisplayName("#332 review (round 5): the client_id suffix is always CLIENT_ID_SUFFIX_LENGTH characters")
	void clientIdSuffixHasAFixedLength(long millis, int random) {
		assertThat(OAuth2ClientManagementController.clientIdSuffix(millis, random))
				.matches("_\\d{13}_[0-9a-f]{8}")
				.hasSize(OAuth2ClientManagementController.CLIENT_ID_SUFFIX_LENGTH);
	}

	@Test
	@DisplayName("#332 review (round 5): the longest prefix gives a client_id of exactly 64; a longer or unsafe one is refused where the id is built")
	void clientIdEnforcesItsLimits() {
		String longest = "a".repeat(OAuth2ClientManagementController.CLIENT_ID_PREFIX_MAX_LENGTH);

		assertThat(OAuth2ClientManagementController.clientId(longest, 1764115200000L, -1))
				.hasSize(OAuth2ClientManagementController.CLIENT_ID_MAX_LENGTH)
				.hasSize(64);
		assertThatThrownBy(() -> OAuth2ClientManagementController.clientId(longest + "a", 1764115200000L, -1))
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> OAuth2ClientManagementController.clientId(" ", 1764115200000L, -1))
				.isInstanceOf(IllegalStateException.class);
		// A 14-digit millisecond value (after the year 2286) would make the suffix one character longer
		assertThatThrownBy(() -> OAuth2ClientManagementController.clientId(longest, 10000000000000L, -1))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("64");
	}

	@Test
	@DisplayName("#332 review (rounds 3-4): safeMessage keeps a long message's start and its reason, and never says \"null\" or \"\"")
	void safeMessageKeepsTheReason() {
		// Quotes every 50 characters: Spring's message for a value that itself contains '"'
		String quoted = "https://tp.example/" + "a\"".repeat(10) + ("b".repeat(49) + "\"").repeat(20) + "\r\n#frag";
		String reason = "\" is not a valid redirect URI or contains fragment";

		assertThat(List.of(
				OAuth2ClientManagementController.safeMessage(new IllegalArgumentException(
						"redirect_uri \"" + quoted + reason)),
				OAuth2ClientManagementController.safeMessage(new IllegalArgumentException("short \"value\" refused")),
				OAuth2ClientManagementController.safeMessage(new IllegalArgumentException()),
				OAuth2ClientManagementController.safeMessage(new IllegalArgumentException("")),
				OAuth2ClientManagementController.safeMessage(new IllegalArgumentException(" \t"))))
				.satisfiesExactly(
						message -> assertThat(message).hasSize(OAuth2ClientManagementController.MAX_MESSAGE_LENGTH)
								.startsWith("redirect_uri \"https://tp.example/a\"")
								.contains("...")
								.endsWith("??#frag" + reason)
								.doesNotContain("\r", "\n"),
						message -> assertThat(message).isEqualTo("short \"value\" refused"),
						message -> assertThat(message).isEqualTo("Invalid request"),
						message -> assertThat(message).isEqualTo("Invalid request"),
						message -> assertThat(message).isEqualTo("Invalid request"));
	}

	@Test
	@DisplayName("#332 review (round 2): a client_name with line breaks is logged without them on create")
	void clientNameIsLoggedWithoutLineBreaks() throws Exception {
		assertThat(create("""
				{"client_name":"TP\\r\\n2026-11-25 INFO admin granted FB_51","redirect_uris":["https://tp.example/cb"]}
				""")).returns(201, Result::status);
		assertThat(logged()).anyMatch(line -> line.contains("TP??2026-11-25 INFO admin granted FB_51"))
				.noneMatch(OAuth2ClientManagementControllerTest::breaksLines);
	}

	private static boolean breaksLines(String line) {
		return line.contains("\r") || line.contains("\n");
	}

	@Test
	@DisplayName("a create request naming a field it does not have (the old spec's camelCase) is refused, not dropped")
	void createWithUnknownFieldIsRefused() throws Exception {
		assertThat(create("""
				{"client_name":"TP","clientSettings":{"requireProofKey":true},
				 "clientAuthenticationMethods":["client_secret_basic"]}
				""")).returns(400, Result::status)
				.extracting(Result::body, STRING).contains("clientAuthenticationMethods").contains("clientSettings");
		verify(clients, never()).save(any());
	}

	@Test
	@DisplayName("an update naming a field it does not apply (settings, authentication methods) is refused, not ignored")
	void updateWithUnknownFieldIsRefused() throws Exception {
		assertThat(update("""
				{"client_name":"Renamed","authentication_methods":["none"],"require_proof_key":true}
				""")).returns(400, Result::status)
				.extracting(Result::body, STRING).contains("authentication_methods").contains("require_proof_key");
		verify(clients, never()).save(any());
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"{\"require_proof_key\":true}", "{\"redirect_uris\":[\"ftp://tp.example/cb\"]}",
			"{\"client_name\":\" \"}", "{\"security_level\":\"bogus\"}", "{\"rate_limit_per_minute\":0}",
			"{\"max_concurrent_sessions\":1001}", "{\"scopes\":[null]}"})
	@DisplayName("#332 review, #344: an update of a client that does not exist is 404, whatever fields or values it has")
	void updateOfAnUnknownClientIsNotFound(String json) throws Exception {
		assertThat(perform(put(CLIENTS + "/does-not-exist"), json)).returns(404, Result::status);
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', value = {
			"{\"security_level\":\"bogus\"}|Security level must be standard, high, or maximum",
			"{\"rate_limit_per_minute\":0}|Rate limit must be at least 1 request per minute",
			"{\"max_concurrent_sessions\":1001}|Max concurrent sessions must not exceed 1000"})
	@DisplayName("#344: an update of a known client still applies the field constraints, after the lookup")
	void updateOfAKnownClientIsValidated(String json, String message) throws Exception {
		assertThat(update(json)).returns(400, Result::status)
				.extracting(Result::body, STRING).contains("invalid_request").contains(message);
		verify(clients, never()).save(any());
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"[null]", "[\"\"]", "[\"  \"]", "[\"FB=4_5_15\",null]"})
	@DisplayName("#344: a null or blank scope is refused on create and update, not saved as \"null\"")
	void blankScopeIsRefused(String scopes) throws Exception {
		assertThat(List.of(
				create("""
						{"client_name":"TP","redirect_uris":["https://tp.example/cb"],"scopes":%s}
						""".formatted(scopes)),
				update("{\"scopes\":%s}".formatted(scopes))))
				.allSatisfy(result -> assertThat(result).returns(400, Result::status)
						.extracting(Result::body, STRING).contains("scopes must not contain a null or blank value"));
		verify(clients, never()).save(any());
	}

	@Test
	@DisplayName("#344: a status change without a reason is applied, audited and answered 200")
	void statusWithoutReasonIsAudited() throws Exception {
		when(clients.findByClientId("tp")).thenReturn(client());

		assertThat(perform(put(CLIENTS + "/tp/status"), "{\"status\":\"suspended\"}")).returns(200, Result::status);
		verify(jdbcTemplate).update(contains("UPDATE oauth2_registered_client"), eq("suspended"), isNull(), isNull(),
				eq("tp"));
		verify(jdbcTemplate).update(contains("INSERT INTO oauth2_audit_log"), eq("client_status_update"), eq("tp"),
				isNull(), eq(true), eq("{\"new_status\":\"suspended\"}"));
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"{\"status\":\"bogus\"}", "{\"status\":\"suspended\",\"lock_duration_minutes\":0}",
			"{\"status\":\"suspended\",\"lockDuration\":5}", "{}"})
	@DisplayName("#344: a status change of a client that does not exist is 404, whatever the body holds")
	void statusOfAnUnknownClientIsNotFound(String json) throws Exception {
		assertThat(perform(put(CLIENTS + "/does-not-exist/status"), json)).returns(404, Result::status);
		verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', value = {
			"{\"status\":\"bogus\"}|Status must be active, suspended, or revoked",
			"{}|Status is required",
			"{\"status\":\"suspended\",\"lockDuration\":5}|Unknown field(s): lockDuration"})
	@DisplayName("#344: a status change of a known client is validated after the lookup and changes nothing when refused")
	void statusOfAKnownClientIsValidated(String json, String message) throws Exception {
		when(clients.findByClientId("tp")).thenReturn(client());

		assertThat(perform(put(CLIENTS + "/tp/status"), json)).returns(400, Result::status)
				.extracting(Result::body, STRING).contains("invalid_request").contains(message);
		verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
	}

	@Test
	@DisplayName("#344: the list and its count use the same filters; the count has no ORDER BY or LIMIT")
	void listCountsWithTheSameFilters() throws Exception {
		when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(3L);

		assertThat(perform(get(CLIENTS + "?status=active&sortBy=client_name&sortDirection=desc&page=1&size=2"), ""))
				.returns(200, Result::status)
				.extracting(Result::body, STRING).contains("\"total_elements\":3").contains("\"total_pages\":2");
		ArgumentCaptor<String> countSql = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate).queryForObject(countSql.capture(), eq(Long.class), eq("active"));
		assertThat(countSql.getValue()).startsWith("SELECT COUNT(*) FROM oauth2_registered_client c")
				.contains("WHERE 1=1 AND c.client_status = ?").doesNotContain("ORDER BY", "LIMIT");
		verify(jdbcTemplate).query(contains("ORDER BY c.client_name DESC LIMIT ? OFFSET ?"),
				eq(new Object[] {"active", 2, 2L}), any(RowMapper.class));
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"sortBy=client_secret", "sortBy=CLIENT_ID", "sortBy=c.client_id", "sortDirection=up", "page=-1",
			"size=0", "size=101"})
	@DisplayName("#344: the list refuses a sort column, direction or page size outside its fixed values, before any query")
	void listRefusesParametersOutsideItsFixedValues(String query) throws Exception {
		assertThat(perform(get(CLIENTS + "?" + query), "")).returns(400, Result::status)
				.extracting(Result::body, STRING).contains("invalid_request");
		verifyNoInteractions(jdbcTemplate);
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"client_id", "client_name", "client_id_issued_at", "last_used_at"})
	@DisplayName("#344: each allowed sort column reaches the ORDER BY as its fixed column")
	void listSortsByAnAllowedColumn(String sortBy) throws Exception {
		assertThat(perform(get(CLIENTS + "?sortBy=" + sortBy), "")).returns(200, Result::status);
		verify(jdbcTemplate).query(contains("ORDER BY c." + sortBy + " ASC LIMIT ? OFFSET ?"), any(Object[].class),
				any(RowMapper.class));
	}

	@Test
	@DisplayName("#344: POST /bulk is gone")
	void bulkIsGone() throws Exception {
		assertThat(perform(post(CLIENTS + "/bulk"), "{\"operation\":\"delete\",\"client_ids\":[\"tp\"]}").status())
				.isIn(404, 405);
		verifyNoInteractions(jdbcTemplate);
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"javascript:alert(1)", "myapp://cb", "ftp://tp.example/cb", ""})
	@DisplayName("#332 review: an update cannot set a redirect URI that create would refuse")
	void updateRefusesARedirectUriCreateWouldRefuse(String uri) throws Exception {
		assertThat(update("""
				{"redirect_uris":["https://tp.example/cb","%s"]}
				""".formatted(uri))).returns(400, Result::status)
				.extracting(Result::body, STRING).contains("invalid_request").contains("redirect_uri");
		verify(clients, never()).save(any());
	}

	@ParameterizedTest(name = "''{0}''")
	@ValueSource(strings = {"", "   "})
	@DisplayName("#332 review: an update with a blank client_name is refused, not answered 200 with no change")
	void updateRefusesABlankName(String name) throws Exception {
		assertThat(update("""
				{"client_name":"%s"}
				""".formatted(name))).returns(400, Result::status)
				.extracting(Result::body, STRING).contains("client_name");
		verify(clients, never()).save(any());
	}

	@Test
	@DisplayName("#332 review: updated_by given alone is recorded")
	void updatedByAloneIsRecorded() throws Exception {
		assertThat(update("""
				{"updated_by":"admin-7"}
				""")).returns(200, Result::status);
		verify(jdbcTemplate).update(eq("UPDATE oauth2_registered_client SET updated_by = ? WHERE client_id = ?"),
				eq("admin-7"), eq("tp"));
	}

	@Test
	@DisplayName("an update with only the fields it applies still succeeds")
	void updateWithKnownFieldsSucceeds() throws Exception {
		assertThat(update("""
				{"client_name":"Renamed","redirect_uris":["https://tp.example/new-cb"]}
				""")).returns(200, Result::status);
		assertThat(saved()).satisfies(c -> assertThat(c.getClientName()).isEqualTo("Renamed"))
				.extracting(RegisteredClient::getRedirectUris, collection(String.class))
				.containsExactly("https://tp.example/new-cb");
	}

	private static RegisteredClient client() {
		return RegisteredClient.withId("id-tp").clientId("tp").clientSecret("{noop}secret")
				.clientIdIssuedAt(Instant.now())
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri("https://tp.example/cb").scope("FB=4_5_15").build();
	}
}
