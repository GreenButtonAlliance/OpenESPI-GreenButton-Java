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
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.dto.usage.UsagePointDto;
import org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository;
import org.greenbuttonalliance.espi.common.service.impl.UsageExportService;
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
 * ESPI UsagePoint resources (#286):
 * <ul>
 *   <li>{@code GET /espi/1_1/resource/UsagePoint}: the UsagePoints the token may read (the admin: all, paged by the
 *       non-ESPI {@code limit}/{@code offset});</li>
 *   <li>{@code GET /espi/1_1/resource/UsagePoint/{usagePointId}}: one of them, else 404.</li>
 * </ul>
 * Which rows, and the entries' links, are {@link SubscriptionAccess} and {@link UsageEntries}.
 */
@RestController
@RequestMapping("/espi/1_1/resource")
@Tag(name = "Usage Points", description = "ESPI Usage Point resource endpoints")
@SecurityRequirement(name = "oauth2")
// open-in-view is off: the entries read lazy collections in this session (#286)
@Transactional(readOnly = true)
public class UsagePointController {

    private final UsagePointRepository usagePointRepository;
    private final UsageExportService usageExportService;
    private final SubscriptionAccess subscriptionAccess;
    private final UsageEntries usageEntries;

    public UsagePointController(UsagePointRepository usagePointRepository, UsageExportService usageExportService,
                                SubscriptionAccess subscriptionAccess, UsageEntries usageEntries) {
        this.usagePointRepository = usagePointRepository;
        this.usageExportService = usageExportService;
        this.subscriptionAccess = subscriptionAccess;
        this.usageEntries = usageEntries;
    }

    @GetMapping(value = "/UsagePoint", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
            summary = "Get Usage Points",
            description = "The Usage Points the token may read: its subscription's, or every one for the DataCustodian admin",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Usage Points retrieved successfully",
                            content = @Content(schema = @Schema(implementation = UsagePointDto.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope, or no subscription")
            }
    )
    public ResponseEntity<byte[]> getAllUsagePoints(
            @Parameter(description = "DataCustodian admin only: page size (not ESPI)", example = "50")
            @RequestParam(defaultValue = "50") int limit,
            @Parameter(description = "DataCustodian admin only: page number (not ESPI)", example = "0")
            @RequestParam(defaultValue = "0") int offset,
            Authentication authentication) {

        Reader reader = subscriptionAccess.reader(authentication);
        List<UsagePointEntity> usagePoints = reader.admin()
                ? usagePointRepository.findAll(PageRequest.of(offset, limit)).getContent()
                : reader.usagePoints();
        String feedHref = ServletUriComponentsBuilder.fromCurrentRequestUri().toUriString();
        String base = UsageEntries.resourceBase(feedHref);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        usageExportService.exportDto(usageExportService.buildFeedOf(feedHref, "UsagePoint", "UsagePoint Feed",
                usagePoints.stream().map(usagePoint -> usageEntries.usagePoint(reader, base, usagePoint)).toList()), out);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(out.toByteArray());
    }

    @GetMapping(value = "/UsagePoint/{usagePointId}", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get Usage Point by ID",
        description = "One Usage Point the token may read, with the same Atom links as in its feeds",
        responses = {
            @ApiResponse(responseCode = "200", description = "Usage Point retrieved successfully",
                content = @Content(schema = @Schema(implementation = UsagePointDto.class))),
            @ApiResponse(responseCode = "404", description = "No such Usage Point the token may read"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope, or no subscription")
        }
    )
    public ResponseEntity<byte[]> getUsagePoint(
            @Parameter(description = "Unique identifier of the Usage Point", required = true)
            @PathVariable UUID usagePointId,
            Authentication authentication) {

        Reader reader = subscriptionAccess.reader(authentication);
        String base = UsageEntries.resourceBase(ServletUriComponentsBuilder.fromCurrentRequestUri().toUriString());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        usageExportService.exportDto(usageExportService.buildEntry(
                usageEntries.usagePoint(reader, base, reader.usagePoint(usagePointId))), out);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(out.toByteArray());
    }
}
