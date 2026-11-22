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
import org.greenbuttonalliance.espi.common.domain.usage.ReadingTypeEntity;
import org.greenbuttonalliance.espi.common.dto.usage.ReadingTypeDto;
import org.greenbuttonalliance.espi.common.repositories.usage.ReadingTypeRepository;
import org.greenbuttonalliance.espi.common.service.impl.ReadingTypeExportService;
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
 * ESPI ReadingType resources (#286):
 * <ul>
 *   <li>{@code GET /espi/1_1/resource/ReadingType}: the ReadingTypes of the MeterReadings the token may read (the
 *       admin: all, paged by the non-ESPI {@code limit}/{@code offset});</li>
 *   <li>{@code GET /espi/1_1/resource/ReadingType/{readingTypeId}}: one of them, else 404.</li>
 * </ul>
 * Which rows, and the entries' links (the MeterReadings that use each), are {@link SubscriptionAccess} and
 * {@link UsageEntries}.
 */
@RestController
@RequestMapping("/espi/1_1/resource")
@Tag(name = "Reading Type", description = "ESPI Reading Type resource endpoints")
@SecurityRequirement(name = "oauth2")
@RequiredArgsConstructor
// open-in-view is off: the entries read lazy collections in this session (#286)
@Transactional(readOnly = true)
public class ReadingTypeRESTController {

    private final ReadingTypeRepository readingTypeRepository;
    private final ReadingTypeExportService readingTypeExportService;
    private final SubscriptionAccess subscriptionAccess;
    private final UsageEntries usageEntries;

    @GetMapping(value = "/ReadingType", produces = MediaType.APPLICATION_ATOM_XML_VALUE)
    @Operation(
        summary = "Get Reading Types",
        description = "The Reading Types of the Meter Readings the token may read, or every one for the DataCustodian admin",
        responses = {
            @ApiResponse(responseCode = "200", description = "Reading Types retrieved successfully",
                content = @Content(schema = @Schema(implementation = ReadingTypeDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope, or no subscription")
        }
    )
    public ResponseEntity<byte[]> getAllReadingTypes(
            @Parameter(description = "DataCustodian admin only: page size (not ESPI)", example = "50")
            @RequestParam(defaultValue = "50") int limit,
            @Parameter(description = "DataCustodian admin only: page number (not ESPI)", example = "0")
            @RequestParam(defaultValue = "0") int offset,
            Authentication authentication) {

        Reader reader = subscriptionAccess.reader(authentication);
        List<ReadingTypeEntity> readingTypes = reader.admin()
                ? readingTypeRepository.findAll(PageRequest.of(offset, limit)).getContent()
                : reader.readingTypes();
        String feedHref = ServletUriComponentsBuilder.fromCurrentRequestUri().toUriString();
        String base = UsageEntries.resourceBase(feedHref);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        readingTypeExportService.exportDto(readingTypeExportService.buildFeedOf(feedHref, "ReadingType",
                "ReadingType Feed",
                readingTypes.stream().map(readingType -> usageEntries.readingType(reader, base, readingType)).toList()),
                out);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_ATOM_XML).body(out.toByteArray());
    }

    @GetMapping(value = "/ReadingType/{readingTypeId}", produces = MediaType.APPLICATION_ATOM_XML_VALUE)
    @Operation(
        summary = "Get Reading Type by ID",
        description = "One Reading Type of a Meter Reading the token may read, with the same Atom links as in its feed",
        responses = {
            @ApiResponse(responseCode = "200", description = "Reading Type retrieved successfully",
                content = @Content(schema = @Schema(implementation = ReadingTypeDto.class))),
            @ApiResponse(responseCode = "404", description = "No such Reading Type the token may read"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope, or no subscription")
        }
    )
    public ResponseEntity<byte[]> getReadingType(
            @Parameter(description = "Unique identifier of the Reading Type", required = true)
            @PathVariable UUID readingTypeId,
            Authentication authentication) {

        Reader reader = subscriptionAccess.reader(authentication);
        String base = UsageEntries.resourceBase(ServletUriComponentsBuilder.fromCurrentRequestUri().toUriString());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        readingTypeExportService.exportDto(readingTypeExportService.buildEntry(
                usageEntries.readingType(reader, base, reader.readingType(readingTypeId))), out);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_ATOM_XML).body(out.toByteArray());
    }
}
