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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto;
import org.greenbuttonalliance.espi.common.dto.usage.UsagePointDto;
import org.greenbuttonalliance.espi.common.mapper.usage.UsagePointMapper;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class UsagePointRESTRepositoryImpl implements UsagePointRESTRepository {
	@Autowired
	@Qualifier("restTemplate")
	private RestTemplate template;

	@Autowired
	private AuthorizationService authorizationService;

	@Autowired
	private UsagePointMapper usagePointMapper;

	@Autowired
	private WebClient webClient;

	private JAXBContext jaxbContext;

	public UsagePointRESTRepositoryImpl() {
		try {
			this.jaxbContext = JAXBContext.newInstance(AtomFeedDto.class);
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

	public void setTemplate(RestTemplate template) {
		this.template = template;
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
				.uri(authorization.getResourceUri())
				.headers(headers -> headers.setBearerAuth(authorization.getAccessToken()))
				.retrieve()
				.bodyToMono(String.class)
				.block();

		// Use openespi-common JAXB unmarshalling
		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		AtomFeedDto feedDto = (AtomFeedDto) unmarshaller.unmarshal(new StringReader(xmlResponse));

		// Use openespi-common mappers for transformation
		List<UsagePointEntity> usagePoints = feedDto.getEntries().stream()
				.map(entry -> {
					UsagePointDto dto = (UsagePointDto) entry.getContent().getResource();
					return usagePointMapper.toEntity(dto);
				})
				.collect(Collectors.toList());

		return usagePoints;
	}

	@Override
	public UsagePointEntity findByHashedId(Long retailCustomerId,
			String usagePointHashedId) throws JAXBException {
		List<UsagePointEntity> usagePoints = findAllByRetailCustomerId(retailCustomerId);

		for (UsagePointEntity usagePoint : usagePoints) {
			if (usagePoint.getHashedId().equalsIgnoreCase(usagePointHashedId)) {
				return usagePoint;
			}
		}

		return null;
	}

	private HttpEntity<String> getUsagePoints(AuthorizationEntity authorization) {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("Authorization",
				"Bearer " + authorization.getAccessToken());
		@SuppressWarnings({ "rawtypes", "unchecked" })
		HttpEntity<?> requestEntity = new HttpEntity(requestHeaders);

		return template.exchange(authorization.getResourceURI(),
				HttpMethod.GET, requestEntity, String.class);
	}

	private AuthorizationEntity findAuthorization(Long retailCustomerId) {
		List<AuthorizationEntity> authorizations = authorizationService
				.findAllByRetailCustomerId(retailCustomerId);
		return authorizations.get(authorizations.size() - 1);
	}

}
