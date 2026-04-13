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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Modern REST Controller for ESPI Data Custodian Management operations.
 *
 * This controller provides administrative management capabilities within the Data Custodian,
 * such as database initialization and resetting.
 *
 * Supported endpoints:
 * - GET /espi/1_1/resource/DataCustodian/manage - Execute administrative commands
 */
@RestController
@RequestMapping("/espi/1_1/resource/DataCustodian/manage")
@Tag(name = "Data Custodian Management", description = "Administrative commands for Data Custodian maintenance")
@SecurityRequirement(name = "oauth2")
public class ManageController {

    private static final Logger log = LoggerFactory.getLogger(ManageController.class);

    /**
     * Execute an administrative command.
     *
     * @param params Map of request parameters, specifically 'command'
     * @return Output of the command as plain text
     */
    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(
        summary = "Execute Administrative Command",
        description = "Execute a restricted management command on the Data Custodian"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Command executed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid command or execution error"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    @PreAuthorize("hasAuthority('SCOPE_DataCustodian_Admin_Access')")
    public ResponseEntity<String> doCommand(
            @Parameter(description = "Map containing 'command' parameter (resetDataCustodianDB or initializeDataCustodianDB)")
            @RequestParam Map<String, String> params) {

        String commandString = params.get("command");
        if (commandString == null) {
            return ResponseEntity.badRequest().body("[Manage] Missing 'command' parameter");
        }

        log.info("[Manage] Request: {}", commandString);

        StringBuilder outputBuilder = new StringBuilder();
        outputBuilder.append("[Manage] Restricted Management Interface\n");
        outputBuilder.append("[Manage] Request: ").append(commandString).append("\n");

        String commandPath = null;
        if ("resetDataCustodianDB".equals(commandString)) {
            commandPath = "/etc/OpenESPI/DataCustodian/resetDatabase.sh";
        } else if ("initializeDataCustodianDB".equals(commandString)) {
            commandPath = "/etc/OpenESPI/DataCustodian/initializeDatabase.sh";
        }

        if (commandPath == null) {
            outputBuilder.append("[Manage] Error: Unsupported command '").append(commandString).append("'\n");
            return ResponseEntity.badRequest().body(outputBuilder.toString());
        }

        try {
            Process process = new ProcessBuilder(commandPath).start();
            boolean completed = process.waitFor(60, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                outputBuilder.append("[Manage] Exception: Command timed out\n");
                return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(outputBuilder.toString());
            }
            int exitCode = process.exitValue();

            outputBuilder.append("[Manage] Result: \n");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[Manage] Output: {}", line);
                    outputBuilder.append("[Manage]: ").append(line).append("\n");
                }
            }

            outputBuilder.append("[Manage] Errors: \n");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.error("[Manage] Error line: {}", line);
                    outputBuilder.append("[Manage]: ").append(line).append("\n");
                }
            }

            outputBuilder.append("[Manage] Process exited with code: ").append(exitCode).append("\n");
            outputBuilder.append("[Manage] Done\n");

            if (exitCode == 0) {
                return ResponseEntity.ok(outputBuilder.toString());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(outputBuilder.toString());

        } catch (IOException | InterruptedException e) {
            log.error("[Manage] Error executing command {}: {}", commandString, e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            outputBuilder.append("[Manage] Exception: ").append(e.getMessage()).append("\n");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(outputBuilder.toString());
        }
    }
}
