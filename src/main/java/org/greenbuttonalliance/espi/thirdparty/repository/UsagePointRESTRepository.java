/*
 *     Copyright (c) 2018-2019 Green Button Alliance, Inc.
 *
 *     Portions copyright (c) 2013-2018 EnergyOS.org
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
 */

package org.greenbuttonalliance.espi.thirdparty.repository;

import org.greenbuttonalliance.espi.common.domain.UsagePoint;

import javax.xml.bind.JAXBException;
import java.util.List;

// TODO repository convergence with common
//
public interface UsagePointRESTRepository {
	List<UsagePoint> findAllByRetailCustomerId(Long id) throws JAXBException;

	UsagePoint findByHashedId(Long retailCustomerId, String usagePointHashedId)
			throws JAXBException;
}
