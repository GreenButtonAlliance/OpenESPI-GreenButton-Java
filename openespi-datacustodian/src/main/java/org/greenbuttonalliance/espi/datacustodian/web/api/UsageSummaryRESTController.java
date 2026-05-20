/*
 *
 *        Copyright (c) 2025 Green Button Alliance, Inc.
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
import org.greenbuttonalliance.espi.common.dto.usage.UsageSummaryDto;
import org.greenbuttonalliance.espi.common.mapper.usage.UsageSummaryMapper;
import org.greenbuttonalliance.espi.common.repositories.usage.UsageSummaryRepository;
import org.greenbuttonalliance.espi.common.service.impl.UsageSummaryExportService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.UUID;

/**
 * Modern REST Controller for ESPI Usage Summary resources.
 * <p>
 * This controller implements the NAESB ESPI 1.0 REST API for Usage Summaries,
 * replacing the legacy Spring MVC controller with modern Spring Boot 3.5 patterns.
 * <p>
 * UsageSummary represents aggregated utility usage data over specific time periods,
 * including totals for consumption, production, and billing determinant values.
 */
@RestController
@RequestMapping("/espi/1_1/resource")
@Tag(name = "Usage Summary", description = "Multi-Commodity Usage Summary Data Management API")
@SecurityRequirement(name = "oauth2")
@RequiredArgsConstructor
public class UsageSummaryRESTController {

    private final UsageSummaryRepository usageSummaryRepository;
    private final UsageSummaryMapper usageSummaryMapper;
    private final UsageSummaryExportService usageSummaryExportService;

    /**
     * Get all Usage Summaries (root collection).
     * Requires DataCustodian admin access or appropriate read scope.
     */
    @GetMapping(value = "/UsageSummary", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get all Usage Summaries",
        description = "Retrieve all Usage Summaries accessible to the authenticated client",
        responses = {
            @ApiResponse(responseCode = "200", description = "Usage Summaries retrieved successfully",
                content = @Content(schema = @Schema(implementation = UsageSummaryDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or " +
                 "hasAuthority('SCOPE_FB_15_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_16_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_36_READ_3rd_party')")
    public ResponseEntity<StreamingResponseBody> getUsageSummaryCollection(
            @Parameter(description = "Maximum number of results to return", example = "50")
            @RequestParam(defaultValue = "50") int limit,
            @Parameter(description = "Offset for pagination", example = "0")
            @RequestParam(defaultValue = "0") int offset,
            Authentication authentication) {

        List<UsageSummaryDto> usageSummaries = usageSummaryRepository.findAll(PageRequest.of(offset, limit)).getContent().stream()
            .map(usageSummaryMapper::toDto)
            .toList();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(out -> {
            usageSummaryExportService.exportDto(usageSummaries, out);
        });
    }

    /**
     * Get specific Usage Summary by ID (root resource).
     */
    @GetMapping(value = "/UsageSummary/{usageSummaryId}", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get Usage Summary by ID",
        description = "Retrieve a specific Usage Summary by its unique identifier",
        responses = {
            @ApiResponse(responseCode = "200", description = "Usage Summary retrieved successfully",
                content = @Content(schema = @Schema(implementation = UsageSummaryDto.class))),
            @ApiResponse(responseCode = "404", description = "Usage Summary not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or " +
                 "hasAuthority('SCOPE_FB_15_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_16_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_36_READ_3rd_party')")
    public ResponseEntity<StreamingResponseBody> getUsageSummary(
            @Parameter(description = "Unique identifier of the Usage Summary", required = true)
            @PathVariable UUID usageSummaryId,
            Authentication authentication) {

        UsageSummaryDto dto = usageSummaryRepository.findById(usageSummaryId)
            .map(usageSummaryMapper::toDto)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usage Summary not found for id: " + usageSummaryId));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(out -> {
            usageSummaryExportService.exportDto(dto, out);
        });
    }

    /**
     * Get Usage Summaries for a specific Subscription and Usage Point.
     */
    @GetMapping(value = "/Subscription/{subscriptionId}/UsagePoint/{usagePointId}/UsageSummary", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get Usage Summaries by Subscription context",
        description = "Retrieve all Usage Summaries associated with a specific subscription and usage point",
        responses = {
            @ApiResponse(responseCode = "200", description = "Usage Summaries retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Subscription or Usage Point not found")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_FB_15_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_16_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_36_READ_3rd_party')")
    public ResponseEntity<StreamingResponseBody> getSubscriptionUsageSummaries(
            @Parameter(description = "Unique identifier of the Subscription", required = true)
            @PathVariable UUID subscriptionId,
            @Parameter(description = "Unique identifier of the Usage Point", required = true)
            @PathVariable UUID usagePointId,
            @Parameter(description = "Maximum number of results to return", example = "50")
            @RequestParam(defaultValue = "50") int limit,
            @Parameter(description = "Offset for pagination", example = "0")
            @RequestParam(defaultValue = "0") int offset,
            Authentication authentication) {
        
        // TODO: Implement subscription and usage point based filtering when relationships are available
        List<UsageSummaryDto> usageSummaries = usageSummaryRepository.findAll(PageRequest.of(offset, limit)).getContent().stream()
            .map(usageSummaryMapper::toDto)
            .toList();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(out -> {
            usageSummaryExportService.exportDto(usageSummaries, out);
        });
    }

    /**
     * Get specific Usage Summary for a Subscription and Usage Point.
     */
    @GetMapping(value = "/Subscription/{subscriptionId}/UsagePoint/{usagePointId}/UsageSummary/{usageSummaryId}", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get specific Usage Summary by Subscription context",
        description = "Retrieve a specific Usage Summary associated with a subscription and usage point",
        responses = {
            @ApiResponse(responseCode = "200", description = "Usage Summary retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Usage Summary, Subscription, or Usage Point not found")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_FB_15_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_16_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_36_READ_3rd_party')")
    public ResponseEntity<StreamingResponseBody> getSubscriptionUsageSummary(
            @Parameter(description = "Unique identifier of the Subscription", required = true)
            @PathVariable UUID subscriptionId,
            @Parameter(description = "Unique identifier of the Usage Point", required = true)
            @PathVariable UUID usagePointId,
            @Parameter(description = "Unique identifier of the Usage Summary", required = true)
            @PathVariable UUID usageSummaryId,
            Authentication authentication) {

        UsageSummaryDto dto = usageSummaryRepository.findById(usageSummaryId)
            .map(usageSummaryMapper::toDto)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usage Summary not found for id: " + usageSummaryId));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(out -> {
            usageSummaryExportService.exportDto(dto, out);
        });
    }
}
