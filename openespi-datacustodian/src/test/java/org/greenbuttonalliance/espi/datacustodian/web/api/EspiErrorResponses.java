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

package org.greenbuttonalliance.espi.datacustodian.web.api;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What an ESPI resource rejection must carry (#316): the bare RFC 6750 challenge on a 403, the reason on a 400. A
 * 403 never says why: a reason naming the subscription or retail customer would let a client probe which exist.
 */
final class EspiErrorResponses {

	private EspiErrorResponses() {
	}

	/**
	 * 403 with exactly {@code WWW-Authenticate: Bearer error="insufficient_scope"} (RFC 6750 §3) and no body. Every
	 * 403 is the same, whoever refused the request, so that it says nothing about other customers' data.
	 */
	static ResultMatcher insufficientScope() {
		return result -> assertThat(result.getResponse()).satisfies(
				response -> assertThat(response.getStatus()).as("status").isEqualTo(403),
				response -> assertThat(response.getHeaders(HttpHeaders.WWW_AUTHENTICATE)).as("WWW-Authenticate")
						.containsExactly("Bearer error=\"insufficient_scope\""),
				response -> assertThat(response.getContentAsString()).as("body").isEmpty());
	}

	/** 400 with the reason as a one-line {@code text/plain} body. */
	static ResultMatcher badRequest(String reason) {
		return result -> assertThat(result.getResponse()).satisfies(
				response -> assertThat(response.getStatus()).as("status").isEqualTo(400),
				response -> assertThat(response.getContentType()).as("Content-Type")
						.startsWith(MediaType.TEXT_PLAIN_VALUE),
				response -> assertThat(response.getContentAsString()).as("body").isEqualTo(reason));
	}
}
