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

package org.greenbuttonalliance.espi.datacustodian.web;

import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

public class BaseController {
	@ModelAttribute("currentCustomer")
	public RetailCustomerEntity currentCustomer(Principal principal) {
		try {
			System.out.printf("BaseController: currentCustomer -- %s\n",
					(RetailCustomerEntity) ((Authentication) principal)
							.getPrincipal());
			return (RetailCustomerEntity) ((Authentication) principal).getPrincipal();
		} catch (Exception e) {
			System.out.printf("BaseController: currentCustomer -- null\n");
			return null;
		}
	}
}
