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
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.greenbuttonalliance.espi.common.domain.usage.ApplicationInformationEntity;
import org.greenbuttonalliance.espi.common.dto.usage.ApplicationInformationDto;
import org.greenbuttonalliance.espi.common.service.ApplicationInformationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Modern RESTful controller for managing ApplicationInformation resources according to the
 * Green Button Alliance ESPI (Energy Services Provider Interface) specification.
 * <p>
 * This controller uses ApplicationInformationService and modern Spring Boot 3.5 patterns.
 */
@RestController
@RequestMapping("/espi/1_1/resource/ApplicationInformation")
@Tag(name = "Application Information", description = "OAuth2 Application Registration and Management API")
@SecurityRequirement(name = "oauth2")
@RequiredArgsConstructor
public class ApplicationInformationRESTController {

    private final ApplicationInformationService applicationInformationService;

    /**
     * Gets all ApplicationInformation resources.
     *
     * @return XML response body
     */
    @GetMapping(produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get All ApplicationInformation",
        description = "Returns a list of all registered applications in XML format"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved ApplicationInformation list",
            content = @Content(schema = @Schema(implementation = ApplicationInformationDto.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or hasAuthority('SCOPE_ThirdParty_Admin_Access')")
    public ResponseEntity<byte[]> getAllApplicationInformation() {
        List<ApplicationInformationEntity> entities = applicationInformationService.findAll();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        applicationInformationService.export(entities, out);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(out.toByteArray());
    }

    /**
     * Gets a specific ApplicationInformation resource by ID.
     *
     * @param applicationInformationId Unique identifier for the ApplicationInformation
     * @return XML response body
     */
    @GetMapping(value = "/{applicationInformationId}", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get ApplicationInformation by ID",
        description = "Returns a specific ApplicationInformation resource in XML format"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved ApplicationInformation",
            content = @Content(schema = @Schema(implementation = ApplicationInformationDto.class))),
        @ApiResponse(responseCode = "404", description = "ApplicationInformation not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or hasAuthority('SCOPE_ThirdParty_Admin_Access')")
    public ResponseEntity<byte[]> getApplicationInformation(
            @Parameter(description = "Unique identifier of the ApplicationInformation", required = true)
            @PathVariable UUID applicationInformationId) {

        ApplicationInformationEntity entity = applicationInformationService.findById(applicationInformationId);
        if (entity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ApplicationInformation not found");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        applicationInformationService.export(entity, out);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(out.toByteArray());
    }

    /**
     * Creates a new ApplicationInformation resource.
     *
     * @param dto ApplicationInformationDto to create
     * @return created ApplicationInformationDto
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Create ApplicationInformation",
        description = "Creates a new ApplicationInformation resource"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successfully created ApplicationInformation"),
        @ApiResponse(responseCode = "400", description = "Invalid ApplicationInformation data")
    })
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access')")
    public ResponseEntity<ApplicationInformationEntity> createApplicationInformation(
            @Parameter(description = "ApplicationInformation data to create", required = true)
            @RequestBody ApplicationInformationDto dto) {

        ApplicationInformationEntity entity = applicationInformationService.fromDto(dto);
        ApplicationInformationEntity savedEntity = applicationInformationService.save(entity);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedEntity.getId())
                .toUri();

        return ResponseEntity.created(location).body(savedEntity);
    }

    /**
     * Updates an existing ApplicationInformation resource.
     *
     * @param applicationInformationId Unique identifier for the ApplicationInformation to update
     * @param dto Updated ApplicationInformationDto data
     * @return updated ApplicationInformationEntity
     */
    @PutMapping(value = "/{applicationInformationId}",
               consumes = MediaType.APPLICATION_JSON_VALUE,
               produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Update ApplicationInformation",
        description = "Updates an existing ApplicationInformation resource"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updated ApplicationInformation"),
        @ApiResponse(responseCode = "404", description = "ApplicationInformation not found"),
        @ApiResponse(responseCode = "400", description = "Invalid ApplicationInformation data")
    })
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access')")
    public ResponseEntity<ApplicationInformationEntity> updateApplicationInformation(
            @Parameter(description = "Unique identifier of the ApplicationInformation to update", required = true)
            @PathVariable UUID applicationInformationId,
            @Parameter(description = "Updated ApplicationInformation data", required = true)
            @RequestBody ApplicationInformationDto dto) {

        ApplicationInformationEntity existing = applicationInformationService.findById(applicationInformationId);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ApplicationInformation not found");
        }

        ApplicationInformationEntity entity = applicationInformationService.fromDto(dto);
        entity.setId(applicationInformationId);
        ApplicationInformationEntity updatedEntity = applicationInformationService.save(entity);

        return ResponseEntity.ok(updatedEntity);
    }

    /**
     * Deletes an ApplicationInformation resource.
     *
     * @param applicationInformationId Unique identifier for the ApplicationInformation to delete
     */
    @DeleteMapping("/{applicationInformationId}")
    @Operation(
        summary = "Delete ApplicationInformation",
        description = "Deletes an ApplicationInformation resource"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully deleted ApplicationInformation"),
        @ApiResponse(responseCode = "404", description = "ApplicationInformation not found")
    })
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access')")
    public ResponseEntity<Void> deleteApplicationInformation(
            @Parameter(description = "Unique identifier of the ApplicationInformation to delete", required = true)
            @PathVariable UUID applicationInformationId) {

        ApplicationInformationEntity existing = applicationInformationService.findById(applicationInformationId);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ApplicationInformation not found");
        }

        applicationInformationService.deleteById(applicationInformationId);
        return ResponseEntity.noContent().build();
    }
}