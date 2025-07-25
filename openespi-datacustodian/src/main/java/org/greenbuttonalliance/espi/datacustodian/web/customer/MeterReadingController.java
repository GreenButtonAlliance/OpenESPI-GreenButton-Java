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

import org.greenbuttonalliance.espi.common.domain.usage.IntervalBlockEntity;
import org.greenbuttonalliance.espi.common.domain.usage.IntervalReadingEntity;
import org.greenbuttonalliance.espi.common.domain.usage.MeterReadingEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.MeterReadingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Iterator;

// @Controller - COMMENTED OUT: UI not needed in resource server
// @Component
public class MeterReadingController {

	@Autowired
	protected MeterReadingRepository meterReadingService;

	@Transactional(readOnly = true)
	@GetMapping("/RetailCustomer/{retailCustomerId}/UsagePoint/{usagePointId}/MeterReading/{meterReadingId}/show")
	public String show(@PathVariable Long retailCustomerId,
			@PathVariable Long usagePointId, @PathVariable Long meterReadingId,
			ModelMap model) {
		// TODO: Implement proper meter reading lookup by path parameters
		// Current repository only supports UUID lookup, need service layer method
		model.put("error", "MeterReading lookup not yet implemented for UI");
		return "/customer/error";

		/*
		// TODO: Implement when proper service layer is available
		MeterReadingEntity newMeterReading = new MeterReadingEntity();
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
		*/
	}

	public void setMeterReadingRepository(MeterReadingRepository meterReadingService) {
		this.meterReadingService = meterReadingService;
	}

	public MeterReadingRepository getMeterReadingRepository(
			MeterReadingRepository meterReadingService) {
		return this.meterReadingService;
	}

}