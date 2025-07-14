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

package org.greenbuttonalliance.espi.thirdparty.web;

import org.greenbuttonalliance.espi.common.domain.usage.ApplicationInformationEntity;

import org.greenbuttonalliance.espi.common.service.ApplicationInformationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jakarta.xml.bind.JAXBException;
import java.util.ArrayList;
import java.util.List;

// @Controller - Disabled during migration: ResourceService removed
@PreAuthorize("hasRole('ROLE_USER')")
class DataCustodianListController { // Disabled during migration

	@Autowired
	private ApplicationInformationService applicationInformationService;

	@RequestMapping(value = "/DataCustodianList", method = RequestMethod.GET)
	public String index(ModelMap model) throws JAXBException {
		// TODO: Replace with proper ApplicationInformationService methods
		List<ApplicationInformationEntity> applicationInformations = new ArrayList<ApplicationInformationEntity>();
		// List<ApplicationInformationEntity> applicationInformations = applicationInformationService.findAll();
		model.put("applicationInformationList", applicationInformations);
		return "/RetailCustomer/DataCustodianList/index";
	}

	public void setApplicationInformationService(ApplicationInformationService applicationInformationService) {
		this.applicationInformationService = applicationInformationService;
	}

	public ApplicationInformationService getApplicationInformationService() {
		return this.applicationInformationService;
	}

}
