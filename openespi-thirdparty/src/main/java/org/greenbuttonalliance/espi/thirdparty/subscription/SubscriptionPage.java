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

import java.util.List;

/**
 * What {@code templates/subscription.html} shows (#89). It never carries a token. At most one of
 * {@code tree} and {@code readError} is set, and neither when no fetch is selected, the fetch was not a
 * 200, or {@code validation} blocks the display (enforce mode, invalid feed).
 *
 * @param authorization the customer's authorization
 * @param fetches       its saved fetches, newest first
 * @param selected      the fetch shown, or null when none is saved
 * @param errorBody     the start of a non-200 response body, or null
 * @param validation    the schema validation of a 200 body, or null
 * @param tree          the feed read into UsagePoints, or null
 * @param readError     why a 200 body could not be read, or null
 * @param message       a note about the data (no UsagePoints in the range), or null
 */
public record SubscriptionPage(Authorization authorization, List<Fetch> fetches, Fetch selected, String errorBody,
		FeedValidation validation, SubscriptionTree tree, String readError, String message) {

	/** The longest part of a non-200 response body the page shows. */
	public static final int ERROR_BODY_LIMIT = 2000;

	public SubscriptionPage {
		fetches = List.copyOf(fetches);
	}

	/**
	 * @param historyPeriod the scope's HistoryLength in calendar units ({@code 13 months (395 days)})
	 * @param authorizeUrl  the registration's authorize button, for "Authorize again", or null
	 */
	public record Authorization(String id, String access, String scope, String historyPeriod, String resourceUri,
			boolean active, String authorizeUrl) {
	}

	/** One saved fetch: {@code status} is e.g. {@code 200 OK}, {@code range} the published-min/max sent. */
	public record Fetch(String id, String fetchedAt, int httpStatus, String status, String range) {
	}
}
