/*
 *
 *        Copyright (c) 2025-2026 Green Button Alliance, Inc.
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

package org.greenbuttonalliance.espi.thirdparty.web.tools;

import org.greenbuttonalliance.espi.common.domain.usage.BatchListEntity;
import org.greenbuttonalliance.espi.common.service.BatchListService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ModelMap;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchListControllerTest {

	@Mock
	private BatchListService batchListService;

	@InjectMocks
	private BatchListController controller;

	@Test
	void index_putsAllBatchListsInModelAndDisplaysView() throws IOException {
		List<BatchListEntity> batchLists = List.of(new BatchListEntity(), new BatchListEntity());
		when(batchListService.findAll()).thenReturn(batchLists);
		ModelMap model = new ModelMap();

		assertThat(controller.index(model)).isEqualTo("tools/batchLists");
		assertThat(model).containsExactly(entry("batchLists", batchLists));
		verify(batchListService).findAll();
	}
}
