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

import org.greenbuttonalliance.espi.common.domain.usage.AuthorizationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.thirdparty.oauth.DataCustodianRegistrations;
import org.greenbuttonalliance.espi.thirdparty.subscription.SubscriptionFetchService.FetchOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The Subscription page (#89): a signed-in TP customer sees the feed the TP fetched for one of their own
 * authorizations, and nothing of anyone else's. The fetch service and the saved fetches are mocked; the
 * feed reader and the schema validator are the real ones, reading the Task 5 fixtures.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubscriptionControllerTest {

	private static final UUID AUTH_ID = UUID.fromString("0f0e0d0c-0b0a-4909-8807-060504030201");
	private static final String PAGE = "/subscription/" + AUTH_ID;
	private static final String RESOURCE_URI =
			"http://localhost:8081/DataCustodian/espi/1_1/resource/Batch/Subscription/10a5db7c-ab59-5b0b-bd7d-63c8b9b20fa2";
	private static final String ACCESS_TOKEN = "secret-access-token-0123456789";
	private static final String REFRESH_TOKEN = "secret-refresh-token-9876543210";
	private static final String USAGE_POINT_ID = "urn:uuid:07171718-b165-522a-b413-11d9eeb1ca43";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SubscriptionFetchService fetchService;

	@MockitoBean
	private SubscriptionFeedFetchRepository fetches;

	private AuthorizationEntity authorization;

	private static RequestPostProcessor alice() {
		return user("alice").roles("USER");
	}

	private static String fixture(String name) throws IOException {
		try (InputStream in = SubscriptionControllerTest.class.getResourceAsStream("/subscription/" + name)) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static SubscriptionFeedFetchEntity savedFetch(int status, String body, XmlValidationMode mode, Boolean valid) {
		return new SubscriptionFeedFetchEntity(AUTH_ID, Instant.parse("2026-11-20T19:39:53Z"), RESOURCE_URI,
				Instant.parse("2025-10-21T00:00:00Z"), Instant.parse("2026-11-20T19:39:53Z"), status, body,
				new FeedValidation(mode, valid, Boolean.FALSE.equals(valid) ? 1 : 0, List.of()));
	}

	private void saved(SubscriptionFeedFetchEntity... saved) {
		when(fetches.findAllByAuthorizationIdOrderByFetchedAtDesc(AUTH_ID)).thenReturn(List.of(saved));
		if (saved.length > 0) {
			when(fetches.findFirstByAuthorizationIdOrderByFetchedAtDesc(AUTH_ID)).thenReturn(Optional.of(saved[0]));
		}
		for (SubscriptionFeedFetchEntity each : saved) {
			when(fetches.findByIdAndAuthorizationId(each.getId(), AUTH_ID)).thenReturn(Optional.of(each));
		}
	}

	private String page() throws Exception {
		return mockMvc.perform(get(PAGE).with(alice()))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
	}

	@BeforeEach
	void setUp() {
		RetailCustomerEntity alice = new RetailCustomerEntity();
		alice.setUsername("alice");
		authorization = new AuthorizationEntity();
		authorization.setId(AUTH_ID);
		authorization.setRetailCustomer(alice);
		authorization.setClientRegistrationId(DataCustodianRegistrations.MONTHLY);
		authorization.setResourceURI(RESOURCE_URI);
		authorization.setScope("FB=4_5_15_16;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=34128000");
		authorization.setAccessToken(ACCESS_TOKEN);
		authorization.setRefreshToken(REFRESH_TOKEN);
		authorization.activate();
		when(fetchService.ownedAuthorization(AUTH_ID, "alice")).thenReturn(authorization);
		when(fetchService.ownedAuthorization(eq(AUTH_ID), eq("mallory"))).thenThrow(new AuthorizationNotFoundException(AUTH_ID));
	}

	@Test
	void ownerSeesSubscription() throws Exception {
		saved(savedFetch(200, fixture("feed-with-278-errors.xml"), XmlValidationMode.REPORT, false));

		assertThat(page())
				.contains(USAGE_POINT_ID, "ElectricPowerQualitySummary", "13 months (395 days)", RESOURCE_URI)
				// the first reading: 420000 x 10^-3 Wh, at midnight Eastern standard time
				.contains("420 WH", "2026-01-01 00:00 (UTC-05:00)", "2026-01-01 05:00 UTC", "0.063 USD", "WH (72)")
				.containsPattern("(?s)(<details.*){2}")
				.doesNotContainPattern("(?s)(<details.*){3}")
				.doesNotContain(ACCESS_TOKEN, REFRESH_TOKEN, ACCESS_TOKEN.substring(0, 8));
		verify(fetchService, never()).fetch(any(), any(), any(), any());
	}

	@Test
	void otherUsersAuthorizationIs404() throws Exception {
		mockMvc.perform(get(PAGE).with(user("mallory").roles("USER")))
				.andExpect(status().isNotFound());
		mockMvc.perform(post(PAGE + "/fetch").with(user("mallory").roles("USER")).with(csrf()))
				.andExpect(status().isNotFound());

		verifyNoInteractions(fetches);
		verify(fetchService, never()).fetch(any(), any(), any(), any());
	}

	@Test
	void fetchOfAnotherAuthorizationIs404() throws Exception {
		saved(savedFetch(200, fixture("valid-feed.xml"), XmlValidationMode.REPORT, true));
		UUID otherFetch = UUID.randomUUID();
		when(fetches.findByIdAndAuthorizationId(otherFetch, AUTH_ID)).thenReturn(Optional.empty());

		mockMvc.perform(get(PAGE).param("fetch", otherFetch.toString()).with(alice()))
				.andExpect(status().isNotFound());
	}

	@Test
	void selectedFetchIsShown() throws Exception {
		SubscriptionFeedFetchEntity latest = savedFetch(200, fixture("empty-feed.xml"), XmlValidationMode.REPORT, true);
		SubscriptionFeedFetchEntity earlier = savedFetch(200, fixture("feed-with-278-errors.xml"), XmlValidationMode.REPORT, false);
		saved(latest, earlier);

		assertThat(mockMvc.perform(get(PAGE).param("fetch", earlier.getId().toString()).with(alice()))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString())
				.contains(USAGE_POINT_ID, "?fetch=" + latest.getId(), "?fetch=" + earlier.getId())
				.doesNotContain("The Data Custodian returned no UsagePoints for this range.");
	}

	@Test
	void revokedShowsAuthorizeAgain() throws Exception {
		authorization.revoke();
		saved(savedFetch(200, fixture("feed-with-278-errors.xml"), XmlValidationMode.REPORT, false));

		assertThat(page())
				.contains("Authorize again", "href=\"/oauth2/authorize/monthly\"", "?fetch=")
				.doesNotContain("Fetch now");
		verify(fetchService, never()).fetch(any(), any(), any(), any());
	}

	@Test
	void revokedWithoutSavedFetchesDoesNotFetch() throws Exception {
		authorization.revoke();
		saved();

		assertThat(page()).contains("Authorize again", "No fetches saved yet.");
		verify(fetchService, never()).fetch(any(), any(), any(), any());
	}

	@Test
	void activeWithoutSavedFetchFetchesThenRedirects() throws Exception {
		saved();
		UUID fetchId = UUID.randomUUID();
		when(fetchService.fetch(eq(AUTH_ID), any(Authentication.class), isNull(), isNull()))
				.thenReturn(new FetchOutcome.Saved(fetchId));

		mockMvc.perform(get(PAGE).with(alice()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl(PAGE + "?fetch=" + fetchId));
	}

	@Test
	void failedAutomaticFetchIsShownAndNotRepeated() throws Exception {
		saved();
		when(fetchService.fetch(eq(AUTH_ID), any(Authentication.class), isNull(), isNull()))
				.thenReturn(new FetchOutcome.Unreachable("Connection refused"));

		mockMvc.perform(get(PAGE).with(alice()))
				.andExpect(redirectedUrl(PAGE))
				.andExpect(flash().attribute("notice", "The Data Custodian could not be reached: Connection refused"));

		// the redirected GET carries the flash message: it shows it and does not fetch again
		assertThat(mockMvc.perform(get(PAGE).with(alice())
						.flashAttr("notice", "The Data Custodian could not be reached: Connection refused"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString())
				.contains("The Data Custodian could not be reached: Connection refused", "No fetches saved yet.");
		verify(fetchService).fetch(eq(AUTH_ID), any(Authentication.class), isNull(), isNull());
	}

	@Test
	void enforceInvalidShowsIssuesNotData() throws Exception {
		saved(savedFetch(200, fixture("feed-with-278-errors.xml"), XmlValidationMode.ENFORCE, false));

		assertThat(page())
				.contains("ENFORCE", "cvc-", "ERROR")
				.doesNotContain(USAGE_POINT_ID, "<details");
	}

	@Test
	void reportInvalidShowsIssuesAndData() throws Exception {
		saved(savedFetch(200, fixture("feed-with-278-errors.xml"), XmlValidationMode.REPORT, false));

		assertThat(page())
				.contains("REPORT", "cvc-", USAGE_POINT_ID, "<details");
	}

	@Test
	void validFeedSaysValid() throws Exception {
		saved(savedFetch(200, fixture("valid-feed.xml"), XmlValidationMode.REPORT, true));

		assertThat(page()).contains("The feed is valid against the ESPI schemas.").doesNotContain("cvc-");
	}

	@Test
	void showsReadErrorForNonFeedBody() throws Exception {
		saved(savedFetch(200, "<html>oops</html>", XmlValidationMode.REPORT, false));

		assertThat(page()).contains("The feed could not be read:").doesNotContain("<details");
	}

	@Test
	void showsEmptyFeedMessage() throws Exception {
		saved(savedFetch(200, fixture("empty-feed.xml"), XmlValidationMode.REPORT, true));

		assertThat(page()).contains("The Data Custodian returned no UsagePoints for this range.");
	}

	@Test
	void showsTheErrorBodyOfANon200FetchEscapedAndShortened() throws Exception {
		// 25 + 1965 + 15 = 2005 characters: the excerpt is the first 1999 and an ellipsis, 2000 in all
		String body = "<script>alert(1)</script>" + "x".repeat(1965) + "abcdefghijklmno";
		saved(savedFetch(403, body, XmlValidationMode.REPORT, null));

		assertThat(page())
				.contains("403 Forbidden", "&lt;script&gt;alert(1)&lt;/script&gt;", "x".repeat(1965) + "abcdefghi…")
				.doesNotContain("<script>alert(1)</script>", "abcdefghij", "<details");
	}

	@Test
	void showsTheUsagePointAndMeterReadingTitles() throws Exception {
		saved(savedFetch(200, fixture("feed-with-278-errors.xml"), XmlValidationMode.REPORT, false));

		assertThat(page()).contains("Sample residential electric service", "Hourly electricity consumption",
				"January 2026 billing period", "January 2026 power quality");
	}

	@Test
	void postOnRevokedDoesNotFetch() throws Exception {
		authorization.revoke();

		mockMvc.perform(post(PAGE + "/fetch").with(alice()).with(csrf()))
				.andExpect(redirectedUrl(PAGE))
				.andExpect(flash().attribute("notice", "The Data Custodian authorization has ended. Authorize again."));

		verify(fetchService, never()).fetch(any(), any(), any(), any());
	}

	@Test
	void postUnparseableDateFlashesTheDateMessageAndDoesNotFetch() throws Exception {
		mockMvc.perform(post(PAGE + "/fetch").param("from", "garbage").param("to", "2026-01-31")
						.with(alice()).with(csrf()))
				.andExpect(redirectedUrl(PAGE))
				.andExpect(flash().attribute("notice", "Dates must be in the form YYYY-MM-DD: garbage"));

		verify(fetchService, never()).fetch(any(), any(), any(), any());
	}

	@Test
	void postRedirectsToSavedFetch() throws Exception {
		UUID fetchId = UUID.randomUUID();
		when(fetchService.fetch(eq(AUTH_ID), any(Authentication.class), eq(LocalDate.of(2026, 1, 1)),
				eq(LocalDate.of(2026, 1, 31)))).thenReturn(new FetchOutcome.Saved(fetchId));

		mockMvc.perform(post(PAGE + "/fetch").param("from", "2026-01-01").param("to", "2026-01-31")
						.with(alice()).with(csrf()))
				.andExpect(redirectedUrl(PAGE + "?fetch=" + fetchId));
	}

	@Test
	void postWithoutDatesAsksForTheWholeGrant() throws Exception {
		UUID fetchId = UUID.randomUUID();
		when(fetchService.fetch(eq(AUTH_ID), any(Authentication.class), isNull(), isNull()))
				.thenReturn(new FetchOutcome.Saved(fetchId));

		mockMvc.perform(post(PAGE + "/fetch").param("from", "").param("to", "").with(alice()).with(csrf()))
				.andExpect(redirectedUrl(PAGE + "?fetch=" + fetchId));
	}

	@Test
	void postNotAuthorizedFlashesAuthorizeAgain() throws Exception {
		when(fetchService.fetch(eq(AUTH_ID), any(Authentication.class), isNull(), isNull()))
				.thenReturn(new FetchOutcome.NotAuthorized());

		mockMvc.perform(post(PAGE + "/fetch").with(alice()).with(csrf()))
				.andExpect(redirectedUrl(PAGE))
				.andExpect(flash().attribute("notice", "The Data Custodian authorization has ended. Authorize again."));
	}

	@Test
	void postInvalidRangeFlashesItsMessage() throws Exception {
		LocalDate from = LocalDate.of(2026, 2, 1);
		LocalDate to = LocalDate.of(2026, 1, 1);
		// the message FetchRange really gives (the fetch service is mocked here)
		String inverted = catchThrowable(() -> FetchRange.of(from, to, null, Clock.systemUTC())).getMessage();
		when(fetchService.fetch(eq(AUTH_ID), any(Authentication.class), eq(from), eq(to)))
				.thenReturn(new FetchOutcome.InvalidRange(inverted));

		mockMvc.perform(post(PAGE + "/fetch").param("from", "2026-02-01").param("to", "2026-01-01")
						.with(alice()).with(csrf()))
				.andExpect(redirectedUrl(PAGE))
				.andExpect(flash().attribute("notice", "The start date must be on or before the end date."));
	}

	@Test
	void postWithoutCsrfIs403() throws Exception {
		mockMvc.perform(post(PAGE + "/fetch").with(alice()))
				.andExpect(status().isForbidden());

		verify(fetchService, never()).fetch(any(), any(), any(), any());
	}

	@Test
	void anonymousIsSentToLogin() throws Exception {
		mockMvc.perform(get(PAGE))
				.andExpect(status().is3xxRedirection());

		verifyNoInteractions(fetchService, fetches);
	}
}
