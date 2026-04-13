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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.greenbuttonalliance.espi.common.domain.usage.TimeConfigurationEntity;
import org.greenbuttonalliance.espi.common.dto.atom.AtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto;
import org.greenbuttonalliance.espi.common.service.DtoExportService;
import org.greenbuttonalliance.espi.common.service.TimeConfigurationService;
import org.greenbuttonalliance.espi.datacustodian.web.api.support.ApiRequestValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Modern REST Controller for ESPI Time Configuration resources.
 * 
 * This controller implements the NAESB ESPI 1.0 REST API for LocalTimeParameters (TimeConfiguration),
 * using modern Spring Boot 3.5 patterns with DTOs and MapStruct mappers.
 */
@RestController
@RequestMapping("/espi/1_1/resource/TimeConfiguration")
@Tag(name = "Time Configurations", description = "ESPI Time Configuration resource endpoints")
@SecurityRequirement(name = "oauth2")
public class TimeConfigurationController {

    private final TimeConfigurationService timeConfigurationService;
    private final DtoExportService exportService;
    private final ApiRequestValidator requestValidator;

    public TimeConfigurationController(TimeConfigurationService timeConfigurationService,
                                       DtoExportService exportService,
                                       ApiRequestValidator requestValidator) {
        this.timeConfigurationService = timeConfigurationService;
        this.exportService = exportService;
        this.requestValidator = requestValidator;
    }

    /**
     * Get all Time Configurations (root collection).
     */
    @GetMapping(produces = {MediaType.APPLICATION_ATOM_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get all Time Configurations",
        description = "Retrieve all Time Configurations accessible to the authenticated client"
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or hasAuthority('SCOPE_FB_15_READ_3rd_party')")
    public ResponseEntity<AtomFeedDto> index(@Parameter(description = "Maximum number of results to return", example = "50")
                                             @RequestParam(defaultValue = "50") int limit,
                                             @Parameter(description = "Offset for pagination", example = "0")
                                             @RequestParam(defaultValue = "0") int offset) {
        requestValidator.validateLimitOffset(limit, offset);
        AtomFeedDto feed = exportService.createTimeConfigurationsFeed();
        return ResponseEntity.ok(feed);
    }

    /**
     * Get specific Time Configuration by ID.
     */
    @GetMapping(value = "/{timeConfigurationId}", produces = {MediaType.APPLICATION_ATOM_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get Time Configuration by ID",
        description = "Retrieve a specific Time Configuration by its unique identifier"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Time Configuration retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Time Configuration not found")
    })
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or hasAuthority('SCOPE_FB_15_READ_3rd_party')")
    public ResponseEntity<AtomEntryDto> show(@Parameter(description = "Unique identifier of the Time Configuration", required = true)
                                             @PathVariable UUID timeConfigurationId) {
        
        AtomEntryDto entry = exportService.createTimeConfigurationEntry(timeConfigurationId);
        if (entry == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(entry);
    }

    /**
     * Create a new Time Configuration.
     */
    @PostMapping(consumes = MediaType.APPLICATION_ATOM_XML_VALUE)
    @Operation(
        summary = "Create Time Configuration",
        description = "Creates a new Time Configuration from an Atom entry"
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access')")
    public ResponseEntity<Void> create() {
        // Implementation for importing resources to be added
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * Update an existing Time Configuration.
     */
    @PutMapping(value = "/{timeConfigurationId}", consumes = MediaType.APPLICATION_ATOM_XML_VALUE)
    @Operation(
        summary = "Update Time Configuration",
        description = "Updates an existing Time Configuration"
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access')")
    public ResponseEntity<Void> update() {
        // Implementation for updating resources to be added
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * Delete a Time Configuration.
     */
    @DeleteMapping(value = "/{timeConfigurationId}")
    @Operation(
        summary = "Delete Time Configuration",
        description = "Deletes a specific Time Configuration"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Time Configuration deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Time Configuration not found")
    })
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access')")
    public ResponseEntity<Void> delete(@PathVariable UUID timeConfigurationId) {
        
        TimeConfigurationEntity timeConfiguration = timeConfigurationService.findById(timeConfigurationId);
        if (timeConfiguration == null) {
            return ResponseEntity.notFound().build();
        }
        timeConfigurationService.delete(timeConfiguration);
        return ResponseEntity.noContent().build();
    }
}
