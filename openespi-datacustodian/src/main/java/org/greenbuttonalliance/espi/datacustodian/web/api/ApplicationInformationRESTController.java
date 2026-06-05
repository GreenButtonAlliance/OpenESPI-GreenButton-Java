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

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;

/**
 * Modern RESTful controller for reading ApplicationInformation resources according to the
 * Green Button Alliance ESPI (Energy Services Provider Interface) specification.
 * <p>
 * GET-only. The CRUD write endpoints (POST/PUT/DELETE) are deferred — they are admin/sandbox-DB
 * management APIs to be delivered in the separate admin-CRUD track (see issue #119 build plan).
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
}
