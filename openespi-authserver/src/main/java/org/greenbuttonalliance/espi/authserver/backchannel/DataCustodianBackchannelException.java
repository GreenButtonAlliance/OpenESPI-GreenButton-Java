/*
 * Copyright 2025 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package org.greenbuttonalliance.espi.authserver.backchannel;

/**
 * Thrown by {@link DataCustodianBackchannelClient} on any failure to reach DC or interpret its
 * response &mdash; network errors, timeouts, 4xx/5xx HTTP responses, and JSON parsing failures all
 * surface as this single type.
 *
 * <p>Callers at token-mint time should translate this into an OAuth2 {@code server_error} so the
 * TP receives a standard error response and no token is issued. The exception message captures
 * the underlying cause for AS-side logging.</p>
 */
public class DataCustodianBackchannelException extends RuntimeException {
	public DataCustodianBackchannelException(String message) {
		super(message);
	}

	public DataCustodianBackchannelException(String message, Throwable cause) {
		super(message, cause);
	}
}
