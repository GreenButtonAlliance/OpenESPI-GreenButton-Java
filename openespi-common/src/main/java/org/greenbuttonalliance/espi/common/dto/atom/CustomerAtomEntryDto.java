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

package org.greenbuttonalliance.espi.common.dto.atom;

import jakarta.xml.bind.annotation.*;
import lombok.NoArgsConstructor;
import org.greenbuttonalliance.espi.common.dto.customer.*;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Atom Entry DTO for Customer Domain resources (ESPI Customer namespace).
 * <p>
 * Specialized entry for customer.xsd resources (Customer, CustomerAccount, etc.)
 * that ensures only the cust namespace (http://naesb.org/espi/customer) is declared.
 * <p>
 * Per NAESB ESPI standard, usage and customer domains are mutually exclusive.
 */
@XmlRootElement(name = "entry", namespace = "http://www.w3.org/2005/Atom")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CustomerAtomEntry", namespace = "http://www.w3.org/2005/Atom", propOrder = {
    "content"
})
@NoArgsConstructor
public class CustomerAtomEntryDto extends AtomEntryDto {

    /**
     * Customer domain resource content - only customer.xsd types.
     * JAXB will only declare xmlns:cust namespace since all types use http://naesb.org/espi/customer.
     */
    @XmlElements({
        @XmlElement(name = "Customer", type = CustomerDto.class, namespace = "http://naesb.org/espi/customer"),
        @XmlElement(name = "CustomerAccount", type = CustomerAccountDto.class, namespace = "http://naesb.org/espi/customer"),
        @XmlElement(name = "CustomerAgreement", type = CustomerAgreementDto.class, namespace = "http://naesb.org/espi/customer"),
        @XmlElement(name = "EndDevice", type = EndDeviceDto.class, namespace = "http://naesb.org/espi/customer"),
        @XmlElement(name = "Meter", type = MeterDto.class, namespace = "http://naesb.org/espi/customer"),
        @XmlElement(name = "ProgramDateIdMappings", type = ProgramDateIdMappingsDto.class, namespace = "http://naesb.org/espi/customer"),
        @XmlElement(name = "ServiceLocation", type = ServiceLocationDto.class, namespace = "http://naesb.org/espi/customer"),
        @XmlElement(name = "Statement", type = StatementDto.class, namespace = "http://naesb.org/espi/customer"),
        @XmlElement(name = "StatementRef", type = StatementRefDto.class, namespace = "http://naesb.org/espi/customer")
        // ServiceSupplier - will be activated when ServiceSupplier DTO is added as part of Issue #28
        // @XmlElement(name = "ServiceSupplier", type = ServiceSupplierDto.class, namespace = "http://naesb.org/espi/customer")
    })
    protected Object content;

    /**
     * All-args constructor.
     */
    public CustomerAtomEntryDto(String id, String title, OffsetDateTime published, OffsetDateTime updated,
                               List<LinkDto> links, Object content) {
        super(id, title, published, updated, links, content);
        this.content = content;
    }

    /**
     * Convenience constructor for testing with auto-generated timestamps.
     *
     * @param id the entry identifier (urn:uuid:xxx format)
     * @param title the entry title
     * @param content the customer resource content
     */
    public CustomerAtomEntryDto(String id, String title, Object content) {
        super(id, title, content);
        this.content = content;
    }

    /**
     * Override getContent to return this class's content field (with @XmlElements)
     * instead of parent's @XmlTransient content field.
     */
    @Override
    public Object getContent() {
        return this.content;
    }

    /**
     * Override setContent to set this class's content field.
     */
    @Override
    public void setContent(Object content) {
        this.content = content;
    }
}
