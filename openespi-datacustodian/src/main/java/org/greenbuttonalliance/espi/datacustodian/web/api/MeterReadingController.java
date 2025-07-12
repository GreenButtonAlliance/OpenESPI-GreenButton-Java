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
import org.greenbuttonalliance.espi.common.dto.usage.MeterReadingDto;
import org.greenbuttonalliance.espi.common.repositories.usage.MeterReadingRepository;
import org.greenbuttonalliance.espi.common.mapper.usage.MeterReadingMapper;
import org.greenbuttonalliance.espi.common.domain.usage.MeterReadingEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Modern REST Controller for ESPI Meter Reading resources.
 * 
 * This controller implements the NAESB ESPI 1.0 REST API for Meter Readings,
 * using modern Spring Boot 3.5 patterns with DTOs and MapStruct mappers.
 * 
 * Supported endpoints:
 * - GET /espi/1_1/resource/MeterReading - List all meter readings
 * - GET /espi/1_1/resource/MeterReading/{meterReadingId} - Get specific meter reading
 */
@RestController
@RequestMapping("/espi/1_1/resource")
@Tag(name = "Meter Readings", description = "ESPI Meter Reading resource endpoints")
@SecurityRequirement(name = "oauth2")
public class MeterReadingController {

    private final MeterReadingRepository meterReadingRepository;
    private final MeterReadingMapper meterReadingMapper;

    public MeterReadingController(MeterReadingRepository meterReadingRepository, MeterReadingMapper meterReadingMapper) {
        this.meterReadingRepository = meterReadingRepository;
        this.meterReadingMapper = meterReadingMapper;
    }

    /**
     * Get all Meter Readings (root collection).
     * Requires DataCustodian admin access or appropriate read scope.
     */
    @GetMapping(value = "/MeterReading", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get all Meter Readings",
        description = "Retrieve all Meter Readings accessible to the authenticated client",
        responses = {
            @ApiResponse(responseCode = "200", description = "Meter Readings retrieved successfully",
                content = @Content(schema = @Schema(implementation = MeterReadingDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or " +
                 "hasAuthority('SCOPE_FB_15_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_16_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_36_READ_3rd_party')")
    public ResponseEntity<List<MeterReadingDto>> getAllMeterReadings(
            @Parameter(description = "Maximum number of results to return", example = "50")
            @RequestParam(defaultValue = "50") int limit,
            @Parameter(description = "Offset for pagination", example = "0")
            @RequestParam(defaultValue = "0") int offset,
            Authentication authentication) {
        
        Pageable pageable = PageRequest.of(offset / limit, limit);
        List<MeterReadingEntity> meterReadingEntities = meterReadingRepository.findAll(pageable).getContent();
        List<MeterReadingDto> meterReadings = meterReadingEntities.stream()
            .map(meterReadingMapper::toDto)
            .toList();
        return ResponseEntity.ok(meterReadings);
    }

    /**
     * Get specific Meter Reading by ID (root resource).
     */
    @GetMapping(value = "/MeterReading/{meterReadingId}", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get Meter Reading by ID",
        description = "Retrieve a specific Meter Reading by its unique identifier",
        responses = {
            @ApiResponse(responseCode = "200", description = "Meter Reading retrieved successfully",
                content = @Content(schema = @Schema(implementation = MeterReadingDto.class))),
            @ApiResponse(responseCode = "404", description = "Meter Reading not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or " +
                 "hasAuthority('SCOPE_FB_15_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_16_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_36_READ_3rd_party')")
    public ResponseEntity<MeterReadingDto> getMeterReading(
            @Parameter(description = "Unique identifier of the Meter Reading", required = true)
            @PathVariable UUID meterReadingId,
            Authentication authentication) {
        
        return meterReadingRepository.findById(meterReadingId)
            .map(meterReadingMapper::toDto)
            .map(meterReading -> ResponseEntity.ok(meterReading))
            .orElse(ResponseEntity.notFound().build());
    }
}