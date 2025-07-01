/*
 *
 *    Copyright (c) 2018-2025 Green Button Alliance, Inc.
 *
 *    Portions (c) 2013-2018 EnergyOS.org
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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.greenbuttonalliance.espi.common.domain.usage.ApplicationInformationEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.ApplicationInformationEntityRepository;
import org.greenbuttonalliance.espi.datacustodian.utils.VerifyURLParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * RESTful controller for managing ApplicationInformation resources according to the 
 * Green Button Alliance ESPI (Energy Services Provider Interface) specification.
 * 
 * This controller uses modern UUID-based ApplicationInformationEntity and repository patterns.
 */
@RestController
@RequestMapping("/espi/1_1/resource")
@Tag(name = "Application Information", description = "OAuth2 Application Registration and Management API")
public class ApplicationInformationRESTController {

    private final ApplicationInformationEntityRepository applicationInformationRepository;

    @Autowired
    public ApplicationInformationRESTController(
            ApplicationInformationEntityRepository applicationInformationRepository) {
        this.applicationInformationRepository = applicationInformationRepository;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void handleGenericException() {
        // Generic exception handler
    }

    /**
     * Gets all ApplicationInformation resources.
     * 
     * @param response HTTP response for returning data
     * @param params Query parameters for filtering
     * @throws IOException if input/output stream operations fail
     */
    @GetMapping(value = "/ApplicationInformation", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get All ApplicationInformation", 
        description = "Returns a list of all registered applications"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved ApplicationInformation list"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public List<ApplicationInformationEntity> getAllApplicationInformation(
            HttpServletResponse response,
            @Parameter(description = "Query parameters for filtering")
            @RequestParam Map<String, String> params) throws IOException {

        // Verify request contains valid query parameters
        if (!VerifyURLParams.verifyEntries("/espi/1_1/resource/ApplicationInformation", params)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, 
                             "Request contains invalid query parameter values!");
            return null;
        }

        return applicationInformationRepository.findAll();
    }

    /**
     * Gets a specific ApplicationInformation resource by ID.
     * 
     * @param applicationInformationId Unique identifier for the ApplicationInformation
     * @param response HTTP response for returning data
     * @param params Query parameters for filtering
     * @throws IOException if input/output stream operations fail
     */
    @GetMapping(value = "/ApplicationInformation/{applicationInformationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get ApplicationInformation by ID", 
        description = "Returns a specific ApplicationInformation resource"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved ApplicationInformation"),
        @ApiResponse(responseCode = "404", description = "ApplicationInformation not found"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ApplicationInformationEntity getApplicationInformation(
            @Parameter(description = "Unique identifier of the ApplicationInformation", required = true)
            @PathVariable UUID applicationInformationId,
            HttpServletResponse response,
            @Parameter(description = "Query parameters for filtering")
            @RequestParam Map<String, String> params) throws IOException {

        // Verify request contains valid query parameters
        if (!VerifyURLParams.verifyEntries("/espi/1_1/resource/ApplicationInformation/{applicationInformationId}", params)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, 
                             "Request contains invalid query parameter values!");
            return null;
        }

        ApplicationInformationEntity entity = applicationInformationRepository.findById(applicationInformationId).orElse(null);
        if (entity == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
        return entity;
    }

    /**
     * Creates a new ApplicationInformation resource.
     * 
     * @param entity ApplicationInformationEntity to create
     * @param response HTTP response for returning created resource
     * @return created ApplicationInformationEntity
     */
    @PostMapping(value = "/ApplicationInformation", 
                consumes = MediaType.APPLICATION_JSON_VALUE, 
                produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Create ApplicationInformation", 
        description = "Creates a new ApplicationInformation resource"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successfully created ApplicationInformation"),
        @ApiResponse(responseCode = "400", description = "Invalid ApplicationInformation data")
    })
    public ApplicationInformationEntity createApplicationInformation(
            @Parameter(description = "ApplicationInformation data to create", required = true)
            @RequestBody ApplicationInformationEntity entity,
            HttpServletResponse response) {

        try {
            ApplicationInformationEntity savedEntity = applicationInformationRepository.save(entity);
            response.setStatus(HttpServletResponse.SC_CREATED);
            return savedEntity;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
    }

    /**
     * Updates an existing ApplicationInformation resource.
     * 
     * @param applicationInformationId Unique identifier for the ApplicationInformation to update
     * @param entity Updated ApplicationInformationEntity data
     * @param response HTTP response for returning updated resource
     * @return updated ApplicationInformationEntity
     */
    @PutMapping(value = "/ApplicationInformation/{applicationInformationId}", 
               consumes = MediaType.APPLICATION_JSON_VALUE,
               produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Update ApplicationInformation", 
        description = "Updates an existing ApplicationInformation resource"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updated ApplicationInformation"),
        @ApiResponse(responseCode = "404", description = "ApplicationInformation not found"),
        @ApiResponse(responseCode = "400", description = "Invalid ApplicationInformation data")
    })
    public ApplicationInformationEntity updateApplicationInformation(
            @Parameter(description = "Unique identifier of the ApplicationInformation to update", required = true)
            @PathVariable UUID applicationInformationId,
            @Parameter(description = "Updated ApplicationInformation data", required = true)
            @RequestBody ApplicationInformationEntity entity,
            HttpServletResponse response) {

        try {
            // Check if entity exists first
            if (!applicationInformationRepository.existsById(applicationInformationId)) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return null;
            }
            
            // Set the ID to ensure we're updating the correct entity
            entity.setId(applicationInformationId);
            ApplicationInformationEntity updatedEntity = applicationInformationRepository.save(entity);
            response.setStatus(HttpServletResponse.SC_OK);
            return updatedEntity;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
    }

    /**
     * Deletes an ApplicationInformation resource.
     * 
     * @param applicationInformationId Unique identifier for the ApplicationInformation to delete
     * @param response HTTP response
     */
    @DeleteMapping("/ApplicationInformation/{applicationInformationId}")
    @Operation(
        summary = "Delete ApplicationInformation", 
        description = "Deletes an ApplicationInformation resource"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully deleted ApplicationInformation"),
        @ApiResponse(responseCode = "404", description = "ApplicationInformation not found")
    })
    public void deleteApplicationInformation(
            @Parameter(description = "Unique identifier of the ApplicationInformation to delete", required = true)
            @PathVariable UUID applicationInformationId,
            HttpServletResponse response) {
        
        try {
            if (applicationInformationRepository.existsById(applicationInformationId)) {
                applicationInformationRepository.deleteById(applicationInformationId);
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}