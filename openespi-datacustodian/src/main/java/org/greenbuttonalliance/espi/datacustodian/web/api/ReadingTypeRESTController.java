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
import org.greenbuttonalliance.espi.common.dto.usage.ReadingTypeDto;
import org.greenbuttonalliance.espi.common.mapper.usage.ReadingTypeMapper;
import org.greenbuttonalliance.espi.common.repositories.usage.ReadingTypeRepository;
import org.greenbuttonalliance.espi.common.service.impl.ReadingTypeExportService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;

/**
 * RESTful controller for managing ReadingType resources according to the
 * Green Button Alliance ESPI (Energy Services Provider Interface) specification.
 * <p>
 * ReadingType represents the type of reading being measured (e.g., energy consumed,
 * energy produced, voltage, current) and includes metadata about units of measure,
 * measurement kind, phase, and accumulation behavior.
 */
@RestController
@RequestMapping("/espi/1_1/resource")
@Tag(name = "Reading Type", description = "ESPI Reading Type resource endpoints")
@SecurityRequirement(name = "oauth2")
@RequiredArgsConstructor
public class ReadingTypeRESTController {

    private final ReadingTypeRepository readingTypeRepository;
    private final ReadingTypeMapper readingTypeMapper;
    private final ReadingTypeExportService readingTypeExportService;

    /**
     * Get all Reading Types (root collection).
     * Requires DataCustodian admin access or appropriate read scope.
     */
    @GetMapping(value = "/ReadingType", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get all Reading Types",
        description = "Retrieve all Reading Types accessible to the authenticated client",
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
    public ResponseEntity<byte[]> getReadingTypeCollection(
            @Parameter(description = "Maximum number of results to return", example = "50")
            @RequestParam(defaultValue = "50") int limit,
            @Parameter(description = "Offset for pagination", example = "0")
            @RequestParam(defaultValue = "0") int offset,
            Authentication authentication) {

        List<ReadingTypeDto> readingTypes = readingTypeRepository.findAll(PageRequest.of(offset, limit)).getContent().stream()
            .map(readingTypeMapper::toDto)
            .toList();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        readingTypeExportService.exportDto(readingTypes, out);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(out.toByteArray());
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
    public ResponseEntity<byte[]> getReadingType(
            @Parameter(description = "Unique identifier of the Reading Type", required = true)
            @PathVariable UUID readingTypeId,
            Authentication authentication) {

        ReadingTypeDto dto = readingTypeRepository.findById(readingTypeId)
            .map(readingTypeMapper::toDto)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reading Type not found for id: " + readingTypeId));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        readingTypeExportService.exportDto(dto, out);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(out.toByteArray());
    }
}
