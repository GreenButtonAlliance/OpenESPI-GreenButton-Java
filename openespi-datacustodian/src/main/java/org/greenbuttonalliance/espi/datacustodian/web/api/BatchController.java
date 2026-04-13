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
 * */

package org.greenbuttonalliance.espi.datacustodian.web.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.domain.usage.SubscriptionEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.dto.atom.AtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto;
import org.greenbuttonalliance.espi.common.service.DtoExportService;
import org.greenbuttonalliance.espi.common.service.RetailCustomerService;
import org.greenbuttonalliance.espi.common.service.SubscriptionService;
import org.greenbuttonalliance.espi.common.service.UsagePointService;
import org.greenbuttonalliance.espi.datacustodian.web.api.support.ApiAccessValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Modern REST Controller for ESPI Batch operations.
 *
 * This controller implements the NAESB ESPI 1.0 REST API for Batch operations,
 * replacing the legacy BatchRESTController with modern Spring Boot 3.5 patterns.
 */
@RestController
@RequestMapping("/espi/1_1/resource/Batch")
@Tag(name = "Batch Operations", description = "Green Button Bulk Data Processing API")
@SecurityRequirement(name = "oauth2")
public class BatchController {

    private final RetailCustomerService retailCustomerService;
    private final UsagePointService usagePointService;
    private final SubscriptionService subscriptionService;
    private final DtoExportService exportService;
    private final ApiAccessValidator accessValidator;

    public BatchController(RetailCustomerService retailCustomerService,
                           UsagePointService usagePointService,
                           SubscriptionService subscriptionService,
                           DtoExportService exportService,
                           ApiAccessValidator accessValidator) {
        this.retailCustomerService = retailCustomerService;
        this.usagePointService = usagePointService;
        this.subscriptionService = subscriptionService;
        this.exportService = exportService;
        this.accessValidator = accessValidator;
    }

    /**
     * Bulk Upload Green Button Data.
     */
    @PostMapping(value = "/RetailCustomer/{retailCustomerId}/UsagePoint",
            consumes = MediaType.APPLICATION_ATOM_XML_VALUE,
            produces = MediaType.APPLICATION_ATOM_XML_VALUE)
    @Operation(
            summary = "Bulk Upload Green Button Data",
            description = "Uploads Green Button DMD files for batch processing."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Bulk upload successful"),
            @ApiResponse(responseCode = "400", description = "Invalid ATOM XML or batch data"),
            @ApiResponse(responseCode = "501", description = "Not Implemented - ImportService missing")
    })
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access')")
    public ResponseEntity<Void> upload() {
        // Legacy implementation was empty/incomplete due to missing ImportService
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /**
     * Download Green Button Data Collection for a Retail Customer.
     */
    @GetMapping(value = "/RetailCustomer/{retailCustomerId}/UsagePoint",
            produces = MediaType.APPLICATION_ATOM_XML_VALUE)
    @Operation(
            summary = "Download Green Button Data Collection",
            description = "Downloads all usage points for a retail customer as Green Button DMD file"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "DMD file generated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access"),
            @ApiResponse(responseCode = "404", description = "Retail customer not found")
    })
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or hasAuthority('SCOPE_FB_15_READ_3rd_party')")
    public ResponseEntity<AtomFeedDto> downloadCollection(@Parameter(description = "Retail customer identifier", required = true)
                                                          @PathVariable Long retailCustomerId,
                                                          @RequestHeader(value = "Authorization", required = false) String authHeader,
                                                          Authentication authentication) {
        accessValidator.enforceRetailCustomerAccess(authentication, authHeader, retailCustomerId);

        RetailCustomerEntity retailCustomer = retailCustomerService.findById(retailCustomerId);
        if (retailCustomer == null) {
            return ResponseEntity.notFound().build();
        }

        List<UsagePointEntity> usagePoints = usagePointService.findAllByRetailCustomer(retailCustomer);
        AtomFeedDto feed = exportService.createUsagePointsFeed(usagePoints);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=GreenButtonDownload.xml")
                .body(feed);
    }

    /**
     * Download Green Button Data Member (Specific Usage Point) for a Retail Customer.
     */
    @GetMapping(value = "/RetailCustomer/{retailCustomerId}/UsagePoint/{usagePointId}",
            produces = MediaType.APPLICATION_ATOM_XML_VALUE)
    @Operation(
            summary = "Download Green Button Data Member",
            description = "Downloads specific usage point data for a retail customer as Green Button DMD file"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "DMD file generated successfully"),
            @ApiResponse(responseCode = "404", description = "Usage point not found")
    })
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or hasAuthority('SCOPE_FB_15_READ_3rd_party')")
    public ResponseEntity<AtomEntryDto> downloadMember(@Parameter(description = "Retail customer identifier", required = true)
                                                       @PathVariable Long retailCustomerId,
                                                       @Parameter(description = "Usage point identifier", required = true)
                                                       @PathVariable UUID usagePointId,
                                                       @RequestHeader(value = "Authorization", required = false) String authHeader,
                                                       Authentication authentication) {
        accessValidator.enforceRetailCustomerAccess(authentication, authHeader, retailCustomerId);

        UsagePointEntity usagePoint = usagePointService.findById(retailCustomerId, usagePointId);
        if (usagePoint == null) {
            return ResponseEntity.notFound().build();
        }

        AtomEntryDto entry = exportService.createUsagePointEntry(usagePoint);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=GreenButtonDownload.xml")
                .body(entry);
    }

    /**
     * Download Subscription Data.
     */
    @Transactional(readOnly = true)
    @GetMapping(value = "/Subscription/{subscriptionId}",
            produces = MediaType.APPLICATION_ATOM_XML_VALUE)
    @Operation(
            summary = "Download Subscription Data",
            description = "Downloads usage points associated with a subscription as Green Button feed"
    )
    @PreAuthorize("hasAuthority('SCOPE_FB_15_READ_3rd_party')")
    public ResponseEntity<AtomFeedDto> subscription(@Parameter(description = "Subscription identifier", required = true)
                                                    @PathVariable UUID subscriptionId,
                                                    @RequestHeader(value = "Authorization", required = false) String authHeader,
                                                    Authentication authentication) {
        accessValidator.enforceSubscriptionPathAccess(authentication, authHeader, subscriptionId);

        SubscriptionEntity subscription = subscriptionService.findById(subscriptionId);
        if (subscription == null) {
            return ResponseEntity.notFound().build();
        }

        List<UUID> usagePointIds = subscriptionService.findUsagePointIds(subscriptionId);
        AtomFeedDto feed = exportService.createUsagePointsFeedByIds(usagePointIds);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=GreenButtonDownload.xml")
                .body(feed);
    }

    /**
     * Download Subscription Usage Points.
     */
    @Transactional(readOnly = true)
    @GetMapping(value = "/Subscription/{subscriptionId}/UsagePoint",
            produces = MediaType.APPLICATION_ATOM_XML_VALUE)
    @Operation(
            summary = "Download Subscription Usage Points",
            description = "Downloads all usage points for a specific subscription as Green Button feed"
    )
    @PreAuthorize("hasAuthority('SCOPE_FB_15_READ_3rd_party')")
    public ResponseEntity<AtomFeedDto> subscriptionUsagePoint(@Parameter(description = "Subscription identifier", required = true)
                                                              @PathVariable UUID subscriptionId,
                                                              @RequestHeader(value = "Authorization", required = false) String authHeader,
                                                              Authentication authentication) {
        return subscription(subscriptionId, authHeader, authentication);
    }

    /**
     * Download Specific Subscription Usage Point.
     */
    @Transactional(readOnly = true)
    @GetMapping(value = "/Subscription/{subscriptionId}/UsagePoint/{usagePointId}",
            produces = MediaType.APPLICATION_ATOM_XML_VALUE)
    @Operation(
            summary = "Download Specific Subscription Usage Point",
            description = "Downloads a specific usage point for a subscription as Green Button feed"
    )
    @PreAuthorize("hasAuthority('SCOPE_FB_15_READ_3rd_party')")
    public ResponseEntity<AtomEntryDto> subscriptionUsagePointMember(@Parameter(description = "Subscription identifier", required = true)
                                                                     @PathVariable UUID subscriptionId,
                                                                     @Parameter(description = "Usage point identifier", required = true)
                                                                     @PathVariable UUID usagePointId,
                                                                     @RequestHeader(value = "Authorization", required = false) String authHeader,
                                                                     Authentication authentication) {
        accessValidator.enforceSubscriptionPathAccess(authentication, authHeader, subscriptionId);
        accessValidator.enforceUsagePointInSubscription(subscriptionId, usagePointId);

        SubscriptionEntity subscription = subscriptionService.findById(subscriptionId);
        if (subscription == null) {
            return ResponseEntity.notFound().build();
        }

        AtomEntryDto entry = exportService.createUsagePointEntry(usagePointId);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=GreenButtonDownload.xml")
                .body(entry);
    }

    /**
     * Bulk Data Delivery.
     */
    @GetMapping(value = "/Bulk/{bulkId}",
            produces = MediaType.APPLICATION_ATOM_XML_VALUE)
    @Operation(
            summary = "Bulk Data Delivery",
            description = "Provides bulk delivery of information for third-party applications."
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access')")
    public ResponseEntity<Void> bulk() {
        // Legacy implementation was heavily dependent on SFTP and caching which aren't fully migrated.
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
