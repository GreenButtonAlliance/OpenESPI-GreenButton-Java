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

package org.greenbuttonalliance.espi.thirdparty.web.custodian;

import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;

import org.greenbuttonalliance.espi.common.service.RetailCustomerService;
import org.greenbuttonalliance.espi.thirdparty.web.BaseController;
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

@Controller
@PreAuthorize("hasRole('ROLE_CUSTODIAN')")
public class RetailCustomerController extends BaseController {

	@Autowired
	private RetailCustomerService service;

	public void setService(RetailCustomerService service) {
		this.service = service;
	}

	public RetailCustomerService getService() {
		return this.service;
	}

	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(new RetailCustomerValidator());
	}

	@RequestMapping(value = "/custodian/retailcustomers", method = RequestMethod.GET)
	public String index(ModelMap model) {
		model.put("customers", service.findAll());

		return "retailcustomers/index";
	}

	@RequestMapping(value = "/custodian/retailcustomers/form", method = RequestMethod.GET)
	public String form(ModelMap model) {
		model.put("retailCustomer", new RetailCustomerEntity());

		return "retailcustomers/form";
	}

	@RequestMapping(value = "/custodian/retailcustomers", method = RequestMethod.POST)
	public String create(
			@ModelAttribute("retailCustomer") @Valid RetailCustomerEntity retailCustomer,
			BindingResult result) {
		if (result.hasErrors()) {
			return "retailcustomers/form";
		} else {
 		try {
 			service.save(retailCustomer);
 			return "redirect:/custodian/retailcustomers";
 		} catch (Exception e) {
 			return "retailcustomers/form";
 		}
		}
	}

	@RequestMapping(value = "/custodian/retailcustomers/{retailCustomerId}", method = RequestMethod.GET)
	public String show(@PathVariable UUID retailCustomerId, ModelMap model) {
		RetailCustomerEntity retailCustomer = service.findById(retailCustomerId);
		model.put("retailCustomer", retailCustomer);
		return "/custodian/retailcustomers/show";
	}

	public static class RetailCustomerValidator implements Validator {

		public boolean supports(@SuppressWarnings("rawtypes") Class clazz) {
			return RetailCustomerEntity.class.isAssignableFrom(clazz);
		}

		public void validate(Object target, Errors errors) {
			ValidationUtils.rejectIfEmptyOrWhitespace(errors, "username",
					"field.required", "Username is required");
			ValidationUtils.rejectIfEmptyOrWhitespace(errors, "password",
					"field.required", "Password is required");
			ValidationUtils.rejectIfEmptyOrWhitespace(errors, "firstName",
					"field.required", "First name is required");
			ValidationUtils.rejectIfEmptyOrWhitespace(errors, "lastName",
					"field.required", "Last name is required");
		}
	}
}
