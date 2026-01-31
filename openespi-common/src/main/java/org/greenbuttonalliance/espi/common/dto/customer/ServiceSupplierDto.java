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

package org.greenbuttonalliance.espi.common.dto.customer;

import org.greenbuttonalliance.espi.common.domain.customer.enums.SupplierKind;

import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * ServiceSupplier DTO class for JAXB XML marshalling/unmarshalling.
 *
 * Represents an organization that provides services to customers.
 * Field order matches customer.xsd:1159-1186.
 */
@XmlRootElement(name = "ServiceSupplier", namespace = "http://naesb.org/espi/customer")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ServiceSupplier", namespace = "http://naesb.org/espi/customer", propOrder = {
    "organisation", "kind", "issuerIdentificationNumber", "effectiveDate"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceSupplierDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Organisation having this role.
     * Maps to customer.xsd Organisation element (lines 1162-1163).
     */
    @XmlElement(name = "Organisation", namespace = "http://naesb.org/espi/customer")
    private OrganisationDto organisation;

    /**
     * Kind of supplier.
     * Maps to customer.xsd SupplierKind element (lines 1164-1174).
     */
    @XmlElement(name = "kind", namespace = "http://naesb.org/espi/customer")
    private SupplierKind kind;

    /**
     * Unique transaction reference prefix number issued to an entity by the International Organization for
     * Standardization for the purpose of tagging onto electronic financial transactions, as defined in
     * ISO/IEC 7812-1 and ISO/IEC 7812-2.
     * Maps to customer.xsd issuerIdentificationNumber element (lines 1175-1181).
     */
    @XmlElement(name = "issuerIdentificationNumber", namespace = "http://naesb.org/espi/customer")
    private String issuerIdentificationNumber;

    /**
     * [extension] Effective Date of Service Activation.
     * Maps to customer.xsd effectiveDate element (lines 1182-1185).
     */
    @XmlElement(name = "effectiveDate", namespace = "http://naesb.org/espi/customer")
    private OffsetDateTime effectiveDate;
}
