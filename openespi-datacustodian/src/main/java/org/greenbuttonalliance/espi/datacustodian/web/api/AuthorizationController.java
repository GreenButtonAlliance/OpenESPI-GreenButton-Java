/*
 *
 *        Copyright (c) 2025-2026 Green Button Alliance, Inc.
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
import org.greenbuttonalliance.espi.common.domain.usage.AuthorizationEntity;
import org.greenbuttonalliance.espi.common.dto.atom.UsageAtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.usage.AuthorizationDto;
import org.greenbuttonalliance.espi.common.mapper.usage.AuthorizationMapper;
import org.greenbuttonalliance.espi.common.repositories.usage.AuthorizationRepository;
import org.greenbuttonalliance.espi.common.service.impl.UsageExportService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;

/**
 * ESPI Authorization resources (admin access only).
 * <ul>
 *   <li>{@code GET /espi/1_1/resource/Authorization}: Atom feed of Authorizations; an empty feed when
 *       there are none.</li>
 *   <li>{@code GET /espi/1_1/resource/Authorization/{authorizationId}}: one Authorization as an Atom
 *       entry, or 404.</li>
 * </ul>
 * Both previously returned {@code null}, i.e. HTTP 200 with an empty body (#247). Token values,
 * authorization codes and state never appear in the XML: those {@link AuthorizationDto} fields are
 * {@code @XmlTransient}.
 * <p>Row-level scope ({@link AdminCaller}): a {@code DataCustodian_Admin_Access} caller sees every
 * Authorization; a {@code ThirdParty_Admin_Access} caller only those granted to its own application.</p>
 */
@RestController
@RequestMapping("/espi/1_1/resource")
@Tag(name = "Authorizations", description = "OAuth2 Authorization Management API")
@SecurityRequirement(name = "oauth2")
@RequiredArgsConstructor
public class AuthorizationController {

    private final AuthorizationRepository authorizationRepository;
    private final AuthorizationMapper authorizationMapper;
    private final UsageExportService usageExportService;

    /**
     * Get all Authorizations (admin access only).
     */
    @GetMapping(value = "/Authorization", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get all Authorizations",
        description = "Retrieve all OAuth2 Authorizations as an Atom feed (admin access only)",
        responses = {
            @ApiResponse(responseCode = "200", description = "Authorizations retrieved successfully",
                content = @Content(schema = @Schema(implementation = AuthorizationDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin access required")
        }
    )
    @PreAuthorize("hasAnyAuthority('SCOPE_DataCustodian_Admin_Access', 'SCOPE_ThirdParty_Admin_Access')")
    public ResponseEntity<byte[]> getAllAuthorizations(
            @Parameter(description = "Maximum number of results to return", example = "50")
            @RequestParam(defaultValue = "50") int limit,
            @Parameter(description = "Offset for pagination", example = "0")
            @RequestParam(defaultValue = "0") int offset,
            Authentication authentication) {

        // Row-level scope: a DataCustodian admin sees all Authorizations; a Third Party admin only those
        // of its own application (token client_id); anyone else nothing. See AdminCaller.
        PageRequest page = PageRequest.of(offset / limit, limit);
        List<AuthorizationEntity> authorizations = AdminCaller.isDataCustodianAdmin(authentication)
                ? authorizationRepository.findAll(page).getContent()
                : AdminCaller.clientId(authentication)
                        .map(clientId -> authorizationRepository.findAllByApplicationClientId(clientId, page).getContent())
                        .orElse(List.of());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        usageExportService.exportFeed(ServletUriComponentsBuilder.fromCurrentRequestUri().toUriString(),
                "Authorization Feed", authorizations, authorizationMapper::toDto, out);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(out.toByteArray());
    }

    /**
     * Get specific Authorization by ID (admin access only).
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
    @PreAuthorize("hasAnyAuthority('SCOPE_DataCustodian_Admin_Access', 'SCOPE_ThirdParty_Admin_Access')")
    public ResponseEntity<byte[]> getAuthorization(
            @Parameter(description = "Unique identifier of the Authorization", required = true)
            @PathVariable UUID authorizationId,
            Authentication authentication) {

        // Another application's Authorization is reported as not found, so its existence is not disclosed.
        AuthorizationEntity authorization = authorizationRepository.findById(authorizationId)
                .filter(a -> AdminCaller.canSee(authentication,
                        a.getApplicationInformation() != null ? a.getApplicationInformation().getClientId() : null))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Authorization not found for id: " + authorizationId));

        String self = ServletUriComponentsBuilder.fromCurrentRequestUri().toUriString();
        String up = self.substring(0, self.lastIndexOf('/'));
        // One entry, built like a feed entry so it carries the same id, links and timestamps.
        UsageAtomEntryDto entry = (UsageAtomEntryDto) usageExportService
                .buildFeed(up, "Authorization", List.of(authorization), authorizationMapper::toDto)
                .getEntries().getFirst();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        usageExportService.exportDto(entry, out);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(out.toByteArray());
    }
}
