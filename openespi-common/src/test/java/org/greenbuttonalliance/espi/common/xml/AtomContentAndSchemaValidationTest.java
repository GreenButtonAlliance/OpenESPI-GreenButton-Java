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

package org.greenbuttonalliance.espi.common.xml;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto;
import org.greenbuttonalliance.espi.common.dto.atom.CustomerAtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.atom.LinkDto;
import org.greenbuttonalliance.espi.common.dto.atom.UsageAtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.customer.CustomerDto;
import org.greenbuttonalliance.espi.common.dto.usage.UsagePointDto;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Atom entries carry their ESPI resource inside {@code atom:content}, as REQ.21 and certified Data
 * Custodian feeds do, and {@link EspiXmlSchemaValidator} checks a document against atom.xsd, espi.xsd and
 * customer.xsd (#197).
 */
class AtomContentAndSchemaValidationTest {

	private static final OffsetDateTime TIME = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
	private static final String UP_ID = "urn:uuid:07171718-b165-522a-b413-11d9eeb1ca43";
	private static final String BASE = "https://dc.example/DataCustodian/espi/1_1/resource";

	/**
	 * The shape of a certified Data Custodian's feed (from a CMD certification ScriptLog), with synthetic
	 * values: each resource inside atom:content, ServiceCategory as a numeric kind.
	 */
	private static final String CERTIFIED_STYLE_FEED = """
			<?xml version="1.0" encoding="UTF-8"?>
			<feed xmlns="http://www.w3.org/2005/Atom" xmlns:espi="http://naesb.org/espi">
			   <id>urn:uuid:3f1c2b2e-9d3a-5b1e-8f0e-000000000001</id>
			   <title>Green Button Usage Feed</title>
			   <updated>2026-01-01T00:00:00Z</updated>
			   <link rel="self" href="https://dc.example/DataCustodian/espi/1_1/resource/Batch/Subscription/1" type="espi-entry/Subscription"/>
			   <entry>
			      <id>urn:uuid:07171718-b165-522a-b413-11d9eeb1ca43</id>
			      <link rel="self" href="https://dc.example/DataCustodian/espi/1_1/resource/Subscription/1/UsagePoint/1" type="espi-entry/UsagePoint"/>
			      <link rel="up" href="https://dc.example/DataCustodian/espi/1_1/resource/Subscription/1/UsagePoint" type="espi-feed/UsagePoint"/>
			      <title>UsagePoint</title>
			      <content>
			         <espi:UsagePoint>
			            <espi:ServiceCategory>
			               <espi:kind>0</espi:kind>
			            </espi:ServiceCategory>
			            <espi:status>1</espi:status>
			         </espi:UsagePoint>
			      </content>
			      <published>2026-01-01T00:00:00Z</published>
			      <updated>2026-01-01T00:00:00Z</updated>
			   </entry>
			</feed>
			""";

	private static String marshal(Object root, Class<?>... types) throws Exception {
		Marshaller marshaller = JAXBContext.newInstance(types).createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		StringWriter writer = new StringWriter();
		marshaller.marshal(root, writer);
		return writer.toString();
	}

	private static UsageAtomEntryDto usagePointEntry() {
		UsagePointDto usagePoint = new UsagePointDto();
		usagePoint.setStatus((short) 1);
		return new UsageAtomEntryDto(UP_ID, "Sample residential electric service", TIME, TIME,
				List.of(new LinkDto("self", BASE + "/UsagePoint/1", "espi-entry/UsagePoint"),
						new LinkDto("up", BASE + "/UsagePoint", "espi-feed/UsagePoint")),
				usagePoint);
	}

	@Test
	@DisplayName("a usage entry writes its resource inside atom:content, and the feed validates against the schemas")
	void usage_entry_wraps_the_resource_in_content() throws Exception {
		AtomFeedDto feed = new AtomFeedDto("urn:uuid:f69c8759-2ef5-5f18-876c-c19f67a26d67", "UsagePoint Feed",
				TIME, List.of(new LinkDto("self", BASE + "/UsagePoint", "espi-feed/UsagePoint")),
				List.of(usagePointEntry()));

		String xml = marshal(feed, AtomFeedDto.class, UsageAtomEntryDto.class);

		assertThat(xml).containsPattern("(?s)<(\\w+:)?content>\\s*<(\\w+:)?UsagePoint>.*</(\\w+:)?UsagePoint>\\s*</(\\w+:)?content>");
		EspiXmlSchemaValidator.Result result = EspiXmlSchemaValidator.instance().validate(xml);
		assertThat(result.valid()).as(String.join("\n", EspiXmlSchemaValidator.describe(result)) + "\n" + xml).isTrue();
	}

	@Test
	@DisplayName("a customer entry writes its resource inside atom:content too")
	void customer_entry_wraps_the_resource_in_content() throws Exception {
		CustomerAtomEntryDto entry = new CustomerAtomEntryDto("urn:uuid:a3a0d3c4-7c8e-5a8b-9a39-000000000002",
				"Customer", TIME, TIME,
				List.of(new LinkDto("self", BASE + "/RetailCustomer/1/Customer/2", "cust-entry/Customer"),
						new LinkDto("up", BASE + "/RetailCustomer/1/Customer", "cust-feed/Customer")),
				new CustomerDto());

		assertThat(marshal(entry, CustomerAtomEntryDto.class))
				.containsPattern("(?s)<(\\w+:)?content>\\s*<(\\w+:)?Customer\\s*/?>");
	}

	@Test
	@DisplayName("a certified-style feed validates, and its entry's resource is read from atom:content")
	void certified_style_feed_is_read() throws Exception {
		assertThat(EspiXmlSchemaValidator.instance().validate(CERTIFIED_STYLE_FEED).valid()).isTrue();

		AtomFeedDto feed = (AtomFeedDto) JAXBContext.newInstance(AtomFeedDto.class, UsageAtomEntryDto.class)
				.createUnmarshaller().unmarshal(new StringReader(CERTIFIED_STYLE_FEED));

		assertThat(feed.getEntries()).singleElement()
				.satisfies(entry -> assertThat(entry.getResource())
						.isSameAs(entry.getContent())
						.isInstanceOfSatisfying(UsagePointDto.class,
								usagePoint -> assertThat(usagePoint.getStatus()).isEqualTo((short) 1)));
	}

	@Test
	@DisplayName("a round trip keeps the resource: getResource() is no longer null after unmarshalling")
	void round_trip_keeps_the_resource() throws Exception {
		JAXBContext context = JAXBContext.newInstance(UsageAtomEntryDto.class);
		String xml = marshal(usagePointEntry(), UsageAtomEntryDto.class);

		UsageAtomEntryDto parsed = (UsageAtomEntryDto) context.createUnmarshaller().unmarshal(new StringReader(xml));

		assertThat(parsed.getResource()).isInstanceOfSatisfying(UsagePointDto.class,
				usagePoint -> assertThat(usagePoint.getStatus()).isEqualTo((short) 1));
	}

	@Test
	@DisplayName("a resource directly in the entry (no atom:content) is not read as the entry's resource")
	void bare_resource_is_not_the_content() throws Exception {
		String bare = CERTIFIED_STYLE_FEED.replace("<content>", "").replace("</content>", "");

		AtomFeedDto feed = (AtomFeedDto) JAXBContext.newInstance(AtomFeedDto.class, UsageAtomEntryDto.class)
				.createUnmarshaller().unmarshal(new StringReader(bare));

		assertThat(feed.getEntries()).singleElement().satisfies(entry -> assertThat(entry.getResource()).isNull());
	}

	@Test
	@DisplayName("schema errors are all reported with their line: an enum constant name for ServiceCategory (#278)")
	void schema_errors_are_reported_with_lines() {
		String invalid = CERTIFIED_STYLE_FEED.replace("""
				<espi:ServiceCategory>
				               <espi:kind>0</espi:kind>
				            </espi:ServiceCategory>""", "<espi:ServiceCategory>ELECTRICITY</espi:ServiceCategory>");

		EspiXmlSchemaValidator.Result result = EspiXmlSchemaValidator.instance().validate(invalid);

		assertThat(result)
				.returns(false, EspiXmlSchemaValidator.Result::valid)
				.extracting(EspiXmlSchemaValidator.Result::issues, InstanceOfAssertFactories.list(EspiXmlSchemaValidator.Issue.class))
				.isNotEmpty()
				.allSatisfy(issue -> assertThat(issue.line()).isEqualTo(14))
				.anySatisfy(issue -> assertThat(issue.message()).contains("ServiceCategory"));
	}

	@Test
	@DisplayName("text is validated as characters, whatever encoding its declaration names; bytes use that encoding")
	void declared_encoding() {
		String latin1 = CERTIFIED_STYLE_FEED
				.replace("encoding=\"UTF-8\"", "encoding=\"ISO-8859-1\"")
				.replace("<title>Green Button Usage Feed</title>", "<title>Données d'énergie</title>");

		assertThat(List.of(
						EspiXmlSchemaValidator.instance().validate(latin1),
						EspiXmlSchemaValidator.instance().validate(latin1.getBytes(StandardCharsets.ISO_8859_1))))
				.allSatisfy(result -> assertThat(result.issues()).isEmpty());
	}

	@Test
	@DisplayName("an empty feed is valid ESPI")
	void empty_feed_is_valid() {
		String empty = CERTIFIED_STYLE_FEED.replaceAll("(?s)<entry>.*</entry>", "");

		assertThat(EspiXmlSchemaValidator.instance().validate(empty).valid()).isTrue();
	}

	@Test
	@DisplayName("not well-formed XML is a fatal issue")
	void malformed_xml_is_fatal() {
		assertThat(EspiXmlSchemaValidator.instance().validate("<feed xmlns=\"http://www.w3.org/2005/Atom\">").issues())
				.singleElement()
				.satisfies(issue -> assertThat(issue.severity()).isEqualTo(EspiXmlSchemaValidator.Severity.FATAL));
	}

	@Test
	@DisplayName("a document's external DTD is never fetched")
	void external_dtd_is_not_followed() {
		String withDtd = """
				<?xml version="1.0"?>
				<!DOCTYPE feed SYSTEM "http://127.0.0.1:1/never-fetched.dtd">
				<feed xmlns="http://www.w3.org/2005/Atom"/>
				""";

		assertThat(EspiXmlSchemaValidator.instance().validate(withDtd).issues())
				.anySatisfy(issue -> assertThat(issue.message()).containsIgnoringCase("accessExternalDTD"));
	}
}
