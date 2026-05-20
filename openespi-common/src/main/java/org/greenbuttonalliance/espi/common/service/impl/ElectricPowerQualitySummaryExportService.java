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
 * Export service for ESPI ElectricPowerQualitySummary resource.
 * <p>
 * This service handles XML marshalling for the ElectricPowerQualitySummary resource defined in usage.xsd.
 * <p>
 * Namespace configuration:
 * - Atom namespace (http://www.w3.org/2005/Atom) - default namespace
 * - ESPI namespace (http://naesb.org/espi) - with "espi:" prefix
 */
@Service("electricPowerQualitySummaryExportService")
public class ElectricPowerQualitySummaryExportService extends BaseExportService {

    private static final String ATOM_NAMESPACE = "http://www.w3.org/2005/Atom";
    private static final String ESPI_NAMESPACE = "http://naesb.org/espi";

    /**
     * Creates JAXBContext with Atom + ElectricPowerQualitySummary domain classes.
     *
     * @return JAXBContext configured for ElectricPowerQualitySummary resource
     * @throws JAXBException if context creation fails
     */
    @Override
    protected JAXBContext createJAXBContext() throws JAXBException {
        return JAXBContext.newInstance(
            // Atom protocol classes
            org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto.class,
            org.greenbuttonalliance.espi.common.dto.atom.LinkDto.class,
            org.greenbuttonalliance.espi.common.dto.atom.UsageAtomEntryDto.class,

            // ElectricPowerQualitySummary resource class (http://naesb.org/espi)
            org.greenbuttonalliance.espi.common.dto.usage.ElectricPowerQualitySummaryDto.class
        );
    }

    /**
     * Returns the 2 namespaces for ElectricPowerQualitySummary domain.
     *
     * @return set containing Atom and ESPI namespaces
     */
    @Override
    protected Set<String> getDomainNamespaces() {
        return Set.of(ATOM_NAMESPACE, ESPI_NAMESPACE);
    }
}
