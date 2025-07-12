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
import org.greenbuttonalliance.espi.common.dto.usage.AuthorizationDto;
import org.greenbuttonalliance.espi.common.repositories.usage.AuthorizationRepository;
import org.greenbuttonalliance.espi.common.mapper.usage.AuthorizationMapper;
import org.greenbuttonalliance.espi.common.domain.usage.AuthorizationEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Modern REST Controller for ESPI Authorization resources.
 * 
 * This controller implements the NAESB ESPI 1.0 REST API for OAuth2 Authorizations,
 * using modern Spring Boot 3.5 patterns with DTOs and MapStruct mappers.
 * 
 * Supported endpoints:
 * - GET /espi/1_1/resource/Authorization - List all authorizations  
 * - GET /espi/1_1/resource/Authorization/{authorizationId} - Get specific authorization
 */
@RestController
@RequestMapping("/espi/1_1/resource")
@Tag(name = "Authorizations", description = "OAuth2 Authorization Management API")
@SecurityRequirement(name = "oauth2")
public class AuthorizationController {

    private final AuthorizationRepository authorizationRepository;
    private final AuthorizationMapper authorizationMapper;

    public AuthorizationController(AuthorizationRepository authorizationRepository, AuthorizationMapper authorizationMapper) {
        this.authorizationRepository = authorizationRepository;
        this.authorizationMapper = authorizationMapper;
    }

    /**
     * Get all Authorizations (admin access only).
     * This endpoint is restricted to DataCustodian administrators.
     */
    @GetMapping(value = "/Authorization", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get all Authorizations",
        description = "Retrieve all OAuth2 Authorizations (admin access only)",
        responses = {
            @ApiResponse(responseCode = "200", description = "Authorizations retrieved successfully",
                content = @Content(schema = @Schema(implementation = AuthorizationDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin access required")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access')")
    public ResponseEntity<List<AuthorizationDto>> getAllAuthorizations(
            @Parameter(description = "Maximum number of results to return", example = "50")
            @RequestParam(defaultValue = "50") int limit,
            @Parameter(description = "Offset for pagination", example = "0")
            @RequestParam(defaultValue = "0") int offset,
            Authentication authentication) {
        
        Pageable pageable = PageRequest.of(offset / limit, limit);
        List<AuthorizationEntity> authorizationEntities = authorizationRepository.findAll(pageable).getContent();
        List<AuthorizationDto> authorizations = authorizationEntities.stream()
            .map(authorizationMapper::toDto)
            .toList();
        return ResponseEntity.ok(authorizations);
    }

    /**
     * Get specific Authorization by ID.
     * Access restricted based on ownership and scope.
     */
    @GetMapping(value = "/Authorization/{authorizationId}", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get Authorization by ID",
        description = "Retrieve a specific OAuth2 Authorization by its unique identifier",
        responses = {
            @ApiResponse(responseCode = "200", description = "Authorization retrieved successfully",
                content = @Content(schema = @Schema(implementation = AuthorizationDto.class))),
            @ApiResponse(responseCode = "404", description = "Authorization not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access')")
    public ResponseEntity<AuthorizationDto> getAuthorization(
            @Parameter(description = "Unique identifier of the Authorization", required = true)
            @PathVariable UUID authorizationId,
            Authentication authentication) {
        
        return authorizationRepository.findById(authorizationId)
            .map(authorizationMapper::toDto)
            .map(authorization -> ResponseEntity.ok(authorization))
            .orElse(ResponseEntity.notFound().build());
    }
}