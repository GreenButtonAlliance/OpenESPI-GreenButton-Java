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
import lombok.RequiredArgsConstructor;
import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerAccountEntity;
import org.greenbuttonalliance.espi.common.dto.customer.CustomerAccountDto;
import org.greenbuttonalliance.espi.common.mapper.customer.CustomerAccountMapper;
import org.greenbuttonalliance.espi.common.repositories.customer.CustomerAccountRepository;
import org.greenbuttonalliance.espi.common.service.customer.CustomerAccountService;
import org.greenbuttonalliance.espi.common.service.impl.CustomerAccountExportService;
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
 * Modern RESTful controller for managing CustomerAccount resources according to the
 * Green Button Alliance ESPI (Energy Services Provider Interface) specification.
 * <p>
 * This controller handles CustomerAccount operations with modern Spring Boot 3.5 patterns,
 * returning DTOs and supporting XML output via StreamingResponseBody.
 */
@RestController
@RequestMapping("/espi/1_1/resource")
@Tag(name = "Customer Account", description = "ESPI Customer Account resource endpoints")
@SecurityRequirement(name = "oauth2")
@RequiredArgsConstructor
public class CustomerAccountRESTController {

    private final CustomerAccountRepository customerAccountRepository;
    private final CustomerAccountMapper customerAccountMapper;
    private final CustomerAccountExportService customerAccountExportService;
    private final CustomerAccountService customerAccountService;

    /**
     * Get all Customer Accounts (root collection).
     */
    @GetMapping(value = "/CustomerAccount", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
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
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or " +
                 "hasAuthority('SCOPE_FB_15_READ_3rd_party')")
    public ResponseEntity<StreamingResponseBody> getCustomerAccountCollection(
            @Parameter(description = "Maximum number of results to return", example = "50")
            @RequestParam(defaultValue = "50") int limit,
            @Parameter(description = "Offset for pagination", example = "0")
            @RequestParam(defaultValue = "0") int offset) {

        List<CustomerAccountDto> dtos = customerAccountRepository.findAll(PageRequest.of(offset, limit)).getContent().stream()
                .map(customerAccountMapper::toDto)
                .toList();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(out -> customerAccountExportService.exportDto(dtos, out));
    }

    /**
     * Get specific Customer Account by ID.
     */
    @GetMapping(value = "/CustomerAccount/{customerAccountId}", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
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
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or " +
                 "hasAuthority('SCOPE_FB_15_READ_3rd_party')")
    public ResponseEntity<StreamingResponseBody> getCustomerAccount(
            @Parameter(description = "Unique identifier of the CustomerAccount", required = true)
            @PathVariable UUID customerAccountId) {

        CustomerAccountDto dto = customerAccountRepository.findById(customerAccountId)
                .map(customerAccountMapper::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CustomerAccount not found for id: " + customerAccountId));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(out -> customerAccountExportService.exportDto(dto, out));
    }

    /**
     * Create a new Customer Account.
     */
    @PostMapping(value = "/CustomerAccount", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Create CustomerAccount",
        description = "Creates a new CustomerAccount resource.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Successfully created CustomerAccount"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or " +
                 "hasAuthority('SCOPE_FB_16_WRITE_3rd_party')")
    public ResponseEntity<CustomerAccountDto> createCustomerAccount(@RequestBody CustomerAccountDto dto) {
        CustomerAccountEntity entity = customerAccountMapper.toEntity(dto);
        CustomerAccountEntity savedEntity = customerAccountService.save(entity);
        CustomerAccountDto savedDto = customerAccountMapper.toDto(savedEntity);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedEntity.getId())
                .toUri();

        return ResponseEntity.created(location).body(savedDto);
    }

    /**
     * Update an existing Customer Account.
     */
    @PutMapping(value = "/CustomerAccount/{customerAccountId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Update CustomerAccount",
        description = "Updates an existing CustomerAccount resource.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successfully updated CustomerAccount"),
            @ApiResponse(responseCode = "404", description = "CustomerAccount not found"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or " +
                 "hasAuthority('SCOPE_FB_16_WRITE_3rd_party')")
    public ResponseEntity<CustomerAccountDto> updateCustomerAccount(
            @PathVariable UUID customerAccountId,
            @RequestBody CustomerAccountDto dto) {

        if (!customerAccountRepository.existsById(customerAccountId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CustomerAccount not found for id: " + customerAccountId);
        }

        CustomerAccountEntity entity = customerAccountMapper.toEntity(dto);
        entity.setId(customerAccountId);
        CustomerAccountEntity updatedEntity = customerAccountService.save(entity);

        return ResponseEntity.ok(customerAccountMapper.toDto(updatedEntity));
    }

    /**
     * Delete a Customer Account.
     */
    @DeleteMapping("/CustomerAccount/{customerAccountId}")
    @Operation(
        summary = "Delete CustomerAccount",
        description = "Deletes an existing CustomerAccount resource.",
        responses = {
            @ApiResponse(responseCode = "204", description = "Successfully deleted CustomerAccount"),
            @ApiResponse(responseCode = "404", description = "CustomerAccount not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or " +
                 "hasAuthority('SCOPE_FB_16_WRITE_3rd_party')")
    public ResponseEntity<Void> deleteCustomerAccount(@PathVariable UUID customerAccountId) {
        if (!customerAccountRepository.existsById(customerAccountId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CustomerAccount not found for id: " + customerAccountId);
        }
        customerAccountRepository.deleteById(customerAccountId);
        return ResponseEntity.noContent().build();
    }
}