/*
 * OpenESPI Authorization client registration id (#146)
 *
 * Copyright (c) 2018-2026 Green Button Alliance, Inc.
 * Licensed under the Apache License, Version 2.0
 *
 * The Third Party stores each token it obtains from a Data Custodian (access and refresh token,
 * scope, resourceURI, authorizationURI, customerResourceURI) in its own authorizations table. A TP
 * customer may hold one Authorization per Spring OAuth2 client registration (for example
 * datacustodian-monthly and datacustodian-daily, which ask for different scopes), so the row records
 * which registration obtained it.
 *
 * Additive and vendor-neutral (MySQL, PostgreSQL, H2): nullable, so existing rows and the Data
 * Custodian's Authorizations (which never set it) are unaffected. Not part of the ESPI standard:
 * Third Party bookkeeping, never exported in XML.
 */

ALTER TABLE authorizations ADD COLUMN client_registration_id VARCHAR(100);

CREATE INDEX idx_authorization_client_registration_id ON authorizations (client_registration_id);
