/*
 * Copyright 2025 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.greenbuttonalliance.espi.common.domain.customer.enums;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Enumeration for MediaType values.
 *
 * [extension] Media type for document as registered by IANA, allowed subset in enumeration.
 * Per ESPI 4.0 customer.xsd lines 1834-1919.
 */
@XmlType(name = "MediaType", namespace = "http://naesb.org/espi/customer")
@XmlEnum
public enum MediaType {

	/**
	 * JSON.
	 * XSD value: "application/json" (line 1841)
	 */
	@XmlEnumValue("application/json")
	APPLICATION_JSON("application/json"),

	/**
	 * PDF.
	 * XSD value: "application/pdf" (line 1846)
	 */
	@XmlEnumValue("application/pdf")
	APPLICATION_PDF("application/pdf"),

	/**
	 * vnd.ms-excel.
	 * XSD value: "application/vnd.ms-excel" (line 1851)
	 */
	@XmlEnumValue("application/vnd.ms-excel")
	APPLICATION_VND_MS_EXCEL("application/vnd.ms-excel"),

	/**
	 * vnd.oasis.opendocument.spreadsheet.
	 * XSD value: "application/vnd.oasis.opendocument.spreadsheet" (line 1856)
	 */
	@XmlEnumValue("application/vnd.oasis.opendocument.spreadsheet")
	APPLICATION_VND_OASIS_OPENDOCUMENT_SPREADSHEET("application/vnd.oasis.opendocument.spreadsheet"),

	/**
	 * vnd.oasis.opendocument.text.
	 * XSD value: "application/vnd.oasis.opendocument.text" (line 1861)
	 */
	@XmlEnumValue("application/vnd.oasis.opendocument.text")
	APPLICATION_VND_OASIS_OPENDOCUMENT_TEXT("application/vnd.oasis.opendocument.text"),

	/**
	 * vnd.openxmlformats-officedocument.spreadsheetml.sheet.
	 * XSD value: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" (line 1866)
	 */
	@XmlEnumValue("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
	APPLICATION_VND_OPENXMLFORMATS_OFFICEDOCUMENT_SPREADSHEETML_SHEET("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),

	/**
	 * ZIP.
	 * XSD value: "application/zip" (line 1871)
	 */
	@XmlEnumValue("application/zip")
	APPLICATION_ZIP("application/zip"),

	/**
	 * GIF.
	 * XSD value: "image/gif" (line 1876)
	 */
	@XmlEnumValue("image/gif")
	IMAGE_GIF("image/gif"),

	/**
	 * JPEG.
	 * XSD value: "image/jpeg" (line 1881)
	 */
	@XmlEnumValue("image/jpeg")
	IMAGE_JPEG("image/jpeg"),

	/**
	 * PNG.
	 * XSD value: "image/png" (line 1886)
	 */
	@XmlEnumValue("image/png")
	IMAGE_PNG("image/png"),

	/**
	 * CSV.
	 * XSD value: "text/csv" (line 1891)
	 */
	@XmlEnumValue("text/csv")
	TEXT_CSV("text/csv"),

	/**
	 * HTML.
	 * XSD value: "text/html" (line 1896)
	 */
	@XmlEnumValue("text/html")
	TEXT_HTML("text/html"),

	/**
	 * plain.
	 * XSD value: "text/plain" (line 1901)
	 */
	@XmlEnumValue("text/plain")
	TEXT_PLAIN("text/plain"),

	/**
	 * RTF.
	 * XSD value: "text/rtf" (line 1906)
	 */
	@XmlEnumValue("text/rtf")
	TEXT_RTF("text/rtf"),

	/**
	 * XML.
	 * XSD value: "text/xml" (line 1911)
	 */
	@XmlEnumValue("text/xml")
	TEXT_XML("text/xml");

	private final String value;

	MediaType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static MediaType fromValue(String value) {
		for (MediaType type : MediaType.values()) {
			if (type.value.equals(value)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Invalid MediaType value: " + value);
	}
}