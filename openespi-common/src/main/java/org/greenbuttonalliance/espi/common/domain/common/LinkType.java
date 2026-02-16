/*
 *
 *        Copyright (c) 2025 Green Button Alliance, Inc.
 *
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package org.greenbuttonalliance.espi.common.domain.common;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Embeddable value object for Atom feed links.
 *
 * <p>Represents Atom link elements used in ESPI feed responses for resource relationships.
 * Supports the Atom Syndication Format (RFC 4287) link element attributes.
 *
 * <p>Note: This is a custom class not directly from ESPI XSD. It supports
 * the Atom namespace requirements for ESPI RESTful resource representations.
 *
 * <p>Common link relationships:
 * <ul>
 *   <li>rel="self" - Link to the resource itself</li>
 *   <li>rel="up" - Link to parent collection</li>
 *   <li>rel="related" - Link to related resources</li>
 * </ul>
 *
 * @see <a href="https://tools.ietf.org/html/rfc4287#section-4.2.7">RFC 4287 - Atom Link Element</a>
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class LinkType {

	/**
	 * The link's IRI (Internationalized Resource Identifier).
	 *
	 * <p>URI reference to the linked resource.
	 * RFC 4287: atom:link element's href attribute.
	 */
	@Column(name = "href")
	private String href;

	/**
	 * The link relationship type.
	 *
	 * <p>Describes the relationship between the current resource and the linked resource.
	 * Common values: "self", "up", "related", "alternate".
	 * RFC 4287: atom:link element's rel attribute.
	 */
	@Column(name = "rel")
	private String rel;

	/**
	 * Advisory media type hint.
	 *
	 * <p>MIME type of the linked resource (e.g., "application/xml", "text/html").
	 * RFC 4287: atom:link element's type attribute.
	 */
	@Column(name = "type")
	private String type;

	/**
	 * Convenience constructor for links without media type.
	 *
	 * @param href the link URI
	 * @param rel the relationship type
	 */
	public LinkType(String href, String rel) {
		this.href = href;
		this.rel = rel;
	}

}