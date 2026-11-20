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

package org.greenbuttonalliance.espi.thirdparty.subscription;

import jakarta.servlet.http.HttpServletResponse;
import org.greenbuttonalliance.espi.common.domain.usage.AuthorizationEntity;
import org.greenbuttonalliance.espi.thirdparty.subscription.SubscriptionFeedFetchRepository.FetchSummary;
import org.greenbuttonalliance.espi.thirdparty.subscription.SubscriptionFetchService.FetchOutcome;
import org.greenbuttonalliance.espi.thirdparty.web.AuthorizationView;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

/**
 * The Subscription page (#89): the {@code Batch/Subscription} feed the Third Party fetched for one of the
 * signed-in customer's own authorizations. Every request first checks ownership
 * ({@link SubscriptionFetchService#ownedAuthorization}); an authorization or a fetch that is not the
 * customer's is a 404.
 */
@Controller
@RequestMapping("/subscription")
public class SubscriptionController {

	/** The flash attribute carrying a fetch's failure to the page after the redirect. */
	static final String NOTICE = "notice";

	static final String NOT_AUTHORIZED = "The Data Custodian authorization has ended. Authorize again.";
	static final String UNREACHABLE = "The Data Custodian could not be reached: ";
	static final String NO_USAGE_POINTS = "The Data Custodian returned no UsagePoints for this range.";
	static final String BAD_DATE = "Dates must be in the form YYYY-MM-DD: ";

	private final SubscriptionFetchService fetchService;
	private final SubscriptionFeedFetchRepository fetches;
	private final SubscriptionFeedValidator validator;
	private final SubscriptionFeedReader reader;
	private final ClientRegistrationRepository registrations;
	private final SubscriptionDisplay display = new SubscriptionDisplay();

	public SubscriptionController(SubscriptionFetchService fetchService, SubscriptionFeedFetchRepository fetches,
								  SubscriptionFeedValidator validator, SubscriptionFeedReader reader,
								  ClientRegistrationRepository registrations) {
		this.fetchService = fetchService;
		this.fetches = fetches;
		this.validator = validator;
		this.reader = reader;
		this.registrations = registrations;
	}

	/**
	 * Shows the fetch {@code fetch}, or the latest. With no saved fetch on an active authorization, fetches
	 * first and redirects (Post/Redirect/Get); after a failed fetch the redirect carries its notice, and the
	 * page then shows it instead of fetching again. A revoked authorization is never fetched.
	 */
	@GetMapping("/{authorizationId}")
	public String show(@PathVariable UUID authorizationId, @RequestParam(required = false) UUID fetch,
					   Authentication user, Model model, RedirectAttributes redirect) {
		AuthorizationEntity authorization = fetchService.ownedAuthorization(authorizationId, user.getName());
		boolean active = AuthorizationView.isStatusActive(authorization);
		List<FetchSummary> saved = fetches.findAllByAuthorizationIdOrderByFetchedAtDesc(authorizationId);

		SubscriptionFeedFetchEntity selected;
		if (fetch != null) {
			selected = fetches.findByIdAndAuthorizationId(fetch, authorizationId).orElseThrow(FetchNotFoundException::new);
		} else if (saved.isEmpty()) {
			// A GET with a side effect, accepted here: it runs only after the ownership check, at most once
			// (a failure comes back with a notice, which stops a second try), and only refreshes the signed-in
			// customer's own data from the Data Custodian they authorized.
			if (active && !model.containsAttribute(NOTICE)) {
				return redirectAfter(authorizationId, fetchService.fetch(authorizationId, user, null, null), redirect);
			}
			selected = null;
		} else {
			// only the shown fetch's body is loaded; the list above holds no bodies
			selected = fetches.findByIdAndAuthorizationId(saved.getFirst().id(), authorizationId)
					.orElseThrow(FetchNotFoundException::new);
		}

		model.addAttribute("page", page(authorization, active, saved, selected));
		model.addAttribute("display", display);
		return "subscription";
	}

	/**
	 * Fetches the feed again (optionally for the {@code from}/{@code to} dates, UTC, {@code to} inclusive; blank
	 * means the default range). An authorization that has ended, or a date that does not parse, is a notice on
	 * the page and no request to the Data Custodian.
	 */
	@PostMapping("/{authorizationId}/fetch")
	public String fetch(@PathVariable UUID authorizationId, @RequestParam(required = false) String from,
						@RequestParam(required = false) String to, Authentication user, RedirectAttributes redirect) {
		AuthorizationEntity authorization = fetchService.ownedAuthorization(authorizationId, user.getName());
		String page = "redirect:/subscription/" + authorizationId;
		if (!AuthorizationView.isStatusActive(authorization)) {
			return withNotice(page, NOT_AUTHORIZED, redirect);
		}
		LocalDate fromDate;
		LocalDate toDate;
		try {
			fromDate = date(from);
			toDate = date(to);
		} catch (DateTimeParseException e) {
			return withNotice(page, BAD_DATE + truncated(e.getParsedString()), redirect);
		}
		return redirectAfter(authorizationId, fetchService.fetch(authorizationId, user, fromDate, toDate), redirect);
	}

	/** User text echoed back in a notice is cut to 40 characters. */
	private static String truncated(String text) {
		return text.length() <= 40 ? text : text.substring(0, 40) + "…";
	}

	private static LocalDate date(String text) {
		return text == null || text.isBlank() ? null : LocalDate.parse(text.strip());
	}

	/** Not the customer's authorization, or not one of its fetches: a plain 404, naming nothing. */
	@ExceptionHandler({AuthorizationNotFoundException.class, FetchNotFoundException.class})
	public void notFound(HttpServletResponse response) throws IOException {
		response.sendError(HttpStatus.NOT_FOUND.value());
	}

	private static String redirectAfter(UUID authorizationId, FetchOutcome outcome, RedirectAttributes redirect) {
		String page = "redirect:/subscription/" + authorizationId;
		return switch (outcome) {
			case FetchOutcome.Saved saved -> page + "?fetch=" + saved.fetchId();
			case FetchOutcome.NotAuthorized ignored -> withNotice(page, NOT_AUTHORIZED, redirect);
			case FetchOutcome.Unreachable unreachable -> withNotice(page, UNREACHABLE + unreachable.message(), redirect);
			case FetchOutcome.InvalidRange invalid -> withNotice(page, invalid.message(), redirect);
		};
	}

	private static String withNotice(String page, String notice, RedirectAttributes redirect) {
		redirect.addFlashAttribute(NOTICE, notice);
		return page;
	}

	private SubscriptionPage page(AuthorizationEntity authorization, boolean active,
								  List<FetchSummary> saved, SubscriptionFeedFetchEntity selected) {
		SubscriptionPage.Authorization summary = new SubscriptionPage.Authorization(authorization.getId().toString(),
				accessName(authorization.getClientRegistrationId()), authorization.getScope(),
				display.historyPeriod(authorization.getScope()), authorization.getResourceURI(), active,
				AuthorizationView.authorizeUrl(authorization.getClientRegistrationId()));
		List<SubscriptionPage.Fetch> list = saved.stream().map(this::fetchSummary).toList();
		if (selected == null) {
			return new SubscriptionPage(summary, list, null, null, null, null, null, null);
		}
		SubscriptionPage.Fetch shown = fetchSummary(new FetchSummary(selected.getId(), selected.getFetchedAt(),
				selected.getHttpStatus(), selected.getPublishedMin(), selected.getPublishedMax()));
		String body = selected.getBody() == null ? "" : selected.getBody();
		if (selected.getHttpStatus() != HttpStatus.OK.value()) {
			// at most ERROR_BODY_LIMIT characters, the ellipsis included
			String excerpt = body.length() > SubscriptionPage.ERROR_BODY_LIMIT
					? body.substring(0, SubscriptionPage.ERROR_BODY_LIMIT - 1) + "…" : body;
			return new SubscriptionPage(summary, list, shown, excerpt, null, null, null, null);
		}
		FeedValidation validation = validator.revalidate(body, selected.getValidationMode());
		if (validation.blocksDisplay()) {
			return new SubscriptionPage(summary, list, shown, null, validation, null, null, null);
		}
		try {
			SubscriptionTree tree = reader.read(body.getBytes(StandardCharsets.UTF_8));
			String message = tree.usagePoints().isEmpty() ? NO_USAGE_POINTS : null;
			return new SubscriptionPage(summary, list, shown, null, validation, tree, null, message);
		} catch (SubscriptionFeedReadException e) {
			String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
			return new SubscriptionPage(summary, list, shown, null, validation, null, reason, null);
		}
	}

	private SubscriptionPage.Fetch fetchSummary(FetchSummary fetch) {
		HttpStatus status = HttpStatus.resolve(fetch.httpStatus());
		String statusText = status == null ? String.valueOf(fetch.httpStatus())
				: fetch.httpStatus() + " " + status.getReasonPhrase();
		return new SubscriptionPage.Fetch(fetch.id().toString(), utc(fetch.fetchedAt()), fetch.httpStatus(),
				statusText, utc(fetch.publishedMin()) + " to " + utc(fetch.publishedMax()));
	}

	private String utc(Instant instant) {
		return display.utc(instant == null ? null : instant.getEpochSecond());
	}

	private String accessName(String registrationId) {
		ClientRegistration registration = registrationId == null ? null : registrations.findByRegistrationId(registrationId);
		return registration == null ? registrationId : registration.getClientName();
	}

	/** A {@code fetch} id that is not one of the authorization's saved fetches. */
	static final class FetchNotFoundException extends RuntimeException {
		FetchNotFoundException() {
			super("No such fetch for this authorization");
		}
	}
}
