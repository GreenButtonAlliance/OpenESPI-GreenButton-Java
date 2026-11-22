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

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * The body of a 400 from the ESPI resource API (#316): one line of {@code text/plain} giving the reason, sent
 * whatever the request's {@code Accept} header says. The resources are {@code application/atom+xml} only (#312),
 * so Spring Boot's error {@code Map} cannot be written for an Atom-only request and the reason was lost.
 *
 * <p>A 401 or 403 has no body: its {@code WWW-Authenticate} challenge carries the RFC 6750 error code only
 * ({@code EspiBearerChallenge}). A 404 has none either, as it says nothing about whether the entry exists for
 * another token (#286). A 400's reason is about the request's form only, so it reveals no data.</p>
 */
@RestControllerAdvice(basePackageClasses = EspiErrorResponseAdvice.class)
class EspiErrorResponseAdvice {

	/** A query parameter of the wrong form, e.g. a {@code published-min} after its {@code published-max}. */
	@ExceptionHandler(EspiBadRequestException.class)
	void badRequest(EspiBadRequestException ex, HttpServletResponse response) throws IOException {
		write(response, ex.getReason());
	}

	/** A path or query value of the wrong type, e.g. an entry id that is not a UUID. Names the parameter only. */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	void typeMismatch(MethodArgumentTypeMismatchException ex, HttpServletResponse response) throws IOException {
		Class<?> type = ex.getRequiredType();
		write(response, ex.getName() + " is not a valid " + (type == null ? "value" : type.getSimpleName()));
	}

	private static void write(HttpServletResponse response, String reason) throws IOException {
		response.setStatus(HttpStatus.BAD_REQUEST.value());
		response.setContentType(MediaType.TEXT_PLAIN_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.getWriter().write(reason == null ? "Bad request" : reason);
	}
}
