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

package org.greenbuttonalliance.espi.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.glassfish.jaxb.runtime.marshaller.NamespacePrefixMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * JAXB Namespace Prefix Mapper for ESPI/Green Button XML output.
 *
 * Controls namespace prefixes used during XML marshalling to ensure
 * consistent Green Button compliant XML output:
 * - Atom elements use no prefix (default namespace)
 * - ESPI usage elements use "espi:" prefix
 * - ESPI customer elements use "cust:" prefix
 *
 * Usage:
 * <pre>
 * marshaller.setProperty("org.glassfish.jaxb.namespacePrefixMapper",
 *                        new EspiNamespacePrefixMapper());
 * </pre>
 */
@Slf4j
public class EspiNamespacePrefixMapper extends NamespacePrefixMapper {

    private static final String ATOM_NAMESPACE = "http://www.w3.org/2005/Atom";
    private static final String ESPI_NAMESPACE = "http://naesb.org/espi";
    private static final String CUSTOMER_NAMESPACE = "http://naesb.org/espi/customer";

    private final Set<String> requiredNamespaces;

    /**
     * Default constructor - declares Atom + both ESPI namespaces.
     * Note: Per NAESB ESPI standard, espi and cust are mutually exclusive in actual use,
     * but this constructor declares both for backward compatibility.
     */
    public EspiNamespacePrefixMapper() {
        this(Set.of(ATOM_NAMESPACE, ESPI_NAMESPACE, CUSTOMER_NAMESPACE));
    }

    /**
     * Constructor with specific required namespaces.
     * Only the specified namespaces will be declared on the root element.
     *
     * @param requiredNamespaces set of namespace URIs to declare
     */
    public EspiNamespacePrefixMapper(Set<String> requiredNamespaces) {
        this.requiredNamespaces = requiredNamespaces != null ? requiredNamespaces : Set.of();
    }

    /**
     * Returns the preferred prefix for the given namespace URI.
     *
     * Namespace prefix mapping:
     * 1. Atom - empty prefix (default namespace) when used with 2 namespaces
     * 2. ESPI - "espi" prefix
     * 3. Customer - "cust" prefix
     *
     * With 2 namespaces (Atom + domain), Atom becomes default namespace.
     * With 3+ namespaces, all get prefixes (JAXB 3.x limitation).
     *
     * @param namespaceUri the namespace URI
     * @param suggestion the suggested prefix (ignored)
     * @param requirePrefix whether a prefix is required (JAXB hint, often ignored for default namespace)
     * @return the prefix to use, or null to use auto-generated prefix
     */
    @Override
    public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
        if (namespaceUri == null) {
            return null;
        }

        String prefix = null;

        // 1. Atom namespace - use "atom" prefix for predictable CMD Certification testing
        //    JAXB 3.x behavior with default namespace (empty prefix) is inconsistent between
        //    usage and customer domains, so we use explicit prefix for both.
        if (ATOM_NAMESPACE.equals(namespaceUri)) {
            prefix = "atom";  // Explicit prefix for consistent, predictable output
        }
        // 2. ESPI usage namespace gets "espi" prefix
        else if (ESPI_NAMESPACE.equals(namespaceUri)) {
            prefix = "espi";
        }
        // 3. ESPI customer namespace gets "cust" prefix
        else if (CUSTOMER_NAMESPACE.equals(namespaceUri)) {
            prefix = "cust";
        }

        log.debug("getPreferredPrefix(uri={}, suggestion={}, requirePrefix={}) -> prefix={} (nsCount={})",
            namespaceUri, suggestion, requirePrefix, prefix, requiredNamespaces.size());

        return prefix;
    }

    // Note: Removed getContextualNamespaceDecls() override
    // Let JAXB automatically declare namespaces it finds during marshalling.
    // We only control the prefix names via getPreferredPrefix().
}
