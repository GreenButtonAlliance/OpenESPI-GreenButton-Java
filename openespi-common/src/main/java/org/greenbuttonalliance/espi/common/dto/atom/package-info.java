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

/**
 * Atom Protocol DTOs for Green Button feed/entry XML structures.
 *
 * This package contains Data Transfer Objects (DTOs) for Atom Syndication Format (RFC 4287)
 * used to wrap ESPI resources in feed and entry containers for Green Button data exchange.
 */
@jakarta.xml.bind.annotation.XmlSchema(
    namespace = "http://www.w3.org/2005/Atom",
    elementFormDefault = jakarta.xml.bind.annotation.XmlNsForm.QUALIFIED,
    xmlns = {
        @jakarta.xml.bind.annotation.XmlNs(prefix = "atom", namespaceURI = "http://www.w3.org/2005/Atom")
    }
)
package org.greenbuttonalliance.espi.common.dto.atom;
