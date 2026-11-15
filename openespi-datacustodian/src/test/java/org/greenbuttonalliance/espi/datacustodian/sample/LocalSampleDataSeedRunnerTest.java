/*
 *
 *    Copyright (c) 2018-2026 Green Button Alliance, Inc.
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

package org.greenbuttonalliance.espi.datacustodian.sample;

import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.IntervalBlockRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.RetailCustomerRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The {@code local} sample dataset (#253) against the real Flyway H2 schema ({@code test} wins over
 * {@code local} for the datasource, so the schema is migrated and validated, not generated): every seeded
 * ESPI feed marshals its entries, and the customer can log in. This is the real-data guard #247 asked for.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"local", "test"})
@DisplayName("Local sample data seed (#253)")
class LocalSampleDataSeedRunnerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalSampleDataSeedRunner runner;

    @Autowired
    private UsagePointRepository usagePointRepository;

    @Autowired
    private IntervalBlockRepository intervalBlockRepository;

    @Autowired
    private RetailCustomerRepository retailCustomerRepository;

    @Autowired
    private PasswordEncoder customerPasswordEncoder;

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "UsagePoint, 1", "MeterReading, 1", "ReadingType, 1", "IntervalBlock, 2", "UsageSummary, 1",
            "ElectricPowerQualitySummary, 1", "ApplicationInformation, 1", "Customer, 1", "CustomerAccount, 1"})
    @DisplayName("each seeded ESPI feed returns its sample entries to the DataCustodian admin")
    void feedsReturnSampleEntries(String resource, int entries) throws Exception {
        String xml = mockMvc.perform(get("/espi/1_1/resource/" + resource)
                        .with(opaqueToken().authorities(new SimpleGrantedAuthority("SCOPE_DataCustodian_Admin_Access"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(xml.split("<atom:entry>", -1)).hasSize(entries + 1);
    }

    @Test
    @Transactional
    @DisplayName("customer/customer owns the sample UsagePoint: 2 daily blocks of 24 hourly readings")
    void customerOwnsUsagePointWithHourlyReadings() {
        assertThat(retailCustomerRepository.findByUsername(LocalSampleDataSeedRunner.CUSTOMER_USERNAME))
                .hasValueSatisfying(customer -> assertThat(customer)
                        .extracting(RetailCustomerEntity::getRole, c -> customerPasswordEncoder.matches("customer", c.getPassword()))
                        .containsExactly(RetailCustomerEntity.ROLE_USER, true));
        assertThat(usagePointRepository.findById(runner.id("UsagePoint/1")))
                .hasValueSatisfying(usagePoint -> assertThat(usagePoint.getRetailCustomer().getUsername())
                        .isEqualTo(LocalSampleDataSeedRunner.CUSTOMER_USERNAME));
        assertThat(intervalBlockRepository.findAll())
                .hasSize(LocalSampleDataSeedRunner.DAYS)
                .allSatisfy(block -> assertThat(block.getIntervalReadings()).hasSize(24));
    }

    @Test
    @DisplayName("is idempotent and uses stable ids")
    void idempotentWithStableIds() throws Exception {
        long before = usagePointRepository.count();

        runner.run();

        assertThat(usagePointRepository.count()).isEqualTo(before);
        assertThat(runner.sampleResourceIds()).doesNotHaveDuplicates()
                .contains(runner.id("UsagePoint/1"));
    }
}
