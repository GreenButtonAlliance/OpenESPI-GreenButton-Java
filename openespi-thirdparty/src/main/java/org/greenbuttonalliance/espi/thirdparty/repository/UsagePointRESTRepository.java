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

package org.greenbuttonalliance.espi.thirdparty.repository;

import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;

import jakarta.xml.bind.JAXBException;
import java.util.List;
import java.util.UUID;

// TODO repository convergence with common
//
public interface UsagePointRESTRepository {
	List<UsagePointEntity> findAllByRetailCustomerId(UUID id) throws JAXBException;

	UsagePointEntity findByHashedId(UUID retailCustomerId, String usagePointHashedId)
			throws JAXBException;
}
