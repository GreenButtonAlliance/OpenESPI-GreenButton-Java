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

package org.greenbuttonalliance.espi.common.domain.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

/**
 * Embeddable class for StatementRef without JAXB concerns.
 *
 * [extension] A sequence of references to a document associated with a Statement.
 *
 * Note: StatementRef extends Object (not IdentifiedObject) per customer.xsd lines 285-307.
 * It is not a top-level resource and has no selfLink/upLink/relatedLinks.
 * Stored as @ElementCollection in StatementEntity.
 */
@Embeddable
@Data
@NoArgsConstructor
@ToString
public class StatementRefEntity implements Serializable {

    /**
     * [extension] Name of document or file including filename extension if present.
     */
    @Column(name = "file_name", length = 512)
    private String fileName;

    /**
     * [extension] Document media type as published by IANA, see https://www.iana.org/assignments/media-types for more information.
     */
    @Column(name = "media_type", length = 256)
    private String mediaType;

    /**
     * [extension] URL used to access a representation of a statement, for example a bill image.
     * Use CDATA or URL encoding to escape characters not allowed in XML.
     */
    @Column(name = "statement_url", length = 2048)
    private String statementURL;
}