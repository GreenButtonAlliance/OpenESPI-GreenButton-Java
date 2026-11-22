/*
 *
 *    Copyright (c) 2018-2026 Green Button Alliance, Inc.
 *
 *    Portions (c) 2013-2018 EnergyOS.org
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

package org.greenbuttonalliance.espi.datacustodian.web.api;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import org.greenbuttonalliance.espi.common.domain.common.ServiceCategory;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every ESPI resource response, feed or entry, is {@code application/atom+xml}, and the generated API docs say
 * so; they do not list the internal back-channel or the Actuator endpoints (#312).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ESPI resources are application/atom+xml (#312)")
class EspiAtomMediaTypeIntegrationTest {

	private static final String RESOURCE = "/espi/1_1/resource/";

	/** Every ESPI resource GET the API docs must list, by name, so a dropped or renamed endpoint fails the docs test. */
	private static final List<String> DOCUMENTED_RESOURCE_GETS = List.of(
			RESOURCE + "ApplicationInformation",
			RESOURCE + "ApplicationInformation/{applicationInformationId}",
			RESOURCE + "Authorization",
			RESOURCE + "Authorization/{authorizationId}",
			RESOURCE + "Batch/Subscription/{subscriptionId}",
			RESOURCE + "Customer",
			RESOURCE + "Customer/{customerId}",
			RESOURCE + "CustomerAccount",
			RESOURCE + "CustomerAccount/{customerAccountId}",
			RESOURCE + "ElectricPowerQualitySummary",
			RESOURCE + "ElectricPowerQualitySummary/{electricPowerQualitySummaryId}",
			RESOURCE + "IntervalBlock",
			RESOURCE + "IntervalBlock/{intervalBlockId}",
			RESOURCE + "MeterReading",
			RESOURCE + "MeterReading/{meterReadingId}",
			RESOURCE + "ReadingType",
			RESOURCE + "ReadingType/{readingTypeId}",
			RESOURCE + "UsagePoint",
			RESOURCE + "UsagePoint/{usagePointId}",
			RESOURCE + "UsageSummary",
			RESOURCE + "UsageSummary/{usageSummaryId}",
			RESOURCE + "RetailCustomer/{retailCustomerId}/UsagePoint/{usagePointId}/MeterReading",
			RESOURCE + "RetailCustomer/{retailCustomerId}/UsagePoint/{usagePointId}/MeterReading/{meterReadingId}/IntervalBlock",
			RESOURCE + "RetailCustomer/{retailCustomerId}/UsagePoint/{usagePointId}/UsageSummary",
			RESOURCE + "RetailCustomer/{retailCustomerId}/UsagePoint/{usagePointId}/ElectricPowerQualitySummary");

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private UsagePointRepository usagePointRepository;

	private static RequestPostProcessor admin() {
		return opaqueToken().authorities(new SimpleGrantedAuthority("SCOPE_DataCustodian_Admin_Access"));
	}

	/** The type and subtype only: a charset parameter does not matter. A missing Content-Type fails by name. */
	private static String mediaType(String request, MockHttpServletResponse response) {
		assertThat(response.getContentType()).as("Content-Type of " + request).isNotBlank();
		MediaType type = MediaType.parseMediaType(response.getContentType());
		return type.getType() + "/" + type.getSubtype();
	}

	@ParameterizedTest(name = "GET {0} feed")
	@ValueSource(strings = {"ApplicationInformation", "Authorization", "Customer", "CustomerAccount", "UsagePoint",
			"MeterReading", "ReadingType", "IntervalBlock", "UsageSummary", "ElectricPowerQualitySummary"})
	@DisplayName("every root collection is an application/atom+xml feed, with or without an Accept header")
	void rootCollectionsAreAtom(String resource) throws Exception {
		MockHttpServletResponse asked = mockMvc.perform(get(RESOURCE + resource).with(admin())
						.accept(MediaType.APPLICATION_ATOM_XML))
				.andExpect(status().isOk())
				.andReturn().getResponse();
		MockHttpServletResponse unasked = mockMvc.perform(get(RESOURCE + resource).with(admin()))
				.andExpect(status().isOk())
				.andReturn().getResponse();

		assertThat(List.of(mediaType(resource + " (Accept: Atom)", asked), mediaType(resource + " (no Accept)", unasked)))
				.containsOnly(MediaType.APPLICATION_ATOM_XML_VALUE);
	}

	@Test
	@DisplayName("an entry is application/atom+xml")
	void entryIsAtom() throws Exception {
		UsagePointEntity usagePoint = new UsagePointEntity();
		usagePoint.setId(UUID.fromString("48c2a019-5598-5e16-b0f9-49e4ff27f5fb"));
		usagePoint.setServiceCategory(ServiceCategory.ELECTRICITY);
		usagePointRepository.saveAndFlush(usagePoint);

		MockHttpServletResponse response = mockMvc.perform(get(RESOURCE + "UsagePoint/" + usagePoint.getId())
						.with(admin()).accept(MediaType.ALL))
				.andExpect(status().isOk())
				.andReturn().getResponse();

		assertThat(mediaType("UsagePoint entry", response)).isEqualTo(MediaType.APPLICATION_ATOM_XML_VALUE);
	}

	@ParameterizedTest(name = "Accept: {0}")
	@ValueSource(strings = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE,
			MediaType.TEXT_XML_VALUE})
	@DisplayName("a request that does not accept application/atom+xml is refused (406)")
	void otherMediaTypesAreNotAcceptable(String accept) throws Exception {
		mockMvc.perform(get(RESOURCE + "UsagePoint").with(admin()).accept(accept))
				.andExpect(status().isNotAcceptable());
	}

	@ParameterizedTest(name = "GET UsagePoint?format={0}")
	@ValueSource(strings = {"xml", "json"})
	@DisplayName("a format parameter does not negotiate: without an Accept header the feed is still application/atom+xml")
	void formatParameterDoesNotNegotiate(String format) throws Exception {
		MockHttpServletResponse response = mockMvc.perform(get(RESOURCE + "UsagePoint").param("format", format)
						.with(admin()))
				.andExpect(status().isOk())
				.andReturn().getResponse();

		assertThat(mediaType("UsagePoint?format=" + format, response)).isEqualTo(MediaType.APPLICATION_ATOM_XML_VALUE);
	}

	@Test
	@DisplayName("the API docs give every ESPI resource GET an application/atom+xml 200 response only")
	void apiDocsDeclareAtomForEveryResource() throws Exception {
		String docs = mockMvc.perform(get("/api-docs")).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		Map<String, Map<String, Object>> paths = JsonPath.read(docs, "$.paths");

		List<String> resourceGets = paths.entrySet().stream()
				.filter(e -> e.getKey().startsWith(RESOURCE) && e.getValue().containsKey("get"))
				.map(Map.Entry::getKey)
				.toList();
		// A GET whose 200 response documents no content at all counts as not Atom-only, by name.
		DocumentContext parsed = JsonPath.using(
				Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS)).parse(docs);
		List<String> notAtomOnly = resourceGets.stream()
				.filter(path -> {
					Map<String, Object> content = parsed.read("$.paths['" + path + "'].get.responses['200'].content");
					return content == null
							|| !content.keySet().equals(Set.of(MediaType.APPLICATION_ATOM_XML_VALUE));
				})
				.toList();

		assertThat(resourceGets).as("ESPI resource GETs in the API docs").containsExactlyInAnyOrderElementsOf(
				DOCUMENTED_RESOURCE_GETS);
		assertThat(notAtomOnly).as("ESPI resource GETs whose 200 response is not application/atom+xml only").isEmpty();
	}

	@Test
	@DisplayName("the API docs define the oauth2 scheme the operations require, as a bearer token (Swagger UI's Authorize)")
	void apiDocsDefineTheOauth2BearerScheme() throws Exception {
		String docs = mockMvc.perform(get("/api-docs")).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		Map<String, Object> scheme = JsonPath.read(docs, "$.components.securitySchemes.oauth2");
		List<Object> required = JsonPath.read(docs, "$.paths['" + RESOURCE + "UsagePoint'].get.security[*].oauth2");

		assertThat(scheme).containsEntry("type", "http").containsEntry("scheme", "bearer");
		assertThat(required).hasSize(1);
	}

	@Test
	@DisplayName("the API docs do not list the internal back-channel or the Actuator endpoints")
	void apiDocsHideInternalAndActuator() throws Exception {
		String docs = mockMvc.perform(get("/api-docs")).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		Map<String, Object> paths = JsonPath.read(docs, "$.paths");

		// Only the ESPI resource API is documented (springdoc.paths-to-match), so nothing else is labelled Atom.
		assertThat(paths.keySet())
				.isNotEmpty()
				.noneMatch(path -> path.startsWith("/internal/") || path.startsWith("/actuator"))
				.allMatch(path -> path.startsWith(RESOURCE));
	}
}
