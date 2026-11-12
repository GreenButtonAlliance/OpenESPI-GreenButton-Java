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

package org.greenbuttonalliance.espi.authserver.config;

import org.greenbuttonalliance.espi.authserver.service.AuthorizationPurgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;

/**
 * Registers the scheduled purge of fully-expired {@code oauth2_authorization} rows (#192).
 *
 * <p>Active unless {@code espi.authserver.authorization-purge.enabled=false}. The job runs with a
 * fixed delay of {@code interval} after an {@code initial-delay}, both taken from the bound
 * {@link AuthorizationPurgeProperties}. A {@link Clock} bean is used when the context defines one
 * (tests); otherwise the system UTC clock.</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(AuthorizationPurgeProperties.class)
@ConditionalOnProperty(prefix = "espi.authserver.authorization-purge", name = "enabled",
		havingValue = "true", matchIfMissing = true)
public class AuthorizationPurgeConfig implements SchedulingConfigurer {

	private static final Logger logger = LoggerFactory.getLogger(AuthorizationPurgeConfig.class);

	private final AuthorizationPurgeProperties properties;
	private final ObjectProvider<AuthorizationPurgeService> purgeService;

	public AuthorizationPurgeConfig(AuthorizationPurgeProperties properties,
			ObjectProvider<AuthorizationPurgeService> purgeService) {
		this.properties = properties;
		this.purgeService = purgeService;
	}

	@Bean
	public AuthorizationPurgeService authorizationPurgeService(JdbcTemplate jdbcTemplate,
			PlatformTransactionManager transactionManager, ObjectProvider<Clock> clock) {
		return new AuthorizationPurgeService(jdbcTemplate, new TransactionTemplate(transactionManager),
				properties, clock.getIfAvailable(Clock::systemUTC));
	}

	@Override
	public void configureTasks(ScheduledTaskRegistrar registrar) {
		registrar.addFixedDelayTask(new FixedDelayTask(this::runPurge, properties.interval(),
				properties.initialDelay()));
	}

	private void runPurge() {
		try {
			purgeService.getObject().purge();
		} catch (RuntimeException e) {
			// Log and keep the schedule alive; the next run retries whatever was left.
			logger.error("Authorization purge run failed", e);
		}
	}
}
