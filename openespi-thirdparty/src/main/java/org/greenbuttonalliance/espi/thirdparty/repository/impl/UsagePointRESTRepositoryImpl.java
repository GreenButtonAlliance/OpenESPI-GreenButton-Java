/*
 *
 *        Copyright (c) 2025-2026 Green Button Alliance, Inc.
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

package org.greenbuttonalliance.espi.thirdparty.repository.impl;

import org.greenbuttonalliance.espi.common.domain.usage.AuthorizationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository;
import org.greenbuttonalliance.espi.common.service.AuthorizationService;
// ImportService removed in migration
import org.greenbuttonalliance.espi.common.service.RetailCustomerService;
// UsagePointService removed in migration
import org.greenbuttonalliance.espi.thirdparty.repository.UsagePointRESTRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;
import org.greenbuttonalliance.espi.common.dto.atom.AtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto;
import org.greenbuttonalliance.espi.common.dto.atom.UsageAtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.usage.UsagePointDto;
import org.greenbuttonalliance.espi.common.mapper.usage.UsagePointMapper;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class UsagePointRESTRepositoryImpl implements UsagePointRESTRepository {

	private static final Logger log = LoggerFactory.getLogger(UsagePointRESTRepositoryImpl.class);
	private static final String URN_UUID = "urn:uuid:";

	@Autowired
	private AuthorizationService authorizationService;

	@Autowired
	private UsagePointMapper usagePointMapper;

	@Autowired
	private WebClient webClient;

	private JAXBContext jaxbContext;

	public UsagePointRESTRepositoryImpl() {
		try {
			// AtomFeedDto.entries is an @XmlElementRef to the abstract AtomEntryDto (#247), so the concrete
			// entry type must be in the context; the Third Party reads usage-domain feeds.
			this.jaxbContext = JAXBContext.newInstance(AtomFeedDto.class, UsageAtomEntryDto.class);
		} catch (JAXBException e) {
			throw new RuntimeException("Failed to initialize JAXB context", e);
		}
	}

	@Autowired
	private UsagePointRepository usagePointRepository;

	@Autowired
	private RetailCustomerService retailCustomerService;

	// services initializers
	//
	public void setUsagePointMapper(UsagePointMapper usagePointMapper) {
		this.usagePointMapper = usagePointMapper;
	}

	public void setWebClient(WebClient webClient) {
		this.webClient = webClient;
	}

	public void setRetailCustomerService(
			RetailCustomerService retailCustomerService) {
		this.retailCustomerService = retailCustomerService;
	}

	public void setUsagePointRepository(
			UsagePointRepository usagePointRepository) {
		this.usagePointRepository = usagePointRepository;
	}


	public void setAuthorizationService(
			AuthorizationService authorizationService) {
		this.authorizationService = authorizationService;
	}

	@Override
	public List<UsagePointEntity> findAllByRetailCustomerId(Long retailCustomerId)
			throws JAXBException {

		AuthorizationEntity authorization = findAuthorization(retailCustomerId);
		
		// Make OAuth2 REST call to data custodian
		String xmlResponse = webClient.get()
				.uri(authorization.getResourceURI())
				.headers(headers -> headers.setBearerAuth(authorization.getAccessToken()))
				.retrieve()
				.bodyToMono(String.class)
				.block();

		// Use openespi-common JAXB unmarshalling
		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		AtomFeedDto feedDto = (AtomFeedDto) unmarshaller.unmarshal(new StringReader(xmlResponse));

		// An empty feed has no entries at all (#197). Each entry's resource is the child of its
		// atom:content; a feed may also carry other resources (MeterReading, ReadingType, ...), which are
		// not UsagePoints.
		if (feedDto == null || feedDto.getEntries() == null) {
			return new ArrayList<>();
		}
		List<UsagePointEntity> usagePoints = new ArrayList<>();
		int skipped = 0;
		for (AtomEntryDto entry : feedDto.getEntries()) {
			if (entry.getResource() instanceof UsagePointDto dto) {
				usagePoints.add(toEntity(entry, dto));
			} else {
				skipped++;
			}
		}
		if (skipped > 0) {
			// Counts only: a feed may carry customer PII, so its content is never logged.
			log.warn("{} of {} feed entries from {} are not UsagePoints (another resource, or no atom:content)",
					skipped, feedDto.getEntries().size(), authorization.getResourceURI());
		}
		return usagePoints;
	}

	/**
	 * The mapper copies only the espi.xsd UsagePoint fields; the resource's id is the entry's
	 * {@code atom:id} ({@code urn:uuid:...}), so it is set here.
	 */
	private UsagePointEntity toEntity(AtomEntryDto entry, UsagePointDto dto) {
		UsagePointEntity entity = usagePointMapper.toEntity(dto);
		entity.setId(entryUuid(entry.getId()));
		return entity;
	}

	static UUID entryUuid(String atomId) {
		if (atomId == null) {
			return null;
		}
		String value = atomId.strip();
		if (value.regionMatches(true, 0, URN_UUID, 0, URN_UUID.length())) {
			value = value.substring(URN_UUID.length());
		}
		try {
			return UUID.fromString(value);
		} catch (IllegalArgumentException ex) {
			log.warn("Feed entry id is not a urn:uuid; the UsagePoint has no id");
			return null;
		}
	}

	@Override
	public UsagePointEntity findByHashedId(Long retailCustomerId,
			String usagePointHashedId) throws JAXBException {
		List<UsagePointEntity> usagePoints = findAllByRetailCustomerId(retailCustomerId);

		for (UsagePointEntity usagePoint : usagePoints) {
			// An entry whose atom:id is not a urn:uuid gives a UsagePoint with no id.
			if (usagePoint.getHashedId() != null && usagePoint.getHashedId().equalsIgnoreCase(usagePointHashedId)) {
				return usagePoint;
			}
		}

		return null;
	}


	private AuthorizationEntity findAuthorization(Long retailCustomerId) {
		List<AuthorizationEntity> authorizations = authorizationService
				.findAllByRetailCustomerId(retailCustomerId);
		return authorizations.get(authorizations.size() - 1);
	}

}
