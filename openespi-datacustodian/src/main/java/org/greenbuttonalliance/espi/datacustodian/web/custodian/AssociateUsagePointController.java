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

// import org.greenbuttonalliance.espi.common.domain.usage.Routes; // Missing class
import org.greenbuttonalliance.espi.common.domain.usage.SubscriptionEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.ResourceRepository;
import org.greenbuttonalliance.espi.common.service.RetailCustomerService;
import org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.UUID;

// @Controller - COMMENTED OUT: UI not needed in resource server
// @Component
@PreAuthorize("hasRole('ROLE_CUSTODIAN')")
public class AssociateUsagePointController {

	@Autowired
	private RetailCustomerService retailCustomerService;

	@Autowired
	private ResourceRepository resourceService;

	// @Autowired
	// private NotificationService notificationService; // TODO: Implement

	@Autowired
	private UsagePointRepository service;

	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(new UsagePointEntityFormValidator());
	}

	@RequestMapping(value = "/custodian/retailcustomers/{retailCustomerId}/usagepoints/form", method = RequestMethod.GET)
	public String form(@PathVariable UUID retailCustomerId, ModelMap model) {
		model.put("usagePointForm", new UsagePointEntityForm());
		model.put("retailCustomerId", retailCustomerId);

		return "/custodian/retailcustomers/usagepoints/form";
	}

	@RequestMapping(value = "/custodian/retailcustomers/{retailCustomerId}/usagepoints/create", method = RequestMethod.POST)
	public String create(
			@PathVariable UUID retailCustomerId,
			@ModelAttribute("usagePointForm") @Valid UsagePointEntityForm usagePointForm,
			BindingResult result) {
		if (result.hasErrors())
			return "/custodian/retailcustomers/usagepoints/form";

		// retailCustomerService returns legacy SubscriptionEntity, not SubscriptionEntityEntity
		var subscription = retailCustomerService.associateByUUID(
				retailCustomerId, UUID.fromString(usagePointForm.getUUID()));

		if (subscription != null) {
			// TODO: Implement NotificationService
			// notificationService.notify(subscription, null, null);
		}
		return "redirect:/custodian/retailcustomers";
	}

	public void setService(UsagePointRepository service) {
		this.service = service;
	}

	public void getService(UsagePointRepository service) {
		this.service = service;
	}

	public static class UsagePointEntityForm {
		private String uuid;
		private String description;

		public String getUUID() {
			return uuid;
		}

		public void setUUID(String uuid) {
			this.uuid = uuid;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}

	public static class UsagePointEntityFormValidator implements Validator {

		public boolean supports(@SuppressWarnings("rawtypes") Class clazz) {
			return UsagePointEntityForm.class.isAssignableFrom(clazz);
		}

		public void validate(Object target, Errors errors) {
			ValidationUtils.rejectIfEmptyOrWhitespace(errors, "UUID",
					"field.required", "UUID is required");

			try {
				UsagePointEntityForm form = (UsagePointEntityForm) target;
				UUID.fromString(form.getUUID());
			} catch (IllegalArgumentException x) {
				errors.rejectValue("UUID", "uuid.required", null,
						"Must be a valid UUID Ex. 550e8400-e29b-41d4-a716-446655440000");
			}
		}
	}

	public void setRetailCustomerService(
			RetailCustomerService retailCustomerService) {
		this.retailCustomerService = retailCustomerService;
	}

	public RetailCustomerService getRetailCustomerService() {
		return this.retailCustomerService;
	}

	public void setResourceRepository(ResourceRepository resourceService) {
		this.resourceService = resourceService;
	}

	public ResourceRepository getResourceRepository() {
		return this.resourceService;
	}

	// public void setNotificationService(NotificationService notificationService) {
	//	this.notificationService = notificationService;
	// }

	// public NotificationService getNotificationService() {
	//	return this.notificationService;
	// }

	public void setUsagePointRepository(UsagePointRepository service) {
		this.service = service;
	}

	public UsagePointRepository getUsagePointRepository() {
		return this.service;
	}

}
