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

import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Validates ESPI XML (an Atom {@code feed} or {@code entry}) against the normative schemas: atom.xsd,
 * espi.xsd and customer.xsd (REQ.21 v4.1 6.5 and 6.6), the same files the GBA DMD validator uses (#197).
 *
 * <p>Atom's {@code content} and {@code entry} accept foreign elements with strict processing, so an ESPI
 * resource inside them ({@code espi:UsagePoint}, {@code cust:Customer}, ...) is validated against its own
 * schema. Every problem is reported with its line and column, not only the first. The schemas are read from
 * the classpath; nothing is fetched from the network, and the document's DTDs and schema locations are
 * never followed.</p>
 *
 * <p>atom.xsd is the GBA's ESPI profile of RFC 4287, not RFC 4287 itself (see
 * {@code docs/ESPI_ATOM_PROFILE.md}). It is stricter than the RFC (every link needs {@code rel} and
 * {@code type}; an entry needs at least two links) and looser (it cannot enforce exactly one {@code id},
 * {@code title} or {@code updated}, or a time zone on dates). This class reports exactly what the schemas
 * report, like the DMD validator. Never add checks that ESPI deliberately differs from, such as a missing
 * {@code atom:author} (privacy) or {@code atom:content} without a {@code type}. Keep the schema files
 * identical to the DMD validator's.</p>
 *
 * <p>Thread-safe: the compiled {@link Schema} is shared, and each call uses its own {@link Validator}.</p>
 */
public final class EspiXmlSchemaValidator {

	private static final String SCHEMA_ROOT = "/schema/";
	private static final String ATOM_XSD = SCHEMA_ROOT + "ESPI_4.1/atom.xsd";
	private static final String XML_XSD = SCHEMA_ROOT + "ESPI_4.1/xml.xsd";
	private static final String ESPI_XSD = SCHEMA_ROOT + "ESPI_4.0/espi.xsd";
	private static final String CUSTOMER_XSD = SCHEMA_ROOT + "ESPI_4.0/customer.xsd";

	/** One problem found in the document. */
	public record Issue(Severity severity, int line, int column, String message) {

		@Override
		public String toString() {
			return severity + " at line " + line + ", column " + column + ": " + message;
		}
	}

	public enum Severity { WARNING, ERROR, FATAL }

	/** The outcome of one validation; valid when there is no ERROR or FATAL issue. */
	public record Result(List<Issue> issues) {

		public Result {
			issues = List.copyOf(issues);
		}

		public boolean valid() {
			return errorCount() == 0;
		}

		public long errorCount() {
			return issues.stream().filter(issue -> issue.severity() != Severity.WARNING).count();
		}
	}

	private static final class Holder {
		private static final EspiXmlSchemaValidator INSTANCE = new EspiXmlSchemaValidator();
	}

	private final Schema schema;

	private EspiXmlSchemaValidator() {
		this.schema = compile();
	}

	/** The shared validator; the schemas are compiled once, on first use. */
	public static EspiXmlSchemaValidator instance() {
		return Holder.INSTANCE;
	}

	/**
	 * Validates already-decoded text. It is read as characters, so an {@code encoding} in the XML
	 * declaration is ignored (the text has no bytes to decode).
	 */
	public Result validate(String xml) {
		return validate(new StreamSource(new StringReader(xml)));
	}

	/** Validates the raw bytes as received, so the XML declaration's encoding is honoured. */
	public Result validate(byte[] xml) {
		return validate(new StreamSource(new ByteArrayInputStream(xml)));
	}

	private Result validate(StreamSource source) {
		List<Issue> issues = new ArrayList<>();
		Validator validator = schema.newValidator();
		try {
			validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			validator.setErrorHandler(new CollectingErrorHandler(issues));
			validator.validate(source);
		} catch (SAXParseException ex) {
			// Normally already recorded by fatalError; record it if the parser threw it without calling the
			// handler, so a document that never finished parsing is never reported as valid.
			if (issues.stream().noneMatch(issue -> issue.severity() == Severity.FATAL)) {
				issues.add(new Issue(Severity.FATAL, ex.getLineNumber(), ex.getColumnNumber(), ex.getMessage()));
			}
		} catch (SAXException | IOException ex) {
			issues.add(new Issue(Severity.FATAL, -1, -1, ex.getMessage()));
		}
		return new Result(issues);
	}

	private static Schema compile() {
		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		try {
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			// The imports are answered from the classpath by the resolver below; only those may load.
			factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file,jar,jrt");
			factory.setResourceResolver(new ClasspathSchemaResolver());
			return factory.newSchema(new Source[] {source(ATOM_XSD), source(ESPI_XSD), source(CUSTOMER_XSD)});
		} catch (SAXException ex) {
			throw new IllegalStateException("The ESPI schemas (atom.xsd, espi.xsd, customer.xsd) could not be compiled", ex);
		}
	}

	private static Source source(String resource) {
		URL url = resourceUrl(resource);
		return new StreamSource(url.toExternalForm());
	}

	private static URL resourceUrl(String resource) {
		URL url = EspiXmlSchemaValidator.class.getResource(resource);
		if (url == null) {
			throw new IllegalStateException("Schema not on the classpath: " + resource);
		}
		return url;
	}

	/**
	 * espi.xsd and customer.xsd import {@code atom.xsd} by a relative location (it lives in another folder
	 * here), and atom.xsd imports {@code xml.xsd} from w3.org. Both are answered from the classpath.
	 */
	private static final class ClasspathSchemaResolver implements LSResourceResolver {

		@Override
		public LSInput resolveResource(String type, String namespaceUri, String publicId, String systemId,
									   String baseUri) {
			if (XMLConstants.XML_NS_URI.equals(namespaceUri)) {
				return input(XML_XSD, publicId);
			}
			if ("http://www.w3.org/2005/Atom".equals(namespaceUri)) {
				return input(ATOM_XSD, publicId);
			}
			return null;
		}

		private static LSInput input(String resource, String publicId) {
			return new ClasspathInput(resourceUrl(resource).toExternalForm(), publicId);
		}
	}

	/** An {@link LSInput} whose system id is a classpath URL; the parser opens it itself. */
	private record ClasspathInput(String systemId, String publicId) implements LSInput {

		@Override public String getSystemId() { return systemId; }
		@Override public String getPublicId() { return publicId; }
		@Override public Reader getCharacterStream() { return null; }
		@Override public InputStream getByteStream() { return null; }
		@Override public String getStringData() { return null; }
		@Override public String getBaseURI() { return null; }
		@Override public String getEncoding() { return null; }
		@Override public boolean getCertifiedText() { return false; }
		@Override public void setCharacterStream(Reader characterStream) { }
		@Override public void setByteStream(InputStream byteStream) { }
		@Override public void setStringData(String stringData) { }
		@Override public void setSystemId(String systemId) { }
		@Override public void setPublicId(String publicId) { }
		@Override public void setBaseURI(String baseUri) { }
		@Override public void setEncoding(String encoding) { }
		@Override public void setCertifiedText(boolean certifiedText) { }
	}

	private record CollectingErrorHandler(List<Issue> issues) implements ErrorHandler {

		@Override
		public void warning(SAXParseException ex) {
			add(Severity.WARNING, ex);
		}

		@Override
		public void error(SAXParseException ex) {
			add(Severity.ERROR, ex);
		}

		@Override
		public void fatalError(SAXParseException ex) throws SAXParseException {
			add(Severity.FATAL, ex);
			throw ex;
		}

		private void add(Severity severity, SAXParseException ex) {
			issues.add(new Issue(severity, ex.getLineNumber(), ex.getColumnNumber(), ex.getMessage()));
		}
	}

	/** For tests and diagnostics: the issues as text, one per line. */
	public static List<String> describe(Result result) {
		return result == null ? Collections.emptyList()
				: result.issues().stream().map(Issue::toString).toList();
	}
}
