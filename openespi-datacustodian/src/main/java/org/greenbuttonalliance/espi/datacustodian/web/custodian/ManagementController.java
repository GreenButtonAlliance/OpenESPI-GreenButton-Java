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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// @Controller - COMMENTED OUT: UI not needed in resource server
// @Component
@PreAuthorize("hasRole('ROLE_CUSTODIAN')")
public class ManagementController {

	// @Autowired
	// private NotificationService notificationService; // TODO: Implement

	@GetMapping("/espi/1_1/NotifyThirdParty/{applicationInformationId}")
	public String notifyThirdParty(@PathVariable Long applicationInformationId,
			ModelMap model) throws Exception {

		// notificationService.notifyAllNeed(); // TODO: Implement NotificationService

		return "redirect:/custodian/home";
	}

	@GetMapping("/espi/1_1/NotifyThirdParty")
	public String notifyAllThirdParties(ModelMap model) throws Exception {

		// notificationService.notifyAllNeed(); // TODO: Implement NotificationService

		return "redirect:/custodian/home";
	}

	// public void setNotificationService(NotificationService notificationService) {
	//	this.notificationService = notificationService;
	// }

	// public NotificationService getNotificationService() {
	//	return this.notificationService;
	// }

}
