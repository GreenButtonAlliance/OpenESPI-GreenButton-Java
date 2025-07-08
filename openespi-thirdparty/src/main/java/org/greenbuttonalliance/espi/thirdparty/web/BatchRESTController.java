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

import org.greenbuttonalliance.espi.common.dto.usage.UsagePointDto;
import org.greenbuttonalliance.espi.thirdparty.service.WebClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/batch")
public class BatchRESTController {

	@Autowired
	private WebClientService webClientService;

	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ResponseEntity<String> handleGenericException(Exception e) {
		return ResponseEntity.badRequest().body("Error processing batch request: " + e.getMessage());
	}

	@GetMapping("/download/{customerId}")
	public ResponseEntity<List<UsagePointDto>> downloadCustomerData(
			@PathVariable Long customerId,
			@RequestHeader("Authorization") String accessToken) {
		
		try {
			// Use WebClient to fetch usage data from datacustodian
			List<UsagePointDto> usagePoints = webClientService
				.getAuthenticatedWebClient(accessToken)
				.get()
				.uri("/api/customers/{customerId}/usage-points", customerId)
				.retrieve()
				.bodyToFlux(UsagePointDto.class)
				.collectList()
				.block();
			
			return ResponseEntity.ok(usagePoints);
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}

	@GetMapping("/download/{customerId}/usage-point/{usagePointId}")
	public ResponseEntity<UsagePointDto> downloadUsagePointData(
			@PathVariable Long customerId,
			@PathVariable Long usagePointId,
			@RequestHeader("Authorization") String accessToken) {
		
		try {
			// Use WebClient to fetch specific usage point data from datacustodian
			UsagePointDto usagePoint = webClientService
				.getAuthenticatedWebClient(accessToken)
				.get()
				.uri("/api/customers/{customerId}/usage-points/{usagePointId}", customerId, usagePointId)
				.retrieve()
				.bodyToMono(UsagePointDto.class)
				.block();
			
			return ResponseEntity.ok(usagePoint);
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}

}
