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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.greenbuttonalliance.espi.common.domain.usage.UsageSummaryEntity;
import org.greenbuttonalliance.espi.common.dto.usage.UsageSummaryDto;
import org.greenbuttonalliance.espi.common.repositories.usage.UsageSummaryRepository;
import org.greenbuttonalliance.espi.common.service.impl.UsageSummaryExportService;
import org.greenbuttonalliance.espi.datacustodian.web.api.SubscriptionAccess.Reader;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;

/**
 * ESPI UsageSummary resources (#286):
 * <ul>
 *   <li>{@code GET /espi/1_1/resource/UsageSummary}: the UsageSummaries the token may read, within its HistoryLength
 *       (the admin: all, paged by the non-ESPI {@code limit}/{@code offset});</li>
 *   <li>{@code GET /espi/1_1/resource/UsageSummary/{usageSummaryId}}: one of them, else 404;</li>
 *   <li>{@code GET /espi/1_1/resource/RetailCustomer/{retailCustomerId}/UsagePoint/{usagePointId}/UsageSummary}: a
 *       UsagePoint's UsageSummaries, the collection a UsagePoint entry links to.</li>
 * </ul>
 * Which rows, and the entries' links, are {@link SubscriptionAccess} and {@link UsageEntries}.
 */
@RestController
@RequestMapping("/espi/1_1/resource")
@Tag(name = "Usage Summaries", description = "ESPI Usage Summary resource endpoints")
@SecurityRequirement(name = "oauth2")
@RequiredArgsConstructor
// open-in-view is off: the entries read lazy collections in this session (#286)
@Transactional(readOnly = true)
public class UsageSummaryRESTController {

    private final UsageSummaryRepository usageSummaryRepository;
    private final UsageSummaryExportService usageSummaryExportService;
    private final SubscriptionAccess subscriptionAccess;
    private final UsageEntries usageEntries;

    @GetMapping(value = "/UsageSummary", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get Usage Summaries",
        description = "The Usage Summaries the token may read: its subscription's within its HistoryLength, or every one for the DataCustodian admin",
        responses = {
            @ApiResponse(responseCode = "200", description = "Usage Summaries retrieved successfully",
                content = @Content(schema = @Schema(implementation = UsageSummaryDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope, or no subscription")
        }
    )
    public ResponseEntity<byte[]> getAllUsageSummaries(
            @Parameter(description = "DataCustodian admin only: page size (not ESPI)", example = "50")
            @RequestParam(defaultValue = "50") int limit,
            @Parameter(description = "DataCustodian admin only: page number (not ESPI)", example = "0")
            @RequestParam(defaultValue = "0") int offset,
            Authentication authentication) {

        Reader reader = subscriptionAccess.reader(authentication);
        return feed(reader.admin()
                ? usageSummaryRepository.findAll(PageRequest.of(offset, limit)).getContent()
                : reader.usageSummaries());
    }

    @GetMapping(value = "/UsageSummary/{usageSummaryId}", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get Usage Summary by ID",
        description = "One Usage Summary the token may read, with the same Atom links as in its feeds",
        responses = {
            @ApiResponse(responseCode = "200", description = "Usage Summary retrieved successfully",
                content = @Content(schema = @Schema(implementation = UsageSummaryDto.class))),
            @ApiResponse(responseCode = "404", description = "No such Usage Summary the token may read"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope, or no subscription")
        }
    )
    public ResponseEntity<byte[]> getUsageSummary(
            @Parameter(description = "Unique identifier of the Usage Summary", required = true)
            @PathVariable UUID usageSummaryId,
            Authentication authentication) {

        Reader reader = subscriptionAccess.reader(authentication);
        String base = UsageEntries.resourceBase(ServletUriComponentsBuilder.fromCurrentRequestUri().toUriString());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        usageSummaryExportService.exportDto(usageSummaryExportService.buildEntry(
                usageEntries.usageSummary(base, reader.usageSummary(usageSummaryId))), out);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(out.toByteArray());
    }

    @GetMapping(value = "/RetailCustomer/{retailCustomerId}/UsagePoint/{usagePointId}/UsageSummary", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get a Usage Point's Usage Summaries",
        description = "The Usage Summaries of one of the retail customer's Usage Points within the token's HistoryLength (RetailCustomer XPath collection)",
        responses = {
            @ApiResponse(responseCode = "200", description = "Usage Summaries retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No such Usage Point of that retail customer whose summaries the token may read"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not the token's retail customer")
        }
    )
    public ResponseEntity<byte[]> getUsagePointUsageSummaries(
            @Parameter(description = "Identifier of the Retail Customer", required = true)
            @PathVariable Long retailCustomerId,
            @Parameter(description = "Unique identifier of the Usage Point", required = true)
            @PathVariable UUID usagePointId,
            Authentication authentication) {

        Reader reader = subscriptionAccess.reader(authentication);
        return feed(reader.usageSummariesOf(reader.usagePoint(retailCustomerId, usagePointId, true)));
    }

    private ResponseEntity<byte[]> feed(List<UsageSummaryEntity> summaries) {
        String feedHref = ServletUriComponentsBuilder.fromCurrentRequestUri().toUriString();
        String base = UsageEntries.resourceBase(feedHref);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        usageSummaryExportService.exportDto(usageSummaryExportService.buildFeedOf(feedHref, "UsageSummary",
                "UsageSummary Feed", summaries.stream().map(summary -> usageEntries.usageSummary(base, summary)).toList()),
                out);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(out.toByteArray());
    }
}
