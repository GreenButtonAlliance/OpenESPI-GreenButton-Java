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


package org.greenbuttonalliance.espi.datacustodian.web.customer;

import org.greenbuttonalliance.espi.common.domain.usage.ApplicationInformationEntity;
// import org.greenbuttonalliance.espi.common.domain.usage.Routes; // Missing class
import org.greenbuttonalliance.espi.common.repositories.usage.ApplicationInformationRepository;
import org.greenbuttonalliance.espi.datacustodian.web.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.NoResultException;
import jakarta.servlet.http.HttpServletRequest;

import static org.greenbuttonalliance.espi.datacustodian.utils.URLHelper.newScopeParams;

// @Controller - COMMENTED OUT: OAuth2 flows handled by openespi-thirdparty
// @PreAuthorize("hasRole('ROLE_USER')")
// @Component
public class ScopeSelectionController extends BaseController {

	@Autowired
	private ApplicationInformationRepository applicationInformationRepository;

	@ExceptionHandler(Exception.class)
	@ResponseStatus(value = HttpStatus.FORBIDDEN, reason = "Access Not Authorized")
	public void handleGenericException() {
	}

	@RequestMapping(value = "/custodian/scope_selection", method = RequestMethod.GET)
	public String scopeSelection(HttpServletRequest request, String[] scopes,
			@RequestParam("ThirdPartyID") String thirdPartyClientId)
			throws Exception {

		try {
			ApplicationInformationEntity applicationInformation = applicationInformationRepository
					.findByClientId(thirdPartyClientId).orElse(null);
			
			if (applicationInformation == null) {
				throw new NoResultException("ApplicationInformation not found for client: " + thirdPartyClientId);
			}

			return "redirect:"
					+ applicationInformation
							.getThirdPartyScopeSelectionScreenURI() + "?"
					+ newScopeParams(applicationInformation.getScope())
					+ "&DataCustodianID="
					+ applicationInformation.getDataCustodianId();
		} catch (NoResultException | EmptyResultDataAccessException e) {
			System.out.printf(
					"ScopeSelectionController: ApplicationInformation record not found!  "
							+ "ThirdPartyID = %s\n", thirdPartyClientId);
			throw new Exception("Access Not Authorized");
		}
	}

	public void setApplicationInformationRepository(
			ApplicationInformationRepository applicationInformationRepository) {
		this.applicationInformationRepository = applicationInformationRepository;
	}
}