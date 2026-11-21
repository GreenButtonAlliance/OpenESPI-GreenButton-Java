/*
 * Copyright (c) 2026 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.greenbuttonalliance.espi.thirdparty.subscription;

/**
 * Development aid: keeps what the Data Custodian answered to a {@code Batch/Subscription} fetch.
 * It sees only the subscription id, the HTTP status and the body, never an access token. Callers
 * take it through an {@code ObjectProvider} and use it only when present.
 */
public interface SubscriptionFeedCapture {

	void capture(String subscriptionId, int httpStatus, byte[] body);
}
