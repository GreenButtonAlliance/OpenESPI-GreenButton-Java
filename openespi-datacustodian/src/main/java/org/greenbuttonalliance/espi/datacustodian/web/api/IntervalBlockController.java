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
import org.greenbuttonalliance.espi.common.domain.usage.IntervalBlockEntity;
import org.greenbuttonalliance.espi.common.dto.usage.IntervalBlockDto;
import org.greenbuttonalliance.espi.common.mapper.usage.IntervalBlockMapper;
import org.greenbuttonalliance.espi.common.repositories.usage.IntervalBlockRepository;
import org.greenbuttonalliance.espi.datacustodian.web.api.support.ApiRequestValidator;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Modern REST Controller for ESPI Interval Block resources.
 * 
 * This controller implements the NAESB ESPI 1.0 REST API for Interval Blocks,
 * using modern Spring Boot 4.0 patterns with DTOs and MapStruct mappers.
 * 
 * Supported endpoints:
 * - GET /espi/1_1/resource/IntervalBlock - List all interval blocks
 * - GET /espi/1_1/resource/IntervalBlock/{intervalBlockId} - Get specific interval block
 */
@RestController
@RequestMapping("/espi/1_1/resource")
@Tag(name = "Interval Blocks", description = "ESPI Interval Block resource endpoints")
@SecurityRequirement(name = "oauth2")
public class IntervalBlockController {

    private final IntervalBlockRepository intervalBlockRepository;
    private final IntervalBlockMapper intervalBlockMapper;
    private final ApiRequestValidator requestValidator;

    public IntervalBlockController(IntervalBlockRepository intervalBlockRepository,
                                   IntervalBlockMapper intervalBlockMapper,
                                   ApiRequestValidator requestValidator) {
        this.intervalBlockRepository = intervalBlockRepository;
        this.intervalBlockMapper = intervalBlockMapper;
        this.requestValidator = requestValidator;
    }

    /**
     * Get all Interval Blocks (root collection).
     * Requires DataCustodian admin access or appropriate read scope.
     */
    @GetMapping(value = "/IntervalBlock", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get all Interval Blocks",
        description = "Retrieve all Interval Blocks accessible to the authenticated client",
        responses = {
            @ApiResponse(responseCode = "200", description = "Interval Blocks retrieved successfully",
                content = @Content(schema = @Schema(implementation = IntervalBlockDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or " +
                 "hasAuthority('SCOPE_FB_15_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_16_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_36_READ_3rd_party')")
    public ResponseEntity<List<IntervalBlockDto>> getAllIntervalBlocks(
            @Parameter(description = "Maximum number of results to return", example = "50")
            @RequestParam(defaultValue = "50") int limit,
            @Parameter(description = "Offset for pagination", example = "0")
            @RequestParam(defaultValue = "0") int offset) {
        
        Pageable pageable = requestValidator.toPageable(limit, offset);
        List<IntervalBlockEntity> intervalBlockEntities = intervalBlockRepository.findAll(pageable).getContent();
        List<IntervalBlockDto> intervalBlocks = intervalBlockEntities.stream()
            .map(intervalBlockMapper::toDto)
            .toList();
        return ResponseEntity.ok(intervalBlocks);
    }

    /**
     * Get specific Interval Block by ID (root resource).
     */
    @GetMapping(value = "/IntervalBlock/{intervalBlockId}", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get Interval Block by ID",
        description = "Retrieve a specific Interval Block by its unique identifier",
        responses = {
            @ApiResponse(responseCode = "200", description = "Interval Block retrieved successfully",
                content = @Content(schema = @Schema(implementation = IntervalBlockDto.class))),
            @ApiResponse(responseCode = "404", description = "Interval Block not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or " +
                 "hasAuthority('SCOPE_FB_15_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_16_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_36_READ_3rd_party')")
    public ResponseEntity<IntervalBlockDto> getIntervalBlock(
            @Parameter(description = "Unique identifier of the Interval Block", required = true)
            @PathVariable UUID intervalBlockId) {
        
        return intervalBlockRepository.findById(intervalBlockId)
            .map(intervalBlockMapper::toDto)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
