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
import org.greenbuttonalliance.espi.common.dto.usage.ElectricPowerQualitySummaryDto;
import org.greenbuttonalliance.espi.common.mapper.usage.ElectricPowerQualitySummaryMapper;
import org.greenbuttonalliance.espi.common.repositories.usage.ElectricPowerQualitySummaryRepository;
import org.greenbuttonalliance.espi.common.service.impl.ElectricPowerQualitySummaryExportService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.UUID;

/**
 * Modern REST Controller for ESPI ElectricPowerQualitySummary resources.
 * <p>
 * This controller implements the NAESB ESPI 1.0 REST API for Electric Power Quality Summaries,
 * using modern Spring Boot 3.5 patterns with DTOs and MapStruct mappers.
 * <p>
 * Supported endpoints:
 * - GET /espi/1_1/resource/ElectricPowerQualitySummary - List all power quality summaries
 * - GET /espi/1_1/resource/ElectricPowerQualitySummary/{electricPowerQualitySummaryId} - Get specific summary
 * - GET /espi/1_1/resource/Subscription/{subscriptionId}/UsagePoint/{usagePointId}/ElectricPowerQualitySummary - List subscription summaries
 * - GET /espi/1_1/resource/Subscription/{subscriptionId}/UsagePoint/{usagePointId}/ElectricPowerQualitySummary/{electricPowerQualitySummaryId} - Get subscription summary
 */
@RestController
@RequestMapping("/espi/1_1/resource")
@Tag(name = "Electric Power Quality Summary", description = "Power Quality Measurement Data Management API")
@SecurityRequirement(name = "oauth2")
@RequiredArgsConstructor
public class ElectricPowerQualitySummaryRESTController {

    private final ElectricPowerQualitySummaryRepository electricPowerQualitySummaryRepository;
    private final ElectricPowerQualitySummaryMapper electricPowerQualitySummaryMapper;
    private final ElectricPowerQualitySummaryExportService electricPowerQualitySummaryExportService;

    /**
     * Get all Electric Power Quality Summaries (root collection).
     * Requires DataCustodian admin access or appropriate read scope.
     */
    @GetMapping(value = "/ElectricPowerQualitySummary", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get ElectricPowerQualitySummary Collection",
        description = "Retrieves all authorized ElectricPowerQualitySummary resources.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Summaries retrieved successfully",
                content = @Content(schema = @Schema(implementation = ElectricPowerQualitySummaryDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or " +
                 "hasAuthority('SCOPE_FB_15_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_16_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_36_READ_3rd_party')")
    public ResponseEntity<StreamingResponseBody> getElectricPowerQualitySummaryCollection(
            @Parameter(description = "Maximum number of results to return", example = "50")
            @RequestParam(defaultValue = "50") int limit,
            @Parameter(description = "Offset for pagination", example = "0")
            @RequestParam(defaultValue = "0") int offset) {

        List<ElectricPowerQualitySummaryDto> summaries = electricPowerQualitySummaryRepository.findAll(PageRequest.of(offset, limit)).getContent().stream()
                .map(electricPowerQualitySummaryMapper::toDto)
                .toList();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(out -> electricPowerQualitySummaryExportService.exportDto(summaries, out));
    }

    /**
     * Get specific Electric Power Quality Summary by ID (root resource).
     * Requires DataCustodian admin access or appropriate read scope.
     */
    @GetMapping(value = "/ElectricPowerQualitySummary/{electricPowerQualitySummaryId}", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get ElectricPowerQualitySummary by ID",
        description = "Retrieves a specific ElectricPowerQualitySummary resource by its unique identifier.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Summary retrieved successfully",
                content = @Content(schema = @Schema(implementation = ElectricPowerQualitySummaryDto.class))),
            @ApiResponse(responseCode = "404", description = "Summary not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or " +
                 "hasAuthority('SCOPE_FB_15_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_16_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_36_READ_3rd_party')")
    public ResponseEntity<StreamingResponseBody> getElectricPowerQualitySummary(
            @Parameter(description = "Unique identifier of the ElectricPowerQualitySummary", required = true)
            @PathVariable UUID electricPowerQualitySummaryId) {

        ElectricPowerQualitySummaryDto dto = electricPowerQualitySummaryRepository.findById(electricPowerQualitySummaryId)
                .map(electricPowerQualitySummaryMapper::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ElectricPowerQualitySummary not found for id: " + electricPowerQualitySummaryId));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(out -> electricPowerQualitySummaryExportService.exportDto(dto, out));
    }

    /**
     * Get Electric Power Quality Summaries for a specific Subscription and Usage Point.
     * Requires appropriate read scope.
     */
    @GetMapping(value = "/Subscription/{subscriptionId}/UsagePoint/{usagePointId}/ElectricPowerQualitySummary", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get ElectricPowerQualitySummaries by Subscription Context",
        description = "Retrieves all ElectricPowerQualitySummary resources associated with a specific subscription and usage point.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Summaries retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Subscription or Usage Point not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_FB_15_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_16_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_36_READ_3rd_party')")
    public ResponseEntity<StreamingResponseBody> getSubscriptionElectricPowerQualitySummaries(
            @Parameter(description = "Unique identifier of the subscription", required = true)
            @PathVariable UUID subscriptionId,
            @Parameter(description = "Unique identifier of the usage point", required = true)
            @PathVariable UUID usagePointId,
            @Parameter(description = "Maximum number of results to return", example = "50")
            @RequestParam(defaultValue = "50") int limit,
            @Parameter(description = "Offset for pagination", example = "0")
            @RequestParam(defaultValue = "0") int offset) {

        // TODO: Implement subscription-based filtering when subscription relationship is available
        // For now, return all summaries with pagination as a temporary solution
        List<ElectricPowerQualitySummaryDto> summaries = electricPowerQualitySummaryRepository.findAll(PageRequest.of(offset, limit)).getContent().stream()
                .map(electricPowerQualitySummaryMapper::toDto)
                .toList();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(out -> electricPowerQualitySummaryExportService.exportDto(summaries, out));
    }

    /**
     * Get specific Electric Power Quality Summary for a Subscription and Usage Point.
     * Requires appropriate read scope.
     */
    @GetMapping(value = "/Subscription/{subscriptionId}/UsagePoint/{usagePointId}/ElectricPowerQualitySummary/{electricPowerQualitySummaryId}", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get Subscription ElectricPowerQualitySummary by ID",
        description = "Retrieves a specific ElectricPowerQualitySummary resource within a subscription context.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Summary retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Summary, Subscription or Usage Point not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_FB_15_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_16_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_36_READ_3rd_party')")
    public ResponseEntity<StreamingResponseBody> getSubscriptionElectricPowerQualitySummary(
            @Parameter(description = "Unique identifier of the subscription", required = true)
            @PathVariable UUID subscriptionId,
            @Parameter(description = "Unique identifier of the usage point", required = true)
            @PathVariable UUID usagePointId,
            @Parameter(description = "Unique identifier of the ElectricPowerQualitySummary", required = true)
            @PathVariable UUID electricPowerQualitySummaryId) {

        // TODO: Implement subscription-based validation when subscription relationship is available
        // For now, return the summary if it exists
        ElectricPowerQualitySummaryDto dto = electricPowerQualitySummaryRepository.findById(electricPowerQualitySummaryId)
                .map(electricPowerQualitySummaryMapper::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ElectricPowerQualitySummary not found for id: " + electricPowerQualitySummaryId));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(out -> electricPowerQualitySummaryExportService.exportDto(dto, out));
    }
}
