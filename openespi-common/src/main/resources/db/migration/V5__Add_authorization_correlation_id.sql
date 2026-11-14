/*
 * OpenESPI Authorization correlation id (#202)
 *
 * Copyright (c) 2018-2026 Green Button Alliance, Inc.
 * Licensed under the Apache License, Version 2.0
 *
 * Adds the AS-supplied back-channel correlation id to the authorizations table so that
 * SubscriptionProvisioningService can make provisioning idempotent per OAuth2 grant: a repeated
 * provision for the same correlation id returns the existing Authorization/Subscription(s)
 * instead of creating new ones.
 *
 * Version 5 because V4 (handoff_nonces) ships from openespi-handoff on the same
 * classpath:db/migration location.
 *
 * Additive and vendor-neutral (MySQL, PostgreSQL, H2):
 * - nullable, so existing rows (and Authorizations not created via the back-channel) are unaffected;
 * - UNIQUE, so two concurrent provisions for the same grant cannot both persist. A UNIQUE index
 *   admits any number of NULLs on all three vendors.
 * Not part of the ESPI standard: this is implementation bookkeeping, never exported in XML.
 */

ALTER TABLE authorizations ADD COLUMN correlation_id VARCHAR(256);

CREATE UNIQUE INDEX uk_authorization_correlation_id ON authorizations (correlation_id);
