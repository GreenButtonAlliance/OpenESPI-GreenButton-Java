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
 * The ESPI 4.0 addressable resource types whose access is gated by OAuth Function Blocks.
 *
 * <p>This enum is the resource-identity half of {@link ResourceFbPolicy}: every constant names an
 * ESPI resource the Data Custodian exposes (energy from {@code espi.xsd}, customer/PII from
 * {@code customer.xsd}) and the policy maps it to the FB predicate that authorizes it. Keeping the
 * identity here (rather than as bare URL strings) lets both the endpoint security layer and the
 * resource entry-assembler refer to the same resource by one name, so the FB rule that guards a GET
 * and the FB rule that decides whether a {@code rel="related"} link is emitted can never diverge
 * (#119 / #187).</p>
 *
 * <p><strong>Only true ESPI resources are listed.</strong> Deliberately absent:</p>
 * <ul>
 *   <li>{@code ApplicationInformation}, {@code Authorization} — admin-only OAuth metadata, not
 *       Function-Block scoped.</li>
 *   <li>{@code Subscription}, {@code Batch} — ESPI REST API access/transport mappings, not
 *       resources; gated as path-level API rules in the security layer.</li>
 *   <li>{@code IntervalReading} — a component of {@link #INTERVAL_BLOCK}, not separately
 *       addressable: it carries no UUID index, so neither a ROOT {@code /IntervalReading} nor an
 *       XPath form is valid.</li>
 * </ul>
 */
public enum EspiResource {

	// === Energy domain (espi.xsd) =============================================================

	// Interval usage data — gated by the energy interval-metering base (FB_4 AND a commodity FB).
	USAGE_POINT,
	METER_READING,
	READING_TYPE,
	INTERVAL_BLOCK,

	// Data-shape summaries.
	USAGE_SUMMARY,                    // data-shape FBs: 15 / 16 / 27 / 28
	ELECTRIC_POWER_QUALITY_SUMMARY,   // FB_17

	/**
	 * Shared DC-global timezone configuration (Standard + DST offsets from UTC). One underlying
	 * table, surfaced in <em>both</em> schemas — the energy feed ({@code espi.xsd}) and the customer
	 * feed ({@code customer.xsd}) — and reachable as a {@code rel="related"} target from both
	 * domains. It is FB-exempt (it has no Function Block of its own; a TP requires it to translate
	 * the mandatory UTC timestamps to local time), so its emission is governed by its neighbours'
	 * FBs rather than one of its own. Listed once here; see the Customer section note below.
	 */
	LOCAL_TIME_PARAMETERS,

	/** Energy ProgramIdMappings — entity/endpoint deferred to #168; identity reserved here. */
	PROGRAM_ID_MAPPINGS,

	// === Customer domain (customer.xsd) — FB_53 base AND the resource-specific PII FB ==========
	// Note: LocalTimeParameters is also a customer-feed resource, but it is the same shared,
	// FB-exempt resource defined above (LOCAL_TIME_PARAMETERS) — not duplicated here.
	CUSTOMER,                 // FB_54
	CUSTOMER_ACCOUNT,         // FB_56
	CUSTOMER_AGREEMENT,       // FB_57
	SERVICE_LOCATION,         // FB_58
	SERVICE_SUPPLIER,         // FB_59
	METER,                    // FB_60
	END_DEVICE,               // FB_61
	PROGRAM_DATE_ID_MAPPINGS  // FB_62
}
