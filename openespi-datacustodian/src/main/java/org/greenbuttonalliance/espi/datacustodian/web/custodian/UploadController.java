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

import org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.xml.sax.SAXException;

import jakarta.xml.bind.JAXBException;
import java.io.IOException;

// @Controller - COMMENTED OUT: UI not needed in resource server
// @Component
@RequestMapping("/custodian/upload")
public class UploadController {

	// @Autowired
	// private ImportService importService; // TODO: Implement

	@Autowired
	private UsagePointRepository usagePointService;

	// @Autowired
	// private NotificationService notificationService; // TODO: Implement

	@ModelAttribute("uploadForm")
	public UploadForm uploadForm() {
		return new UploadForm();
	}

	@GetMapping
	public String upload() {
		return "/custodian/upload";
	}

	@PostMapping
	public String uploadPost(@ModelAttribute UploadForm uploadForm,
			BindingResult result) throws IOException, JAXBException {
		
		try {
			// TODO: Implement ImportService
			// importService.importData(uploadForm.getFile().getInputStream(), null);
			result.addError(new ObjectError("uploadForm", "Import functionality not yet implemented"));
			return "/custodian/upload";
			
		} catch (Exception e) {
				
			result.addError(new ObjectError("uploadForm",
						"Unable to process file"));
			return "/custodian/upload";
		} 
	}

	// public void setImportService(ImportService importService) {
	//	this.importService = importService;
	// }

	// public ImportService getImportService() {
	//	return this.importService;
	// }

	public void setUsagePointRepository(UsagePointRepository usagePointService) {
		this.usagePointService = usagePointService;
	}

	public UsagePointRepository getUsagePointRepository() {
		return this.usagePointService;
	}

	// public void setNotificationService(NotificationService notificationService) {
	//	this.notificationService = notificationService;
	// }

	// public NotificationService getNotificationService() {
	//	return this.notificationService;
	// }

}
