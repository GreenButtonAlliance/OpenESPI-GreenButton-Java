/*
 *
 *        Copyright (c) 2025-2026 Green Button Alliance, Inc.
 *
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package org.greenbuttonalliance.espi.thirdparty.oauth;

import org.greenbuttonalliance.espi.common.domain.common.enums.GrantType;
import org.greenbuttonalliance.espi.common.domain.common.enums.TokenType;
import org.greenbuttonalliance.espi.common.domain.usage.AuthorizationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.AuthorizationRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.RetailCustomerRepository;
import org.greenbuttonalliance.espi.common.service.EspiIdGeneratorService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Stores the tokens the Third Party obtains from a Data Custodian in its ESPI {@code authorizations}
 * table (#146), instead of Spring's in-memory store: they survive a restart, and the row keeps the
 * REQ.21 token-response URIs ({@code resourceURI}, {@code authorizationURI},
 * {@code customerResourceURI}) that the TP later fetches data from.
 *
 * <ul>
 *   <li><strong>authorization_code</strong> registrations ({@code datacustodian-monthly},
 *       {@code datacustodian-daily}): one row per TP customer and registration, linked to the
 *       customer's {@link RetailCustomerEntity}. The principal is the signed-in TP customer.</li>
 *   <li><strong>client_credentials</strong> ({@code datacustodian-admin}): the TP's client access
 *       token belongs to the Third Party, not to whoever asked for it, so it is one row with no
 *       customer, whatever the principal.</li>
 * </ul>
 *
 * <p>Re-authorizing replaces the row's tokens and URIs. Removal (Spring calls it when a refresh token
 * is rejected) revokes the row, which clears its tokens, so the history stays visible.</p>
 */
public class EspiAuthorizationOAuth2AuthorizedClientService implements OAuth2AuthorizedClientService {

	private static final String AUTHORIZATION_ENTITY_TYPE = "ThirdPartyAuthorization";

	private final ClientRegistrationRepository clientRegistrationRepository;
	private final AuthorizationRepository authorizationRepository;
	private final RetailCustomerRepository retailCustomerRepository;
	private final EspiIdGeneratorService idGenerator;
	private final EspiTokenResponseParameters tokenResponseParameters;

	public EspiAuthorizationOAuth2AuthorizedClientService(ClientRegistrationRepository clientRegistrationRepository,
														  AuthorizationRepository authorizationRepository,
														  RetailCustomerRepository retailCustomerRepository,
														  EspiIdGeneratorService idGenerator,
														  EspiTokenResponseParameters tokenResponseParameters) {
		this.clientRegistrationRepository = clientRegistrationRepository;
		this.authorizationRepository = authorizationRepository;
		this.retailCustomerRepository = retailCustomerRepository;
		this.idGenerator = idGenerator;
		this.tokenResponseParameters = tokenResponseParameters;
	}

	@Override
	@Transactional(readOnly = true)
	@SuppressWarnings("unchecked")
	public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId, String principalName) {
		ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(clientRegistrationId);
		if (registration == null) {
			return null;
		}
		return (T) find(registration, principalName)
				.filter(authorization -> authorization.getAccessToken() != null && !authorization.isRevoked())
				.map(authorization -> toAuthorizedClient(registration, principalName, authorization))
				.orElse(null);
	}

	/** The stored Authorization for a registration and principal, whatever its status. */
	@Transactional(readOnly = true)
	public Optional<AuthorizationEntity> findAuthorization(String clientRegistrationId, String principalName) {
		ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(clientRegistrationId);
		return registration == null ? Optional.empty() : find(registration, principalName);
	}

	@Override
	@Transactional
	public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal) {
		ClientRegistration registration = authorizedClient.getClientRegistration();
		AuthorizationEntity authorization = find(registration, principal.getName())
				.orElseGet(() -> newAuthorization(registration, principal.getName()));

		OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
		authorization.setAccessToken(accessToken.getTokenValue());
		authorization.setTokenType(TokenType.BEARER);
		// AuthorizationEntity.isExpired() compares expiresIn with System.currentTimeMillis().
		authorization.setExpiresIn(accessToken.getExpiresAt() == null ? null : accessToken.getExpiresAt().toEpochMilli());
		authorization.setScope(accessToken.getScopes().isEmpty() ? null
				: accessToken.getScopes().stream().sorted().collect(Collectors.joining(" ")));
		authorization.setRefreshToken(authorizedClient.getRefreshToken() == null ? null
				: authorizedClient.getRefreshToken().getTokenValue());
		authorization.setGrantType(isClientCredentials(registration) ? GrantType.CLIENT_CREDENTIALS
				: GrantType.AUTHORIZATION_CODE);
		// A refresh response that repeats no URI keeps the ones the authorization_code response gave.
		tokenResponseParameters.take(accessToken.getTokenValue()).ifPresent(uris -> {
			if (uris.resourceUri() != null) {
				authorization.setResourceURI(uris.resourceUri());
			}
			if (uris.authorizationUri() != null) {
				authorization.setAuthorizationURI(uris.authorizationUri());
			}
			if (uris.customerResourceUri() != null) {
				authorization.setCustomerResourceURI(uris.customerResourceUri());
			}
		});
		authorization.activate();
		authorizationRepository.save(authorization);
	}

	@Override
	@Transactional
	public void removeAuthorizedClient(String clientRegistrationId, String principalName) {
		findAuthorization(clientRegistrationId, principalName).ifPresent(authorization -> {
			authorization.revoke();
			authorizationRepository.save(authorization);
		});
	}

	private Optional<AuthorizationEntity> find(ClientRegistration registration, String principalName) {
		return isClientCredentials(registration)
				? authorizationRepository.findFirstByClientRegistrationIdAndRetailCustomerIsNullOrderByUpdatedDesc(
						registration.getRegistrationId())
				: authorizationRepository.findFirstByClientRegistrationIdAndRetailCustomer_UsernameOrderByUpdatedDesc(
						registration.getRegistrationId(), principalName);
	}

	private AuthorizationEntity newAuthorization(ClientRegistration registration, String principalName) {
		RetailCustomerEntity customer = null;
		if (!isClientCredentials(registration)) {
			customer = retailCustomerRepository.findByUsername(principalName)
					.orElseThrow(() -> new IllegalStateException(
							"Only a Third Party customer can authorize " + registration.getRegistrationId()));
		}
		AuthorizationEntity authorization = new AuthorizationEntity(customer, null, null);
		authorization.setId(authorizationId(registration, principalName));
		authorization.setThirdParty(registration.getClientId());
		authorization.setClientRegistrationId(registration.getRegistrationId());
		return authorization;
	}

	/**
	 * The row's id: a UUID5 of the registration and, for authorization_code, the customer. Each pair
	 * has exactly one row, so two saves racing to create it cannot both insert one: the second fails
	 * on the primary key instead of adding a duplicate.
	 */
	UUID authorizationId(ClientRegistration registration, String principalName) {
		String naturalKey = isClientCredentials(registration) ? registration.getRegistrationId()
				: registration.getRegistrationId() + ":" + principalName;
		return idGenerator.generateEntityId(AUTHORIZATION_ENTITY_TYPE, naturalKey);
	}

	private static OAuth2AuthorizedClient toAuthorizedClient(ClientRegistration registration, String principalName,
															 AuthorizationEntity authorization) {
		Instant expiresAt = authorization.getExpirationInstant();
		Instant issuedAt = issuedAt(authorization.getUpdated(), expiresAt);
		Set<String> scopes = authorization.getScope() == null ? Set.of()
				: new LinkedHashSet<>(Arrays.asList(authorization.getScope().trim().split("\\s+")));
		OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
				authorization.getAccessToken(), issuedAt, expiresAt, scopes);
		OAuth2RefreshToken refreshToken = authorization.getRefreshToken() == null ? null
				: new OAuth2RefreshToken(authorization.getRefreshToken(), issuedAt);
		return new OAuth2AuthorizedClient(registration, principalName, accessToken, refreshToken);
	}

	/** When the row was last written; never after the expiry (Spring requires issuedAt before expiresAt). */
	private static Instant issuedAt(LocalDateTime updated, Instant expiresAt) {
		Instant issuedAt = updated == null ? Instant.now() : updated.atZone(ZoneId.systemDefault()).toInstant();
		return expiresAt != null && !issuedAt.isBefore(expiresAt) ? expiresAt.minusSeconds(1) : issuedAt;
	}

	private static boolean isClientCredentials(ClientRegistration registration) {
		return AuthorizationGrantType.CLIENT_CREDENTIALS.equals(registration.getAuthorizationGrantType());
	}
}
