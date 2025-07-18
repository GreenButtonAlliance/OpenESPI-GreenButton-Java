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

package org.greenbuttonalliance.espi.thirdparty.web;

import org.greenbuttonalliance.espi.common.domain.usage.IntervalBlockEntity;
import org.greenbuttonalliance.espi.common.domain.usage.IntervalReadingEntity;
import org.greenbuttonalliance.espi.common.domain.usage.MeterReadingEntity;
import org.greenbuttonalliance.espi.thirdparty.service.MeterReadingRESTService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Iterator;

@Controller
@RequestMapping()
public class MeterReadingController extends BaseController {

	@Autowired
	protected MeterReadingRESTService meterReadingService;

	@Transactional(readOnly = true)
	@RequestMapping(value = "/RetailCustomer/{retailCustomerId}/UsagePoint/{usagePointId}/MeterReading/{meterReadingId}/show", method = RequestMethod.GET)
	public String show(@PathVariable Long retailCustomerId,
			@PathVariable Long usagePointId, @PathVariable Long meterReadingId,
			ModelMap model) {
		// TODO need to walk the subtree to force the load (for now)
		// TODO: Implement using REST service - MeterReadingRESTService doesn't have findById method
		// MeterReadingEntity mr = meterReadingService.findByUUID(retailCustomerId, UUID.fromString(meterReadingId.toString()));
		MeterReadingEntity mr = new MeterReadingEntity(); // Placeholder
		MeterReadingEntity newMeterReading = new MeterReadingEntity();
		newMeterReading.merge(mr);
		Iterator<IntervalBlockEntity> it = newMeterReading.getIntervalBlocks()
				.iterator();
		while (it.hasNext()) {
			IntervalBlockEntity temp = it.next();
			Iterator<IntervalReadingEntity> it1 = temp.getIntervalReadings()
					.iterator();
			while (it1.hasNext()) {
				IntervalReadingEntity temp1 = it1.next();
				temp1.getCost();
			}

		}
		model.put("meterReading", newMeterReading);
		return "/customer/meterreadings/show";
	}

	public void setMeterReadingService(MeterReadingRESTService service) {
		this.meterReadingService = service;
	}
}

/*
 * @Controller
 * 
 * @PreAuthorize("hasRole('ROLE_USER')") public class MeterReadingController
 * extends BaseController {
 * 
 * @Autowired private MeterReadingRESTService meterReadingService;
 * 
 * @RequestMapping(value = Routes.THIRD_PARTY_METER_READINGS_SHOW, method =
 * RequestMethod, produces = "application/atom+xml") @ResponseBody public String
 * show(@PathVariable String meterReadingId, ModelMap model, Principal
 * principal) throws JAXBException { RetailCustomer currentCustomer =
 * currentCustomer(principal); model.put("meterReading",
 * meterReadingService.findByUUID(currentCustomer.getId(),
 * UUID.fromString(meterReadingId))); return "/meterreadings/show"; }
 * 
 * public void setMeterReadingService(MeterReadingRESTService
 * meterReadingService) { this.meterReadingService = meterReadingService; } }
 */
