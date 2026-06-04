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
import org.greenbuttonalliance.espi.common.dto.usage.MeterReadingDto;
import org.greenbuttonalliance.espi.common.mapper.usage.MeterReadingMapper;
import org.greenbuttonalliance.espi.common.repositories.usage.MeterReadingRepository;
import org.greenbuttonalliance.espi.common.service.impl.MeterReadingExportService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;

/**
 * Modern REST Controller for ESPI Meter Reading resources.
 * <p>
 * This controller implements the NAESB ESPI 1.0 REST API for Meter Readings,
 * using modern Spring Boot 3.5 patterns with DTOs and MapStruct mappers.
 * <p>
 * Supported endpoints:
 * - GET /espi/1_1/resource/MeterReading - List all meter readings
 * - GET /espi/1_1/resource/MeterReading/{meterReadingId} - Get specific meter reading
 */
@RestController
@RequestMapping("/espi/1_1/resource")
@Tag(name = "Meter Readings", description = "ESPI Meter Reading resource endpoints")
@SecurityRequirement(name = "oauth2")
@RequiredArgsConstructor
public class MeterReadingController {
    
    private final MeterReadingRepository meterReadingRepository;
    private final MeterReadingMapper meterReadingMapper;
    private final MeterReadingExportService meterReadingExportService;

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
    public ResponseEntity<byte[]> getAllMeterReadings(
            @Parameter(description = "Maximum number of results to return", example = "50")
            @RequestParam(defaultValue = "50") int limit,
            @Parameter(description = "Offset for pagination", example = "0")
            @RequestParam(defaultValue = "0") int offset,
            Authentication authentication) {

        List<MeterReadingDto> meterReadings = meterReadingRepository.findAll(PageRequest.of(offset, limit)).getContent().stream()
            .map(meterReadingMapper::toDto)
            .toList();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        meterReadingExportService.exportDto(meterReadings, out);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(out.toByteArray());
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
    public ResponseEntity<byte[]> getMeterReading(
            @Parameter(description = "Unique identifier of the Meter Reading", required = true)
            @PathVariable UUID meterReadingId,
            Authentication authentication) {

        MeterReadingDto dto = meterReadingRepository.findById(meterReadingId)
            .map(meterReadingMapper::toDto)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meter Reading not found for id: " + meterReadingId));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        meterReadingExportService.exportDto(dto, out);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(out.toByteArray());
    }
}
