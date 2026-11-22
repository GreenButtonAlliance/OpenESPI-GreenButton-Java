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
import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerAccountEntity;
import org.greenbuttonalliance.espi.common.dto.customer.CustomerAccountDto;
import org.greenbuttonalliance.espi.common.mapper.customer.CustomerAccountMapper;
import org.greenbuttonalliance.espi.common.repositories.customer.CustomerAccountRepository;
import org.greenbuttonalliance.espi.common.service.impl.CustomerAccountExportService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;

/**
 * Modern RESTful controller for reading CustomerAccount resources according to the
 * Green Button Alliance ESPI (Energy Services Provider Interface) specification.
 * <p>
 * GET-only, returning XML via JAXB-marshalled responses. The CRUD write endpoints
 * (POST/PUT/DELETE) are deferred — they are admin/sandbox-DB management APIs to be delivered
 * in the separate admin-CRUD track (see issue #119 build plan).
 */
@RestController
@RequestMapping("/espi/1_1/resource")
@Tag(name = "Customer Account", description = "ESPI Customer Account resource endpoints")
@SecurityRequirement(name = "oauth2")
@RequiredArgsConstructor
// open-in-view is off: map and marshal inside one read-only session, or the lazy notifications fail (#253)
@Transactional(readOnly = true)
public class CustomerAccountRESTController {

    private final CustomerAccountRepository customerAccountRepository;
    private final CustomerAccountMapper customerAccountMapper;
    private final CustomerAccountExportService customerAccountExportService;
    private final SubscriptionAccess subscriptionAccess;

    /**
     * Get all Customer Accounts (root collection): every customer's PII, so DataCustodian admin only (#286).
     */
    @GetMapping(value = "/CustomerAccount", produces = MediaType.APPLICATION_ATOM_XML_VALUE)
    @Operation(
        summary = "Get CustomerAccount Collection",
        description = "Retrieves all authorized CustomerAccount resources.",
        responses = {
            @ApiResponse(responseCode = "200", description = "CustomerAccounts retrieved successfully",
                content = @Content(schema = @Schema(implementation = CustomerAccountDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    public ResponseEntity<byte[]> getCustomerAccountCollection(
            @Parameter(description = "Maximum number of results to return", example = "50")
            @RequestParam(defaultValue = "50") int limit,
            @Parameter(description = "Offset for pagination", example = "0")
            @RequestParam(defaultValue = "0") int offset,
            Authentication authentication) {

        subscriptionAccess.requireAdmin(authentication);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        customerAccountExportService.exportFeed(ServletUriComponentsBuilder.fromCurrentRequestUri().toUriString(),
                "CustomerAccount Feed", customerAccountRepository.findAll(PageRequest.of(offset, limit)).getContent(),
                customerAccountMapper::toDto, out);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_ATOM_XML)
                .body(out.toByteArray());
    }

    /**
     * Get specific Customer Account by ID.
     */
    @GetMapping(value = "/CustomerAccount/{customerAccountId}", produces = MediaType.APPLICATION_ATOM_XML_VALUE)
    @Operation(
        summary = "Get CustomerAccount by ID",
        description = "Retrieves a specific CustomerAccount resource by its unique identifier.",
        responses = {
            @ApiResponse(responseCode = "200", description = "CustomerAccount retrieved successfully",
                content = @Content(schema = @Schema(implementation = CustomerAccountDto.class))),
            @ApiResponse(responseCode = "404", description = "CustomerAccount not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    public ResponseEntity<byte[]> getCustomerAccount(
            @Parameter(description = "Unique identifier of the CustomerAccount", required = true)
            @PathVariable UUID customerAccountId,
            Authentication authentication) {

        subscriptionAccess.requireAdmin(authentication);
        CustomerAccountEntity customerAccount = customerAccountRepository.findById(customerAccountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CustomerAccount not found for id: " + customerAccountId));

        // The same Atom entry, with the same links, as in the /CustomerAccount feed
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        customerAccountExportService.exportDto(customerAccountExportService.buildEntry(UsageEntries.resourceBase(
                ServletUriComponentsBuilder.fromCurrentRequestUri().toUriString()) + "/CustomerAccount", customerAccount,
                customerAccountMapper::toDto), out);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_ATOM_XML)
                .body(out.toByteArray());
    }
}
