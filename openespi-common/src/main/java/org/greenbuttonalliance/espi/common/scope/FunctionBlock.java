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

import org.greenbuttonalliance.espi.common.domain.usage.enums.ServiceKind;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Authoritative catalog of NAESB ESPI 4.0 OAuth scope Function Block (FB) terms,
 * per &sect;REQ.21.4.2.1.3.1 (ScopeFBTerms).
 *
 * <p>This enum is the single in-code home for ESPI FB semantics. Each constant carries the
 * FB's numeric id (the wire value used in the {@code FB=} scope term), its
 * {@link FunctionBlockCategory}, the spec title, and &mdash; for most {@code COMMODITY} FBs &mdash;
 * the {@link ServiceKind} it selects.</p>
 *
 * <p>Only <em>active</em> (non-deprecated) FBs are enumerated. FB ids the spec marks
 * {@code [DEPRECATED]} are recorded in {@link #DEPRECATED_IDS} so that {@link #categoryOf(int)}
 * reports {@link FunctionBlockCategory#DEPRECATED} rather than {@link FunctionBlockCategory#UNKNOWN}.
 * Any id absent from both is genuinely outside the spec table and resolves to {@code UNKNOWN}.</p>
 *
 * <p>The {@link EspiScope} parser is intentionally FB-agnostic: it carries whatever FB ids appear
 * in a scope string, so future or unrecognized FBs still parse cleanly. This catalog is consulted
 * only when a category or service kind is needed. The {@code title} is diagnostic only (logging /
 * documentation) &mdash; it is never serialized and never drives an authorization decision.</p>
 *
 * @see FunctionBlockCategory
 * @see EspiScope
 */
public enum FunctionBlock {

	// --- Energy Usage domain -------------------------------------------------
	FB_01(1, FunctionBlockCategory.PLATFORM, "Common (Energy Usage)"),
	FB_02(2, FunctionBlockCategory.INTERACTION, "Download My Data (Energy Usage)"),
	FB_03(3, FunctionBlockCategory.INTERACTION, "Connect My Data (Energy Usage)"),
	FB_04(4, FunctionBlockCategory.BASE, "Interval Metering"),
	FB_05(5, FunctionBlockCategory.COMMODITY, ServiceKind.ELECTRICITY, "Interval Electricity Metering"),
	FB_06(6, FunctionBlockCategory.COMMODITY, ServiceKind.ELECTRICITY, "Demand Electricity Metering"),
	FB_07(7, FunctionBlockCategory.COMMODITY, ServiceKind.ELECTRICITY, "Net Metering"),
	FB_08(8, FunctionBlockCategory.COMMODITY, ServiceKind.ELECTRICITY, "Forward and Reverse Metering"),
	FB_09(9, FunctionBlockCategory.COMMODITY, ServiceKind.ELECTRICITY, "Register Values"),
	FB_10(10, FunctionBlockCategory.COMMODITY, ServiceKind.GAS, "Gas Interval Metering"),
	FB_11(11, FunctionBlockCategory.COMMODITY, ServiceKind.WATER, "Water Interval Metering"),
	FB_12(12, FunctionBlockCategory.ENERGY_DATA_SHAPE, "Cost of Interval Data"),
	FB_13(13, FunctionBlockCategory.PLATFORM, "Security and Privacy Classes (Energy Usage)"),
	FB_15(15, FunctionBlockCategory.ENERGY_DATA_SHAPE, "Usage Summary"),
	FB_16(16, FunctionBlockCategory.ENERGY_DATA_SHAPE, "Usage Summary with Cost"),
	FB_17(17, FunctionBlockCategory.ENERGY_DATA_SHAPE, "Power Quality Summary"),
	FB_27(27, FunctionBlockCategory.ENERGY_DATA_SHAPE, "Usage Summary with Demands and Previous Day Attributes"),
	FB_28(28, FunctionBlockCategory.ENERGY_DATA_SHAPE, "Usage Summary Costs for Current Billing Period"),

	/**
	 * Temperature Interval Metering &mdash; commodity FB with <em>no</em> {@link ServiceKind} mapping.
	 *
	 * <p>The ESPI 4.0 {@code ServiceKind} XSD enumeration (electricity, gas, water, time, heat,
	 * refuse, sewerage, rates, TV licence, internet) has no value for temperature, so a UsagePoint's
	 * {@code ServiceCategory.kind} cannot encode it &mdash; a known gap in the standard. FB 29 is
	 * therefore {@link FunctionBlockCategory#COMMODITY} with no service kind, and
	 * {@link EspiScope#commodityServiceKinds()} will not include any kind solely on its account.</p>
	 */
	FB_29(29, FunctionBlockCategory.COMMODITY, "Temperature Interval Metering"),

	FB_30(30, FunctionBlockCategory.PLATFORM, "Common User Experience"),
	FB_31(31, FunctionBlockCategory.AUTHORIZATION, "Authorization and Authentication w/o Pre-Negotiated Scope"),
	FB_35(35, FunctionBlockCategory.BULK_TRANSFER, "REST for Energy Usage Bulk"),
	FB_37(37, FunctionBlockCategory.INTERACTION, "Query Parameters (Energy Usage)"),
	FB_39(39, FunctionBlockCategory.INTERACTION, "PUSH Model (Energy Usage)"),
	FB_40(40, FunctionBlockCategory.AUTHORIZATION, "Offline Authorization (Energy Usage)"),
	FB_41(41, FunctionBlockCategory.ADMINISTRATION, "Manage ApplicationInformation Resource"),
	FB_44(44, FunctionBlockCategory.ADMINISTRATION, "Manage Authorization Resource"),

	// --- Retail Customer domain ----------------------------------------------
	FB_51(51, FunctionBlockCategory.PLATFORM, "Common (Retail Customer)"),
	FB_52(52, FunctionBlockCategory.INTERACTION, "Download My Data (Retail Customer)"),
	FB_53(53, FunctionBlockCategory.BASE, "Connect My Data (Retail Customer)"),
	FB_54(54, FunctionBlockCategory.CUSTOMER_PII, "Basic Retail Customer Information"),
	FB_55(55, FunctionBlockCategory.CUSTOMER_PII, "Retail Customer Address Information"),
	FB_56(56, FunctionBlockCategory.CUSTOMER_PII, "Retail Customer Billing Information"),
	FB_57(57, FunctionBlockCategory.CUSTOMER_PII, "Retail Customer AccountAgreement Information"),
	FB_58(58, FunctionBlockCategory.CUSTOMER_PII, "Retail Customer ServiceLocation Information"),
	FB_59(59, FunctionBlockCategory.CUSTOMER_PII, "Retail Customer ServiceSupplier Information"),
	FB_60(60, FunctionBlockCategory.CUSTOMER_PII, "Retail Customer Meter Information"),
	FB_61(61, FunctionBlockCategory.CUSTOMER_PII, "Retail Customer EndDevice Information"),
	FB_62(62, FunctionBlockCategory.CUSTOMER_PII, "Retail Customer ProgramDateIdMapping Information"),
	FB_63(63, FunctionBlockCategory.PLATFORM, "Common User Experience (Retail Customer)"),
	FB_64(64, FunctionBlockCategory.PLATFORM, "Security and Privacy Classes (Retail Customer)"),
	FB_65(65, FunctionBlockCategory.AUTHORIZATION, "Authorization and Authentication w/o Pre-Negotiated Scope (Retail Customer)"),
	FB_67(67, FunctionBlockCategory.BULK_TRANSFER, "REST for Retail Customer Bulk"),
	FB_68(68, FunctionBlockCategory.INTERACTION, "Query Parameters (Retail Customer)"),
	FB_69(69, FunctionBlockCategory.INTERACTION, "PUSH Model (Retail Customer)"),
	FB_70(70, FunctionBlockCategory.AUTHORIZATION, "Offline Authorization (Retail Customer)");

	/**
	 * FB ids that NAESB ESPI 4.0 marks {@code [DEPRECATED]} (no longer assignable but reserved so
	 * the numbers are not reused). Recorded so {@link #categoryOf(int)} can distinguish a deprecated
	 * FB from one that was never defined. Includes the SFTP bulk FBs (34, 66), which the standard
	 * retired in favor of the REST bulk FBs (35, 67).
	 */
	public static final Set<Integer> DEPRECATED_IDS =
			Set.of(14, 18, 19, 32, 33, 34, 36, 38, 46, 47, 48, 49, 50, 66);

	private static final Map<Integer, FunctionBlock> BY_ID =
			Stream.of(values()).collect(Collectors.toUnmodifiableMap(FunctionBlock::getId, Function.identity()));

	private final int id;
	private final FunctionBlockCategory category;
	private final ServiceKind serviceKind;
	private final String title;

	FunctionBlock(int id, FunctionBlockCategory category, String title) {
		this(id, category, null, title);
	}

	FunctionBlock(int id, FunctionBlockCategory category, ServiceKind serviceKind, String title) {
		this.id = id;
		this.category = category;
		this.serviceKind = serviceKind;
		this.title = title;
	}

	/** @return the FB's numeric id (the value used in the {@code FB=} scope term). */
	public int getId() {
		return id;
	}

	/** @return the functional category this FB belongs to. */
	public FunctionBlockCategory getCategory() {
		return category;
	}

	/**
	 * @return the {@link ServiceKind} selected by this commodity FB, or empty for non-commodity FBs
	 *         and for commodity FBs with no XSD-defined kind (FB 29 / Temperature)
	 */
	public Optional<ServiceKind> getServiceKind() {
		return Optional.ofNullable(serviceKind);
	}

	/** @return the NAESB spec title for this FB (diagnostic only). */
	public String getTitle() {
		return title;
	}

	/**
	 * Look up an active FB by its numeric id.
	 *
	 * @param id the FB number
	 * @return the catalogued FB, or empty if the id is deprecated or undefined
	 */
	public static Optional<FunctionBlock> byId(int id) {
		return Optional.ofNullable(BY_ID.get(id));
	}

	/**
	 * Categorize an FB id, tolerating ids that are not catalogued.
	 *
	 * @param id the FB number
	 * @return the active FB's category; {@link FunctionBlockCategory#DEPRECATED} for a
	 *         spec-deprecated id; {@link FunctionBlockCategory#UNKNOWN} otherwise
	 */
	public static FunctionBlockCategory categoryOf(int id) {
		FunctionBlock fb = BY_ID.get(id);
		if (fb != null) {
			return fb.category;
		}
		return DEPRECATED_IDS.contains(id) ? FunctionBlockCategory.DEPRECATED : FunctionBlockCategory.UNKNOWN;
	}

	/**
	 * Resolve the {@link ServiceKind} a commodity FB id selects.
	 *
	 * @param id the FB number
	 * @return the service kind for a commodity FB, or empty for any other (or undefined) id
	 */
	public static Optional<ServiceKind> serviceKindOf(int id) {
		FunctionBlock fb = BY_ID.get(id);
		return fb == null ? Optional.empty() : fb.getServiceKind();
	}

	// --- Authorization Screen role helpers (PR C2b) -------------------------------------------
	//
	// These predicates classify an FB by the consent-UI role it plays:
	//   - implicit base   : structural prerequisite, never a customer-visible choice (FB 1, 4, 51)
	//   - commodity       : ServiceKind discriminator, drives commodity sections
	//   - data-shape mod. : display-only modifier on the energy-data response (e.g. UsageSummary)
	//   - PII-selectable  : individual consent checkbox in the customer/PII section
	//
	// They are computed from {@link #getId()} / {@link #getCategory()} and are non-localized
	// behavior. The customer-facing strings live in {@code messages.properties} (Spring i18n).

	/**
	 * Implicit-base FBs are structural prerequisites that must accompany any meaningful grant:
	 * <ul>
	 *   <li>{@code FB 1} &mdash; Common (Energy Usage): UsagePoint + ServiceCategory +
	 *       LocalTimeParameters in the energy feed.</li>
	 *   <li>{@code FB 4} &mdash; Interval Metering: MeterReading / ReadingType registry.</li>
	 *   <li>{@code FB 51} &mdash; Common (Retail Customer): cust:LocalTimeParameters for the
	 *       customer feed.</li>
	 * </ul>
	 * The Authorization Screen treats these as display-only context (a footer note), never as
	 * customer-selectable checkboxes.
	 */
	public boolean isImplicitBase() {
		return id == 1 || id == 4 || id == 51;
	}

	/** @see #isImplicitBase() */
	public static boolean isImplicitBase(int id) {
		return id == 1 || id == 4 || id == 51;
	}

	/**
	 * Commodity FBs (FB 5&ndash;11, 29) name what kind of meter the data comes from
	 * (electricity / gas / water / temperature) and, within a commodity, the measurement profile
	 * (e.g. interval / demand / net / reverse / register). The Authorization Screen groups
	 * customer usage points under their {@link ServiceKind}-matching commodity section and
	 * displays the requested profiles as a sub-line; the customer toggles individual usage
	 * points but not the profile FBs themselves.
	 */
	public boolean isCommodityProfile() {
		return category == FunctionBlockCategory.COMMODITY;
	}

	/** @see #isCommodityProfile() */
	public static boolean isCommodityProfile(int id) {
		return byId(id).map(FunctionBlock::isCommodityProfile).orElse(false);
	}

	/**
	 * Data-shape modifiers (FB 12, 15, 16, 17, 27, 28) shape <em>which response sections</em>
	 * are emitted by the energy endpoint (UsageSummary, ElectricPowerQualitySummary, etc.) but
	 * are orthogonal to the commodity. The Authorization Screen shows them as a display-only
	 * "you will receive" sub-line under each commodity section; the customer cannot toggle
	 * individual modifiers.
	 */
	public boolean isDataShapeModifier() {
		return category == FunctionBlockCategory.ENERGY_DATA_SHAPE;
	}

	/** @see #isDataShapeModifier() */
	public static boolean isDataShapeModifier(int id) {
		return byId(id).map(FunctionBlock::isDataShapeModifier).orElse(false);
	}

	/**
	 * Customer/PII FBs (FB 54&ndash;62) each carry a distinct {@code customer.xsd} resource
	 * family. The Authorization Screen renders one individually-toggleable consent checkbox per
	 * PII FB present in the requested scope, default-unchecked (explicit opt-in per FB).
	 */
	public boolean isPiiSelectable() {
		return category == FunctionBlockCategory.CUSTOMER_PII;
	}

	/** @see #isPiiSelectable() */
	public static boolean isPiiSelectable(int id) {
		return byId(id).map(FunctionBlock::isPiiSelectable).orElse(false);
	}
}
