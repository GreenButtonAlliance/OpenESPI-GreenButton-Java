/*
 *
 *        Copyright (c) 2025 Green Button Alliance, Inc.
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

package org.greenbuttonalliance.espi.common.uri;

import java.util.Optional;

/**
 * The single canonical builder and parser for the ESPI 4.0 <em>Batch</em> resource URIs carried in the
 * token-response / introspection {@code resourceURI}, {@code authorizationURI}, and
 * {@code customerResourceURI} fields (NAESB ESPI 4.0, REQ.21.6.2.1 API Interface).
 *
 * <p>The canonical forms (relative to a resource base such as
 * {@code https://{host}/DataCustodian/espi/1_1/resource}) are:</p>
 * <ul>
 *   <li>{@code .../Batch/Subscription/{subscriptionId}} — Subscription (authorization-code) flow</li>
 *   <li>{@code .../Batch/Bulk/{bulkId}} — Bulk (client-credentials) flow; the {@code bulkId} comes from
 *       the scope's {@code BR=} parameter</li>
 *   <li>{@code .../Batch/RetailCustomer/{retailCustomerId}} — customer/PII batch</li>
 *   <li>{@code .../Authorization/{authorizationId}}</li>
 * </ul>
 *
 * <p><strong>Why one class builds and parses:</strong> the producer (DC subscription provisioning) and
 * the consumers (DC {@code ResourceValidationFilter}, the resource-server introspector) previously
 * encoded these URL formats independently and drifted — the producer emitted {@code /Subscription/{id}}
 * while the consumer expected {@code /Batch/Subscription/{id}} (issue #160). Routing both sides through
 * this single class makes that drift structurally impossible.</p>
 */
public final class EspiBatchUri {

	private EspiBatchUri() {
	}

	// ------------------------------------------------------------------ builders

	/** {@code {resourceBase}/Batch/Subscription/{subscriptionId}}. */
	public static String batchSubscription(String resourceBase, Object subscriptionId) {
		return base(resourceBase) + "/Batch/Subscription/" + require(subscriptionId, "subscriptionId");
	}

	/** {@code {resourceBase}/Batch/Bulk/{bulkId}}. */
	public static String batchBulk(String resourceBase, Object bulkId) {
		return base(resourceBase) + "/Batch/Bulk/" + require(bulkId, "bulkId");
	}

	/** {@code {resourceBase}/Batch/RetailCustomer/{retailCustomerId}}. */
	public static String batchRetailCustomer(String resourceBase, Object retailCustomerId) {
		return base(resourceBase) + "/Batch/RetailCustomer/" + require(retailCustomerId, "retailCustomerId");
	}

	/** {@code {resourceBase}/Authorization/{authorizationId}}. */
	public static String authorization(String resourceBase, Object authorizationId) {
		return base(resourceBase) + "/Authorization/" + require(authorizationId, "authorizationId");
	}

	// ------------------------------------------------------------------ parsers

	/** Extract {@code subscriptionId} from a {@code .../Batch/Subscription/{id}...} URI. */
	public static Optional<String> subscriptionId(String uri) {
		return idAfter(uri, "/Batch/Subscription/");
	}

	/** Extract {@code bulkId} from a {@code .../Batch/Bulk/{id}...} URI. */
	public static Optional<String> bulkId(String uri) {
		return idAfter(uri, "/Batch/Bulk/");
	}

	/** Extract {@code retailCustomerId} from a {@code .../Batch/RetailCustomer/{id}...} URI. */
	public static Optional<String> retailCustomerId(String uri) {
		return idAfter(uri, "/Batch/RetailCustomer/");
	}

	/** Extract {@code authorizationId} from a {@code .../Authorization/{id}...} URI. */
	public static Optional<String> authorizationId(String uri) {
		return idAfter(uri, "/Authorization/");
	}

	/**
	 * Return the path segment immediately following {@code marker}, tolerant of deeper path segments and
	 * query/fragment suffixes — e.g. {@code .../Batch/Subscription/42/UsagePoint/7?x=1} yields {@code 42}.
	 * Returns empty when the marker is absent or the id segment is empty.
	 */
	static Optional<String> idAfter(String uri, String marker) {
		if (uri == null) {
			return Optional.empty();
		}
		int at = uri.indexOf(marker);
		if (at < 0) {
			return Optional.empty();
		}
		int start = at + marker.length();
		int end = start;
		while (end < uri.length()) {
			char c = uri.charAt(end);
			if (c == '/' || c == '?' || c == '#') {
				break;
			}
			end++;
		}
		String id = uri.substring(start, end);
		return id.isEmpty() ? Optional.empty() : Optional.of(id);
	}

	private static String base(String resourceBase) {
		if (resourceBase == null || resourceBase.isBlank()) {
			throw new IllegalArgumentException("resourceBase is required");
		}
		String trimmed = resourceBase.strip();
		return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
	}

	private static String require(Object id, String name) {
		if (id == null || id.toString().isBlank()) {
			throw new IllegalArgumentException(name + " is required");
		}
		return id.toString();
	}
}
