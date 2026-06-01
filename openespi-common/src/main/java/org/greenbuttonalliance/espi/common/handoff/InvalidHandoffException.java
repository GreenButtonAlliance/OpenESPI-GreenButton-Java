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

package org.greenbuttonalliance.espi.common.handoff;

/**
 * Thrown when a signed handoff token is malformed, tampered, expired, replayed, or otherwise
 * rejected. Callers should treat this as a 400 / "go back to start" condition &mdash; never reveal
 * which sub-check failed to the user-agent.
 */
public class InvalidHandoffException extends RuntimeException {

	public InvalidHandoffException(String message) {
		super(message);
	}

	public InvalidHandoffException(String message, Throwable cause) {
		super(message, cause);
	}
}
