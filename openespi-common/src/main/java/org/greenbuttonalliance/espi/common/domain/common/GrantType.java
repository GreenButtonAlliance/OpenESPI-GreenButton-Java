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

public enum GrantType {
	AUTHORIZATION_CODE("authorization_code"),
	CLIENT_CREDENTIALS("client_credentials"),
	REFRESH_TOKEN("refresh_token");

	private final String value;

	GrantType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static GrantType fromValue(String value) {
		for (GrantType type : GrantType.values()) {
			if (type.value.equals(value)) {
				return type;
			}
		}
		return null;
	}
}