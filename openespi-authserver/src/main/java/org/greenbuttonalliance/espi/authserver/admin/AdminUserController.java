/*
 *
 *    Copyright (c) 2018-2026 Green Button Alliance, Inc.
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

package org.greenbuttonalliance.espi.authserver.admin;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Admin-user management API (#205).
 *
 * <p>The URL rules protect it: on the session chain, ROLE_ADMIN or DC_ADMIN plus the password and
 * passkey factors; on the bearer chain, an {@code authserver.admin} token. On top of that, the
 * method-level check requires {@code ROLE_ADMIN}, because managing administrators is not delegated to
 * DC_ADMIN. Responses never include password hashes.</p>
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    record CreateUserRequest(String username, String password, Set<String> roles) {
    }

    record EnabledRequest(Boolean enabled) {
    }

    record PasswordRequest(String password) {
    }

    record RolesRequest(Set<String> roles) {
    }

    private final AdminUserService service;

    public AdminUserController(AdminUserService service) {
        this.service = service;
    }

    @GetMapping
    public List<AdminUser> list() {
        return service.list();
    }

    @PostMapping
    public ResponseEntity<AdminUser> create(@RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(request.username(), request.password(), request.roles()));
    }

    @PutMapping("/{id}/enabled")
    public AdminUser setEnabled(@PathVariable String id, @RequestBody EnabledRequest request,
                                Authentication authentication) {
        if (request.enabled() == null) {
            throw new AdminUserService.InvalidRequestException("enabled is required");
        }
        return service.setEnabled(id, request.enabled(), authentication.getName());
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Void> resetPassword(@PathVariable String id, @RequestBody PasswordRequest request) {
        service.resetPassword(id, request.password());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/roles")
    public AdminUser setRoles(@PathVariable String id, @RequestBody RolesRequest request,
                              Authentication authentication) {
        return service.setRoles(id, request.roles(), authentication.getName());
    }

    @DeleteMapping("/{id}/passkeys")
    public Map<String, Integer> resetPasskeys(@PathVariable String id) {
        return Map.of("removed", service.resetPasskeys(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, Authentication authentication) {
        service.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(AdminUserService.InvalidRequestException.class)
    ResponseEntity<Map<String, String>> badRequest(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("error", "invalid_request", "message", e.getMessage()));
    }

    @ExceptionHandler(AdminUserService.ConflictException.class)
    ResponseEntity<Map<String, String>> conflict(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "conflict", "message", e.getMessage()));
    }

    @ExceptionHandler(AdminUserService.NotFoundException.class)
    ResponseEntity<Map<String, String>> notFound(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "not_found", "message", e.getMessage()));
    }
}
