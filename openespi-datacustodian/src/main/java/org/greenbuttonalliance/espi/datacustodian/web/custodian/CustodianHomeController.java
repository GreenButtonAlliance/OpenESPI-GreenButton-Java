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

package org.greenbuttonalliance.espi.datacustodian.web.custodian;

import org.greenbuttonalliance.espi.common.domain.usage.AuthorizationEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository;
import org.greenbuttonalliance.espi.common.service.AuthorizationService;
import org.greenbuttonalliance.espi.common.service.RetailCustomerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Custodian portal landing page (admin dashboard).
 *
 * <p>The custodian login flow (C2a) redirects to {@code /custodian/home} on success
 * (CustomerLoginSecurityConfiguration DEFAULT_SUCCESS_URL), so this landing controller must be
 * enabled — otherwise the post-login redirect 404s and is re-dispatched to /error, which the
 * resource-server chain returns as 401 (#171). This controller also answers the bare
 * {@code /custodian} path so the navbar brand/Dashboard link resolves.</p>
 *
 * <p>Read-only: it renders the dashboard and populates the overview stat tiles with simple entity
 * counts. It performs no writes (CRUD remains deferred per #166). Template:
 * templates/custodian/home.html.</p>
 */
@Controller
@PreAuthorize("hasRole('ROLE_CUSTODIAN')")
public class CustodianHomeController {

	private final RetailCustomerService retailCustomerService;
	private final AuthorizationService authorizationService;
	private final UsagePointRepository usagePointRepository;

	public CustodianHomeController(RetailCustomerService retailCustomerService,
								   AuthorizationService authorizationService,
								   UsagePointRepository usagePointRepository) {
		this.retailCustomerService = retailCustomerService;
		this.authorizationService = authorizationService;
		this.usagePointRepository = usagePointRepository;
	}

	@GetMapping({"/custodian", "/custodian/home"})
	public String index(Model model) {
		List<AuthorizationEntity> authorizations = authorizationService.findAll();
		long activeTokens = authorizations.stream().filter(AuthorizationEntity::isActive).count();

		model.addAttribute("totalCustomers", retailCustomerService.findAll().size());
		model.addAttribute("activeTokens", activeTokens);
		model.addAttribute("totalUsagePoints", usagePointRepository.count());
		model.addAttribute("todayRequests", 0);

		return "custodian/home";
	}
}
