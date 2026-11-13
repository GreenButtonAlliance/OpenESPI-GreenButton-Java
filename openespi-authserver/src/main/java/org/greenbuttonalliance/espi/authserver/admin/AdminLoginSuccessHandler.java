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

package org.greenbuttonalliance.espi.authserver.admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import java.io.IOException;

/**
 * Form-login (password factor) success handler for administrators (#205).
 *
 * <p>Records {@code last_login_at}. An admin who has not enrolled a passkey yet is sent to Spring
 * Security's default registration page ({@value #REGISTRATION_PAGE}). Until enrolment, the admin
 * paths reject the password-only session because the WebAuthn factor is missing. Otherwise the
 * saved request is resumed, which for an admin path triggers the passkey step.</p>
 */
public class AdminLoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    static final String REGISTRATION_PAGE = "/webauthn/register";

    private final AdminUserService adminUserService;

    public AdminLoginSuccessHandler(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {
        adminUserService.recordLogin(authentication.getName());
        if (!adminUserService.hasPasskey(authentication.getName())) {
            clearAuthenticationAttributes(request);
            getRedirectStrategy().sendRedirect(request, response, REGISTRATION_PAGE);
            return;
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
