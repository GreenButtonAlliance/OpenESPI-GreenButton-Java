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

import org.greenbuttonalliance.espi.common.domain.usage.BatchListEntity;
import org.greenbuttonalliance.espi.common.dto.usage.BatchListDto;
import org.greenbuttonalliance.espi.common.service.*;
import org.greenbuttonalliance.espi.common.xml.BatchListXmlCodec;
import org.greenbuttonalliance.espi.thirdparty.service.WebClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.function.client.WebClient;

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

	@Autowired
	private WebClient webClient;

	private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

	@Autowired
	private AuthorizationService authorizationService;

	@PostMapping("/espi/1_1/Notification") // TODO: Use Routes.THIRD_PARTY_NOTIFICATION when available
	public ResponseEntity<Void> notification(@RequestBody String xmlPayload) {

		BatchListDto batchList;
		try {
			// Parse the ESPI BatchList through the single canonical codec (#158) — the wire type is
			// the JAXB DTO, not the JPA entity (per the project's strict JAXB/JPA separation rule).
			batchList = BatchListXmlCodec.unmarshal(xmlPayload);
		} catch (Exception e) {
			// A payload we cannot parse is a bad request, not a server fault.
			logger.warn("Notification: unparseable BatchList payload", e);
			return ResponseEntity.badRequest().build();
		}

		try {
			batchListService.save(new BatchListEntity(batchList.getResources()));

			for (String resourceUri : batchList.getResources()) {
				importResource(resourceUri);
			}

			logger.info("Successfully processed notification with {} resources", batchList.getResources().size());
			return ResponseEntity.ok().build();
		} catch (Exception e) {
			logger.error("Error processing notification", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Pull and import a single source URL advertised in the received BatchList.
	 *
	 * <p>ESPI flow: each {@code <resource>} element is a URL; the Third Party — acting as an OAuth
	 * client — performs an authenticated {@code GET} on that URL to retrieve the resource data from
	 * the Data Custodian, then persists it. (The inbound notification POST itself is not OAuth-
	 * protected — it is secured at the transport layer, TLS; the access token is required only on
	 * this outbound fetch.)</p>
	 *
	 * <p>The GET is issued through the OAuth2-enabled {@link WebClient}, which attaches the access
	 * token of the authorized client. Two pieces complete a fully-functional fetch: selecting the
	 * correct OAuth token for the resource's Authorization (the unattended-notification token source,
	 * #146) and unmarshalling/persisting the returned ESPI payload (the import pipeline, #89). A fetch
	 * failure for one resource is logged and does not abort the others.</p>
	 *
	 * <p>(The legacy {@code sftp://} delivery branch has been removed: the current ESPI standard no
	 * longer permits SFTP notification delivery.)</p>
	 */
	protected void importResource(String resourceUri) {
		try {
			// As an OAuth client, GET the source URL with an access token (attached by the
			// OAuth2-enabled WebClient) to obtain the resource data.
			String payload = webClient.get()
					.uri(resourceUri)
					.retrieve()
					.bodyToMono(String.class)
					.block();

			// TODO(#89): unmarshal the returned ESPI Atom payload and persist the resources.
			logger.info("Notification: fetched resource {} ({} bytes) for import",
					resourceUri, payload == null ? 0 : payload.length());
		}
		catch (Exception e) {
			// e.g. no OAuth token resolvable for this resource yet (#146), or the DC returns 401/4xx.
			logger.warn("Notification: could not fetch resource {} for import: {}",
					resourceUri, e.getMessage());
		}
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
}
