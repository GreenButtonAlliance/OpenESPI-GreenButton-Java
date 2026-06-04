/*
 * Copyright 2025 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.greenbuttonalliance.espi.authserver.integration;

import org.greenbuttonalliance.espi.authserver.AuthorizationServerApplication;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

/**
 * Shared base class for Authorization Server integration tests.
 *
 * <p>Provides the one harness combination that actually works for AS integration tests:</p>
 * <ul>
 *   <li>{@code webEnvironment = MOCK} + {@link AutoConfigureMockMvc} — a {@code MockMvc} bean is only
 *       created in the MOCK environment. The legacy {@code RANDOM_PORT + @AutoConfigureWebMvc}
 *       combination never produced one, so every test autowiring {@code MockMvc} failed at startup.</li>
 *   <li>A <strong>singleton</strong> MySQL Testcontainer started once for the whole JVM and reused by
 *       every subclass (Testcontainers' "singleton container" pattern). Far cheaper than a
 *       {@code @Container}-per-class container, and a real MySQL avoids the H2 {@code test} profile's
 *       Flyway/dialect drift.</li>
 *   <li>Flyway pointed at {@code classpath:db/vendor/mysql} (the AS's real MySQL migrations, V1/V2/V7).
 *       No {@code spring.flyway.schemas} — on MySQL the database is selected by the JDBC URL, and a
 *       Postgres-style {@code schemas: public} fails with "Unable to create schema `public`".</li>
 * </ul>
 *
 * <p>Subclasses just add {@code @Autowired MockMvc} and their test methods; the container, datasource,
 * and Flyway wiring are inherited. The {@code testcontainers} Spring profile supplies the rest of the
 * AS test configuration (see {@code application-testcontainers.yml}).</p>
 */
// Tagged so CI can run ONLY the Docker-backed integration tests (-Dgroups=testcontainers-it),
// excluding the AS module's pre-existing broken H2-profile unit tests. Inherited by subclasses.
@Tag("testcontainers-it")
@SpringBootTest(classes = AuthorizationServerApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("testcontainers")
public abstract class AbstractAuthserverIntegrationTest {

    /**
     * Singleton container: started once per JVM in the static initializer, never stopped explicitly
     * (Ryuk, Testcontainers' reaper, tears it down at JVM exit). Deliberately NOT {@code withReuse}:
     * the AS seeds its default clients at context startup with insert-if-absent semantics, so a
     * container surviving across runs would carry stale seed rows and silently mask seed-definition
     * changes (e.g. a client's {@code requireProofKey}). A fresh container per run keeps seed-dependent
     * assertions deterministic in both CI and the local inner loop.
     */
    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("oauth2_authserver")
            .withUsername("test_user")
            .withPassword("test_password");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);

        // AS MySQL migrations live at db/vendor/mysql (V1/V2/V7). No spring.flyway.schemas on MySQL.
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.locations", () -> "classpath:db/vendor/mysql");
        registry.add("spring.flyway.baseline-on-migrate", () -> true);

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");
    }
}
