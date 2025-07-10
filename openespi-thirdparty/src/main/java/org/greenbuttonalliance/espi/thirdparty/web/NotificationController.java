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

import org.greenbuttonalliance.espi.common.domain.usage.AuthorizationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.BatchListEntity;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
//  // TODO: Find correct Routes import
import org.greenbuttonalliance.espi.common.service.*;
import org.greenbuttonalliance.espi.thirdparty.service.WebClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.*;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import jakarta.servlet.http.HttpServletResponse;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

@Controller
public class NotificationController extends BaseController {

	@Autowired
	private BatchListService batchListService;

	// @Autowired
	// private ResourceService resourceService;

	// @Autowired
	// private UsagePointService usagePointService;

	// @Autowired
	// private ImportService importService;

	@Autowired
	private WebClientService webClientService;

	private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

	@Autowired
	private AuthorizationService authorizationService;

	@Autowired
	@Qualifier(value = "atomMarshaller")
	public Jaxb2Marshaller marshaller;

	@PostMapping("/espi/1_1/Notification") // TODO: Use Routes.THIRD_PARTY_NOTIFICATION when available
	public ResponseEntity<Void> notification(@RequestBody String xmlPayload) {

		try {
			ByteArrayInputStream inputStream = new ByteArrayInputStream(xmlPayload.getBytes());
			BatchListEntity batchList = (BatchListEntity) marshaller.unmarshal(new StreamSource(inputStream));

			batchListService.persist(batchList);

			for (String resourceUri : batchList.getResources()) {
				doImportAsynchronously(resourceUri);
			}

			logger.info("Successfully processed notification with {} resources", batchList.getResources().size());
			return ResponseEntity.ok().build();
		} catch (Exception e) {
			logger.error("Error processing notification", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@Async
	private void doImportAsynchronously(String subscriptionUri) {

		// The import related to a subscription is performed here (in a separate
		// thread)
		// This must be provably secure b/c the access_token is visible here
		String threadName = Thread.currentThread().getName();
		logger.debug("Start Asynchronous Input: {}: {}", threadName, subscriptionUri);

		String resourceUri = subscriptionUri;
		String accessToken = "";
		AuthorizationEntity authorization = null;
		RetailCustomerEntity retailCustomer = null;

		if (subscriptionUri.indexOf("?") > -1) { // Does message contain a query
													// element
			resourceUri = subscriptionUri.substring(0,
					subscriptionUri.indexOf("?")); // Yes, remove the query
													// element
		}
		if (resourceUri.contains("sftp://")) {

			try {
				String command = "sftp mget "
						+ resourceUri.substring(resourceUri.indexOf("sftp://"));

				logger.info("[Manage] Restricted Management Interface");
				logger.info("[Manage] Request: {}", command);

				Process p = Runtime.getRuntime().exec(command);

				// the sftp script will get the file and make a RESTful api call
				// to add it into the workspace.

			} catch (IOException e1) {
				logger.error("**** [Manage] IO Error: {}", e1.toString());

			} catch (Exception e) {
				logger.error("**** [Manage] Error: {}", e.toString());
			}

		} else {
			try {
				if ((resourceUri.contains("/Batch/Bulk"))
						|| (resourceUri.contains("/Authorization"))) {
					// mutate the resourceUri to be of the form .../Batch/Bulk
					resourceUri = (resourceUri.substring(
							0,
							resourceUri.indexOf("/resource/")
									+ "/resource/".length())
							.concat("Batch/Bulk"));

				} else {
					if (resourceUri.contains("/Subscription")) {
						// mutate the resourceUri for the form
						// /Subscription/{subscriptionId}/**
						String temp = resourceUri.substring(resourceUri
								.indexOf("/Subscription/")
								+ "/Subscription/".length());
						if (temp.contains("/")) {
							resourceUri = resourceUri.substring(
									0,
									resourceUri.indexOf("/Subscription")
											+ "/Subscription".length()).concat(
									temp.substring(0, temp.indexOf("/")));
						}
					}
				}

				// Authorization x = resourceService.findById(2L,
				// 		AuthorizationEntity.class);

				// if (x.getResourceURI().equals(resourceUri)) {
				// 	logger.debug("ResourceURIs Equal: {}", resourceUri);
				// } else {
				// 	logger.debug("ResourceURIs Not Equal: {}", resourceUri);
				// }
				// authorization = resourceService.findByResourceUri(resourceUri,
				// 		AuthorizationEntity.class);
				// retailCustomer = authorization.getRetailCustomer();
				// accessToken = authorization.getAccessToken();

				try {
					// Create authenticated WebClient for resource access
					WebClient authenticatedClient = webClientService.createAuthenticatedWebClient(accessToken);

					// Get the subscription data using WebClient
					String responseBody = webClientService.getForObject(
						authenticatedClient, subscriptionUri, String.class);

					// if (responseBody != null) {
					// 	// Import data into the repository
					// 	ByteArrayInputStream bs = new ByteArrayInputStream(responseBody.getBytes());
					// 	importService.importData(bs, retailCustomer.getId());
					// 	logger.debug("Successfully imported data from subscription: {}", subscriptionUri);
					// } else {
					// 	logger.warn("No data received from subscription: {}", subscriptionUri);
					// }

				} catch (WebClientResponseException e) {
					logger.error("HTTP error during subscription import: {} - {}", 
						e.getStatusCode(), e.getResponseBodyAsString());
				} catch (Exception e) {
					logger.error("Error during asynchronous import from subscription: {}", subscriptionUri, e);
				}

			} catch (EmptyResultDataAccessException e) {
				// No authorization found - data will be imported later when authorization is available
				logger.info("No authorization found for resource URI: {} - will import later", resourceUri);
			}
		}

		logger.debug("Asynchronous import completed for thread {}: {}", threadName, resourceUri);
	}

	public void setBatchListService(BatchListService batchListService) {
		this.batchListService = batchListService;
	}

	// public void setImportService(ImportService importService) {
	// 	this.importService = importService;
	// }

	// public void setResourceService(ResourceService resourceService) {
	// 	this.resourceService = resourceService;
	// }

	// public void setUsagePointService(UsagePointService usagePointService) {
	// 	this.usagePointService = usagePointService;
	// }

	public WebClient getWebClient() {
		return webClient;
	}

	public void setMarshaller(Jaxb2Marshaller marshaller) {
		this.marshaller = marshaller;
	}
}
