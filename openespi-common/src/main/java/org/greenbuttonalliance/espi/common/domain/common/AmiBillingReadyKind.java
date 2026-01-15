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
 * Lifecycle states of the metering installation at a usage point with respect to
 * readiness for billing via advanced metering infrastructure reads.
 * Per ESPI 4.0 XSD: AmiBillingReadyKind enumeration.
 */
public enum AmiBillingReadyKind {
	/**
	 * Usage point is equipped with an AMI capable meter that is not yet currently
	 * equipped with a communications module.
	 */
	AMI_CAPABLE("amiCapable"),

	/**
	 * Usage point is equipped with an AMI capable meter; however, the AMI functionality
	 * has been disabled or is not being used.
	 */
	AMI_DISABLED("amiDisabled"),

	/**
	 * Usage point is equipped with an operating AMI capable meter and accuracy has been
	 * certified for billing purposes.
	 */
	BILLING_APPROVED("billingApproved"),

	/**
	 * Usage point is equipped with an AMI capable meter having communications capability.
	 */
	ENABLED("enabled"),

	/**
	 * Usage point is equipped with a non AMI capable meter.
	 */
	NON_AMI("nonAmi"),

	/**
	 * Usage point is not currently equipped with a meter.
	 */
	NON_METERED("nonMetered"),

	/**
	 * Usage point is equipped with an AMI capable meter that is functioning and
	 * communicating with the AMI network.
	 */
	OPERABLE("operable");

	private final String value;

	AmiBillingReadyKind(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	/**
	 * Converts a string value to the corresponding AmiBillingReadyKind enum constant.
	 *
	 * @param value the string value from XML/XSD
	 * @return the matching enum constant, or null if not found
	 */
	public static AmiBillingReadyKind fromValue(String value) {
		if (value == null) {
			return null;
		}
		for (AmiBillingReadyKind kind : AmiBillingReadyKind.values()) {
			if (kind.value.equals(value)) {
				return kind;
			}
		}
		return null;
	}
}
