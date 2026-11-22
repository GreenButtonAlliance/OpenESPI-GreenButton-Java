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

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * A 400 on the ESPI resource API whose reason the client is told: {@link EspiErrorResponseAdvice} writes it as a
 * one-line {@code text/plain} body (#316). The reason is the Data Custodian's own text about the request's form
 * (which parameter, what it must look like), never a value the request sent.
 */
public class EspiBadRequestException extends ResponseStatusException {

	public EspiBadRequestException(String reason) {
		super(HttpStatus.BAD_REQUEST, reason);
	}
}
