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
import org.greenbuttonalliance.espi.common.dto.usage.IntervalBlockDto;
import org.greenbuttonalliance.espi.common.mapper.usage.IntervalBlockMapper;
import org.greenbuttonalliance.espi.common.repositories.usage.IntervalBlockRepository;
import org.greenbuttonalliance.espi.common.service.impl.IntervalBlockExportService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;

/**
 * Modern REST Controller for ESPI Interval Block resources.
 * <p>
 * This controller implements the NAESB ESPI 4.0 REST API for Interval Blocks,
 * using modern Spring Boot 3.5 patterns with DTOs and MapStruct mappers.
 */
@RestController
@RequestMapping("/espi/1_1/resource")
@Tag(name = "Interval Block", description = "ESPI Interval Block resource endpoints")
@SecurityRequirement(name = "oauth2")
@RequiredArgsConstructor
public class IntervalBlockRESTController {

    private final IntervalBlockRepository intervalBlockRepository;
    private final IntervalBlockMapper intervalBlockMapper;
    private final IntervalBlockExportService intervalBlockExportService;

    /**
     * Get all Interval Blocks (root collection).
     */
    @GetMapping(value = "/IntervalBlock", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get all Interval Blocks",
        description = "Retrieve all Interval Blocks accessible to the authenticated client",
        responses = {
            @ApiResponse(responseCode = "200", description = "Interval Blocks retrieved successfully",
                content = @Content(schema = @Schema(implementation = IntervalBlockDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or " +
                 "hasAuthority('SCOPE_FB_15_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_16_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_36_READ_3rd_party')")
    public ResponseEntity<byte[]> getIntervalBlockCollection(
            @Parameter(description = "Maximum number of results to return", example = "50")
            @RequestParam(defaultValue = "50") int limit,
            @Parameter(description = "Offset for pagination", example = "0")
            @RequestParam(defaultValue = "0") int offset,
            Authentication authentication) {

        List<IntervalBlockDto> dtos = intervalBlockRepository.findAll(PageRequest.of(offset, limit)).getContent().stream()
            .map(intervalBlockMapper::toDto)
            .toList();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        intervalBlockExportService.exportDto(dtos, out);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(out.toByteArray());
    }

    /**
     * Get specific Interval Block by ID (root resource).
     */
    @GetMapping(value = "/IntervalBlock/{intervalBlockId}", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get Interval Block by ID",
        description = "Retrieve a specific Interval Block by its unique identifier",
        responses = {
            @ApiResponse(responseCode = "200", description = "Interval Block retrieved successfully",
                content = @Content(schema = @Schema(implementation = IntervalBlockDto.class))),
            @ApiResponse(responseCode = "404", description = "Interval Block not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access') or " +
                 "hasAuthority('SCOPE_FB_15_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_16_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_36_READ_3rd_party')")
    public ResponseEntity<byte[]> getIntervalBlock(
            @Parameter(description = "Unique identifier of the Interval Block", required = true)
            @PathVariable UUID intervalBlockId,
            Authentication authentication) {

        IntervalBlockDto dto = intervalBlockRepository.findById(intervalBlockId)
            .map(intervalBlockMapper::toDto)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interval Block not found for id: " + intervalBlockId));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        intervalBlockExportService.exportDto(dto, out);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(out.toByteArray());
    }

    /**
     * Get Interval Blocks for a specific Meter Reading.
     */
    @GetMapping(value = "/Subscription/{subscriptionId}/UsagePoint/{usagePointId}/MeterReading/{meterReadingId}/IntervalBlock", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get Interval Blocks for Meter Reading",
        description = "Retrieve all Interval Blocks associated with a specific meter reading within a subscription",
        responses = {
            @ApiResponse(responseCode = "200", description = "Interval Blocks retrieved successfully",
                content = @Content(schema = @Schema(implementation = IntervalBlockDto.class))),
            @ApiResponse(responseCode = "404", description = "Meter Reading not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_FB_15_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_16_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_36_READ_3rd_party')")
    public ResponseEntity<byte[]> getSubscriptionIntervalBlocks(
            @Parameter(description = "Unique identifier of the Subscription", required = true)
            @PathVariable UUID subscriptionId,
            @Parameter(description = "Unique identifier of the Usage Point", required = true)
            @PathVariable UUID usagePointId,
            @Parameter(description = "Unique identifier of the Meter Reading", required = true)
            @PathVariable UUID meterReadingId,
            Authentication authentication) {

        // Use the specialized repository method for hierarchical access
        List<IntervalBlockDto> dtos = intervalBlockRepository.findAllByMeterReadingId(meterReadingId).stream()
            .map(intervalBlockMapper::toDto)
            .toList();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        intervalBlockExportService.exportDto(dtos, out);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(out.toByteArray());
    }

    /**
     * Get specific Interval Block for a Meter Reading.
     */
    @GetMapping(value = "/Subscription/{subscriptionId}/UsagePoint/{usagePointId}/MeterReading/{meterReadingId}/IntervalBlock/{intervalBlockId}", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Get Interval Block for Meter Reading",
        description = "Retrieve a specific Interval Block associated with a meter reading within a subscription",
        responses = {
            @ApiResponse(responseCode = "200", description = "Interval Block retrieved successfully",
                content = @Content(schema = @Schema(implementation = IntervalBlockDto.class))),
            @ApiResponse(responseCode = "404", description = "Interval Block or Meter Reading not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient scope")
        }
    )
    @PreAuthorize("hasAuthority('SCOPE_FB_15_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_16_READ_3rd_party') or " +
                 "hasAuthority('SCOPE_FB_36_READ_3rd_party')")
    public ResponseEntity<byte[]> getSubscriptionIntervalBlock(
            @Parameter(description = "Unique identifier of the Subscription", required = true)
            @PathVariable UUID subscriptionId,
            @Parameter(description = "Unique identifier of the Usage Point", required = true)
            @PathVariable UUID usagePointId,
            @Parameter(description = "Unique identifier of the Meter Reading", required = true)
            @PathVariable UUID meterReadingId,
            @Parameter(description = "Unique identifier of the Interval Block", required = true)
            @PathVariable UUID intervalBlockId,
            Authentication authentication) {

        IntervalBlockDto dto = intervalBlockRepository.findById(intervalBlockId)
            .map(intervalBlockMapper::toDto)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interval Block not found for id: " + intervalBlockId));

        // TODO: Validate relationship between intervalBlock and meterReading/usagePoint/subscription

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        intervalBlockExportService.exportDto(dto, out);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(out.toByteArray());
    }
}
