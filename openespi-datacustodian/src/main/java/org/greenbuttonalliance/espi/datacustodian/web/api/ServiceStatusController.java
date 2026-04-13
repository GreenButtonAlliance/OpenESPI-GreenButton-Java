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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.greenbuttonalliance.espi.common.domain.usage.ApplicationInformationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.AuthorizationEntity;
import org.greenbuttonalliance.espi.common.dto.atom.AtomEntryDto;
import org.greenbuttonalliance.espi.common.service.AuthorizationService;
import org.greenbuttonalliance.espi.common.service.DtoExportService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Modern REST Controller for ESPI Service Status.
 *
 * This controller implements the NAESB ESPI 1.1 REST API for ServiceStatus,
 * using modern Spring Boot 3.5 patterns with DTOs.
 */
@RestController
@RequestMapping("/espi/1_1/resource")
@Tag(name = "Service Status", description = "System Service Status Information API")
@SecurityRequirement(name = "oauth2")
public class ServiceStatusController {

    private final AuthorizationService authorizationService;
    private final DtoExportService exportService;

    public ServiceStatusController(AuthorizationService authorizationService,
                                   DtoExportService exportService) {
        this.authorizationService = authorizationService;
        this.exportService = exportService;
    }

    /**
     * Get Service Status.
     */
    @GetMapping(value = "/ServiceStatus", produces = {MediaType.APPLICATION_ATOM_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
            summary = "Get Service Status",
            description = "Returns the current service status information including application status and system health"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Service status retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_ATOM_XML_VALUE,
                            schema = @Schema(implementation = AtomEntryDto.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized access"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or hasAuthority('SCOPE_FB_15_READ_3rd_party')")
    public ResponseEntity<AtomEntryDto> index(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String applicationStatus = "0";

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            AuthorizationEntity authorization = authorizationService.findByAccessToken(token);
            if (authorization != null) {
                ApplicationInformationEntity applicationInformation = authorization.getApplicationInformation();
                if (applicationInformation != null && applicationInformation.getDataCustodianApplicationStatus() != null) {
                    applicationStatus = applicationInformation.getDataCustodianApplicationStatus();
                }
            }
        }

        AtomEntryDto entry = exportService.createServiceStatusEntry(applicationStatus);
        return ResponseEntity.ok(entry);
    }
}
