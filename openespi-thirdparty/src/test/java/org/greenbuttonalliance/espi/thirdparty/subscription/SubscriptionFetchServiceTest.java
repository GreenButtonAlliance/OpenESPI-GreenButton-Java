package org.greenbuttonalliance.espi.thirdparty.subscription;

import org.greenbuttonalliance.espi.common.domain.usage.AuthorizationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.AuthorizationRepository;
import org.greenbuttonalliance.espi.thirdparty.subscription.SubscriptionFetchService.FetchOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionOperations;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SubscriptionFetchServiceTest {

	private static final String URI = "https://dc.example/espi/1_1/resource/Batch/Subscription/sub-1";
	private static final UUID FETCH_ID = UUID.fromString("00000000-0000-5000-8000-000000000001");
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-20T12:00:00Z"), ZoneOffset.UTC);

	private final UUID id = UUID.randomUUID();
	private final AuthorizationRepository authorizations = mock(AuthorizationRepository.class);
	private final OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);
	private final SubscriptionClient client = mock(SubscriptionClient.class);
	private final SubscriptionFeedValidator validator = mock(SubscriptionFeedValidator.class);
	private final SubscriptionFeedFetchRepository fetches = mock(SubscriptionFeedFetchRepository.class);
	@SuppressWarnings("unchecked")
	private final ObjectProvider<SubscriptionFeedCapture> capture = mock(ObjectProvider.class);
	private final SubscriptionFeedCapture captureBean = mock(SubscriptionFeedCapture.class);
	private final Authentication user = new TestingAuthenticationToken("alice", "x");
	private final AuthorizationEntity row = authorization(URI);
	private SubscriptionFetchService service;

	private AuthorizationEntity authorization(String resourceUri) {
		RetailCustomerEntity owner = new RetailCustomerEntity();
		owner.setUsername("alice");
		AuthorizationEntity entity = new AuthorizationEntity();
		entity.setId(id);
		entity.setRetailCustomer(owner);
		entity.setClientRegistrationId("datacustodian-monthly");
		entity.setResourceURI(resourceUri);
		entity.setScope("FB=4_5_13;HistoryLength=34128000");
		return entity;
	}

	@BeforeEach
	void setUp() {
		when(authorizations.findById(id)).thenReturn(Optional.of(row));
		when(validator.mode()).thenReturn(XmlValidationMode.REPORT);
		when(capture.getIfAvailable()).thenReturn(captureBean);
		when(fetches.save(any(SubscriptionFeedFetchEntity.class))).thenAnswer(call -> {
			SubscriptionFeedFetchEntity saved = call.getArgument(0);
			ReflectionTestUtils.setField(saved, "id", FETCH_ID);
			return saved;
		});
		service = new SubscriptionFetchService(authorizations, manager, client, validator, fetches, capture, TransactionOperations.withoutTransaction(), CLOCK);
	}

	private void grantToken() {
		ClientRegistration registration = ClientRegistration.withRegistrationId("datacustodian-monthly")
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.clientId("c").authorizationUri("https://a").tokenUri("https://t").redirectUri("https://r").build();
		OAuth2AccessToken token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "tok",
				Instant.parse("2026-11-20T11:00:00Z"), Instant.parse("2026-11-20T13:00:00Z"));
		when(manager.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(new OAuth2AuthorizedClient(registration, "alice", token));
	}

	@Test
	void otherUsersAuthorizationIsNotFound() {
		assertThatThrownBy(() -> service.ownedAuthorization(id, "mallory")).isInstanceOf(AuthorizationNotFoundException.class);
	}

	@Test
	void noResourceUriIsNotFound() {
		row.setResourceURI(null);

		assertThatThrownBy(() -> service.ownedAuthorization(id, "alice")).isInstanceOf(AuthorizationNotFoundException.class);
	}

	@Test
	void ownedAuthorizationReturnsTheRow() {
		assertThat(service.ownedAuthorization(id, "alice")).isSameAs(row);
	}

	@Test
	void noAuthorizedClientIsNotAuthorized() {
		when(manager.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(null);

		FetchOutcome outcome = service.fetch(id, user, null, null);

		assertThat(outcome).isEqualTo(new FetchOutcome.NotAuthorized());
		verifyNoInteractions(client);
		verify(fetches, never()).save(any());
	}

	@Test
	void usesResourceUriReloadedAfterAuthorize() {
		String changed = "https://dc.example/espi/1_1/resource/Batch/Subscription/sub-2";
		AuthorizationEntity reloaded = authorization(changed);
		when(authorizations.findById(id)).thenReturn(Optional.of(row), Optional.of(reloaded));
		grantToken();
		when(client.get(eq(changed), eq("tok"), any(FetchRange.class)))
				.thenReturn(new SubscriptionClient.Response(403, "no".getBytes(StandardCharsets.UTF_8)));

		FetchOutcome outcome = service.fetch(id, user, null, null);

		assertThat(outcome).isEqualTo(new FetchOutcome.Saved(FETCH_ID));
		verify(client).get(eq(changed), eq("tok"), any(FetchRange.class));
	}

	@Test
	void savesNon200WithoutValidation() {
		grantToken();
		when(client.get(eq(URI), eq("tok"), any(FetchRange.class)))
				.thenReturn(new SubscriptionClient.Response(403, "denied".getBytes(StandardCharsets.UTF_8)));

		FetchOutcome outcome = service.fetch(id, user, null, null);

		ArgumentCaptor<SubscriptionFeedFetchEntity> saved = ArgumentCaptor.forClass(SubscriptionFeedFetchEntity.class);
		verify(fetches).save(saved.capture());
		verify(validator, never()).validate(any());
		verify(captureBean).capture("sub-1", 403, "denied".getBytes(StandardCharsets.UTF_8));
		assertThat(outcome).isEqualTo(new FetchOutcome.Saved(FETCH_ID));
		assertThat(saved.getValue()).extracting(SubscriptionFeedFetchEntity::getHttpStatus, SubscriptionFeedFetchEntity::getBody,
						SubscriptionFeedFetchEntity::getSchemaValid, SubscriptionFeedFetchEntity::getErrorCount,
						SubscriptionFeedFetchEntity::getValidationMode, SubscriptionFeedFetchEntity::getResourceUri)
				.containsExactly(403, "denied", null, 0, XmlValidationMode.REPORT, URI);
	}

	@Test
	void unreachableSavesNothing() {
		grantToken();
		when(client.get(any(), any(), any())).thenThrow(new DataCustodianUnreachableException("no route", new IOException("x")));

		FetchOutcome outcome = service.fetch(id, user, null, null);

		assertThat(outcome).isEqualTo(new FetchOutcome.Unreachable("no route"));
		verify(fetches, never()).save(any());
		verify(captureBean, never()).capture(any(), anyInt(), any());
	}

	@Test
	void invalidRangeMakesNoRequest() {
		FetchOutcome outcome = service.fetch(id, user, LocalDate.of(2026, 1, 3), LocalDate.of(2026, 1, 2));

		assertThat(outcome).isEqualTo(new FetchOutcome.InvalidRange("The start date must be on or before the end date."));
		verifyNoInteractions(manager, client);
	}

	@Test
	void savesValidationResultFor200() {
		grantToken();
		byte[] xml = "<feed/>".getBytes(StandardCharsets.UTF_8);
		FeedValidation validation = new FeedValidation(XmlValidationMode.REPORT, false, 2, List.of());
		when(client.get(eq(URI), eq("tok"), any(FetchRange.class))).thenReturn(new SubscriptionClient.Response(200, xml));
		when(validator.validate(xml)).thenReturn(validation);

		FetchOutcome outcome = service.fetch(id, user, null, null);

		ArgumentCaptor<SubscriptionFeedFetchEntity> saved = ArgumentCaptor.forClass(SubscriptionFeedFetchEntity.class);
		verify(fetches).save(saved.capture());
		assertThat(outcome).isEqualTo(new FetchOutcome.Saved(FETCH_ID));
		assertThat(saved.getValue()).extracting(SubscriptionFeedFetchEntity::getHttpStatus, SubscriptionFeedFetchEntity::getBody,
						SubscriptionFeedFetchEntity::getSchemaValid, SubscriptionFeedFetchEntity::getErrorCount)
				.containsExactly(200, "<feed/>", false, 2);
	}
}
