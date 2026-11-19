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

import org.greenbuttonalliance.espi.common.domain.common.ServiceCategory;
import org.greenbuttonalliance.espi.common.scope.FunctionBlock;
import org.greenbuttonalliance.espi.common.scope.FunctionBlockCategory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
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

	/**
	 * The commodity FBs that cover a UsagePoint service category (FB_5 to FB_9 electricity, FB_10 gas, FB_11
	 * water): one is needed with {@link #INTERVAL_METERING} for {@code Batch/Subscription}. FB_29 (temperature,
	 * no service kind yet: #283) is not one of them.
	 */
	public static final List<String> SERVICE_COMMODITY = Stream.of(FunctionBlock.values())
			.filter(fb -> fb.getCategory() == FunctionBlockCategory.COMMODITY && fb.getServiceKind().isPresent())
			.map(fb -> "FB_" + fb.getId())
			.toList();

	private UsageFunctionBlockAuthorities() {
	}

	/**
	 * The UsagePoint service categories the token's commodity Function Blocks grant, from each FB's service
	 * kind: FB_5 to FB_9 grant ELECTRICITY, FB_10 GAS, FB_11 WATER. A commodity FB without a service kind
	 * (FB_29, temperature: #283) grants no category. Interval metering (FB_4) alone grants none.
	 */
	public static Set<ServiceCategory> serviceCategories(Authentication authentication) {
		Set<ServiceCategory> categories = EnumSet.noneOf(ServiceCategory.class);
		if (authentication == null) {
			return categories;
		}
		for (GrantedAuthority granted : authentication.getAuthorities()) {
			String name = granted.getAuthority();
			if (name == null || !SERVICE_COMMODITY.contains(name)) {
				continue;
			}
			FunctionBlock.serviceKindOf(Integer.parseInt(name.substring(3)))
					.flatMap(kind -> Stream.of(ServiceCategory.values())
							.filter(category -> category.name().equals(kind.name()))
							.findFirst())
					.ifPresent(categories::add);
		}
		return categories;
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
