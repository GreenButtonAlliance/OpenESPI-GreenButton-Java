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

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Immutable value object for one parsed NAESB ESPI 4.0 OAuth scope string.
 *
 * <p>ESPI scope grammar is a {@code ;}-delimited list of {@code key=value} terms, e.g.
 * {@code FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13}. The {@code FB}
 * term carries a {@code _}-delimited set of Function Block ids.</p>
 *
 * <p>The parser is intentionally <strong>FB-agnostic</strong>: it carries whatever FB ids appear
 * in the string (including spec-deprecated or future ids) without consulting the catalog. Semantic
 * questions &mdash; category, commodity {@link ServiceKind} &mdash; are answered on demand via
 * {@link FunctionBlock}. Unrecognized non-{@code FB} terms are preserved in
 * {@link #additionalParameters()} so no information is lost.</p>
 *
 * <p>This object holds <em>no</em> OAuth, HTTP, persistence, or resource knowledge. It is the shared
 * foundation for grant&rarr;subscription candidate resolution and resource-server enforcement (#122).</p>
 *
 * @param raw                  the original scope string as parsed
 * @param functionBlocks       FB ids from the {@code FB=} term, sorted ascending and immutable
 * @param intervalDuration     {@code IntervalDuration} term in seconds, or {@code null} if absent
 * @param blockDuration        {@code BlockDuration} term (e.g. {@code monthly}), or {@code null} if absent
 * @param historyLength        {@code HistoryLength} term, or {@code null} if absent
 * @param additionalParameters any other {@code key=value} terms (and bare tokens, with empty value),
 *                             insertion-ordered and immutable
 * @see FunctionBlock
 * @see FunctionBlockCategory
 */
public record EspiScope(
		String raw,
		Set<Integer> functionBlocks,
		Integer intervalDuration,
		String blockDuration,
		Integer historyLength,
		Map<String, String> additionalParameters
) {

	/** Canonical constructor; defensively copies collections into sorted/insertion-ordered immutable views. */
	public EspiScope {
		functionBlocks = functionBlocks == null
				? Collections.emptySortedSet()
				: Collections.unmodifiableSortedSet(new TreeSet<>(functionBlocks));
		additionalParameters = additionalParameters == null
				? Collections.emptyMap()
				: Collections.unmodifiableMap(new LinkedHashMap<>(additionalParameters));
	}

	/**
	 * Parse an ESPI scope string into an {@code EspiScope}.
	 *
	 * <p>Tolerant of surrounding whitespace, stray/empty {@code ;} segments, and mixed-case term
	 * keys. The recognized keys ({@code FB}, {@code IntervalDuration}, {@code BlockDuration},
	 * {@code HistoryLength}) are matched case-insensitively; every other {@code key=value} term is
	 * preserved in {@link #additionalParameters()}, and a bare token with no {@code =} is preserved
	 * with an empty value.</p>
	 *
	 * @param scope the scope string (e.g. {@code FB=4_5_15;IntervalDuration=3600})
	 * @return the parsed value object
	 * @throws IllegalArgumentException if {@code scope} is null/blank, or an {@code FB},
	 *                                  {@code IntervalDuration}, or {@code HistoryLength} value is
	 *                                  not an integer
	 */
	public static EspiScope parse(String scope) {
		if (scope == null || scope.isBlank()) {
			throw new IllegalArgumentException("ESPI scope must not be null or blank");
		}

		Set<Integer> fbs = new TreeSet<>();
		Integer intervalDuration = null;
		String blockDuration = null;
		Integer historyLength = null;
		Map<String, String> extras = new LinkedHashMap<>();

		for (String segment : scope.split(";")) {
			String term = segment.trim();
			if (term.isEmpty()) {
				continue;
			}
			int eq = term.indexOf('=');
			if (eq < 0) {
				extras.put(term, "");
				continue;
			}
			String key = term.substring(0, eq).trim();
			String value = term.substring(eq + 1).trim();
			switch (key.toLowerCase(Locale.ROOT)) {
				case "fb" -> {
					for (String token : value.split("_")) {
						String fb = token.trim();
						if (!fb.isEmpty()) {
							fbs.add(parseIntTerm(key, fb));
						}
					}
				}
				case "intervalduration" -> intervalDuration = parseIntTerm(key, value);
				case "historylength" -> historyLength = parseIntTerm(key, value);
				case "blockduration" -> blockDuration = value;
				default -> extras.put(key, value);
			}
		}

		return new EspiScope(scope, fbs, intervalDuration, blockDuration, historyLength, extras);
	}

	private static int parseIntTerm(String key, String value) {
		try {
			return Integer.parseInt(value);
		}
		catch (NumberFormatException e) {
			throw new IllegalArgumentException(
					"ESPI scope term '" + key + "' expected an integer but was '" + value + "'", e);
		}
	}

	/** @return whether the given FB id is present in this scope. */
	public boolean containsFunctionBlock(int functionBlock) {
		return functionBlocks.contains(functionBlock);
	}

	/**
	 * @param category the category to filter by
	 * @return the FB ids in this scope that belong to {@code category}, sorted and immutable
	 */
	public Set<Integer> functionBlocksIn(FunctionBlockCategory category) {
		TreeSet<Integer> result = new TreeSet<>();
		for (int fb : functionBlocks) {
			if (FunctionBlock.categoryOf(fb) == category) {
				result.add(fb);
			}
		}
		return Collections.unmodifiableSortedSet(result);
	}

	/**
	 * @return the distinct {@link ServiceKind}s selected by the commodity FBs in this scope.
	 *         Commodity FBs with no XSD-defined kind (FB 29 / Temperature) contribute none.
	 */
	public Set<ServiceKind> commodityServiceKinds() {
		Set<ServiceKind> kinds = EnumSet.noneOf(ServiceKind.class);
		for (int fb : functionBlocks) {
			FunctionBlock.serviceKindOf(fb).ifPresent(kinds::add);
		}
		return Collections.unmodifiableSet(kinds);
	}

	/** @return whether this scope grants any customer/PII FB (54-62). */
	public boolean includesCustomerPii() {
		return !functionBlocksIn(FunctionBlockCategory.CUSTOMER_PII).isEmpty();
	}

	/** @return whether this scope grants any energy data-shape FB (12, 15, 16, 17, 27, 28). */
	public boolean includesEnergyData() {
		return !functionBlocksIn(FunctionBlockCategory.ENERGY_DATA_SHAPE).isEmpty();
	}
}
