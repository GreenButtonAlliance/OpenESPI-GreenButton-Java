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

package org.greenbuttonalliance.espi.common.service.impl;

import lombok.RequiredArgsConstructor;
import org.greenbuttonalliance.espi.common.domain.usage.BatchListEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.BatchListRepository;
import org.greenbuttonalliance.espi.common.service.BatchListService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = { jakarta.xml.bind.JAXBException.class }, noRollbackFor = {
		jakarta.persistence.NoResultException.class,
		org.springframework.dao.EmptyResultDataAccessException.class })
@RequiredArgsConstructor
public class BatchListServiceImpl implements BatchListService {

	private final BatchListRepository repository;

	@Override
	public BatchListEntity save(BatchListEntity batchList) {
		return repository.save(batchList);
	}

	@Override
	public List<BatchListEntity> findAll() {
		return repository.findAll();
	}


	public BatchListRepository getRepository(BatchListRepository repository) {
		return repository;
	}
}
