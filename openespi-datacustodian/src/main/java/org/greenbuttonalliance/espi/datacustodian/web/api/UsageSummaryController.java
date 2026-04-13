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
import org.greenbuttonalliance.espi.common.domain.usage.UsageSummaryEntity;
import org.greenbuttonalliance.espi.common.dto.atom.AtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto;
import org.greenbuttonalliance.espi.common.service.DtoExportService;
import org.greenbuttonalliance.espi.common.service.UsageSummaryService;
import org.greenbuttonalliance.espi.datacustodian.web.api.support.ApiAccessValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Modern REST Controller for ESPI UsageSummary.
 *
 * This controller implements the NAESB ESPI 1.1 REST API for UsageSummary,
 * using modern Spring Boot 3.5 patterns with DTOs and Atom feeds.
 */
@RestController("usageSummaryApiController")
@RequestMapping("/espi/1_1/resource")
@Tag(name = "Usage Summary", description = "Energy Usage Summary API")
public class UsageSummaryController {

    private final UsageSummaryService usageSummaryService;
    private final DtoExportService exportService;
    private final ApiAccessValidator accessValidator;

    public UsageSummaryController(UsageSummaryService usageSummaryService,
                                   DtoExportService exportService,
                                   ApiAccessValidator accessValidator) {
        this.usageSummaryService = usageSummaryService;
        this.exportService = exportService;
        this.accessValidator = accessValidator;
    }

    /**
     * Get UsageSummary Collection.
     */
    @GetMapping(value = "/UsageSummary", produces = {MediaType.APPLICATION_ATOM_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
            summary = "Get UsageSummary Collection",
            description = "Retrieves all authorized UsageSummary resources."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved UsageSummary collection",
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
        AtomFeedDto feed = exportService.createUsageSummariesFeed();
        return ResponseEntity.ok(feed);
    }

    /**
     * Get UsageSummary by ID.
     */
    @GetMapping(value = "/UsageSummary/{usageSummaryId}", produces = {MediaType.APPLICATION_ATOM_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
            summary = "Get UsageSummary by ID",
            description = "Retrieves a specific UsageSummary resource by its unique identifier."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved UsageSummary",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_ATOM_XML_VALUE,
                            schema = @Schema(implementation = AtomEntryDto.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "UsageSummary not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access")
    })
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or hasAuthority('SCOPE_FB_15_READ_3rd_party')")
    public ResponseEntity<AtomEntryDto> show(@PathVariable UUID usageSummaryId,
                                             @RequestHeader(value = "Authorization", required = false) String authHeader,
                                             org.springframework.security.core.Authentication authentication) {
        UsageSummaryEntity entity = usageSummaryService.findById(usageSummaryId);
        if (entity == null) {
            return ResponseEntity.notFound().build();
        }

        if (!accessValidator.isAdmin(authentication)) {
            UUID tokenSubscriptionId = accessValidator.requireSubscriptionId(authHeader);
            if (entity.getUsagePoint() == null || entity.getUsagePoint().getId() == null) {
                return ResponseEntity.notFound().build();
            }
            accessValidator.enforceUsagePointInSubscription(tokenSubscriptionId, entity.getUsagePoint().getId());
        }

        AtomEntryDto entry = exportService.createUsageSummaryEntry(entity);
        return ResponseEntity.ok(entry);
    }

    /**
     * Get UsageSummary Collection for a specific UsagePoint.
     */
    @GetMapping(value = "/Subscription/{subscriptionId}/UsagePoint/{usagePointId}/UsageSummary", produces = {MediaType.APPLICATION_ATOM_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
            summary = "Get UsageSummary Collection by UsagePoint",
            description = "Retrieves all UsageSummary resources associated with a specific UsagePoint."
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or hasAuthority('SCOPE_FB_15_READ_3rd_party')")
    public ResponseEntity<AtomFeedDto> indexByUsagePoint(@PathVariable UUID subscriptionId,
                                                        @PathVariable UUID usagePointId,
                                                        @RequestHeader(value = "Authorization", required = false) String authHeader,
                                                        org.springframework.security.core.Authentication authentication) {
        accessValidator.enforceSubscriptionPathAccess(authentication, authHeader, subscriptionId);
        accessValidator.enforceUsagePointInSubscription(subscriptionId, usagePointId);
        AtomFeedDto feed = exportService.createUsageSummariesFeedByUsagePointId(usagePointId);
        return ResponseEntity.ok(feed);
    }

    /**
     * Get UsageSummary by ID for a specific UsagePoint.
     */
    @GetMapping(value = "/Subscription/{subscriptionId}/UsagePoint/{usagePointId}/UsageSummary/{usageSummaryId}", produces = {MediaType.APPLICATION_ATOM_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
            summary = "Get UsageSummary by ID for UsagePoint",
            description = "Retrieves a specific UsageSummary resource associated with a specific UsagePoint."
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or hasAuthority('SCOPE_FB_15_READ_3rd_party')")
    public ResponseEntity<AtomEntryDto> showByUsagePoint(@PathVariable UUID subscriptionId,
                                                         @PathVariable UUID usagePointId,
                                                         @PathVariable UUID usageSummaryId,
                                                         @RequestHeader(value = "Authorization", required = false) String authHeader,
                                                         org.springframework.security.core.Authentication authentication) {
        accessValidator.enforceSubscriptionPathAccess(authentication, authHeader, subscriptionId);
        accessValidator.enforceUsagePointInSubscription(subscriptionId, usagePointId);

        UsageSummaryEntity entity = usageSummaryService.findById(usageSummaryId);
        if (entity == null || entity.getUsagePoint() == null || !entity.getUsagePoint().getId().equals(usagePointId)) {
            return ResponseEntity.notFound().build();
        }

        AtomEntryDto entry = exportService.createUsageSummaryEntry(entity);
        return ResponseEntity.ok(entry);
    }

    /**
     * Create UsageSummary (Not Implemented).
     */
    @PostMapping(value = {"/UsageSummary", "/Subscription/{subscriptionId}/UsagePoint/{usagePointId}/UsageSummary"})
    @Operation(summary = "Create UsageSummary", description = "NOT IMPLEMENTED - Returns 501")
    public ResponseEntity<Void> create() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /**
     * Update UsageSummary (Not Implemented).
     */
    @PutMapping(value = {"/UsageSummary/{usageSummaryId}", "/Subscription/{subscriptionId}/UsagePoint/{usagePointId}/UsageSummary/{usageSummaryId}"})
    @Operation(summary = "Update UsageSummary", description = "NOT IMPLEMENTED - Returns 501")
    public ResponseEntity<Void> update() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /**
     * Delete UsageSummary (Not Implemented).
     */
    @DeleteMapping(value = {"/UsageSummary/{usageSummaryId}", "/Subscription/{subscriptionId}/UsagePoint/{usagePointId}/UsageSummary/{usageSummaryId}"})
    @Operation(summary = "Delete UsageSummary", description = "NOT IMPLEMENTED - Returns 501")
    public ResponseEntity<Void> delete() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
