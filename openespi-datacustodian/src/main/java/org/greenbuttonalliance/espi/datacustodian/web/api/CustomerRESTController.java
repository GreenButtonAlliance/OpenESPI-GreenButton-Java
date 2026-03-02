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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerEntity;
import org.greenbuttonalliance.espi.common.dto.customer.CustomerDto;
import org.greenbuttonalliance.espi.common.mapper.customer.CustomerMapper;
import org.greenbuttonalliance.espi.common.repositories.customer.CustomerRepository;
import org.greenbuttonalliance.espi.common.service.customer.CustomerService;
import org.greenbuttonalliance.espi.common.service.impl.CustomerExportService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Modern RESTful controller for managing Customer resources according to the
 * Green Button Alliance ESPI (Energy Services Provider Interface) specification.
 * <p>
 * This controller handles Customer operations with modern Spring Boot 3.5 patterns,
 * returning DTOs and supporting XML output via StreamingResponseBody.
 */
@RestController
@RequestMapping("/espi/1_1/resource")
@Tag(name = "Customer", description = "ESPI Customer resource endpoints")
@SecurityRequirement(name = "oauth2")
@RequiredArgsConstructor
public class CustomerRESTController {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final CustomerExportService customerExportService;
    private final CustomerService customerService;

    /**
     * Get all Customers (root collection).
     */
    @GetMapping(value = "/Customer", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get Customer Collection",
        description = "Retrieves all authorized Customer resources.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Customers retrieved successfully",
                content = @Content(schema = @Schema(implementation = CustomerDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or " +
                 "hasAuthority('SCOPE_FB_15_READ_3rd_party')")
    public ResponseEntity<StreamingResponseBody> getCustomerCollection(
            @Parameter(description = "Maximum number of results to return", example = "50")
            @RequestParam(defaultValue = "50") int limit,
            @Parameter(description = "Offset for pagination", example = "0")
            @RequestParam(defaultValue = "0") int offset) {

        List<CustomerDto> dtos = customerRepository.findAll(PageRequest.of(offset, limit)).getContent().stream()
                .map(customerMapper::toDto)
                .toList();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(out -> customerExportService.exportDto(dtos, out));
    }

    /**
     * Get specific Customer by ID.
     */
    @GetMapping(value = "/Customer/{customerId}", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get Customer by ID",
        description = "Retrieves a specific Customer resource by its unique identifier.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Customer retrieved successfully",
                content = @Content(schema = @Schema(implementation = CustomerDto.class))),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or " +
                 "hasAuthority('SCOPE_FB_15_READ_3rd_party')")
    public ResponseEntity<StreamingResponseBody> getCustomer(
            @Parameter(description = "Unique identifier of the Customer", required = true)
            @PathVariable UUID customerId) {

        CustomerDto dto = customerRepository.findById(customerId)
                .map(customerMapper::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(out -> customerExportService.exportDto(dto, out));
    }

    /**
     * Create a new Customer.
     */
    @PostMapping(value = "/Customer", consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Create Customer",
        description = "Creates a new Customer resource.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Customer created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or " +
                 "hasAuthority('SCOPE_FB_15_WRITE_3rd_party')")
    public ResponseEntity<CustomerDto> createCustomer(@Valid @RequestBody CustomerDto customerDto) {
        CustomerEntity entity = customerMapper.toEntity(customerDto);
        CustomerEntity savedEntity = customerService.save(entity);
        CustomerDto savedDto = customerMapper.toDto(savedEntity);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedEntity.getId())
                .toUri();

        return ResponseEntity.created(location).body(savedDto);
    }

    /**
     * Update an existing Customer.
     */
    @PutMapping(value = "/Customer/{customerId}", consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Update Customer",
        description = "Updates an existing Customer resource.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Customer updated successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or " +
                 "hasAuthority('SCOPE_FB_15_WRITE_3rd_party')")
    public ResponseEntity<CustomerDto> updateCustomer(
            @Parameter(description = "Unique identifier of the Customer", required = true)
            @PathVariable UUID customerId,
            @Valid @RequestBody CustomerDto customerDto) {

        if (!customerService.existsById(customerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found");
        }

        CustomerEntity entity = customerMapper.toEntity(customerDto);
        entity.setId(customerId);
        CustomerEntity savedEntity = customerService.save(entity);
        return ResponseEntity.ok(customerMapper.toDto(savedEntity));
    }

    /**
     * Delete a Customer.
     */
    @DeleteMapping(value = "/Customer/{customerId}")
    @Operation(
        summary = "Delete Customer",
        description = "Deletes an existing Customer resource.",
        responses = {
            @ApiResponse(responseCode = "204", description = "Customer deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or " +
                 "hasAuthority('SCOPE_FB_15_WRITE_3rd_party')")
    public ResponseEntity<Void> deleteCustomer(
            @Parameter(description = "Unique identifier of the Customer", required = true)
            @PathVariable UUID customerId) {

        if (!customerService.existsById(customerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found");
        }

        customerService.deleteById(customerId);
        return ResponseEntity.noContent().build();
    }
}