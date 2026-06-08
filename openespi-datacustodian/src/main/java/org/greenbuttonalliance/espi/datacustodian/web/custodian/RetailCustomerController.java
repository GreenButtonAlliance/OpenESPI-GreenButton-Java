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

import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.service.RetailCustomerService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Controller
@PreAuthorize("hasRole('ROLE_CUSTODIAN')")
public class RetailCustomerController {

	@Autowired
	private RetailCustomerService service;

	@Autowired
	private PasswordEncoder customerPasswordEncoder;

	public void setService(RetailCustomerService service) {
		this.service = service;
	}

	public void setCustomerPasswordEncoder(PasswordEncoder customerPasswordEncoder) {
		this.customerPasswordEncoder = customerPasswordEncoder;
	}

	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(new RetailCustomerValidator());
	}

	@GetMapping("/custodian/retailcustomers")
	public String index(ModelMap model) {
		model.put("customers", service.findAll());

		return "custodian/retailcustomers/index";
	}

	@GetMapping("/custodian/retailcustomers/form")
	public String form(ModelMap model) {
		model.put("retailCustomer", new RetailCustomerEntity());
		model.put("formAction", "/custodian/retailcustomers/create");
		model.put("editMode", false);

		return "custodian/retailcustomers/form";
	}

	@PostMapping("/custodian/retailcustomers/create")
	public String create(
			@ModelAttribute("retailCustomer") @Valid RetailCustomerEntity retailCustomer,
			BindingResult result, ModelMap model) {
		if (result.hasErrors()) {
			model.put("formAction", "/custodian/retailcustomers/create");
			model.put("editMode", false);
			return "custodian/retailcustomers/form";
		}
		// BCrypt-hash the raw password before persistence; the form submits cleartext
		// over the (presumed-TLS) admin chain, the DB stores the bcrypt hash.
		retailCustomer.setPassword(customerPasswordEncoder.encode(retailCustomer.getPassword()));
		try {
			service.save(retailCustomer);
		}
		catch (DataIntegrityViolationException e) {
			// Most commonly a duplicate username (unique constraint). Surface it on the form
			// instead of bubbling a 500.
			result.rejectValue("username", "username.duplicate", "That username is already taken");
			model.put("formAction", "/custodian/retailcustomers/create");
			model.put("editMode", false);
			return "custodian/retailcustomers/form";
		}
		return "redirect:/custodian/retailcustomers";
	}

	@GetMapping("/custodian/retailcustomers/{retailCustomerId}/edit")
	public String edit(@PathVariable Long retailCustomerId, ModelMap model) {
		RetailCustomerEntity retailCustomer = service.findById(retailCustomerId);
		if (retailCustomer == null) {
			return "redirect:/custodian/retailcustomers";
		}
		// Never round-trip the stored (hashed) password to the edit form.
		retailCustomer.setPassword(null);
		model.put("retailCustomer", retailCustomer);
		model.put("formAction", "/custodian/retailcustomers/" + retailCustomerId + "/update");
		model.put("editMode", true);
		return "custodian/retailcustomers/form";
	}

	@PostMapping("/custodian/retailcustomers/{retailCustomerId}/update")
	public String update(@PathVariable Long retailCustomerId,
			@ModelAttribute("retailCustomer") RetailCustomerEntity form,
			BindingResult result, ModelMap model) {
		RetailCustomerEntity existing = service.findById(retailCustomerId);
		if (existing == null) {
			return "redirect:/custodian/retailcustomers";
		}
		// On edit, password is optional (blank = keep current); the other fields stay required.
		// We validate manually rather than via the create-time @Valid validator.
		rejectIfBlank(result, "username", form.getUsername(), "Username is required");
		rejectIfBlank(result, "firstName", form.getFirstName(), "First name is required");
		rejectIfBlank(result, "lastName", form.getLastName(), "Last name is required");
		if (result.hasErrors()) {
			model.put("formAction", "/custodian/retailcustomers/" + retailCustomerId + "/update");
			model.put("editMode", true);
			return "custodian/retailcustomers/form";
		}
		existing.setUsername(form.getUsername());
		existing.setFirstName(form.getFirstName());
		existing.setLastName(form.getLastName());
		if (form.getRole() != null && !form.getRole().isBlank()) {
			existing.setRole(form.getRole());
		}
		// Checkbox bound via th:field renders a hidden field, so a non-null value always arrives;
		// default to enabled if somehow absent.
		existing.setEnabled(form.getEnabled() != null ? form.getEnabled() : Boolean.TRUE);
		if (form.getPassword() != null && !form.getPassword().isBlank()) {
			existing.setPassword(customerPasswordEncoder.encode(form.getPassword()));
		}
		service.save(existing);
		return "redirect:/custodian/retailcustomers";
	}

	@PostMapping("/custodian/retailcustomers/{retailCustomerId}/delete")
	public String delete(@PathVariable Long retailCustomerId, RedirectAttributes redirectAttributes) {
		try {
			service.deleteById(retailCustomerId);
		}
		catch (DataIntegrityViolationException e) {
			// e.g. the customer still has authorizations/usage points referencing it.
			redirectAttributes.addFlashAttribute("message",
					"Cannot delete this customer because other records still reference it.");
		}
		return "redirect:/custodian/retailcustomers";
	}

	private static void rejectIfBlank(BindingResult result, String field, String value, String message) {
		if (value == null || value.isBlank()) {
			result.rejectValue(field, "field.required", message);
		}
	}

	@GetMapping("/custodian/retailcustomers/{retailCustomerId}/show")
	public String show(@PathVariable Long retailCustomerId, ModelMap model) {
		RetailCustomerEntity retailCustomer = service.findById(retailCustomerId);
		model.put("retailCustomer", retailCustomer);
		return "custodian/retailcustomers/show";
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