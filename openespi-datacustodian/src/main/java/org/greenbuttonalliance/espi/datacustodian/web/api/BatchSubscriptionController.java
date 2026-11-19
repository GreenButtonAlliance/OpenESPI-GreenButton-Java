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
import org.greenbuttonalliance.espi.datacustodian.config.UsageFunctionBlockAuthorities;
import org.greenbuttonalliance.espi.datacustodian.web.api.SubscriptionFeedAssembler.Grants;
import org.greenbuttonalliance.espi.datacustodian.web.api.SubscriptionFeedAssembler.Range;
import org.greenbuttonalliance.espi.datacustodian.web.api.scope.ScopeResolver;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
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
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * {@code GET /espi/1_1/resource/Batch/Subscription/{subscriptionId}}: the {@code resourceURI} of a customer
 * access token (REQ.21 6.1.2), answered with one Atom feed of the subscription's energy usage data (#279).
 *
 * <p>Access ({@link ScopeResolver#canReadSubscription}): the DataCustodian admin, or a customer access
 * token for exactly this subscription. Anything else gets 403, whether or not the subscription exists. The
 * URL rule in {@code SecurityConfiguration} requires interval metering (FB_4) first. The token's commodity
 * Function Blocks decide which UsagePoints the feed holds (by service category; none with FB_4 alone), and its
 * summary Function Blocks which summaries ({@link UsageFunctionBlockAuthorities}).</p>
 */
@RestController
@RequestMapping("/espi/1_1/resource")
@Tag(name = "Batch", description = "ESPI Batch resource endpoints")
@SecurityRequirement(name = "oauth2")
public class BatchSubscriptionController {

	private static final String BATCH_SUBSCRIPTION = "/Batch/Subscription/";

	private final ScopeResolver scopeResolver;
	private final SubscriptionFeedAssembler assembler;
	private final UsageExportService usageExportService;

	public BatchSubscriptionController(ScopeResolver scopeResolver, SubscriptionFeedAssembler assembler,
									   UsageExportService usageExportService) {
		this.scopeResolver = scopeResolver;
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
					@ApiResponse(responseCode = "400", description = "A query parameter is not an xs:dateTime"),
					@ApiResponse(responseCode = "401", description = "Unauthorized"),
					@ApiResponse(responseCode = "403", description = "Not this token's subscription"),
					@ApiResponse(responseCode = "404", description = "No such subscription (DataCustodian admin only)")
			}
	)
	public ResponseEntity<byte[]> getSubscription(
			@Parameter(description = "The subscription id from the token's resourceURI", required = true)
			@PathVariable UUID subscriptionId,
			@Parameter(description = "Return data whose interval ends after this time (xs:dateTime)."
					+ " Without published-min and published-max: the last 24 hours")
			@RequestParam(name = "published-min", required = false) @DateTimeFormat(iso = ISO.DATE_TIME)
			OffsetDateTime publishedMin,
			@Parameter(description = "Return data whose interval starts at or before this time (xs:dateTime)")
			@RequestParam(name = "published-max", required = false) @DateTimeFormat(iso = ISO.DATE_TIME)
			OffsetDateTime publishedMax,
			@Parameter(description = "Return data whose atom:updated is at or after this time (xs:dateTime)")
			@RequestParam(name = "updated-min", required = false) @DateTimeFormat(iso = ISO.DATE_TIME)
			OffsetDateTime updatedMin,
			@Parameter(description = "Return data whose atom:updated is at or before this time (xs:dateTime)")
			@RequestParam(name = "updated-max", required = false) @DateTimeFormat(iso = ISO.DATE_TIME)
			OffsetDateTime updatedMax,
			Authentication authentication) {

		if (!scopeResolver.canReadSubscription(authentication, subscriptionId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		Range range = new Range(instant(publishedMin), instant(publishedMax), instant(updatedMin), instant(updatedMax));
		String feedHref = ServletUriComponentsBuilder.fromCurrentRequestUri().toUriString();
		String resourceBase = feedHref.substring(0, feedHref.indexOf(BATCH_SUBSCRIPTION));
		AtomFeedDto feed = assembler.build(subscriptionId, resourceBase, feedHref, grants(authentication), range)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		usageExportService.exportDto(feed, out);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_ATOM_XML)
				.body(out.toByteArray());
	}

	/**
	 * The admin sees everything; a customer token sees what its Function Blocks grant, no further back than its
	 * HistoryLength. An FB scope that does not parse is refused (403) rather than read as "no limit".
	 */
	private Grants grants(Authentication authentication) {
		if (scopeResolver.isAdmin(authentication)) {
			return Grants.all();
		}
		Long historyLength;
		try {
			historyLength = scopeResolver.historyLengthSeconds(authentication).orElse(null);
		} catch (IllegalArgumentException ex) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unreadable ESPI scope");
		}
		return new Grants(
				UsageFunctionBlockAuthorities.serviceCategories(authentication),
				UsageFunctionBlockAuthorities.hasAny(authentication, UsageFunctionBlockAuthorities.USAGE_SUMMARY),
				UsageFunctionBlockAuthorities.hasAny(authentication, UsageFunctionBlockAuthorities.POWER_QUALITY),
				historyLength);
	}

	private static Instant instant(OffsetDateTime time) {
		return time == null ? null : time.toInstant();
	}
}
