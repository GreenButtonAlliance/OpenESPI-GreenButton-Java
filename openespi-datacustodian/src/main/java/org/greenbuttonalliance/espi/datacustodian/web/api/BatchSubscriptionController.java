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

package org.greenbuttonalliance.espi.datacustodian.web.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto;
import org.greenbuttonalliance.espi.common.service.impl.UsageExportService;
import org.greenbuttonalliance.espi.datacustodian.web.api.SubscriptionFeedAssembler.Range;
import org.greenbuttonalliance.espi.datacustodian.web.api.scope.ScopeResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * {@code GET /espi/1_1/resource/Batch/Subscription/{subscriptionId}}: the {@code resourceURI} of a customer
 * access token (REQ.21 6.1.2), answered with one Atom feed of the subscription's energy usage data (#279).
 *
 * <p>Access ({@link ScopeResolver#canReadSubscription}): the DataCustodian admin, or a customer access
 * token for exactly this subscription. Anything else gets 403, whether or not the subscription exists. The
 * URL rule in {@code SecurityConfiguration} requires interval metering (FB_4) and an electricity, gas or water
 * Function Block (FB_5 to FB_11) first. Those commodity Function Blocks decide which UsagePoints the feed holds
 * (by service category), and the summary Function Blocks which summaries ({@link SubscriptionAccess#grants}).
 * Its entries are the same as in the root feeds, with ROOT-form links ({@link UsageEntries}): a resource is never
 * addressed through the Subscription, whose id changes with every grant.</p>
 */
@RestController
@RequestMapping("/espi/1_1/resource")
@Tag(name = "Batch", description = "ESPI Batch resource endpoints")
@SecurityRequirement(name = "oauth2")
public class BatchSubscriptionController {

	private static final String BATCH_SUBSCRIPTION = "/Batch/Subscription/";

	/** {@code YYYY-MM-DDThh:mm:ss[.fraction]Z}: UTC, ending with Z and nothing after it. */
	private static final Pattern UTC_TIMESTAMP =
			Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,9})?Z");

	private final ScopeResolver scopeResolver;
	private final SubscriptionAccess subscriptionAccess;
	private final SubscriptionFeedAssembler assembler;
	private final UsageExportService usageExportService;

	public BatchSubscriptionController(ScopeResolver scopeResolver, SubscriptionAccess subscriptionAccess,
									   SubscriptionFeedAssembler assembler, UsageExportService usageExportService) {
		this.scopeResolver = scopeResolver;
		this.subscriptionAccess = subscriptionAccess;
		this.assembler = assembler;
		this.usageExportService = usageExportService;
	}

	// No "produces": WebConfiguration's default content type is JSON when a request has no Accept header, so
	// an XML-only mapping would answer such requests 406. The response is always application/atom+xml.
	@GetMapping("/Batch/Subscription/{subscriptionId}")
	@Operation(
			summary = "Get a Subscription's energy usage data",
			description = "One Atom feed with the subscription's UsagePoints and their MeterReadings, ReadingTypes,"
					+ " IntervalBlocks, summaries and LocalTimeParameters, as granted by the token's scope",
			responses = {
					@ApiResponse(responseCode = "200", description = "The subscription's feed"),
					@ApiResponse(responseCode = "400",
							description = "A query parameter is not a UTC timestamp ending in Z, or a -min is after its -max"),
					@ApiResponse(responseCode = "401", description = "Unauthorized"),
					@ApiResponse(responseCode = "403", description = "Not this token's subscription"),
					@ApiResponse(responseCode = "404", description = "No such subscription (DataCustodian admin only)")
			}
	)
	public ResponseEntity<byte[]> getSubscription(
			@Parameter(description = "The subscription id from the token's resourceURI", required = true)
			@PathVariable UUID subscriptionId,
			@Parameter(description = "Return data whose interval ends after this UTC time (e.g. 2026-11-01T00:00:00Z)."
					+ " With none of the four range parameters: the last 24 hours")
			@RequestParam(name = "published-min", required = false) String publishedMin,
			@Parameter(description = "Return data whose interval starts at or before this UTC time (…Z)")
			@RequestParam(name = "published-max", required = false) String publishedMax,
			@Parameter(description = "Return data whose atom:updated is at or after this UTC time (…Z)")
			@RequestParam(name = "updated-min", required = false) String updatedMin,
			@Parameter(description = "Return data whose atom:updated is at or before this UTC time (…Z)")
			@RequestParam(name = "updated-max", required = false) String updatedMax,
			Authentication authentication) {

		// Access first: a caller without rights to the subscription gets 403 whatever its query parameters.
		if (!scopeResolver.canReadSubscription(authentication, subscriptionId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		Range range = new Range(utc("published-min", publishedMin), utc("published-max", publishedMax),
				utc("updated-min", updatedMin), utc("updated-max", updatedMax));
		if (reversed(range.publishedMin(), range.publishedMax()) || reversed(range.updatedMin(), range.updatedMax())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A -min query parameter is after its -max");
		}
		// The self link (and so the feed's UUID v5 id) keeps the query: a different range is a different feed.
		String feedHref = ServletUriComponentsBuilder.fromCurrentRequest().toUriString();
		String resourceBase = feedHref.substring(0, feedHref.indexOf(BATCH_SUBSCRIPTION));
		AtomFeedDto feed = assembler.build(subscriptionId, resourceBase, feedHref,
						subscriptionAccess.reader(authentication), range)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		usageExportService.exportDto(feed, out);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_ATOM_XML)
				.body(out.toByteArray());
	}

	private static boolean reversed(Instant min, Instant max) {
		return min != null && max != null && min.isAfter(max);
	}

	/**
	 * A query timestamp: a GBA timestamp that is not epoch seconds is UTC and ends with {@code Z}, with nothing
	 * after it ({@code 2026-11-01T00:00:00Z}, optionally with fractional seconds). A value without {@code Z}, or
	 * with an offset such as {@code +00:00}, is a 400.
	 */
	private static Instant utc(String name, String value) {
		if (value == null) {
			return null;
		}
		if (!UTC_TIMESTAMP.matcher(value).matches()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					name + " must be a UTC timestamp ending in Z, e.g. 2026-11-01T00:00:00Z");
		}
		try {
			return Instant.parse(value);
		} catch (DateTimeParseException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, name + " is not a valid timestamp");
		}
	}
}
