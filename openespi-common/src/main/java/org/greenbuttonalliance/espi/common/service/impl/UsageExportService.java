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
 * Export service for ESPI Usage Domain resources.
 * <p>
 * This service handles XML marshalling for usage domain resources defined in usage.xsd,
 * including UsagePoint, MeterReading, IntervalBlock, ReadingType, etc.
 * <p>
 * Namespace configuration:
 * - Atom namespace (http://www.w3.org/2005/Atom) - default namespace
 * - ESPI namespace (http://naesb.org/espi) - with "espi:" prefix
 * <p>
 * This service does NOT register customer domain classes (CustomerDto, CustomerAccountDto, etc.)
 * to prevent xmlns:cust namespace pollution. Per NAESB ESPI standard, usage and customer
 * domains are mutually exclusive.
 * <p>
 * Expected XML output:
 * <pre>
 * &lt;entry xmlns="http://www.w3.org/2005/Atom" xmlns:espi="http://naesb.org/espi"&gt;
 *   &lt;id&gt;urn:uuid:...&lt;/id&gt;
 *   &lt;title&gt;Usage Point&lt;/title&gt;
 *   &lt;espi:UsagePoint&gt;
 *     ...
 *   &lt;/espi:UsagePoint&gt;
 * &lt;/entry&gt;
 * </pre>
 */
@Service("usageExportService")
public class UsageExportService extends BaseExportService {

    private static final String ATOM_NAMESPACE = "http://www.w3.org/2005/Atom";
    private static final String ESPI_NAMESPACE = "http://naesb.org/espi";

    /**
     * Creates JAXBContext with ONLY Atom + Usage domain classes.
     * <p>
     * Registers exactly 2 namespaces:
     * 1. Atom namespace (for entry, feed, link elements)
     * 2. ESPI namespace (for usage domain resources)
     * <p>
     * Does NOT register:
     * - CustomerAtomEntryDto (would bring in cust namespace)
     * - Customer domain DTOs (CustomerDto, CustomerAccountDto, etc.)
     *
     * @return JAXBContext configured for usage domain
     * @throws JAXBException if context creation fails
     */
    @Override
    protected JAXBContext createJAXBContext() throws JAXBException {
        return JAXBContext.newInstance(
            // Atom protocol classes - ORDER MATTERS!
            // AtomFeedDto and LinkDto FIRST to establish Atom namespace priority
            org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto.class,
            org.greenbuttonalliance.espi.common.dto.atom.LinkDto.class,
            org.greenbuttonalliance.espi.common.dto.atom.UsageAtomEntryDto.class,  // Usage-specific entry

            // Usage domain classes (http://naesb.org/espi) - from usage.xsd
            org.greenbuttonalliance.espi.common.dto.usage.UsagePointDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.MeterReadingDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.IntervalBlockDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.ReadingTypeDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.ElectricPowerQualitySummaryDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.UsageSummaryDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.TimeConfigurationDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.ApplicationInformationDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.AuthorizationDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.SubscriptionDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.BatchListDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.LineItemDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.ServiceDeliveryPointDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.ReadingQualityDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.IntervalReadingDto.class,
            org.greenbuttonalliance.espi.common.dto.common.DateTimeIntervalDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.TariffRiderRefDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.TariffRiderRefsDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.PnodeRefDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.PnodeRefsDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.AggregatedNodeRefDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.AggregatedNodeRefsDto.class

            // ❌ NO CustomerAtomEntryDto
            // ❌ NO Customer domain DTOs (would bring in xmlns:cust)
        );
    }

    /**
     * Returns the 2 namespaces for usage domain.
     *
     * @return set containing Atom and ESPI namespaces
     */
    @Override
    protected Set<String> getDomainNamespaces() {
        return Set.of(ATOM_NAMESPACE, ESPI_NAMESPACE);
    }
}
