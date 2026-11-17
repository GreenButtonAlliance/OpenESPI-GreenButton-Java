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

package org.greenbuttonalliance.espi.thirdparty.oauth;

/** The Spring OAuth2 client registration ids the Third Party uses with the Data Custodian (application.yml). */
public final class DataCustodianRegistrations {

	/** authorization_code: a TP customer's monthly usage data. */
	public static final String MONTHLY = "datacustodian-monthly";

	/** authorization_code: a TP customer's daily usage data. */
	public static final String DAILY = "datacustodian-daily";

	/** client_credentials: the TP's client access token (ESPI Authorization and Bulk APIs). */
	public static final String CLIENT_ACCESS = "datacustodian-admin";

	private DataCustodianRegistrations() {
	}
}
