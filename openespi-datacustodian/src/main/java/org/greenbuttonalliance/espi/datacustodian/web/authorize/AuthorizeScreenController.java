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

package org.greenbuttonalliance.espi.datacustodian.web.authorize;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.greenbuttonalliance.espi.common.domain.usage.ApplicationInformationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.handoff.HandoffNonceService;
import org.greenbuttonalliance.espi.handoff.InvalidHandoffException;
import org.greenbuttonalliance.espi.handoff.SignedHandoff;
import org.greenbuttonalliance.espi.handoff.SignedHandoffCodec;
import org.greenbuttonalliance.espi.common.repositories.usage.RetailCustomerRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * The customer-facing OAuth2 Authorization Screen.
 *
 * <p>Reached after the customer has authenticated through {@link
 * org.greenbuttonalliance.espi.datacustodian.web.LoginController} (PR C2a). The flow:</p>
 *
 * <ol>
 *   <li>AS redirects user-agent here with {@code ?handoff=<signed.payload>} (PR C1 codec).</li>
 *   <li>GET decodes + verifies the handoff, consumes the nonce, looks up
 *       {@link ApplicationInformationEntity}, validates the granted scope is a subset of the
 *       third party's registered scopes, builds the view model, renders.</li>
 *   <li>Customer makes selections, POSTs to this controller.</li>
 *   <li>POST re-verifies the handoff (the form posted it back), computes the effective approved
 *       scope from the customer's checkbox decisions, builds a {@link SignedHandoff.Return},
 *       redirects user-agent to the AS's {@code return_url} with the signed return token.</li>
 * </ol>
 *
 * <p>Mounted on the customer-login {@code SecurityFilterChain} (PR C2a). Any
 * {@link InvalidHandoffException} surfaces as HTTP 400 with the uniform error page &mdash; the
 * exception handler in this class logs the specific sub-cause for security audit but reveals
 * nothing to the user-agent (security principle: don't tell an attacker which check failed).</p>
 */
@Slf4j
@Controller
@RequestMapping("/oauth/authorize-screen")
@RequiredArgsConstructor
public class AuthorizeScreenController {

	private static final Duration RETURN_HANDOFF_TTL = Duration.ofMinutes(5);

	private final SignedHandoffCodec codec;
	private final HandoffNonceService nonceService;
	private final AuthorizeScreenService authorizeScreenService;
	private final RetailCustomerRepository retailCustomerRepository;
	private final UsagePointRepository usagePointRepository;
	private final Clock clock = Clock.systemUTC();

	@GetMapping
	public String show(@RequestParam("handoff") String handoffToken,
					   @AuthenticationPrincipal UserDetails principal,
					   Locale locale,
					   Model model) {
		SignedHandoff.Outbound outbound = decodeAndConsume(handoffToken);
		ApplicationInformationEntity application = authorizeScreenService.validateClientAndScope(
				outbound.clientId(), outbound.grantedScope());

		RetailCustomerEntity customer = customerOrReject(principal);

		AuthorizeScreenViewModel viewModel = authorizeScreenService.buildViewModel(
				application, outbound.grantedScope(), customer.getId(), handoffToken, locale);

		model.addAttribute("viewModel", viewModel);
		return "authorize-screen";
	}

	@PostMapping
	public String submit(@RequestParam("handoff") String handoffToken,
						 @RequestParam("decision") String decision,
						 @RequestParam(value = "selected_usage_point_ids", required = false) Set<UUID> selectedUsagePointIds,
						 @RequestParam(value = "approved_pii_fbs", required = false) Set<Integer> approvedPiiFbs,
						 @AuthenticationPrincipal UserDetails principal) {

		SignedHandoff.Outbound outbound = decodeAndConsume(handoffToken);
		ApplicationInformationEntity application = authorizeScreenService.validateClientAndScope(
				outbound.clientId(), outbound.grantedScope());

		RetailCustomerEntity customer = customerOrReject(principal);
		boolean isAllow = "allow".equalsIgnoreCase(decision);

		Set<UUID> selectedUps = selectedUsagePointIds != null && isAllow
				? new HashSet<>(selectedUsagePointIds) : Set.of();
		Set<Integer> approvedPii = approvedPiiFbs != null && isAllow
				? new TreeSet<>(approvedPiiFbs) : Set.of();

		List<UsagePointEntity> customerUsagePoints =
				usagePointRepository.findAllByRetailCustomerId(customer.getId());

		String approvedScope = isAllow
				? authorizeScreenService.computeApprovedScope(
						outbound.grantedScope(), selectedUps, approvedPii, customerUsagePoints)
				: null;

		String consent = (isAllow && approvedScope != null)
				? SignedHandoff.Return.CONSENT_ALLOW
				: SignedHandoff.Return.CONSENT_DENY;

		Instant now = clock.instant();
		SignedHandoff.Return returnPayload = SignedHandoff.Return.of(
				outbound.correlationId(),
				now,
				now.plus(RETURN_HANDOFF_TTL),
				nonceService.generate(),
				String.valueOf(customer.getId()),
				List.copyOf(selectedUps),
				/* customerResourceUri */ null, // DC produces the actual URI in PR B2's flow; the
				                                // Return handoff just signals approvalshapes
				consent,
				approvedScope);

		String returnToken = codec.encode(returnPayload);
		String separator = outbound.returnUrl().contains("?") ? "&" : "?";
		log.info("Authorization Screen: client={}, customer={}, decision={}, ups={}, piiFbs={}, correlation_id={}",
				application.getClientId(), customer.getId(), consent,
				selectedUps.size(), approvedPii.size(), outbound.correlationId());

		return "redirect:" + outbound.returnUrl() + separator + "handoff=" + returnToken;
	}

	@ExceptionHandler(InvalidHandoffException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public String handleInvalidHandoff(InvalidHandoffException e, HttpServletRequest request) {
		// Internal: full diagnostic for security audit
		log.warn("Authorization Screen handoff rejected: cause='{}', remoteAddr={}, userAgent='{}', path={}",
				e.getMessage(),
				request.getRemoteAddr(),
				request.getHeader("User-Agent"),
				request.getRequestURI());
		// External: uniform, generic, content-free
		return "error/400";
	}

	// --- helpers --------------------------------------------------------------------------

	private SignedHandoff.Outbound decodeAndConsume(String handoffToken) {
		SignedHandoff.Outbound outbound = codec.decodeOutbound(handoffToken);
		nonceService.consume(outbound.nonce(), outbound.expiresAt());
		return outbound;
	}

	private RetailCustomerEntity customerOrReject(UserDetails principal) {
		if (principal == null || principal.getUsername() == null) {
			throw new InvalidHandoffException("no authenticated principal on Authorization Screen request");
		}
		return retailCustomerRepository.findByUsername(principal.getUsername())
				.orElseThrow(() -> new InvalidHandoffException(
						"authenticated username has no RetailCustomer row: " + principal.getUsername()));
	}
}
