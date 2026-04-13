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
import org.greenbuttonalliance.espi.common.domain.usage.ElectricPowerQualitySummaryEntity;
import org.greenbuttonalliance.espi.common.dto.usage.ElectricPowerQualitySummaryDto;
import org.greenbuttonalliance.espi.common.mapper.usage.ElectricPowerQualitySummaryMapper;
import org.greenbuttonalliance.espi.common.repositories.usage.ElectricPowerQualitySummaryRepository;
import org.greenbuttonalliance.espi.datacustodian.web.api.support.ApiRequestValidator;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Modern REST Controller for ESPI Electric Power Quality Summary resources.
 *
 * This controller implements the NAESB ESPI 1.0 REST API for Electric Power Quality Summaries,
 * using modern Spring Boot 4.0 patterns with DTOs and MapStruct mappers.
 *
 * Supported endpoints:
 * - GET /espi/1_1/resource/ElectricPowerQualitySummary - List all power quality summaries
 * - GET /espi/1_1/resource/ElectricPowerQualitySummary/{id} - Get specific power quality summary
 */
@RestController
@RequestMapping("/espi/1_1/resource")
@Tag(name = "Electric Power Quality Summary", description = "Power Quality Measurement Data Management API")
@SecurityRequirement(name = "oauth2")
public class ElectricPowerQualitySummaryController {

    private final ElectricPowerQualitySummaryRepository electricPowerQualitySummaryRepository;
    private final ElectricPowerQualitySummaryMapper electricPowerQualitySummaryMapper;
    private final ApiRequestValidator requestValidator;

    public ElectricPowerQualitySummaryController(ElectricPowerQualitySummaryRepository electricPowerQualitySummaryRepository,
                                                 ElectricPowerQualitySummaryMapper electricPowerQualitySummaryMapper,
                                                 ApiRequestValidator requestValidator) {
        this.electricPowerQualitySummaryRepository = electricPowerQualitySummaryRepository;
        this.electricPowerQualitySummaryMapper = electricPowerQualitySummaryMapper;
        this.requestValidator = requestValidator;
    }

    /**
     * Get all Electric Power Quality Summaries (root collection).
     * Requires DataCustodian admin access or appropriate read scope.
     */
    @GetMapping(value = "/ElectricPowerQualitySummary", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get ElectricPowerQualitySummary Collection",
        description = "Retrieves all authorized ElectricPowerQualitySummary resources with optional filtering and pagination",
        responses = {
            @ApiResponse(responseCode = "200", description = "Electric Power Quality Summaries retrieved successfully",
                content = @Content(schema = @Schema(implementation = ElectricPowerQualitySummaryDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or " +
                 "hasAuthority('SCOPE_FB_15_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_16_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_36_READ_3rd_party')")
    public ResponseEntity<List<ElectricPowerQualitySummaryDto>> getAllElectricPowerQualitySummaries(
            @Parameter(description = "Maximum number of results to return", example = "50")
            @RequestParam(defaultValue = "50") int limit,
            @Parameter(description = "Offset for pagination", example = "0")
            @RequestParam(defaultValue = "0") int offset) {

        Pageable pageable = requestValidator.toPageable(limit, offset);
        List<ElectricPowerQualitySummaryEntity> entities = electricPowerQualitySummaryRepository.findAll(pageable).getContent();

        List<ElectricPowerQualitySummaryDto> dtos = entities.stream()
            .map(electricPowerQualitySummaryMapper::toDto)
            .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * Get specific Electric Power Quality Summary by ID (root resource).
     */
    @GetMapping(value = "/ElectricPowerQualitySummary/{electricPowerQualitySummaryId}", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get Electric Power Quality Summary by ID",
        description = "Retrieve a specific Electric Power Quality Summary by its unique identifier",
        responses = {
            @ApiResponse(responseCode = "200", description = "Electric Power Quality Summary retrieved successfully",
                content = @Content(schema = @Schema(implementation = ElectricPowerQualitySummaryDto.class))),
            @ApiResponse(responseCode = "404", description = "Electric Power Quality Summary not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or " +
                 "hasAuthority('SCOPE_FB_15_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_16_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_36_READ_3rd_party')")
    public ResponseEntity<ElectricPowerQualitySummaryDto> getElectricPowerQualitySummary(
            @Parameter(description = "Unique identifier of the Electric Power Quality Summary", required = true)
            @PathVariable UUID electricPowerQualitySummaryId) {

        return electricPowerQualitySummaryRepository.findById(electricPowerQualitySummaryId)
            .map(electricPowerQualitySummaryMapper::toDto)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
