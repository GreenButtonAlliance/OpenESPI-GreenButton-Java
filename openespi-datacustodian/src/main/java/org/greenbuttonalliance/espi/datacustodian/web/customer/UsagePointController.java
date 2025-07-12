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


package org.greenbuttonalliance.espi.datacustodian.web.customer;

import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.domain.usage.ApplicationInformationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.MeterReadingEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsageSummaryEntity;
import org.greenbuttonalliance.espi.common.domain.usage.ElectricPowerQualitySummaryEntity;
import org.greenbuttonalliance.espi.common.domain.usage.TimeConfigurationEntity;
import org.greenbuttonalliance.espi.common.service.ApplicationInformationService;
import org.greenbuttonalliance.espi.common.service.DtoExportService;
import org.greenbuttonalliance.espi.common.repositories.usage.ResourceRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository;
import org.greenbuttonalliance.espi.common.service.RetailCustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

// @Controller - COMMENTED OUT: UI not needed in resource server
// @Component
public class UsagePointController {

	@Autowired
	private UsagePointRepository usagePointService;

	@Autowired
	private ResourceRepository resourceService;

	@Autowired
	private DtoExportService exportService;

	@Autowired
	private ApplicationInformationService applicationInformationService;

	@Autowired
	private RetailCustomerService retailCustomerService;

	@ModelAttribute
	public List<UsagePointEntity> usagePoints(Principal principal) {
		if (principal instanceof Authentication auth) {
			var customer = retailCustomerService.findByUsername(auth.getName());
			if (customer != null) {
				// TODO: Implement findAllByRetailCustomer method in repository
			return new ArrayList<>(); // usagePointService.findAllByRetailCustomer(customer);
			}
		}
		return new ArrayList<>();
	}

	@GetMapping("/RetailCustomer/{retailCustomerId}/UsagePoint")
	public String index() {
		return "/customer/usagepoints/index";
	}

	@Transactional(readOnly = true)
	@GetMapping("/RetailCustomer/{retailCustomerId}/UsagePoint/{usagePointId}/show")
	public String show(@PathVariable Long retailCustomerId,
			@PathVariable Long usagePointId, ModelMap model) {
		try {

			HashMap<String, Object> displayBag = buildDisplayBag(
					retailCustomerId, usagePointId);

			model.put("displayBag", displayBag);

			return "/customer/usagepoints/show";
		} catch (Exception e) {
			model.put("errorString", e.toString());
			try {
				return "/customer/error";
			} catch (Exception ex) {
				return "/customer/error";
			}
		}
	}

	@SuppressWarnings("rawtypes")
	@Transactional(readOnly = true)
	private HashMap<String, Object> buildDisplayBag(Long retailCustomerId,
													Long usagePointId) {

		HashMap<String, Object> displayBag = new HashMap<String, Object>();
		UsagePointEntity usagePoint = resourceService.findById(usagePointId,
				UsagePointEntity.class);
		displayBag.put("Description", usagePoint.getDescription());
		displayBag.put("ServiceCategory", usagePoint.getServiceCategory());
		displayBag.put("Uri", usagePoint.getSelfHref());
		displayBag.put("usagePointId", usagePoint.getId());
		// put the meterReadings
		List<HashMap> meterReadings = new ArrayList<HashMap>();
		Iterator<MeterReadingEntity> it = usagePoint.getMeterReadings().iterator();
		while (it.hasNext()) {
			HashMap<String, Object> mrBag = new HashMap<String, Object>();
			MeterReadingEntity mr = it.next();
			mrBag.put("Description", mr.getDescription());
			// TODO replace the hardcoded 1L in ApplicationInformationId
			String dataCustodianResourceEndpoint = resourceService.findById(1L,
					ApplicationInformationEntity.class)
					.getDataCustodianResourceEndpoint();

			String uriTail = "/RetailCustomer/" + retailCustomerId
					+ "/UsagePoint/" + usagePointId + "/MeterReading/"
					+ mr.getId() + "/show";
			mrBag.put(
					"Uri",
					dataCustodianResourceEndpoint.replace("/espi/1_1/resource",
							"") + uriTail);
			mrBag.put("ReadingType", mr.getReadingType().getDescription());
			meterReadings.add(mrBag);
		}
		displayBag.put("MeterReadings", meterReadings);
		// find the summary rollups
		List<ElectricPowerQualitySummaryEntity> qualitySummaryList = usagePoint
				.getElectricPowerQualitySummaries();
		// TODO: Fix method name - getElectricPowerUsageSummaries() doesn't exist
		List<UsageSummaryEntity> usageSummaryList = new ArrayList<>(); // usagePoint.getUsageSummaries();
		displayBag.put("QualitySummaryList", qualitySummaryList);
		displayBag.put("UsageSummaryList", usageSummaryList);

		TimeConfigurationEntity timeConfiguration = usagePoint
				.getLocalTimeParameters();
		displayBag.put("localTimeParameters", timeConfiguration);
		return displayBag;
	}

	public void setUsagePointRepository(UsagePointRepository usagePointService) {
		this.usagePointService = usagePointService;
	}

	public UsagePointRepository getUsagePointRepository() {
		return this.usagePointService;
	}

	public void setResourceRepository(ResourceRepository resourceService) {
		this.resourceService = resourceService;
	}

	public ResourceRepository getResourceRepository() {
		return this.resourceService;
	}

	public void setDtoExportService(DtoExportService exportService) {
		this.exportService = exportService;
	}

	public DtoExportService getDtoExportService() {
		return this.exportService;
	}

	public void setApplicationInformationService(
			ApplicationInformationService applicationInformationService) {
		this.applicationInformationService = applicationInformationService;
	}

	public ApplicationInformationService getApplicationInformationService() {
		return this.applicationInformationService;
	}

}