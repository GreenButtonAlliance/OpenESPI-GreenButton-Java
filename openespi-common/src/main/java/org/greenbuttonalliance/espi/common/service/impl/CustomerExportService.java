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

package org.greenbuttonalliance.espi.common.service.impl;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.greenbuttonalliance.espi.common.service.BaseExportService;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Export service for ESPI Customer Domain resources.
 * <p>
 * This service handles XML marshalling for customer domain resources defined in customer.xsd,
 * including Customer, CustomerAccount, CustomerAgreement, ServiceLocation, Meter, etc.
 * <p>
 * Namespace configuration:
 * - Atom namespace (http://www.w3.org/2005/Atom) - default namespace
 * - Customer namespace (http://naesb.org/espi/customer) - with "cust:" prefix
 * <p>
 * This service does NOT register usage domain classes (UsagePointDto, MeterReadingDto, etc.)
 * to prevent xmlns:espi namespace pollution. Per NAESB ESPI standard, usage and customer
 * domains are mutually exclusive.
 * <p>
 * Expected XML output:
 * <pre>
 * &lt;entry xmlns="http://www.w3.org/2005/Atom" xmlns:cust="http://naesb.org/espi/customer"&gt;
 *   &lt;id&gt;urn:uuid:...&lt;/id&gt;
 *   &lt;title&gt;Customer&lt;/title&gt;
 *   &lt;cust:Customer&gt;
 *     ...
 *   &lt;/cust:Customer&gt;
 * &lt;/entry&gt;
 * </pre>
 */
@Service("customerExportService")
public class CustomerExportService extends BaseExportService {

    private static final String ATOM_NAMESPACE = "http://www.w3.org/2005/Atom";
    private static final String CUSTOMER_NAMESPACE = "http://naesb.org/espi/customer";

    /**
     * Creates JAXBContext with ONLY Atom + Customer domain classes.
     * <p>
     * Registers exactly 2 namespaces:
     * 1. Atom namespace (for entry, feed, link elements)
     * 2. Customer namespace (for customer domain resources)
     * <p>
     * Does NOT register:
     * - UsageAtomEntryDto (would bring in espi namespace)
     * - Usage domain DTOs (UsagePointDto, MeterReadingDto, etc.)
     *
     * @return JAXBContext configured for customer domain
     * @throws JAXBException if context creation fails
     */
    @Override
    protected JAXBContext createJAXBContext() throws JAXBException {
        return JAXBContext.newInstance(
            // Atom protocol classes
            org.greenbuttonalliance.espi.common.dto.atom.CustomerAtomEntryDto.class,  // Customer-specific entry
            org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto.class,
            org.greenbuttonalliance.espi.common.dto.atom.LinkDto.class,

            // Customer domain classes (http://naesb.org/espi/customer) - from customer.xsd
            org.greenbuttonalliance.espi.common.dto.customer.CustomerDto.class,
            org.greenbuttonalliance.espi.common.dto.customer.CustomerAccountDto.class,
            org.greenbuttonalliance.espi.common.dto.customer.CustomerAgreementDto.class,
            org.greenbuttonalliance.espi.common.dto.customer.EndDeviceDto.class,
            org.greenbuttonalliance.espi.common.dto.customer.MeterDto.class,
            org.greenbuttonalliance.espi.common.dto.customer.ProgramDateIdMappingsDto.class,
            org.greenbuttonalliance.espi.common.dto.customer.ServiceLocationDto.class,
            org.greenbuttonalliance.espi.common.dto.customer.StatementDto.class,
            org.greenbuttonalliance.espi.common.dto.customer.StatementRefDto.class

            // ❌ NO UsageAtomEntryDto
            // ❌ NO Usage domain DTOs (would bring in xmlns:espi)
        );
    }

    /**
     * Returns the 2 namespaces for customer domain.
     *
     * @return set containing Atom and Customer namespaces
     */
    @Override
    protected Set<String> getDomainNamespaces() {
        return Set.of(ATOM_NAMESPACE, CUSTOMER_NAMESPACE);
    }
}