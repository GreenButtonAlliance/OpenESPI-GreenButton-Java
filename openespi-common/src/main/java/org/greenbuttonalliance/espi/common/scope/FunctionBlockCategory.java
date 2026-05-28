/*
 * Copyright 2025 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.greenbuttonalliance.espi.common.scope;

/**
 * Functional categorization of ESPI 4.0 OAuth scope Function Block (FB) terms,
 * per NAESB ESPI 4.0 &sect;REQ.21.4.2.1.3.1 (ScopeFBTerms).
 *
 * <p>A granted ESPI scope is a set of Function Block IDs (e.g. {@code FB=4_5_15}).
 * Every FB defined by the standard falls into exactly one of these categories.
 * The first four are <em>load-bearing</em> &mdash; they drive grant&rarr;subscription
 * resolution and resource-server enforcement (Phase 2, #122). The remainder exist so
 * that a real spec FB is never mislabeled {@link #UNKNOWN}.</p>
 *
 * @see FunctionBlock
 * @see EspiScope
 */
public enum FunctionBlockCategory {

	/** Root resource an FB unlocks: FB 04 (Interval Metering / UsagePoint), FB 53 (Connect My Data, Retail Customer). */
	BASE,

	/**
	 * Commodity selector &mdash; maps to {@code ServiceCategory.kind} (FB 05-11, 29).
	 * Note FB 29 (Temperature Interval Metering) is a commodity FB with no {@code ServiceKind}
	 * equivalent in the ESPI XSD enumeration.
	 */
	COMMODITY,

	/** Shape of energy data exposed: summaries, cost, power quality (FB 12, 15, 16, 17, 27, 28). */
	ENERGY_DATA_SHAPE,

	/** Customer / PII resources from customer.xsd (FB 54-62). */
	CUSTOMER_PII,

	/** Delivery and query models: Download/Connect My Data, Query Parameters, PUSH (FB 02, 03, 37, 39, 52, 68, 69). */
	INTERACTION,

	/** Bulk transfer transport: REST bulk (FB 35, 67). The SFTP bulk FBs (34, 66) are deprecated. */
	BULK_TRANSFER,

	/** Authorization / authentication function blocks, incl. offline authorization (FB 31, 40, 65, 70). */
	AUTHORIZATION,

	/** Resource-management function blocks (FB 41, 44). */
	ADMINISTRATION,

	/** Cross-cutting platform FBs: Common, Common User Experience, Security and Privacy Classes (FB 01, 13, 30, 51, 63, 64). */
	PLATFORM,

	/** An FB ID that NAESB ESPI 4.0 marks DEPRECATED (FB 14, 18, 19, 32, 33, 34, 36, 38, 46-50, 66). */
	DEPRECATED,

	/** An FB ID not defined by the NAESB ESPI 4.0 ScopeFBTerms table. */
	UNKNOWN
}
