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
import org.greenbuttonalliance.espi.common.domain.usage.ApplicationInformationEntity;
import org.greenbuttonalliance.espi.common.dto.usage.ApplicationInformationDto;
import org.greenbuttonalliance.espi.common.mapper.usage.ApplicationInformationMapper;
import org.greenbuttonalliance.espi.common.repositories.usage.ApplicationInformationRepository;
import org.greenbuttonalliance.espi.datacustodian.web.api.support.ApiRequestValidator;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Modern REST Controller for ESPI Application Information resources.
 *
 * This controller implements the NAESB ESPI REST API for Application Information,
 * using modern Spring Boot 4.0 patterns with DTOs and MapStruct mappers.
 *
 * Supported endpoints:
 * - GET /espi/1_1/resource/ApplicationInformation - List all application registrations
 * - GET /espi/1_1/resource/ApplicationInformation/{id} - Get specific application registration
 * - POST /espi/1_1/resource/ApplicationInformation - Create new application registration
 * - PUT /espi/1_1/resource/ApplicationInformation/{id} - Update application registration
 * - DELETE /espi/1_1/resource/ApplicationInformation/{id} - Delete application registration
 */
@RestController
@RequestMapping("/espi/1_1/resource")
@Tag(name = "Application Information", description = "OAuth2 Application Registration and Management API")
@SecurityRequirement(name = "oauth2")
public class ApplicationInformationController {

    private final ApplicationInformationRepository applicationInformationRepository;
    private final ApplicationInformationMapper applicationInformationMapper;
    private final ApiRequestValidator requestValidator;

    public ApplicationInformationController(ApplicationInformationRepository applicationInformationRepository,
                                            ApplicationInformationMapper applicationInformationMapper,
                                            ApiRequestValidator requestValidator) {
        this.applicationInformationRepository = applicationInformationRepository;
        this.applicationInformationMapper = applicationInformationMapper;
        this.requestValidator = requestValidator;
    }

    /**
     * Get all Application Information (root collection).
     */
    @GetMapping(value = "/ApplicationInformation", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get ApplicationInformation Collection",
        description = "Retrieves all authorized ApplicationInformation resources with optional filtering and pagination",
        responses = {
            @ApiResponse(responseCode = "200", description = "Application Information list retrieved successfully",
                content = @Content(schema = @Schema(implementation = ApplicationInformationDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access')")
    public ResponseEntity<List<ApplicationInformationDto>> getAllApplicationInformation(
            @Parameter(description = "Maximum number of results to return", example = "50")
            @RequestParam(defaultValue = "50") int limit,
            @Parameter(description = "Offset for pagination", example = "0")
            @RequestParam(defaultValue = "0") int offset) {

        Pageable pageable = requestValidator.toPageable(limit, offset);
        List<ApplicationInformationEntity> entities = applicationInformationRepository.findAll(pageable).getContent();

        List<ApplicationInformationDto> dtos = entities.stream()
            .map(applicationInformationMapper::toDto)
            .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * Get specific Application Information by ID.
     */
    @GetMapping(value = "/ApplicationInformation/{applicationInformationId}", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get Application Information by ID",
        description = "Retrieve a specific Application Information by its unique identifier",
        responses = {
            @ApiResponse(responseCode = "200", description = "Application Information retrieved successfully",
                content = @Content(schema = @Schema(implementation = ApplicationInformationDto.class))),
            @ApiResponse(responseCode = "404", description = "Application Information not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access')")
    public ResponseEntity<ApplicationInformationDto> getApplicationInformation(
            @Parameter(description = "Unique identifier of the Application Information", required = true)
            @PathVariable UUID applicationInformationId) {

        return applicationInformationRepository.findById(applicationInformationId)
            .map(applicationInformationMapper::toDto)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new Application Information resource.
     */
    @PostMapping(value = "/ApplicationInformation", consumes = {MediaType.APPLICATION_JSON_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Create Application Information",
        description = "Creates a new Application Information registration",
        responses = {
            @ApiResponse(responseCode = "201", description = "Application Information created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid Application Information data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access')")
    public ResponseEntity<ApplicationInformationDto> createApplicationInformation(
            @RequestBody ApplicationInformationDto applicationInformationDto) {

        ApplicationInformationEntity entity = applicationInformationMapper.toEntity(applicationInformationDto);
        ApplicationInformationEntity savedEntity = applicationInformationRepository.save(entity);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(applicationInformationMapper.toDto(savedEntity));
    }

    /**
     * Update an existing Application Information resource.
     */
    @PutMapping(value = "/ApplicationInformation/{applicationInformationId}", consumes = {MediaType.APPLICATION_JSON_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Update Application Information",
        description = "Updates an existing Application Information registration",
        responses = {
            @ApiResponse(responseCode = "200", description = "Application Information updated successfully"),
            @ApiResponse(responseCode = "404", description = "Application Information not found"),
            @ApiResponse(responseCode = "400", description = "Invalid Application Information data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access')")
    public ResponseEntity<ApplicationInformationDto> updateApplicationInformation(
            @PathVariable UUID applicationInformationId,
            @RequestBody ApplicationInformationDto applicationInformationDto) {

        if (!applicationInformationRepository.existsById(applicationInformationId)) {
            return ResponseEntity.notFound().build();
        }

        ApplicationInformationEntity entity = applicationInformationMapper.toEntity(applicationInformationDto);
        entity.setId(applicationInformationId);
        ApplicationInformationEntity updatedEntity = applicationInformationRepository.save(entity);
        
        return ResponseEntity.ok(applicationInformationMapper.toDto(updatedEntity));
    }

    /**
     * Delete an Application Information resource.
     */
    @DeleteMapping("/ApplicationInformation/{applicationInformationId}")
    @Operation(
        summary = "Delete Application Information",
        description = "Deletes an existing Application Information registration",
        responses = {
            @ApiResponse(responseCode = "200", description = "Application Information deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Application Information not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access')")
    public ResponseEntity<Void> deleteApplicationInformation(
            @PathVariable UUID applicationInformationId) {

        if (!applicationInformationRepository.existsById(applicationInformationId)) {
            return ResponseEntity.notFound().build();
        }

        applicationInformationRepository.deleteById(applicationInformationId);
        return ResponseEntity.ok().build();
    }
}
