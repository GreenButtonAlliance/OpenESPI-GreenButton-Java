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
import org.greenbuttonalliance.espi.common.domain.usage.ReadingTypeEntity;
import org.greenbuttonalliance.espi.common.dto.usage.ReadingTypeDto;
import org.greenbuttonalliance.espi.common.mapper.usage.ReadingTypeMapper;
import org.greenbuttonalliance.espi.common.service.ReadingTypeService;
import org.greenbuttonalliance.espi.datacustodian.web.api.support.ApiRequestValidator;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Modern REST Controller for ESPI Reading Type resources.
 *
 * This controller implements the NAESB ESPI 1.0 REST API for Reading Types,
 * using modern Spring Boot 3.5 patterns with DTOs and MapStruct mappers.
 *
 * Supported endpoints:
 * - GET /espi/1_1/resource/ReadingType - List all reading types
 * - GET /espi/1_1/resource/ReadingType/{readingTypeId} - Get specific reading type
 */
@RestController
@RequestMapping("/espi/1_1/resource")
@Tag(name = "Reading Type", description = "Smart Meter Reading Type Metadata Management API")
@SecurityRequirement(name = "oauth2")
public class ReadingTypeController {

    private final ReadingTypeService readingTypeService;
    private final ReadingTypeMapper readingTypeMapper;
    private final ApiRequestValidator requestValidator;

    public ReadingTypeController(ReadingTypeService readingTypeService,
                                 ReadingTypeMapper readingTypeMapper,
                                 ApiRequestValidator requestValidator) {
        this.readingTypeService = readingTypeService;
        this.readingTypeMapper = readingTypeMapper;
        this.requestValidator = requestValidator;
    }

    /**
     * Get all Reading Types (root collection).
     * Requires DataCustodian admin access or appropriate read scope.
     */
    @GetMapping(value = "/ReadingType", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get ReadingType Collection",
        description = "Retrieves all ReadingType resources accessible to the authenticated client",
        responses = {
            @ApiResponse(responseCode = "200", description = "Reading Types retrieved successfully",
                content = @Content(schema = @Schema(implementation = ReadingTypeDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or " +
                 "hasAuthority('SCOPE_FB_15_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_16_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_36_READ_3rd_party')")
    public ResponseEntity<List<ReadingTypeDto>> getAllReadingTypes(
            @Parameter(description = "Maximum number of results to return", example = "50")
            @RequestParam(defaultValue = "50") int limit,
            @Parameter(description = "Offset for pagination", example = "0")
            @RequestParam(defaultValue = "0") int offset) {

        List<ReadingTypeEntity> readingTypeEntities = readingTypeService.findAll();
        // Manual pagination if needed, but for now we follow the reference patterns
        // Some reference controllers use repositories directly, some use services.
        // ReadingTypeService.findAll() returns a list.
        
        List<ReadingTypeDto> readingTypes = requestValidator.paginate(
            readingTypeEntities.stream().map(readingTypeMapper::toDto).toList(),
            limit,
            offset
        );
            
        return ResponseEntity.ok(readingTypes);
    }

    /**
     * Get specific Reading Type by ID (root resource).
     */
    @GetMapping(value = "/ReadingType/{readingTypeId}", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get Reading Type by ID",
        description = "Retrieve a specific Reading Type by its unique identifier",
        responses = {
            @ApiResponse(responseCode = "200", description = "Reading Type retrieved successfully",
                content = @Content(schema = @Schema(implementation = ReadingTypeDto.class))),
            @ApiResponse(responseCode = "404", description = "Reading Type not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or " +
                 "hasAuthority('SCOPE_FB_15_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_16_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_36_READ_3rd_party')")
    public ResponseEntity<ReadingTypeDto> getReadingType(
            @Parameter(description = "Unique identifier of the Reading Type", required = true)
            @PathVariable UUID readingTypeId) {

        ReadingTypeEntity readingTypeEntity = readingTypeService.findById(readingTypeId);
        if (readingTypeEntity == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(readingTypeMapper.toDto(readingTypeEntity));
    }
}
