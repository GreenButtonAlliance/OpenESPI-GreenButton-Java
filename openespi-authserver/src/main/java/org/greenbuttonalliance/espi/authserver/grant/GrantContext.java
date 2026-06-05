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

package org.greenbuttonalliance.espi.authserver.grant;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * The Return-handoff fields the AS needs to remember between
 * {@code /oauth2/authorize/continue} (where the customer's selections arrive from DC) and the
 * back-channel call at {@code /oauth2/token} time. Saved into the HTTP session by
 * {@link org.greenbuttonalliance.espi.authserver.web.delegate.AuthorizeContinueController} and
 * stamped onto the corresponding {@code OAuth2Authorization.attributes} by
 * {@link GrantContextEnrichingAuthorizationService} when Spring AS persists the authorization at
 * code-issue time.
 *
 * <p>Implements {@link Serializable} because {@link jakarta.servlet.http.HttpSession} requires it
 * for replication/persistence.</p>
 *
 * @param correlationId         the AS-supplied trace id that ties the outbound, return, and
 *                              back-channel calls together in logs
 * @param retailCustomerId      DC primary key of the authenticated customer; numeric per DC's
 *                              {@code SubscriptionProvisionRequest} contract. The AS parses the
 *                              return handoff's opaque {@code principal} into this Long at
 *                              {@code /continue} time; non-numeric principals are rejected.
 * @param approvedScope         the customer's effective scope (post Authorization-Screen
 *                              narrowing); {@code ;}-delimited ESPI tokens
 * @param selectedUsagePointIds usage points the customer chose to share with the TP; may be empty
 *                              for grants that include only Customer/PII scope
 */
public record GrantContext(
		String correlationId,
		Long retailCustomerId,
		String approvedScope,
		List<UUID> selectedUsagePointIds
) implements Serializable {
}
