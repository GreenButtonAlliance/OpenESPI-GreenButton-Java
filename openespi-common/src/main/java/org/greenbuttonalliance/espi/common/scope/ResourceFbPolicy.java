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

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Single source of truth for the ESPI Function Blocks a token must carry to access a given
 * {@link EspiResource}.
 *
 * <p>The same FB rule has to be enforced in two places that historically drifted apart: the Data
 * Custodian's endpoint security ({@code SecurityConfiguration}, which 403s a GET whose token lacks
 * the resource's FBs) and the resource entry-assembler (which emits a {@code rel="related"} link
 * only if the caller could actually retrieve the target). Centralising the predicate here means a
 * link is emitted iff the same rule that guards the endpoint holds — they cannot diverge (#119 /
 * #187).</p>
 *
 * <p>This class is intentionally pure (no Spring dependency): it is a predicate over the set of
 * granted FB ids. The {@code DataCustodian_Admin_Access} override (admin sees everything) is
 * orthogonal and is applied by each caller around this policy, not inside it.</p>
 *
 * <p>The FB&rarr;resource mapping is authoritative from the DMD-validator conformance XSLTs
 * ({@code FB_NN.xsl}); it mirrors the endpoint rules formerly inlined in {@code SecurityConfiguration}.</p>
 */
public final class ResourceFbPolicy {

	// --- Energy domain FBs ---------------------------------------------------------------------
	/** FB_4 Interval Metering — the base for all interval usage data. */
	private static final int FB_INTERVAL_METERING = 4;
	/** Commodity FBs (electricity/gas/water/temperature), derived from the single ESPI FB catalog. */
	private static final Set<Integer> COMMODITY_FBS = commodityFbIds();
	/** Data-shape FBs that emit a usage summary. */
	private static final Set<Integer> USAGE_SUMMARY_FBS = Set.of(15, 16, 27, 28);
	/** FB_17 Power Quality Summary. */
	private static final int FB_POWER_QUALITY_SUMMARY = 17;

	// --- Customer / PII domain FBs -------------------------------------------------------------
	/** FB_53 Connect My Data (Retail Customer) — the prerequisite base for any customer/PII access. */
	private static final int FB_CUSTOMER_BASE = 53;
	private static final int FB_CUSTOMER = 54;
	private static final int FB_CUSTOMER_ACCOUNT = 56;
	private static final int FB_CUSTOMER_AGREEMENT = 57;
	private static final int FB_SERVICE_LOCATION = 58;
	private static final int FB_SERVICE_SUPPLIER = 59;
	private static final int FB_METER = 60;
	private static final int FB_END_DEVICE = 61;
	private static final int FB_PROGRAM_DATE_ID_MAPPINGS = 62;

	/**
	 * The energy interval-metering base: {@code FB_4 (Interval Metering) AND at least one commodity
	 * FB}. This is the floor for {@link EspiResource#USAGE_POINT} and its interval data, and is also
	 * the gate the security layer applies to the {@code Subscription} and {@code Batch} ESPI REST API
	 * transport mappings (which are not {@link EspiResource}s). Exposed so those path-level rules and
	 * the typed-resource rules share one definition.
	 *
	 * @param grantedFbs the FB ids the token carries
	 * @return {@code true} if the token satisfies the energy interval-metering base
	 */
	public boolean requiresEnergyIntervalBase(Set<Integer> grantedFbs) {
		return grantedFbs != null
				&& grantedFbs.contains(FB_INTERVAL_METERING)
				&& intersects(grantedFbs, COMMODITY_FBS);
	}

	/**
	 * Decide whether a token carrying {@code grantedFbs} may access {@code resource} on its own FB
	 * merits. The admin override is handled by the caller, not here.
	 *
	 * @param resource   the ESPI resource being accessed
	 * @param grantedFbs the FB ids the token carries
	 * @return {@code true} if the FBs authorize the resource
	 */
	public boolean isAuthorized(EspiResource resource, Set<Integer> grantedFbs) {
		if (resource == null || grantedFbs == null) {
			return false;
		}
		return switch (resource) {
			// Energy interval data, plus the FB-exempt LocalTimeParameters whose energy-feed endpoint
			// inherits the energy base today (FB-exempt link emission is graph-driven in the assembler).
			case USAGE_POINT, METER_READING, READING_TYPE, INTERVAL_BLOCK, LOCAL_TIME_PARAMETERS ->
					requiresEnergyIntervalBase(grantedFbs);
			case USAGE_SUMMARY -> intersects(grantedFbs, USAGE_SUMMARY_FBS);
			case ELECTRIC_POWER_QUALITY_SUMMARY -> grantedFbs.contains(FB_POWER_QUALITY_SUMMARY);
			// Energy ProgramIdMappings — no endpoint until #168; fail-closed until its FB rule is defined.
			case PROGRAM_ID_MAPPINGS -> false;
			case CUSTOMER -> pii(grantedFbs, FB_CUSTOMER);
			case CUSTOMER_ACCOUNT -> pii(grantedFbs, FB_CUSTOMER_ACCOUNT);
			case CUSTOMER_AGREEMENT -> pii(grantedFbs, FB_CUSTOMER_AGREEMENT);
			case SERVICE_LOCATION -> pii(grantedFbs, FB_SERVICE_LOCATION);
			case SERVICE_SUPPLIER -> pii(grantedFbs, FB_SERVICE_SUPPLIER);
			case METER -> pii(grantedFbs, FB_METER);
			case END_DEVICE -> pii(grantedFbs, FB_END_DEVICE);
			case PROGRAM_DATE_ID_MAPPINGS -> pii(grantedFbs, FB_PROGRAM_DATE_ID_MAPPINGS);
		};
	}

	/** Customer/PII gate: the base Connect-My-Data FB AND the resource-specific PII FB. */
	private static boolean pii(Set<Integer> grantedFbs, int resourceFb) {
		return grantedFbs.contains(FB_CUSTOMER_BASE) && grantedFbs.contains(resourceFb);
	}

	private static boolean intersects(Set<Integer> grantedFbs, Set<Integer> required) {
		for (Integer fb : required) {
			if (grantedFbs.contains(fb)) {
				return true;
			}
		}
		return false;
	}

	private static Set<Integer> commodityFbIds() {
		return Stream.of(FunctionBlock.values())
				.filter(fb -> fb.getCategory() == FunctionBlockCategory.COMMODITY)
				.map(FunctionBlock::getId)
				.collect(Collectors.toUnmodifiableSet());
	}
}
