
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

import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.domain.usage.SubscriptionEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.SubscriptionRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository;
import org.greenbuttonalliance.espi.common.service.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(rollbackFor = { jakarta.xml.bind.JAXBException.class }, noRollbackFor = {
		jakarta.persistence.NoResultException.class,
		org.springframework.dao.EmptyResultDataAccessException.class })
public class SubscriptionServiceImpl implements SubscriptionService {

	private final Log logger = LogFactory.getLog(getClass());

	@Autowired
	private SubscriptionRepository subscriptionRepository;

	@Autowired
	private UsagePointRepository usagePointRepository;

	@Autowired
	private ApplicationInformationService applicationInformationService;

	@Autowired
	private RetailCustomerService retailCustomerService;

	@Override
	public SubscriptionEntity createSubscription(String username, Set<String> roles, String clientId) {
		SubscriptionEntity subscription = new SubscriptionEntity();
		subscription.setUuid(UUID.randomUUID());

		if (roles.contains("ROLE_USER")) {
			// For user-based subscriptions, find the retail customer by username
			RetailCustomerEntity retailCustomer = retailCustomerService.findByUsername(username);
			if (retailCustomer != null) {
				subscription.setRetailCustomerEntity(retailCustomer);
				subscription.setUsagePointEntities(new ArrayList<UsagePointEntity>());
				// TODO - scope this to only a selected (proper) subset of the
				// usagePoints as passed through from the UX or a restful call.
				List<UsagePointEntity> usagePoints = usagePointRepository.findAllByRetailCustomerEntity(retailCustomer);
				subscription.setUsagePointEntities(usagePoints);
			}
		} else {
			// For client-based subscriptions, process the client ID
			String ci = clientId;
			if (ci != null) {
				if (ci.indexOf("REGISTRATION_") != -1) {
					if (ci.substring(0, "REGISTRATION_".length()).equals(
							"REGISTRATION_")) {
						ci = ci.substring("REGISTRATION_".length());
					}
				}
				if (ci.indexOf("_admin") != -1) {
					ci = ci.substring(0, ci.indexOf("_admin"));
				}
				subscription.setApplicationInformationEntity(applicationInformationService.findByClientId(ci));
			}
			subscription.setRetailCustomerEntity(retailCustomerService.findById((long) 0));
		}
		subscription.setLastUpdateTime(new GregorianCalendar());
		subscriptionRepository.save(subscription);

		logger.info("Created subscription for username: " + username);
		return subscription;
	}

	@Override
	public SubscriptionEntity findByHashedId(String hashedId) {
		return subscriptionRepository.findByHashedId(hashedId).orElse(null);
	}

	@Override
	public void setRepository(SubscriptionRepository subscriptionRepository) {
		this.subscriptionRepository = subscriptionRepository;
	}

	@Override
	public SubscriptionEntity save(SubscriptionEntity subscription) {
		return subscriptionRepository.save(subscription);
	}

	@Override
	public SubscriptionEntity findById(Long subscriptionId) {
		return subscriptionRepository.findById(subscriptionId).orElse(null);
	}

	@Override
	public List<Long> findUsagePointIds(Long subscriptionId) {
		List<Long> result = new ArrayList<Long>();
		SubscriptionEntity subscription = findById(subscriptionId);
		if (subscription != null && subscription.getUsagePointEntities() != null) {
			for (UsagePointEntity up : subscription.getUsagePointEntities()) {
				result.add(up.getId());
			}
		}
		return result;
	}

	@Override
	public SubscriptionEntity findByAuthorizationId(Long id) {
		return subscriptionRepository.findByAuthorizationId(id).orElse(null);
	}

	@Override
	public SubscriptionEntity addUsagePoint(SubscriptionEntity subscription,
			UsagePointEntity usagePoint) {
		if (subscription.getUsagePointEntities() == null) {
			subscription.setUsagePointEntities(new ArrayList<UsagePointEntity>());
		}
		subscription.getUsagePointEntities().add(usagePoint);
		logger.info("Added usage point " + usagePoint.getId() + " to subscription " + subscription.getId());
		return subscription;
	}

	@Override
	public Long findRetailCustomerId(Long subscriptionId, Long usagePointId) {
		Long result = null;
		SubscriptionEntity subscription = findById(subscriptionId);
		if (subscription != null && subscription.getRetailCustomerEntity() != null) {
			result = subscription.getRetailCustomerEntity().getId();
			if (result.equals(0L)) {
				// we have a subscription that is based upon client credentials
				// now we must find the actual retail customer associated with
				// this particular usagePoint
				UsagePointEntity usagePoint = usagePointRepository.findById(usagePointId).orElse(null);
				if (usagePoint != null && usagePoint.getRetailCustomerEntity() != null) {
					result = usagePoint.getRetailCustomerEntity().getId();
				}
			}
		}
		return result;
	}


}
