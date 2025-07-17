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

package org.greenbuttonalliance.espi.common.service.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.greenbuttonalliance.espi.common.domain.usage.ApplicationInformationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.AuthorizationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.BatchListEntity;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.domain.usage.SubscriptionEntity;
import org.greenbuttonalliance.espi.common.service.AuthorizationService;
import org.greenbuttonalliance.espi.common.service.NotificationService;
import org.greenbuttonalliance.espi.common.service.SubscriptionService;
import org.greenbuttonalliance.espi.common.repositories.usage.AuthorizationRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

/**
 * @author John Teeter
 *
 */
@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {
	private final Log logger = LogFactory.getLog(getClass());

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private AuthorizationRepository authorizationRepository;

	@Autowired
	private AuthorizationService authorizationService;

	@Autowired
	private SubscriptionService subscriptionService;

	@Override
	public void notify(SubscriptionEntity subscription,
					   XMLGregorianCalendar startDate, XMLGregorianCalendar endDate) {
		
		String thirdPartyNotificationURI = subscription
				.getApplicationInformationEntity().getThirdPartyNotifyUri();
		String separator = "?";
		String subscriptionURI = subscription.getApplicationInformationEntity()
				.getDataCustodianResourceEndpoint()
				+ "/Batch/Subscription/"
				+ subscription.getId();
		
		if (startDate != null) {
			subscriptionURI = subscriptionURI + separator + "published-min="
					+ startDate.toXMLFormat();
			separator = "&";
		}
		
		if (endDate != null) {
			subscriptionURI = subscriptionURI + separator + "published-max="
					+ endDate.toXMLFormat();
		}

		BatchListEntity batchList = new BatchListEntity();
		batchList.getResources().add(subscriptionURI);
		notifyInternal(thirdPartyNotificationURI, batchList);
	}

	@Override
	public void notify(RetailCustomerEntity retailCustomer,
					   XMLGregorianCalendar startDate, XMLGregorianCalendar endDate) {

		if (retailCustomer != null) {

			SubscriptionEntity subscription = null;

			// find and iterate across all relevant authorizations
			List<AuthorizationEntity> authorizationList = authorizationService
					.findAllByRetailCustomerId(retailCustomer.getId());
			Iterator<AuthorizationEntity> authorizationIterator = authorizationList
					.iterator();

			while (authorizationIterator.hasNext()) {
				AuthorizationEntity authorization = authorizationIterator.next();

				try {
					subscription = subscriptionService
							.findByAuthorizationId(authorization.getId());
				} catch (Exception e) {
					// an Authorization w/o an associated subscription breaks
					// the propagation chain
					// TODO: if we want to continue the propagation forward, we
					// just need to hook in the subscription substructure

				}
				if (subscription != null) {
					notify(subscription, startDate, endDate);
				}
			}
		}
	}

	@Async
	// we want to spawn this to a separate thread so we can get back
	// and commit the actual data import transaction
	private void notifyInternal(String thirdPartyNotificationURI,
			BatchListEntity batchList) {

		if(thirdPartyNotificationURI != null){
			try {
				restTemplate.postForLocation(thirdPartyNotificationURI, batchList);
			} catch (Exception e) {
				if(logger.isErrorEnabled()) {
					logger.info("NotificationServiceImpl: notifyInternal - POST for " + thirdPartyNotificationURI +
							" caused an " + e.getMessage() + " Exception&n");
				}
			}
		}
	}

	@Override
	public void notifyAllNeed() {

		List<UUID> authList = authorizationRepository.findAllIds();

		Map<UUID, BatchListEntity> notifyList = new HashMap<UUID, BatchListEntity>();

		for (UUID id : authList) {

			AuthorizationEntity authorization = authorizationRepository.findById(id).orElse(null);
			if (authorization == null) continue;

			String tempResourceUri = authorization.getResourceURI();

			if(logger.isInfoEnabled()) {
				logger.info("NotificationServiceImpl: notifyAllNeed - resourceURI: " + tempResourceUri);
			}

			// Ignore client_access_tokens which contain "/Batch/Bulk/
			// for their ResourceUri values
			if(!tempResourceUri.contains("/Batch/Bulk/")) {
				
				try {
					// Modern approach: validate authorization exists
					authorizationRepository.findById(id);
				
				} catch (Exception ex) {
				
					if(logger.isErrorEnabled()) {
						logger.error("NotificationServiceImpl: notifyAllNeed - Processing Authorization: " + id +
								", Resource: " + tempResourceUri + ", Exception Cause: " + ex.getCause() +
								", Exception Message: " + ex.getMessage() + "&n");
					}
				}
			}

			String thirdParty = authorization.getThirdParty();

			// do not do any of the local authorizations
			//
			if (!((thirdParty.equals("data_custodian_admin")) || (thirdParty
					.equals("upload_admin")))) {

				// if this is the first time we have seen this third party, add
				// it to the notification list.
				if (!(notifyList.containsKey(thirdParty))) {
					notifyList.put(id, new BatchListEntity());
				}

				// and now add the appropriate resource URIs to the batchList of
				// this third party
				//
				String resourceUri = authorization.getResourceURI();

				// resourceUri's that contain /Batch/Bulk are
				// client-access-token and will be ignored here with the
				// actual Batch/Bulk ids will be picked up by looking at the
				// scope strings of the individual
				// authorization/subscription pairs
				if (!(resourceUri.contains("/Batch/Bulk"))) {
					String scope = authorization.getScope();
					for (String term : scope.split(";")) {
						if (term.contains("BR=")) {
							// we have a bulkId to deal with
							term = term.substring(scope.indexOf("=") + 1);
							// TODO the following getResourceURI() should be
							// changed to getBulkRequestURI when the seed tables
							// have non-null values for that attribute.
							String bulkResourceUri = authorization
									.getResourceURI() + "/Batch/Bulk/" + term;
							if (!(notifyList.get(id).getResources()
									.contains(bulkResourceUri))) {
								notifyList.get(id).getResources()
										.add(bulkResourceUri);
							}
						} else {
							// just add the resourceUri
							if (!(notifyList.get(id).getResources()
									.contains(resourceUri))) {
								notifyList.get(id).getResources()
										.add(resourceUri);
							}
						}
					}
				}

			}
		}

		// now notify each ThirdParty
		for (Entry<UUID, BatchListEntity> entry : notifyList.entrySet()) {
			Optional<AuthorizationEntity> authOpt = authorizationRepository.findById(entry.getKey());
			if (authOpt.isPresent()) {
				String notifyUri = authOpt.get().getApplicationInformationEntity().getThirdPartyNotifyUri();
				BatchListEntity batchList = entry.getValue();
				if (!(batchList.getResources().isEmpty())) {
					notifyInternal(notifyUri, batchList);
				}
			}
		}
	}

	public void setRestTemplate(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@Override
	public void notify(ApplicationInformationEntity applicationInformation,
					   Long bulkId) {
		String bulkRequestUri = applicationInformation
				.getDataCustodianBulkRequestURI() + "/" + bulkId;
		String thirdPartyNotificationURI = applicationInformation
				.getThirdPartyNotifyUri();
		BatchListEntity batchList = new BatchListEntity();
		batchList.getResources().add(bulkRequestUri);

		notifyInternal(thirdPartyNotificationURI, batchList);

	}

	public RestTemplate getRestTemplate() {
		return this.restTemplate;
	}

	// Legacy ResourceService methods removed - using modern repositories and services

	public void setAuthorizationService(
			AuthorizationService authorizationService) {
		this.authorizationService = authorizationService;
	}

	public AuthorizationService getAuthorizationService() {
		return this.authorizationService;
	}

	public void setSubscriptionService(SubscriptionService subscriptionService) {
		this.subscriptionService = subscriptionService;
	}

	public SubscriptionService getSubscriptionService() {
		return this.subscriptionService;
	}

}
