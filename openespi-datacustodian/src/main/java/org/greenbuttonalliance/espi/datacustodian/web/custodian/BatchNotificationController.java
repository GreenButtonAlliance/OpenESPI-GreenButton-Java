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
import org.springframework.core.env.Environment;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Stream;

/**
 * Custodian "Notify Third Party" page (#177). Lets an admin compose and send an ESPI
 * {@code BatchList} (Atom) of resource URLs — ApplicationInformation, Authorization feed,
 * Authorization entry, Subscription — to a Third Party notification endpoint, reusing the #158
 * notification contract via {@link NotificationService#notifyBatchList}.
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
		List<String> resources = Stream.of(
						form.getApplicationInformationUrl(),
						form.getAuthorizationFeedUrl(),
						form.getAuthorizationEntryUrl(),
						form.getSubscriptionUrl())
				.filter(u -> u != null && !u.isBlank())
				.toList();
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
		// Preserve what the admin typed so they can correct and resend.
		redirectAttributes.addFlashAttribute("notifyForm", form);
		return "redirect:/custodian/notifications";
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
		f.setSubscriptionUrl(resourceBase + "/Subscription/{subscriptionId}");
		return f;
	}

	/** Backing form for the notify page. */
	public static class NotifyForm {
		private String notificationUri;
		private String applicationInformationUrl;
		private String authorizationFeedUrl;
		private String authorizationEntryUrl;
		private String subscriptionUrl;

		public String getNotificationUri() { return notificationUri; }
		public void setNotificationUri(String notificationUri) { this.notificationUri = notificationUri; }
		public String getApplicationInformationUrl() { return applicationInformationUrl; }
		public void setApplicationInformationUrl(String v) { this.applicationInformationUrl = v; }
		public String getAuthorizationFeedUrl() { return authorizationFeedUrl; }
		public void setAuthorizationFeedUrl(String v) { this.authorizationFeedUrl = v; }
		public String getAuthorizationEntryUrl() { return authorizationEntryUrl; }
		public void setAuthorizationEntryUrl(String v) { this.authorizationEntryUrl = v; }
		public String getSubscriptionUrl() { return subscriptionUrl; }
		public void setSubscriptionUrl(String v) { this.subscriptionUrl = v; }
	}
}
