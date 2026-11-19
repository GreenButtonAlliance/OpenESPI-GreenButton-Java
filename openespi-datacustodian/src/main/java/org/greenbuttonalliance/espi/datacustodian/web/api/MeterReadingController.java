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
import org.greenbuttonalliance.espi.common.domain.usage.MeterReadingEntity;
import org.greenbuttonalliance.espi.common.dto.usage.MeterReadingDto;
import org.greenbuttonalliance.espi.common.repositories.usage.MeterReadingRepository;
import org.greenbuttonalliance.espi.common.service.impl.MeterReadingExportService;
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
 * ESPI MeterReading resources (#286):
 * <ul>
 *   <li>{@code GET /espi/1_1/resource/MeterReading}: the MeterReadings the token may read (the admin: all, paged by
 *       the non-ESPI {@code limit}/{@code offset});</li>
 *   <li>{@code GET /espi/1_1/resource/MeterReading/{meterReadingId}}: one of them, else 404;</li>
 *   <li>{@code GET /espi/1_1/resource/RetailCustomer/{retailCustomerId}/UsagePoint/{usagePointId}/MeterReading}: a
 *       UsagePoint's MeterReadings, the collection a UsagePoint entry links to.</li>
 * </ul>
 * Which rows, and the entries' links, are {@link SubscriptionAccess} and {@link UsageEntries}.
 */
@RestController
@RequestMapping("/espi/1_1/resource")
@Tag(name = "Meter Readings", description = "ESPI Meter Reading resource endpoints")
@SecurityRequirement(name = "oauth2")
@RequiredArgsConstructor
// open-in-view is off: the entries read lazy collections in this session (#286)
@Transactional(readOnly = true)
public class MeterReadingController {

    private final MeterReadingRepository meterReadingRepository;
    private final MeterReadingExportService meterReadingExportService;
    private final SubscriptionAccess subscriptionAccess;
    private final UsageEntries usageEntries;

    @GetMapping(value = "/MeterReading", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get Meter Readings",
        description = "The Meter Readings the token may read: its subscription's, or every one for the DataCustodian admin",
        responses = {
            @ApiResponse(responseCode = "200", description = "Meter Readings retrieved successfully",
                content = @Content(schema = @Schema(implementation = MeterReadingDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope, or no subscription")
        }
    )
    public ResponseEntity<byte[]> getAllMeterReadings(
            @Parameter(description = "DataCustodian admin only: page size (not ESPI)", example = "50")
            @RequestParam(defaultValue = "50") int limit,
            @Parameter(description = "DataCustodian admin only: page number (not ESPI)", example = "0")
            @RequestParam(defaultValue = "0") int offset,
            Authentication authentication) {

        Reader reader = subscriptionAccess.reader(authentication);
        return feed(reader, reader.admin()
                ? meterReadingRepository.findAll(PageRequest.of(offset, limit)).getContent()
                : reader.meterReadings());
    }

    @GetMapping(value = "/MeterReading/{meterReadingId}", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get Meter Reading by ID",
        description = "One Meter Reading the token may read, with the same Atom links as in its feeds",
        responses = {
            @ApiResponse(responseCode = "200", description = "Meter Reading retrieved successfully",
                content = @Content(schema = @Schema(implementation = MeterReadingDto.class))),
            @ApiResponse(responseCode = "404", description = "No such Meter Reading the token may read"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope, or no subscription")
        }
    )
    public ResponseEntity<byte[]> getMeterReading(
            @Parameter(description = "Unique identifier of the Meter Reading", required = true)
            @PathVariable UUID meterReadingId,
            Authentication authentication) {

        Reader reader = subscriptionAccess.reader(authentication);
        String base = UsageEntries.resourceBase(ServletUriComponentsBuilder.fromCurrentRequestUri().toUriString());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        meterReadingExportService.exportDto(meterReadingExportService.buildEntry(
                usageEntries.meterReading(reader, base, reader.meterReading(meterReadingId))), out);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(out.toByteArray());
    }

    @GetMapping(value = "/RetailCustomer/{retailCustomerId}/UsagePoint/{usagePointId}/MeterReading", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get a Usage Point's Meter Readings",
        description = "The Meter Readings of one of the retail customer's Usage Points (RetailCustomer XPath collection)",
        responses = {
            @ApiResponse(responseCode = "200", description = "Meter Readings retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No such Usage Point of that retail customer the token may read"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not the token's retail customer")
        }
    )
    public ResponseEntity<byte[]> getUsagePointMeterReadings(
            @Parameter(description = "Identifier of the Retail Customer", required = true)
            @PathVariable Long retailCustomerId,
            @Parameter(description = "Unique identifier of the Usage Point", required = true)
            @PathVariable UUID usagePointId,
            Authentication authentication) {

        Reader reader = subscriptionAccess.reader(authentication);
        return feed(reader, reader.meterReadingsOf(reader.usagePoint(retailCustomerId, usagePointId, false)));
    }

    private ResponseEntity<byte[]> feed(Reader reader, List<MeterReadingEntity> meterReadings) {
        String feedHref = ServletUriComponentsBuilder.fromCurrentRequestUri().toUriString();
        String base = UsageEntries.resourceBase(feedHref);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        meterReadingExportService.exportDto(meterReadingExportService.buildFeedOf(feedHref, "MeterReading",
                "MeterReading Feed",
                meterReadings.stream().map(meterReading -> usageEntries.meterReading(reader, base, meterReading)).toList()),
                out);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(out.toByteArray());
    }
}
