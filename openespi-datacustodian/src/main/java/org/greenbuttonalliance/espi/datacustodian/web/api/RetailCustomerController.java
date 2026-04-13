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
 *
 */

package org.greenbuttonalliance.espi.datacustodian.web.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.dto.atom.AtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto;
import org.greenbuttonalliance.espi.common.service.DtoExportService;
import org.greenbuttonalliance.espi.common.service.RetailCustomerService;
import org.greenbuttonalliance.espi.datacustodian.web.api.support.ApiAccessValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Modern REST Controller for ESPI RetailCustomer.
 *
 * This controller implements the NAESB ESPI 1.1 REST API for RetailCustomer,
 * using modern Spring Boot 3.5 patterns with DTOs and Atom feeds.
 */
@RestController("retailCustomerApiController")
@RequestMapping("/espi/1_1/resource/RetailCustomer")
@Tag(name = "Retail Customer", description = "Utility Customer Account Management API")
public class RetailCustomerController {

    private final RetailCustomerService retailCustomerService;
    private final DtoExportService exportService;
    private final ApiAccessValidator accessValidator;

    public RetailCustomerController(RetailCustomerService retailCustomerService,
                                    DtoExportService exportService,
                                    ApiAccessValidator accessValidator) {
        this.retailCustomerService = retailCustomerService;
        this.exportService = exportService;
        this.accessValidator = accessValidator;
    }

    /**
     * Get RetailCustomer Collection.
     */
    @GetMapping(produces = {MediaType.APPLICATION_ATOM_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
            summary = "Get RetailCustomer Collection",
            description = "Retrieves all authorized RetailCustomer resources with optional filtering and pagination."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved RetailCustomer collection",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_ATOM_XML_VALUE,
                            schema = @Schema(implementation = AtomFeedDto.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized access"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access')")
    public ResponseEntity<AtomFeedDto> index() {
        AtomFeedDto feed = exportService.createRetailCustomersFeed();
        return ResponseEntity.ok(feed);
    }

    /**
     * Get RetailCustomer by ID.
     */
    @GetMapping(value = "/{retailCustomerId}", produces = {MediaType.APPLICATION_ATOM_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
            summary = "Get RetailCustomer by ID",
            description = "Retrieves a specific RetailCustomer resource by its unique identifier."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved RetailCustomer",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_ATOM_XML_VALUE,
                            schema = @Schema(implementation = AtomEntryDto.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "RetailCustomer not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access")
    })
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or " +
            "hasAuthority('SCOPE_FB_15_READ_3rd_party')")
    public ResponseEntity<AtomEntryDto> show(@PathVariable Long retailCustomerId,
                                             @RequestHeader(value = "Authorization", required = false) String authHeader,
                                             Authentication authentication) {
        accessValidator.enforceRetailCustomerAccess(authentication, authHeader, retailCustomerId);

        RetailCustomerEntity entity = retailCustomerService.findById(retailCustomerId);
        if (entity == null) {
            return ResponseEntity.notFound().build();
        }

        AtomEntryDto entry = exportService.createRetailCustomerEntry(entity);
        return ResponseEntity.ok(entry);
    }

    /**
     * Create RetailCustomer (Not Implemented).
     */
    @PostMapping
    @Operation(summary = "Create RetailCustomer", description = "NOT IMPLEMENTED - Returns 501")
    public ResponseEntity<Void> create() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /**
     * Update RetailCustomer (Not Implemented).
     */
    @PutMapping(value = "/{retailCustomerId}")
    @Operation(summary = "Update RetailCustomer", description = "NOT IMPLEMENTED - Returns 501")
    public ResponseEntity<Void> update() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /**
     * Delete RetailCustomer (Not Implemented).
     */
    @DeleteMapping("/{retailCustomerId}")
    @Operation(summary = "Delete RetailCustomer", description = "NOT IMPLEMENTED - Returns 501")
    public ResponseEntity<Void> delete() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
