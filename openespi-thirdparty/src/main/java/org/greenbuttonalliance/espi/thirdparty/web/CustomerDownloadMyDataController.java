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

import com.sun.syndication.io.FeedException;

import org.greenbuttonalliance.espi.common.service.DtoExportService;
// ExportFilter removed in migration
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

// @Controller - Disabled during migration: ExportService removed
// @PreAuthorize("hasRole('ROLE_USER')")
public class CustomerDownloadMyDataController { // Disabled during migration - TODO: Remove or replace ExportService

	@Autowired
	private DtoExportService dtoExportService; // Replaced ExportService with DtoExportService

	@RequestMapping(value = "/RetailCustomer/download", method = RequestMethod.GET)
	public void downloadMyData(HttpServletResponse response,
			@PathVariable Long retailCustomerId,
			@PathVariable Long usagePointId,
			@RequestParam Map<String, String> params) throws IOException,
			FeedException {
		response.setContentType(MediaType.TEXT_HTML_VALUE);
		response.addHeader("Content-Disposition",
				"attachment; filename=GreenButtonDownload.xml");
		try {

			// TODO: Implement export using DtoExportService
			// ExportFilter functionality needs to be recreated
			// dtoExportService.exportUsagePointFull(retailCustomerId, usagePointId, response.getOutputStream());

		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	@RequestMapping(value = "/RetailCustomer/download/collection", method = RequestMethod.GET)
	public void downloadMyDataCollection(HttpServletResponse response,
			@PathVariable Long retailCustomerId,
			@RequestParam Map<String, String> params) throws IOException,
			FeedException {

		response.setContentType(MediaType.TEXT_HTML_VALUE);
		response.addHeader("Content-Disposition",
				"attachment; filename=GreenButtonDownload.xml");
		try {
			// TODO -- need authorization hook
			// TODO: Implement export using DtoExportService
			// ExportFilter functionality needs to be recreated
			// dtoExportService.exportUsagePointsFull(retailCustomerId, response.getOutputStream());

		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	public void setDtoExportService(DtoExportService dtoExportService) {
		this.dtoExportService = dtoExportService;
	}

	public DtoExportService getDtoExportService() {
		return this.dtoExportService;
	}
}