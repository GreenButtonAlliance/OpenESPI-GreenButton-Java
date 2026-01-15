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

/**
 * State of the usage point with respect to connection to the network.
 * Per ESPI 4.0 XSD: UsagePointConnectedKind enumeration.
 */
public enum UsagePointConnectedKind {
	/**
	 * The usage point is connected to the network and able to receive or send
	 * the applicable commodity (electricity, gas, water, etc.).
	 */
	CONNECTED("connected"),

	/**
	 * The usage point has been disconnected through operation of a disconnect function
	 * within the meter present at the usage point. The usage point is unable to receive
	 * or send the applicable commodity (electricity, gas, water, etc.). A logical
	 * disconnect can often be achieved without utilising a field crew.
	 */
	LOGICALLY_DISCONNECTED("logicallyDisconnected"),

	/**
	 * The usage point has been disconnected from the network at a point upstream of the meter.
	 * The usage point is unable to receive or send the applicable commodity (electricity,
	 * gas, water, etc.). A physical disconnect is often achieved by utilising a field crew.
	 */
	PHYSICALLY_DISCONNECTED("physicallyDisconnected");

	private final String value;

	UsagePointConnectedKind(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	/**
	 * Converts a string value to the corresponding UsagePointConnectedKind enum constant.
	 *
	 * @param value the string value from XML/XSD
	 * @return the matching enum constant, or null if not found
	 */
	public static UsagePointConnectedKind fromValue(String value) {
		if (value == null) {
			return null;
		}
		for (UsagePointConnectedKind kind : UsagePointConnectedKind.values()) {
			if (kind.value.equals(value)) {
				return kind;
			}
		}
		return null;
	}
}