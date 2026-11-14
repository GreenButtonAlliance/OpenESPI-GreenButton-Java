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

package org.greenbuttonalliance.espi.datacustodian.web.api;

import org.greenbuttonalliance.espi.common.domain.common.ServiceCategory;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * #247: feed (collection) endpoints with real data and the REAL export services. The controller
 * mock tests stub every exporter, which is how the 500s on every non-empty feed went unnoticed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ESPI feed endpoints with real exporters (#247)")
class FeedEndpointsIntegrationTest {

    private static final String BASE = "http://localhost/espi/1_1/resource";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsagePointRepository usagePointRepository;

    @Test
    @DisplayName("GET /UsagePoint with data returns an Atom feed with one entry per usage point")
    void usagePointFeedWithData() throws Exception {
        UUID id = UUID.fromString("48c2a019-5598-5e16-b0f9-49e4ff27f5fb");
        UsagePointEntity usagePoint = new UsagePointEntity();
        usagePoint.setId(id);
        usagePoint.setDescription("Front of house meter");
        usagePoint.setServiceCategory(ServiceCategory.ELECTRICITY);
        usagePointRepository.saveAndFlush(usagePoint);

        String xml = mockMvc.perform(get("/espi/1_1/resource/UsagePoint").with(admin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(xml)
                .contains("<atom:feed", "<atom:title>UsagePoint Feed</atom:title>")
                .contains("rel=\"self\" href=\"" + BASE + "/UsagePoint\" type=\"espi-feed/UsagePoint\"")
                .contains("<atom:id>urn:uuid:" + id + "</atom:id>", "<atom:title>Front of house meter</atom:title>")
                .contains("href=\"" + BASE + "/UsagePoint/" + id + "\" type=\"espi-entry/UsagePoint\"")
                .contains("<espi:UsagePoint", "ELECTRICITY")
                .doesNotContain("xsi:type", "<atom:updated/>", "href=\"self\"");
    }

    @Test
    @DisplayName("GET /Authorization with no authorizations returns an empty Atom feed, not an empty body")
    void emptyAuthorizationFeed() throws Exception {
        String xml = mockMvc.perform(get("/espi/1_1/resource/Authorization").with(admin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(xml)
                .contains("<atom:feed", "<atom:title>Authorization Feed</atom:title>",
                        "rel=\"self\" href=\"" + BASE + "/Authorization\"")
                .doesNotContain("<atom:entry");
    }

    @Test
    @DisplayName("GET /Authorization/{unknown id} returns 404, not an empty 200")
    void unknownAuthorizationIs404() throws Exception {
        mockMvc.perform(get("/espi/1_1/resource/Authorization/" + UUID.randomUUID()).with(admin()))
                .andExpect(status().isNotFound());
    }

    private static RequestPostProcessor admin() {
        return opaqueToken().authorities(new SimpleGrantedAuthority("SCOPE_DataCustodian_Admin_Access"));
    }
}
