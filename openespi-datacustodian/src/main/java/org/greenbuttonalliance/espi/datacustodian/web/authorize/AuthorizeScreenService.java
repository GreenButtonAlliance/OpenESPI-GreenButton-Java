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

import lombok.RequiredArgsConstructor;
import org.greenbuttonalliance.espi.common.domain.common.ServiceCategory;
import org.greenbuttonalliance.espi.common.domain.usage.ApplicationInformationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.domain.usage.enums.ServiceKind;
import org.greenbuttonalliance.espi.handoff.InvalidHandoffException;
import org.greenbuttonalliance.espi.common.repositories.usage.ApplicationInformationRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository;
import org.greenbuttonalliance.espi.common.scope.EspiScope;
import org.greenbuttonalliance.espi.common.scope.FunctionBlock;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Pure business logic backing the Authorization Screen. Lives separately from
 * {@link AuthorizeScreenController} so the FB-subset validation, scope-narrowing, and view-model
 * construction can be unit-tested without spinning up Spring web context.
 *
 * <p>This service does NOT verify the signed handoff or consume the nonce &mdash; those are the
 * controller's responsibility (they're cross-cutting input verification, not authorization-screen
 * logic). The service starts from an already-verified {@code grantedScope} string.</p>
 */
@Service
@RequiredArgsConstructor
public class AuthorizeScreenService {

	private final ApplicationInformationRepository applicationInformationRepository;
	private final UsagePointRepository usagePointRepository;
	private final MessageSource messages;

	/**
	 * Validate the requesting third party and that the granted scope is a subset of what the third
	 * party registered. Throws {@link InvalidHandoffException} on any failure &mdash; the
	 * controller catches that and renders the uniform 400 error page.
	 *
	 * @param clientId       client_id from the verified outbound handoff
	 * @param grantedScope   scope from the verified outbound handoff
	 * @return the resolved {@link ApplicationInformationEntity}
	 * @throws InvalidHandoffException if the client_id is unknown or the granted scope's FB set
	 *                                 is not a subset of any combination of the third party's
	 *                                 registered scopes (defense in depth: AS is supposed to
	 *                                 enforce this at {@code /authorize}, DC re-verifies)
	 */
	public ApplicationInformationEntity validateClientAndScope(String clientId, String grantedScope) {
		ApplicationInformationEntity application = applicationInformationRepository
				.findByClientId(clientId)
				.orElseThrow(() -> new InvalidHandoffException("unknown client_id"));

		Set<Integer> requestedFbs = parseScope(grantedScope).functionBlocks();
		Set<Integer> registeredFbs = registeredFbSet(application);
		if (!registeredFbs.containsAll(requestedFbs)) {
			throw new InvalidHandoffException(
					"scope contains FB(s) not registered by client_id " + clientId + ": "
							+ requestedFbs.stream().filter(fb -> !registeredFbs.contains(fb)).toList());
		}
		return application;
	}

	/** Build the view model for the GET render. Customer authentication is assumed already done. */
	public AuthorizeScreenViewModel buildViewModel(ApplicationInformationEntity application,
												   String grantedScope,
												   Long retailCustomerId,
												   String handoffToken,
												   Locale locale) {
		EspiScope scope = parseScope(grantedScope);

		List<UsagePointEntity> customerUsagePoints =
				usagePointRepository.findAllByRetailCustomerId(retailCustomerId);

		List<AuthorizeScreenViewModel.CommoditySection> commoditySections =
				buildCommoditySections(scope, customerUsagePoints, locale);

		List<AuthorizeScreenViewModel.PiiOption> piiOptions = buildPiiOptions(scope, locale);

		List<String> implicitBaseLabels = scope.functionBlocks().stream()
				.filter(FunctionBlock::isImplicitBase)
				.sorted()
				.map(fb -> i18n("fb." + leftPad(fb) + ".label", locale))
				.toList();

		return new AuthorizeScreenViewModel(
				resolveTpDisplayName(application),
				commoditySections,
				piiOptions,
				implicitBaseLabels,
				handoffToken);
	}

	/**
	 * Compute the effective approved scope from the customer's checkbox decisions. The original
	 * granted scope is narrowed by:
	 * <ul>
	 *   <li>Commodity FBs: kept if at least one usage point of that commodity was selected.
	 *       Commodities the customer rejected entirely (no usage points checked) are dropped.</li>
	 *   <li>Energy data-shape FBs: kept if any commodity FB remains (data-shape modifiers only
	 *       matter alongside energy data).</li>
	 *   <li>Customer/PII FBs: kept only if the customer explicitly opted in.</li>
	 *   <li>Implicit base FBs (1, 4, 51): kept iff still relevant after the above narrowing
	 *       (energy bases iff any commodity remains; customer base iff any PII remains).</li>
	 *   <li>Other terms (IntervalDuration, BlockDuration, HistoryLength): preserved.</li>
	 * </ul>
	 *
	 * @return the narrowed scope string, or {@code null} if the customer denied everything
	 *         (no usage points selected AND no PII opt-ins)
	 */
	public String computeApprovedScope(String originalScope,
									   Set<UUID> selectedUsagePointIds,
									   Set<Integer> approvedPiiFbs,
									   List<UsagePointEntity> customerUsagePoints) {
		EspiScope scope = parseScope(originalScope);

		// Which commodities are still represented by at least one selected usage point?
		Set<ServiceKind> grantedKinds = EnumSet.noneOf(ServiceKind.class);
		customerUsagePoints.stream()
				.filter(up -> selectedUsagePointIds.contains(up.getId()))
				.map(up -> toServiceKind(up.getServiceCategory()))
				.filter(k -> k != null)
				.forEach(grantedKinds::add);

		Set<Integer> keptFbs = new TreeSet<>();
		for (Integer fb : scope.functionBlocks()) {
			if (FunctionBlock.isCommodityProfile(fb)) {
				// Keep the commodity profile only if its commodity has at least one selected up.
				// Special case FB 29 (Temperature): no XSD ServiceKind, can't pair to a UsagePoint
				// via standard mapping — drop unless explicitly granted via PII analogue (out of
				// scope here; FB 29 is a known standard gap).
				FunctionBlock.byId(fb).ifPresent(f -> {
					if (f.getServiceKind().map(grantedKinds::contains).orElse(false)) {
						keptFbs.add(fb);
					}
				});
			}
			else if (FunctionBlock.isPiiSelectable(fb)) {
				if (approvedPiiFbs.contains(fb)) {
					keptFbs.add(fb);
				}
			}
			else if (FunctionBlock.isDataShapeModifier(fb)) {
				// Data-shape modifiers only matter if some commodity is being granted.
				if (!grantedKinds.isEmpty()) {
					keptFbs.add(fb);
				}
			}
			else if (FunctionBlock.isImplicitBase(fb)) {
				// Implicit-base FBs are added below from the outcome, not preserved from input.
			}
			else {
				// Unknown / non-screen-aware FB: preserve as-is (defensive — the AS will further
				// reject anything truly invalid).
				keptFbs.add(fb);
			}
		}

		// Implicit base FBs are added based on the OUTCOME, not on whether they were in the
		// original scope: they are by definition structural prerequisites — the response cannot
		// be well-formed without them. Auto-adding here avoids the case where the TP requests
		// "FB=4_54" without FB 51 and the DC emits malformed customer feed content.
		if (!grantedKinds.isEmpty()) {
			keptFbs.add(1);   // Common (Energy Usage)
			keptFbs.add(4);   // Interval Metering registry
		}
		if (!approvedPiiFbs.isEmpty()) {
			keptFbs.add(51);  // Common (Retail Customer)
		}

		if (grantedKinds.isEmpty() && approvedPiiFbs.isEmpty()) {
			return null;
		}
		return rebuildScope(scope, keptFbs);
	}

	// --- helpers --------------------------------------------------------------------------

	private List<AuthorizeScreenViewModel.CommoditySection> buildCommoditySections(
			EspiScope scope, List<UsagePointEntity> customerUsagePoints, Locale locale) {

		Set<ServiceKind> requestedKinds = scope.commodityServiceKinds();
		if (requestedKinds.isEmpty()) {
			return List.of();
		}

		// Group profile FBs and data-shape FBs by category for the section labels.
		List<Integer> profileFbsSorted = scope.functionBlocks().stream()
				.filter(FunctionBlock::isCommodityProfile)
				.sorted().toList();
		List<String> dataShapeLabels = scope.functionBlocks().stream()
				.filter(FunctionBlock::isDataShapeModifier)
				.sorted()
				.map(fb -> i18n("fb." + leftPad(fb) + ".label", locale))
				.toList();

		List<AuthorizeScreenViewModel.CommoditySection> sections = new ArrayList<>(requestedKinds.size());
		for (ServiceKind kind : sortedKinds(requestedKinds)) {
			List<AuthorizeScreenViewModel.UsagePointChoice> ups = customerUsagePoints.stream()
					.filter(up -> kind == toServiceKind(up.getServiceCategory()))
					.sorted(Comparator.comparing(UsagePointEntity::getId))
					.map(up -> new AuthorizeScreenViewModel.UsagePointChoice(
							up.getId(),
							describeUsagePoint(up, locale)))
					.toList();

			List<String> profileLabelsForKind = profileFbsSorted.stream()
					.filter(fb -> FunctionBlock.byId(fb)
							.flatMap(FunctionBlock::getServiceKind)
							.map(k -> k == kind).orElse(false))
					.map(fb -> i18n("fb." + leftPad(fb) + ".label", locale))
					.toList();

			sections.add(new AuthorizeScreenViewModel.CommoditySection(
					i18n("commodity." + kind.name().toLowerCase(Locale.ROOT) + ".title", locale),
					kind,
					ups,
					profileLabelsForKind,
					dataShapeLabels));
		}
		return sections;
	}

	private List<AuthorizeScreenViewModel.PiiOption> buildPiiOptions(EspiScope scope, Locale locale) {
		return scope.functionBlocks().stream()
				.filter(FunctionBlock::isPiiSelectable)
				.sorted()
				.map(fb -> new AuthorizeScreenViewModel.PiiOption(
						fb,
						i18n("fb." + leftPad(fb) + ".label", locale),
						i18n("fb." + leftPad(fb) + ".description", locale)))
				.toList();
	}

	private Set<Integer> registeredFbSet(ApplicationInformationEntity application) {
		Set<Integer> fbs = new TreeSet<>();
		for (String registered : application.getScope()) {
			try {
				fbs.addAll(EspiScope.parse(registered).functionBlocks());
			}
			catch (IllegalArgumentException ignored) {
				// A malformed registered scope is a registration-time bug; conservatively skip it.
			}
		}
		return fbs;
	}

	private String resolveTpDisplayName(ApplicationInformationEntity application) {
		String desc = application.getThirdPartyApplicationDescription();
		if (desc != null && !desc.isBlank()) return desc;
		return application.getClientId();
	}

	private String describeUsagePoint(UsagePointEntity up, Locale locale) {
		// Sandbox-friendly: kind + short id. Production utilities would override the template
		// or supply a localized description from their CRM.
		String kindLabel = up.getServiceCategory() != null
				? i18n("commodity." + up.getServiceCategory().name().toLowerCase(Locale.ROOT) + ".title", locale)
				: "Unknown";
		String shortId = up.getId() != null ? up.getId().toString().substring(0, 8) : "?";
		return kindLabel + " (" + shortId + ")";
	}

	private static EspiScope parseScope(String raw) {
		try {
			return EspiScope.parse(raw);
		}
		catch (IllegalArgumentException e) {
			throw new InvalidHandoffException("unparseable scope", e);
		}
	}

	private static String rebuildScope(EspiScope original, Set<Integer> keptFbs) {
		StringBuilder sb = new StringBuilder();
		sb.append("FB=").append(keptFbs.stream().map(String::valueOf).collect(Collectors.joining("_")));
		if (original.intervalDuration() != null) {
			sb.append(";IntervalDuration=").append(original.intervalDuration());
		}
		if (original.blockDuration() != null) {
			sb.append(";BlockDuration=").append(original.blockDuration());
		}
		if (original.historyLength() != null) {
			sb.append(";HistoryLength=").append(original.historyLength());
		}
		original.additionalParameters().forEach((k, v) -> {
			sb.append(';').append(k);
			if (!v.isEmpty()) sb.append('=').append(v);
		});
		return sb.toString();
	}

	private static ServiceKind toServiceKind(ServiceCategory category) {
		if (category == null) return null;
		try {
			return ServiceKind.valueOf(category.name());
		}
		catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static List<ServiceKind> sortedKinds(Set<ServiceKind> kinds) {
		return kinds.stream().sorted(Comparator.comparingInt(ServiceKind::getValue)).toList();
	}

	private String i18n(String key, Locale locale) {
		return messages.getMessage(key, null, key, locale);
	}

	private static String leftPad(int fbId) {
		return fbId < 10 ? "0" + fbId : String.valueOf(fbId);
	}
}
