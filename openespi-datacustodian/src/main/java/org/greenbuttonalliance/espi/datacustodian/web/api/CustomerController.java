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
import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerEntity;
import org.greenbuttonalliance.espi.common.dto.customer.CustomerDto;
import org.greenbuttonalliance.espi.common.mapper.customer.CustomerMapper;
import org.greenbuttonalliance.espi.common.service.customer.CustomerService;
import org.greenbuttonalliance.espi.datacustodian.web.api.support.ApiRequestValidator;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Modern REST Controller for ESPI Customer resources.
 *
 * This controller implements the NAESB ESPI 4.0 REST API for Customers,
 * using modern Spring Boot 4.0 patterns with DTOs and MapStruct mappers.
 *
 * Supported endpoints:
 * - GET /espi/1_1/resource/Customer - List all customers
 * - GET /espi/1_1/resource/Customer/{customerId} - Get specific customer
 */
@RestController
@RequestMapping("/espi/1_1/resource")
@Tag(name = "Customer", description = "ESPI Customer PII Data Management API")
@SecurityRequirement(name = "oauth2")
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerMapper customerMapper;
    private final ApiRequestValidator requestValidator;

    public CustomerController(CustomerService customerService,
                              CustomerMapper customerMapper,
                              ApiRequestValidator requestValidator) {
        this.customerService = customerService;
        this.customerMapper = customerMapper;
        this.requestValidator = requestValidator;
    }

    /**
     * Get all Customers (root collection).
     * Requires DataCustodian admin access or appropriate read scope.
     */
    @GetMapping(value = "/Customer", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get Customer Collection",
        description = "Retrieves all Customer resources accessible to the authenticated client",
        responses = {
            @ApiResponse(responseCode = "200", description = "Customers retrieved successfully",
                content = @Content(schema = @Schema(implementation = CustomerDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access')")
    public ResponseEntity<List<CustomerDto>> getAllCustomers(
            @Parameter(description = "Maximum number of results to return", example = "50")
            @RequestParam(defaultValue = "50") int limit,
            @Parameter(description = "Offset for pagination", example = "0")
            @RequestParam(defaultValue = "0") int offset) {

        List<CustomerEntity> entities = customerService.findAll();

        List<CustomerDto> dtos = requestValidator.paginate(
            entities.stream().map(customerMapper::toDto).toList(),
            limit,
            offset
        );

        return ResponseEntity.ok(dtos);
    }

    /**
     * Get specific Customer by ID (root resource).
     */
    @GetMapping(value = "/Customer/{customerId}", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get Customer by ID",
        description = "Retrieve a specific Customer by its unique identifier",
        responses = {
            @ApiResponse(responseCode = "200", description = "Customer retrieved successfully",
                content = @Content(schema = @Schema(implementation = CustomerDto.class))),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access')")
    public ResponseEntity<CustomerDto> getCustomer(
            @Parameter(description = "Unique identifier of the Customer", required = true)
            @PathVariable UUID customerId) {

        return customerService.findById(customerId)
            .map(customerMapper::toDto)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
