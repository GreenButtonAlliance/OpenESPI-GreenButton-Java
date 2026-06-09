/*
 *
 *        Copyright (c) 2025 Green Button Alliance, Inc.
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

package org.greenbuttonalliance.espi.datacustodian.web.custodian;

import org.greenbuttonalliance.espi.common.domain.usage.ApplicationInformationEntity;
import org.greenbuttonalliance.espi.common.service.ApplicationInformationService;
import org.greenbuttonalliance.espi.common.service.NotificationService;
import org.greenbuttonalliance.espi.common.uri.EspiBatchUri;
import org.springframework.core.env.Environment;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Custodian "Notify Third Party" page (#177, #181). The admin composes an ESPI {@code BatchList}
 * (Atom) and POSTs it to a Third Party notification endpoint, reusing the #158 notification contract
 * via {@link NotificationService#notifyBatchList}.
 *
 * <p>The admin <strong>selects which and how many</strong> of the available resource URLs to include
 * (each has an "include" checkbox and an editable URL). The available set covers ApplicationInformation,
 * the Authorization feed, an Authorization entry, and the two subscription formats — an <em>energy</em>
 * subscription ({@code …/Batch/Subscription/{id}}) and a <em>PII/customer</em> subscription
 * ({@code …/Batch/RetailCustomer/{id}}), built via {@link EspiBatchUri}. Only checked, non-blank URLs
 * are marshalled into the BatchList.</p>
 */
@Controller
@PreAuthorize("hasRole('ROLE_CUSTODIAN')")
public class BatchNotificationController {

	private static final String DEFAULT_TP_NOTIFY_URI =
			"http://localhost:8082/ThirdParty/espi/1_1/Notification";

	private final NotificationService notificationService;
	private final ApplicationInformationService applicationInformationService;
	private final Environment environment;

	public BatchNotificationController(NotificationService notificationService,
									   ApplicationInformationService applicationInformationService,
									   Environment environment) {
		this.notificationService = notificationService;
		this.applicationInformationService = applicationInformationService;
		this.environment = environment;
	}

	@GetMapping("/custodian/notifications")
	public String form(Model model) {
		List<ApplicationInformationEntity> apps = applicationInformationService.findAll();
		if (!model.containsAttribute("notifyForm")) {
			model.addAttribute("notifyForm", defaultForm(apps));
		}
		// Notification URLs the admin can pick from (the registered third parties' notify endpoints).
		List<String> notifyUris = apps.stream()
				.map(ApplicationInformationEntity::getThirdPartyNotifyUri)
				.filter(u -> u != null && !u.isBlank())
				.distinct()
				.toList();
		model.addAttribute("notifyUris", notifyUris);
		return "custodian/notifications";
	}

	@PostMapping("/custodian/notifications/send")
	public String send(@ModelAttribute("notifyForm") NotifyForm form, RedirectAttributes redirectAttributes) {
		// Only the checked, non-blank URLs go into the BatchList — the admin chooses which/how many.
		List<String> resources = new ArrayList<>();
		addIf(resources, form.isIncludeApplicationInformation(), form.getApplicationInformationUrl());
		addIf(resources, form.isIncludeAuthorizationFeed(), form.getAuthorizationFeedUrl());
		addIf(resources, form.isIncludeAuthorizationEntry(), form.getAuthorizationEntryUrl());
		addIf(resources, form.isIncludeEnergySubscription(), form.getEnergySubscriptionUrl());
		addIf(resources, form.isIncludePiiSubscription(), form.getPiiSubscriptionUrl());

		try {
			notificationService.notifyBatchList(form.getNotificationUri(), resources);
			redirectAttributes.addFlashAttribute("message",
					"BatchList sent to " + form.getNotificationUri() + " (" + resources.size() + " resource(s)).");
			redirectAttributes.addFlashAttribute("messageType", "success");
		}
		catch (Exception e) {
			redirectAttributes.addFlashAttribute("message", "Failed to send BatchList: " + e.getMessage());
			redirectAttributes.addFlashAttribute("messageType", "danger");
		}
		// Preserve what the admin selected/typed so they can correct and resend.
		redirectAttributes.addFlashAttribute("notifyForm", form);
		return "redirect:/custodian/notifications";
	}

	private static void addIf(List<String> resources, boolean include, String url) {
		if (include && url != null && !url.isBlank()) {
			resources.add(url.trim());
		}
	}

	private NotifyForm defaultForm(List<ApplicationInformationEntity> apps) {
		String base = environment.getProperty("espi.datacustodian.base-url",
				"http://localhost:8081/DataCustodian");
		String resourceBase = base + "/espi/1_1/resource";
		NotifyForm f = new NotifyForm();
		f.setNotificationUri(apps.stream()
				.map(ApplicationInformationEntity::getThirdPartyNotifyUri)
				.filter(u -> u != null && !u.isBlank())
				.findFirst()
				.orElse(DEFAULT_TP_NOTIFY_URI));
		f.setApplicationInformationUrl(resourceBase + "/ApplicationInformation/{applicationInformationId}");
		f.setAuthorizationFeedUrl(resourceBase + "/Authorization");
		f.setAuthorizationEntryUrl(resourceBase + "/Authorization/{authorizationId}");
		// Two distinct ESPI subscription formats, built from the canonical URI builder (#160).
		f.setEnergySubscriptionUrl(EspiBatchUri.batchSubscription(resourceBase, "{subscriptionId}"));
		f.setPiiSubscriptionUrl(EspiBatchUri.batchRetailCustomer(resourceBase, "{retailCustomerId}"));
		return f;
	}

	/** Backing form: each candidate resource has an include flag + an editable URL. */
	public static class NotifyForm {
		private String notificationUri;

		private boolean includeApplicationInformation = true;
		private String applicationInformationUrl;
		private boolean includeAuthorizationFeed = true;
		private String authorizationFeedUrl;
		private boolean includeAuthorizationEntry = true;
		private String authorizationEntryUrl;
		private boolean includeEnergySubscription = true;
		private String energySubscriptionUrl;
		private boolean includePiiSubscription = true;
		private String piiSubscriptionUrl;

		public String getNotificationUri() { return notificationUri; }
		public void setNotificationUri(String v) { this.notificationUri = v; }

		public boolean isIncludeApplicationInformation() { return includeApplicationInformation; }
		public void setIncludeApplicationInformation(boolean v) { this.includeApplicationInformation = v; }
		public String getApplicationInformationUrl() { return applicationInformationUrl; }
		public void setApplicationInformationUrl(String v) { this.applicationInformationUrl = v; }

		public boolean isIncludeAuthorizationFeed() { return includeAuthorizationFeed; }
		public void setIncludeAuthorizationFeed(boolean v) { this.includeAuthorizationFeed = v; }
		public String getAuthorizationFeedUrl() { return authorizationFeedUrl; }
		public void setAuthorizationFeedUrl(String v) { this.authorizationFeedUrl = v; }

		public boolean isIncludeAuthorizationEntry() { return includeAuthorizationEntry; }
		public void setIncludeAuthorizationEntry(boolean v) { this.includeAuthorizationEntry = v; }
		public String getAuthorizationEntryUrl() { return authorizationEntryUrl; }
		public void setAuthorizationEntryUrl(String v) { this.authorizationEntryUrl = v; }

		public boolean isIncludeEnergySubscription() { return includeEnergySubscription; }
		public void setIncludeEnergySubscription(boolean v) { this.includeEnergySubscription = v; }
		public String getEnergySubscriptionUrl() { return energySubscriptionUrl; }
		public void setEnergySubscriptionUrl(String v) { this.energySubscriptionUrl = v; }

		public boolean isIncludePiiSubscription() { return includePiiSubscription; }
		public void setIncludePiiSubscription(boolean v) { this.includePiiSubscription = v; }
		public String getPiiSubscriptionUrl() { return piiSubscriptionUrl; }
		public void setPiiSubscriptionUrl(String v) { this.piiSubscriptionUrl = v; }
	}
}
