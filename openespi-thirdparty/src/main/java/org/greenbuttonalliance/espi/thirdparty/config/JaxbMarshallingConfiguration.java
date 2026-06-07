/*
 *
 *        Copyright (c) 2025 Green Button Alliance, Inc.
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

package org.greenbuttonalliance.espi.thirdparty.config;

import jakarta.xml.bind.Marshaller;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import java.util.Map;

/**
 * JAXB marshalling configuration for the Third Party.
 *
 * <p>Defines the {@code atomMarshaller} {@link Jaxb2Marshaller} that the resource-fetch layer
 * (e.g. {@code ResourceRESTRepositoryImpl}) requires to (un)marshal ESPI Atom feeds/entries fetched
 * from the Data Custodian. The bean was missing, so the TP context failed to start (#146); this
 * supplies it, scanning the shared ESPI DTO (JAXB) packages — the same context the Data Custodian's
 * marshaller uses.</p>
 */
@Configuration
public class JaxbMarshallingConfiguration {

    @Bean(name = "atomMarshaller")
    public Jaxb2Marshaller atomMarshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        // Domain entities carry no JAXB annotations (per the project's JPA/JAXB separation rule);
        // the JAXB-annotated DTOs (incl. the Atom envelope DTOs) live under common.dto.
        marshaller.setPackagesToScan(
                "org.greenbuttonalliance.espi.common.domain",
                "org.greenbuttonalliance.espi.common.dto");
        marshaller.setMarshallerProperties(Map.of(
                Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE,
                Marshaller.JAXB_ENCODING, "UTF-8"));
        return marshaller;
    }
}
