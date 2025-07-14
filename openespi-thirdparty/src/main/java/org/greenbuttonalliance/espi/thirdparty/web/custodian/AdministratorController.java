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

import org.greenbuttonalliance.espi.common.domain.usage.ApplicationInformationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.AuthorizationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
// Routes class removed in migration
// import org.greenbuttonalliance.espi.common.domain.Routes;
import org.greenbuttonalliance.espi.common.dto.atom.AtomEntryDto;
// ImportService and ResourceService removed in migration
// import org.greenbuttonalliance.espi.common.service.ImportService;
// import org.greenbuttonalliance.espi.common.service.ResourceService;
import org.greenbuttonalliance.espi.common.service.RetailCustomerService;
import org.greenbuttonalliance.espi.thirdparty.web.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.util.List;

@Controller
@PreAuthorize("hasRole('ROLE_CUSTODIAN')")
public class AdministratorController extends BaseController {

	@Autowired
	private RetailCustomerService service;

	@Autowired
	private WebClient webClient;

	// ResourceService and ImportService removed in migration
	// @Autowired
	// private ResourceService resourceService;

	// @Autowired
	// private ImportService importService;

	@RequestMapping(value = "/espi/1_1/ServiceStatus", method = RequestMethod.GET)
	public String showServiceStatus(ModelMap model) {

		// TODO: Replace with ApplicationInformationService
		// ApplicationInformationEntity applicationInformation = applicationInformationService.findById(1L);
		ApplicationInformationEntity applicationInformation = new ApplicationInformationEntity(); // Placeholder
		String statusUri = applicationInformation
				.getAuthorizationServerAuthorizationEndpoint()
				+ "/ReadServiceStatus";
		// not sure this will work w/o the right seed information
		//
		// TODO: Replace with AuthorizationService
		// AuthorizationEntity authorization = authorizationService.findByResourceUri(statusUri);
		AuthorizationEntity authorization = new AuthorizationEntity(); // Placeholder
		RetailCustomerEntity retailCustomer = authorization.getRetailCustomer();

		String accessToken = authorization.getAccessToken();
		String serviceStatus = "OK";

		try {

			// get the subscription using WebClient
			Mono<String> resultMono = webClient.get()
					.uri(statusUri)
					.headers(headers -> headers.setBearerAuth(accessToken))
					.retrieve()
					.bodyToMono(String.class);

			String result = resultMono.block();

			// import it into the repository
			ByteArrayInputStream bs = new ByteArrayInputStream(result.getBytes());

			// TODO: Replace with modern import/parsing using openespi-common DTOs
			// importService.importData(bs, retailCustomer.getId());

			// List<AtomEntryDto> entries = parseDataUsingDtos(bs);
			// Placeholder for now

			// TODO: Use-Case 1 registration - service status

		} catch (Exception e) {
			// nothing there, so log the fact and move on. It will
			// get imported later.
			e.printStackTrace();
		}
		model.put("serviceStatus", serviceStatus);

		return "/custodian/datacustodian/showservicestatus";
	}

	public void setRetailCustomerService(RetailCustomerService service) {
		this.service = service;
	}

	public RetailCustomerService getRetailCustomerService() {
		return this.service;
	}

	public void setWebClient(WebClient webClient) {
		this.webClient = webClient;
	}

	public WebClient getWebClient() {
		return this.webClient;
	}

	// ResourceService and ImportService removed in migration
	// public void setResourceService(ResourceService resourceService) {
	// 	this.resourceService = resourceService;
	// }

	// public ResourceService getResourceService() {
	// 	return this.resourceService;
	// }

	// public void setImportService(ImportService importService) {
	// 	this.importService = importService;
	// }

	// public ImportService getImportService() {
	// 	return this.importService;
	// }

}
