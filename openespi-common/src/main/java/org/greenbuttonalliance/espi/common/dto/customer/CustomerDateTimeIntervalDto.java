/*
 *
 *        Copyright (c) 2025-2026 Green Button Alliance, Inc.
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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * customer.xsd's own {@code DateTimeInterval} (cust namespace): {@code duration} then {@code start}, both
 * optional. The usage-domain {@code DateTimeIntervalDto} is in the espi namespace, so using it here would put
 * espi elements inside customer data and declare a second ESPI namespace in customer feeds (#279).
 * <p>
 * The all-args constructor takes {@code (start, duration)}, like {@code DateTimeIntervalDto}.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DateTimeInterval", namespace = "http://naesb.org/espi/customer", propOrder = {
    "duration", "start"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDateTimeIntervalDto implements Serializable {

    @XmlElement(name = "start", namespace = "http://naesb.org/espi/customer")
    private Long start;

    @XmlElement(name = "duration", namespace = "http://naesb.org/espi/customer")
    private Long duration;
}
