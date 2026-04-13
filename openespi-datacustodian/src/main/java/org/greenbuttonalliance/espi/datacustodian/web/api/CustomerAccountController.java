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
import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerAccountEntity;
import org.greenbuttonalliance.espi.common.dto.customer.CustomerAccountDto;
import org.greenbuttonalliance.espi.common.mapper.customer.CustomerAccountMapper;
import org.greenbuttonalliance.espi.common.service.customer.CustomerAccountService;
import org.greenbuttonalliance.espi.datacustodian.web.api.support.ApiRequestValidator;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Modern REST Controller for ESPI Customer Account resources.
 *
 * This controller implements the NAESB ESPI 1.0 REST API for Customer Accounts,
 * using modern Spring Boot 4.0 patterns with DTOs and MapStruct mappers.
 *
 * Supported endpoints:
 * - GET /espi/1_1/resource/CustomerAccount - List all authorized customer accounts
 * - GET /espi/1_1/resource/CustomerAccount/{id} - Get specific customer account details
 */
@RestController
@RequestMapping("/espi/1_1/resource")
@Tag(name = "Customer Account", description = "Customer Billing Account Management API")
@SecurityRequirement(name = "oauth2")
public class CustomerAccountController {

    private final CustomerAccountService customerAccountService;
    private final CustomerAccountMapper customerAccountMapper;
    private final ApiRequestValidator requestValidator;

    public CustomerAccountController(CustomerAccountService customerAccountService,
                                     CustomerAccountMapper customerAccountMapper,
                                     ApiRequestValidator requestValidator) {
        this.customerAccountService = customerAccountService;
        this.customerAccountMapper = customerAccountMapper;
        this.requestValidator = requestValidator;
    }

    /**
     * Get all Customer Accounts (root collection).
     * Requires DataCustodian admin access or appropriate read scope.
     */
    @GetMapping(value = "/CustomerAccount", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get CustomerAccount Collection",
        description = "Retrieves all authorized CustomerAccount resources with optional filtering and pagination",
        responses = {
            @ApiResponse(responseCode = "200", description = "Customer Accounts retrieved successfully",
                content = @Content(schema = @Schema(implementation = CustomerAccountDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or " +
                 "hasAuthority('SCOPE_FB_15_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_16_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_36_READ_3rd_party')")
    public ResponseEntity<List<CustomerAccountDto>> getAllCustomerAccounts(
            @Parameter(description = "Maximum number of results to return", example = "50")
            @RequestParam(defaultValue = "50") int limit,
            @Parameter(description = "Offset for pagination", example = "0")
            @RequestParam(defaultValue = "0") int offset) {

        // Note: CustomerAccountService current implementation doesn't support pagination,
        // so we retrieve all and filter. For production, service should be updated.
        List<CustomerAccountEntity> entities = customerAccountService.findAll();

        List<CustomerAccountDto> dtos = requestValidator.paginate(
            entities.stream().map(customerAccountMapper::toDto).toList(),
            limit,
            offset
        );

        return ResponseEntity.ok(dtos);
    }

    /**
     * Get specific Customer Account by ID (root resource).
     */
    @GetMapping(value = "/CustomerAccount/{customerAccountId}", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get Customer Account by ID",
        description = "Retrieve a specific Customer Account by its unique identifier",
        responses = {
            @ApiResponse(responseCode = "200", description = "Customer Account retrieved successfully",
                content = @Content(schema = @Schema(implementation = CustomerAccountDto.class))),
            @ApiResponse(responseCode = "404", description = "Customer Account not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or " +
                 "hasAuthority('SCOPE_FB_15_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_16_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_36_READ_3rd_party')")
    public ResponseEntity<CustomerAccountDto> getCustomerAccount(
            @Parameter(description = "Unique identifier of the Customer Account", required = true)
            @PathVariable UUID customerAccountId) {

        return customerAccountService.findById(customerAccountId)
            .map(customerAccountMapper::toDto)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
