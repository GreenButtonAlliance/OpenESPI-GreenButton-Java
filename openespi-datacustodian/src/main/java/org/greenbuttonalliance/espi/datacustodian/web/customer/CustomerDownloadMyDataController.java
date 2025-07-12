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

import org.greenbuttonalliance.espi.common.service.DtoExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

// @Controller - COMMENTED OUT: UI not needed in resource server
// @Component
@PreAuthorize("hasRole('ROLE_USER')")
public class CustomerDownloadMyDataController {

	@Autowired
	private DtoExportService exportService;

	@GetMapping("/RetailCustomer/{retailCustomerId}/DownloadMyData/UsagePoint/{usagePointId}")
	public void downloadMyData(HttpServletResponse response,
			@PathVariable Long retailCustomerId,
			@PathVariable Long usagePointId,
			@RequestParam Map<String, String> params) throws IOException {
		response.setContentType(MediaType.APPLICATION_XML_VALUE);
		response.addHeader("Content-Disposition",
				"attachment; filename=GreenButtonDownload.xml");
		try {
			// TODO: Implement export methods in DtoExportService
			// exportService.exportUsagePointFull(0L, retailCustomerId, usagePointId, response.getOutputStream(), new ExportFilter(params));
			response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
			response.getWriter().write("Download My Data not yet implemented");
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	@GetMapping("/RetailCustomer/{retailCustomerId}/DownloadMyData/UsagePoint")
	public void downloadMyDataCollection(HttpServletResponse response,
			@PathVariable Long retailCustomerId,
			@RequestParam Map<String, String> params) throws IOException {

		response.setContentType(MediaType.APPLICATION_XML_VALUE);
		response.addHeader("Content-Disposition",
				"attachment; filename=GreenButtonDownload.xml");
		try {
			// TODO: Implement export methods in DtoExportService  
			// exportService.exportUsagePointsFull(0L, retailCustomerId, response.getOutputStream(), params);
			response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
			response.getWriter().write("Download My Data Collection not yet implemented");

		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	public void setDtoExportService(DtoExportService exportService) {
		this.exportService = exportService;
	}

	public DtoExportService getDtoExportService() {
		return this.exportService;
	}
}