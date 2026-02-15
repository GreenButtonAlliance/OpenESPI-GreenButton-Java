/*
 *
 *        Copyright (c) 2025 Green Button Alliance, Inc.
 *
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package org.greenbuttonalliance.espi.common.domain.common;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Date and time interval with duration.
 *
 * <p>Embeddable component used for time period specifications in ESPI entities.
 * Represents a time interval with start timestamp and duration in seconds.
 * Both fields are required per ESPI 4.0 specification.
 *
 * <p>Per ESPI 4.0 espi.xsd lines 1337-1357.
 *
 * @see <a href="http://naesb.org/espi">ESPI Specification</a>
 */
@Embeddable
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DateTimeInterval", namespace = "http://naesb.org/espi", propOrder = {
	"duration",
	"start"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DateTimeInterval {

	/**
	 * Duration of the interval, in seconds.
	 *
	 * <p>Required field. Type: UInt32 (unsigned 32-bit integer, max 4,294,967,295).
	 * XSD: espi.xsd line 1344
	 */
	@XmlElement(name = "duration", namespace = "http://naesb.org/espi", required = true)
	@Column(name = "duration", nullable = false)
	private Long duration;

	/**
	 * Date and time that this interval started.
	 *
	 * <p>Required field. Type: TimeType (seconds since Unix epoch, Jan 1, 1970 UTC).
	 * XSD: espi.xsd line 1349
	 */
	@XmlElement(name = "start", namespace = "http://naesb.org/espi", required = true)
	@Column(name = "start", nullable = false)
	private Long start;

}