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
 * Enumeration of phase identifiers. Allows designation of phases for both transmission
 * and distribution equipment, circuits and loads. Residential and small commercial loads
 * are often served from single-phase, or split-phase, secondary circuits. Phases 1 and 2
 * refer to hot wires that are 180 degrees out of phase, while N refers to the neutral wire.
 * Through single-phase transformer connections, these secondary circuits may be served from
 * one or two of the primary phases A, B, and C. For three-phase loads, use the A, B, C
 * phase codes instead of s12N.
 * <p>
 * Per ESPI 4.0 XSD: PhaseCodeKind enumeration (UInt16 values).
 */
public enum PhaseCodeKind {
	/**
	 * ABC to Neutral (three-phase, four-wire).
	 */
	ABCN(225),

	/**
	 * Involving all phases (three-phase).
	 */
	ABC(224),

	/**
	 * AB to Neutral.
	 */
	ABN(193),

	/**
	 * Phases A, C and neutral.
	 */
	ACN(41),

	/**
	 * BC to neutral.
	 */
	BCN(97),

	/**
	 * Phases A to B.
	 */
	AB(132),

	/**
	 * Phases A and C.
	 */
	AC(96),

	/**
	 * Phases B to C.
	 */
	BC(66),

	/**
	 * Phases A to neutral.
	 */
	AN(129),

	/**
	 * Phases B to neutral.
	 */
	BN(65),

	/**
	 * Phases C to neutral.
	 */
	CN(33),

	/**
	 * Phase A.
	 */
	A(128),

	/**
	 * Phase B.
	 */
	B(64),

	/**
	 * Phase C.
	 */
	C(32),

	/**
	 * Neutral.
	 */
	N(16),

	/**
	 * Phase S2 to neutral.
	 */
	S2N(272),

	/**
	 * Phase S1, S2 to neutral (split-phase secondary).
	 */
	S12N(784),

	/**
	 * Phase S1 to Neutral.
	 */
	S1N(528),

	/**
	 * Phase S2.
	 */
	S2(256),

	/**
	 * Phase S1 to S2.
	 */
	S12(768),

	/**
	 * Phase S1.
	 */
	S1(512),

	/**
	 * Not applicable to any phase.
	 */
	NONE(0),

	/**
	 * Phase A current relative to Phase A voltage.
	 */
	A_TO_AV(136),

	/**
	 * Phase B current or voltage relative to Phase A voltage.
	 */
	B_AV(72),

	/**
	 * Phase C current or voltage relative to Phase A voltage.
	 */
	C_AV(40),

	/**
	 * Neutral to ground.
	 */
	NG(17);

	private final Integer value;

	PhaseCodeKind(Integer value) {
		this.value = value;
	}

	public Integer getValue() {
		return value;
	}

	/**
	 * Converts an integer value to the corresponding PhaseCodeKind enum constant.
	 *
	 * @param value the integer value from XML/XSD
	 * @return the matching enum constant, or null if not found
	 */
	public static PhaseCodeKind fromValue(Integer value) {
		if (value == null) {
			return null;
		}
		for (PhaseCodeKind kind : PhaseCodeKind.values()) {
			if (kind.value.equals(value)) {
				return kind;
			}
		}
		return null;
	}
}
