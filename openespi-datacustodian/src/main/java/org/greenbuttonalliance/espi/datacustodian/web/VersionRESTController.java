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

package org.greenbuttonalliance.espi.datacustodian.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Manifest;

@Controller
// @RequestMapping - DISABLED
public class VersionRESTController extends BaseController {

	/**
	 * Handling GET request to retrieve details from MANIFEST.MF file
	 *
	 * @param request
	 *            HTTP Servlet Request
	 * @param model
	 *            ModelMap data
	 * @throws IOException
	 *            Exception thrown by failed or interrupted I/O operations.
	 *
	 * @return implementation details
	 */
	@RequestMapping(value = "/about-version", method = RequestMethod.GET)
	public String getBuildNumber(HttpServletRequest request, ModelMap model)
			throws IOException {

		ServletContext context = request.getSession().getServletContext();
		InputStream manifestStream = context
				.getResourceAsStream("/META-INF/MANIFEST.MF");
		Manifest manifest = new Manifest(manifestStream);

		Map<String, String> aboutInfo = new HashMap<>();
		aboutInfo.put("Implementation-Vendor", manifest.getMainAttributes()
				.getValue("Implementation-Vendor"));
		aboutInfo.put("Implementation-Title", manifest.getMainAttributes()
				.getValue("Implementation-Title"));
		aboutInfo.put("Implementation-Version", manifest.getMainAttributes()
				.getValue("Implementation-Version"));
		aboutInfo.put("Implementation-Jdk", manifest.getMainAttributes()
				.getValue("Build-Jdk"));
		aboutInfo.put("Implementation-Build", manifest.getMainAttributes()
				.getValue("Implementation-Build"));
		aboutInfo.put("Implementation-Build-Time", manifest.getMainAttributes()
				.getValue("Implementation-Build-Time"));

		model.put("aboutInfo", aboutInfo);
		return "/about";

	}

}