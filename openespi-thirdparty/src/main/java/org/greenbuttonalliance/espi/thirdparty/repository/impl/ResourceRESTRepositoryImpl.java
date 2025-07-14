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
import org.greenbuttonalliance.espi.common.domain.common.IdentifiedObject;
import org.greenbuttonalliance.espi.thirdparty.repository.ResourceRESTRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;

@Repository
public class ResourceRESTRepositoryImpl implements ResourceRESTRepository {

	@Autowired
	private WebClient webClient;

	@Autowired
	private OAuth2AuthorizedClientManager authorizedClientManager;

	@Autowired
	@Qualifier(value = "atomMarshaller")
	private Jaxb2Marshaller marshaller;

	public IdentifiedObject get(AuthorizationEntity authorization, String url) {
		// Use OAuth2 client registration to get authorized client
		OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
				.withClientRegistrationId("datacustodian-monthly")
				.principal(authorization.getRetailCustomer().getUsername())
				.build();

		OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);

		// Make authenticated request using WebClient with OAuth2 support
		Mono<String> responseMono = webClient.get()
				.uri(url)
				.attributes(static1 -> static1.put("oauth2AuthorizedClient", authorizedClient))
				.retrieve()
				.bodyToMono(String.class);

		String responseBody = responseMono.block();

		return (IdentifiedObject) marshaller.unmarshal(new StreamSource(
				new StringReader(responseBody)));
	}

	public void setWebClient(WebClient webClient) {
		this.webClient = webClient;
	}

	public WebClient getWebClient() {
		return this.webClient;
	}

	public void setJaxb2Marshaller(Jaxb2Marshaller marshaller) {
		this.marshaller = marshaller;
	}

	public Jaxb2Marshaller getJaxb2Marshaller() {
		return this.marshaller;
	}

}
