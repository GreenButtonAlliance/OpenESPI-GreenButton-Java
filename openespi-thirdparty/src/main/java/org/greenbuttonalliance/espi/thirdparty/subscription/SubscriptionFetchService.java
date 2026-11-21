package org.greenbuttonalliance.espi.thirdparty.subscription;

import org.greenbuttonalliance.espi.common.domain.usage.AuthorizationEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.AuthorizationRepository;
import org.greenbuttonalliance.espi.common.uri.EspiBatchUri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.web.client.ResourceAccessException;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Fetches a customer's {@code Batch/Subscription} feed from the Data Custodian and saves what came
 * back (#89). It holds no database transaction across the HTTP call: the lookups each run in their
 * own short one.
 */
@Service
public class SubscriptionFetchService {

	/** What a fetch came to. */
	public sealed interface FetchOutcome {
		/** The Data Custodian answered (any status) and the answer is saved. */
		record Saved(UUID fetchId) implements FetchOutcome {
		}

		/** The customer has no usable token for this Authorization. */
		record NotAuthorized() implements FetchOutcome {
		}

		/** The Data Custodian could not be reached; nothing is saved. */
		record Unreachable(String message) implements FetchOutcome {
		}

		/** The requested date range is not valid; no request was made. */
		record InvalidRange(String message) implements FetchOutcome {
		}

		/** The Data Custodian answered, but the database refused to save the answer (e.g. MySQL max_allowed_packet). */
		record NotSaved(String message) implements FetchOutcome {
		}
	}

	static final String AS_UNREACHABLE = "The Authorization Server could not be reached.";
	static final String NOT_SAVED = "The Data Custodian's answer could not be saved.";
	private static final Logger log = LoggerFactory.getLogger(SubscriptionFetchService.class);

	private final AuthorizationRepository authorizations;
	private final OAuth2AuthorizedClientManager authorizedClientManager;
	private final SubscriptionClient client;
	private final SubscriptionFeedValidator validator;
	private final SubscriptionFeedFetchRepository fetches;
	private final ObjectProvider<SubscriptionFeedCapture> capture;
	private final TransactionOperations transactions;
	private final Clock clock;

	@Autowired
	public SubscriptionFetchService(AuthorizationRepository authorizations,
									OAuth2AuthorizedClientManager authorizedClientManager,
									SubscriptionClient client,
									SubscriptionFeedValidator validator,
									SubscriptionFeedFetchRepository fetches,
									ObjectProvider<SubscriptionFeedCapture> capture,
									TransactionOperations transactions) {
		this(authorizations, authorizedClientManager, client, validator, fetches, capture, transactions, Clock.systemUTC());
	}

	SubscriptionFetchService(AuthorizationRepository authorizations,
							 OAuth2AuthorizedClientManager authorizedClientManager,
							 SubscriptionClient client,
							 SubscriptionFeedValidator validator,
							 SubscriptionFeedFetchRepository fetches,
							 ObjectProvider<SubscriptionFeedCapture> capture,
							 TransactionOperations transactions,
							 Clock clock) {
		this.authorizations = authorizations;
		this.authorizedClientManager = authorizedClientManager;
		this.client = client;
		this.validator = validator;
		this.fetches = fetches;
		this.capture = capture;
		this.transactions = transactions;
		this.clock = clock;
	}

	/**
	 * The Authorization when it is the signed-in customer's own and has a registration and a resource URI.
	 *
	 * @throws AuthorizationNotFoundException otherwise (also for another customer's, so ids are not probed)
	 */
	public AuthorizationEntity ownedAuthorization(UUID authorizationId, String username) {
		AuthorizationEntity authorization = transactions.execute(status -> authorizations.findById(authorizationId)
				.filter(a -> a.getRetailCustomer() != null && username != null
						&& username.equals(a.getRetailCustomer().getUsername()))
				.filter(a -> notBlank(a.getClientRegistrationId()) && notBlank(a.getResourceURI()))
				.orElse(null));
		if (authorization == null) {
			throw new AuthorizationNotFoundException(authorizationId);
		}
		return authorization;
	}

	public FetchOutcome fetch(UUID authorizationId, Authentication user, LocalDate from, LocalDate to) {
		AuthorizationEntity owned = ownedAuthorization(authorizationId, user.getName());

		FetchRange range;
		try {
			range = FetchRange.of(from, to, owned.getScope(), clock);
		} catch (IllegalArgumentException e) {
			return new FetchOutcome.InvalidRange(e.getMessage());
		}

		OAuth2AuthorizedClient authorizedClient;
		try {
			authorizedClient = authorizedClientManager.authorize(
					OAuth2AuthorizeRequest.withClientRegistrationId(owned.getClientRegistrationId()).principal(user).build());
		} catch (OAuth2AuthorizationException e) {
			if (authorizationServerUnreachable(e)) {
				return new FetchOutcome.Unreachable(AS_UNREACHABLE);
			}
			// the token is gone (ClientAuthorizationRequiredException) or its refresh was rejected
			authorizedClient = null;
		}
		if (authorizedClient == null) {
			return new FetchOutcome.NotAuthorized();
		}

		// Authorizing may have re-authorized and rewritten the row: use its current resource URI.
		String resourceUri = ownedAuthorization(authorizationId, user.getName()).getResourceURI();

		SubscriptionClient.Response response;
		try {
			response = client.get(resourceUri, authorizedClient.getAccessToken().getTokenValue(), range);
		} catch (DataCustodianUnreachableException e) {
			return new FetchOutcome.Unreachable(e.getMessage());
		}

		String subscriptionId = EspiBatchUri.subscriptionId(resourceUri).orElse("");
		SubscriptionFeedCapture feedCapture = capture.getIfAvailable();
		if (feedCapture != null) {
			feedCapture.capture(subscriptionId, response.status(), response.body());
		}

		FeedValidation validation = response.status() == 200
				? validator.validate(response.body())
				: new FeedValidation(validator.mode(), null, 0, List.of());
		SubscriptionFeedFetchEntity saved;
		try {
			saved = fetches.save(new SubscriptionFeedFetchEntity(authorizationId,
					clock.instant(), resourceUri, range.publishedMin(), range.publishedMax(), response.status(),
					// decoded as UTF-8 whatever the XML declaration says: ESPI feeds are UTF-8
					new String(response.body(), StandardCharsets.UTF_8), validation));
		} catch (DataAccessException e) {
			// the class and the database's reason only: the exception's SQL may carry the feed body
			log.warn("A Subscription fetch of {} bytes for authorization {} could not be saved: {}",
					response.body().length, authorizationId, e.getMostSpecificCause().getClass().getName());
			return new FetchOutcome.NotSaved(NOT_SAVED);
		}
		return new FetchOutcome.Saved(saved.getId());
	}

	/**
	 * A refresh that failed because the Authorization Server could not be reached or answered no token response
	 * (Spring's {@code invalid_token_response}) says nothing about the customer's authorization.
	 */
	private static boolean authorizationServerUnreachable(OAuth2AuthorizationException e) {
		if (e.getError() != null && "invalid_token_response".equals(e.getError().getErrorCode())) {
			return true;
		}
		for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
			if (cause instanceof ResourceAccessException) {
				return true;
			}
		}
		return false;
	}

	private static boolean notBlank(String value) {
		return value != null && !value.isBlank();
	}
}
