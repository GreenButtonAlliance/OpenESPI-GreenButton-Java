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

package org.greenbuttonalliance.espi.datacustodian.web.authorize;

import org.greenbuttonalliance.espi.common.domain.usage.enums.ServiceKind;

import java.util.List;
import java.util.UUID;

/**
 * View model for the Authorization Screen (PR C2b). All strings here are pre-localized by the
 * controller against the current request locale; the template renders raw values without further
 * i18n lookups.
 *
 * @param tpName                Customer-visible name of the third party (from
 *                              {@code ApplicationInformation.thirdPartyApplicationDescription},
 *                              falling back to {@code client_id})
 * @param commoditySections     One section per {@link ServiceKind} present in the requested scope,
 *                              ordered electricity &rarr; gas &rarr; water &rarr; temperature.
 *                              Empty if the grant is PII-only.
 * @param piiOptions            One entry per Customer/PII FB in the requested scope. Customer
 *                              checks each individually (default-unchecked).
 * @param implicitBaseLabels    Display-only footer note for FB 1 / 4 / 51 that are part of the
 *                              grant but are not customer-selectable (structural prerequisites).
 * @param handoffToken          The signed outbound handoff token, round-tripped through the form
 *                              as a hidden field so the POST handler can re-verify and rebuild the
 *                              return payload.
 */
public record AuthorizeScreenViewModel(
		String tpName,
		List<CommoditySection> commoditySections,
		List<PiiOption> piiOptions,
		List<String> implicitBaseLabels,
		String handoffToken
) {

	/**
	 * One commodity (electricity / gas / water / temperature) section. The customer toggles
	 * individual usage points; the {@code profileLabels} and {@code dataShapeLabels} are
	 * display-only ("this is what you'll receive") because the third party chose those when
	 * requesting the scope, not the customer.
	 *
	 * @param sectionTitle      Localized commodity title (e.g. "Electricity")
	 * @param kind              The {@link ServiceKind} this section covers
	 * @param usagePoints       Customer's usage points of this commodity that are eligible to be
	 *                          shared; empty if the customer has no usage points of this kind
	 * @param profileLabels     Localized labels for the commodity-profile FBs in the requested
	 *                          scope that apply to this commodity (e.g. "Hourly delivered",
	 *                          "Solar export")
	 * @param dataShapeLabels   Localized labels for the data-shape FBs in the requested scope
	 *                          (e.g. "Billing-period summaries", "Power quality"). These are
	 *                          orthogonal to commodity but rendered under each section so the
	 *                          customer can see what's included.
	 */
	public record CommoditySection(
			String sectionTitle,
			ServiceKind kind,
			List<UsagePointChoice> usagePoints,
			List<String> profileLabels,
			List<String> dataShapeLabels
	) {}

	/**
	 * A customer usage point the third party may receive data for. Customer toggles a checkbox per
	 * choice (default-checked &mdash; the third party explicitly requested the matching commodity).
	 *
	 * @param id           UUID; round-trips through the form to identify the customer's selection
	 * @param description  Localized human-readable description (kind + meter/location identifier)
	 */
	public record UsagePointChoice(
			UUID id,
			String description
	) {}

	/**
	 * A Customer/PII consent option. One per PII FB in the requested scope. Default-unchecked;
	 * customer must explicitly opt in to each.
	 *
	 * @param fbId         The FB number (54&ndash;62) &mdash; round-trips through the form
	 * @param label        Localized short label (e.g. "Your mailing address")
	 * @param description  Localized one-line description of what the third party receives
	 */
	public record PiiOption(
			int fbId,
			String label,
			String description
	) {}
}
