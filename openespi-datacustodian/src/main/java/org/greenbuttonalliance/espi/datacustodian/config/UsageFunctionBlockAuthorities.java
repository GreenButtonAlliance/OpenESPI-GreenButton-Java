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

package org.greenbuttonalliance.espi.datacustodian.config;

import org.greenbuttonalliance.espi.common.scope.FunctionBlock;
import org.greenbuttonalliance.espi.common.scope.FunctionBlockCategory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.stream.Stream;

/**
 * The Function Block authorities ({@code FB_<n>}, from {@link EspiScopeOpaqueTokenIntrospector}) that grant
 * each kind of usage data. {@link SecurityConfiguration}'s URL rules and the {@code Batch/Subscription} feed
 * (#279), which holds several resource types behind one URL, both read them here, so a rule is stated once
 * until #187's {@code ResourceFbPolicy} replaces them.
 */
public final class UsageFunctionBlockAuthorities {

	/** Interval metering: the base for UsagePoint, MeterReading, ReadingType, IntervalBlock, LocalTimeParameters. */
	public static final String INTERVAL_METERING = "FB_4";

	/** Usage summaries (the data-shape FBs). */
	public static final List<String> USAGE_SUMMARY = List.of("FB_15", "FB_16", "FB_27", "FB_28");

	/** Power-quality summary. */
	public static final List<String> POWER_QUALITY = List.of("FB_17");

	/** Every commodity FB (electricity, gas, water, ...): at least one is needed with {@link #INTERVAL_METERING}. */
	public static final List<String> COMMODITY = Stream.of(FunctionBlock.values())
			.filter(fb -> fb.getCategory() == FunctionBlockCategory.COMMODITY)
			.map(fb -> "FB_" + fb.getId())
			.toList();

	private UsageFunctionBlockAuthorities() {
	}

	/** Whether the authentication holds at least one of the authorities. */
	public static boolean hasAny(Authentication authentication, List<String> authorities) {
		if (authentication == null) {
			return false;
		}
		for (GrantedAuthority granted : authentication.getAuthorities()) {
			if (authorities.contains(granted.getAuthority())) {
				return true;
			}
		}
		return false;
	}
}
