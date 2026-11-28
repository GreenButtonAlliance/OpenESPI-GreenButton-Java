-- #344 review: deleting a registered client deletes every row that refers to it, in the database.
--
-- ESPI application info, consent details and metrics have a foreign key to oauth2_registered_client
-- with no ON DELETE action, so a client that had been used could not be deleted until the application
-- deleted those rows first, from a list of tables it had to keep in step with the schema (the
-- daily_metrics_calculation event writes oauth2_client_metrics every day). Those keys now cascade.
--
-- oauth2_authorization and oauth2_authorization_consent had no foreign key at all, so a deleted
-- client's tokens could outlive it. They get one that cascades, as on PostgreSQL. Rows that already
-- refer to no client are deleted first, or the new keys could not be added: Spring Authorization
-- Server cannot load such an authorization (its client is gone), so no token in one still works.
-- These deletions are deliberately not audited: the rows were already unusable, and no deployment
-- holds any yet (#298 comes first).
-- oauth2_audit_log has no foreign key and keeps its rows: they are history.
--
-- Adding a foreign key rebuilds oauth2_authorization and blocks writes to it meanwhile: on a database
-- with many authorizations, run this migration in a maintenance window.

DELETE FROM oauth2_authorization
WHERE registered_client_id NOT IN (SELECT id FROM oauth2_registered_client);

DELETE FROM oauth2_authorization_consent
WHERE registered_client_id NOT IN (SELECT id FROM oauth2_registered_client);

ALTER TABLE oauth2_authorization
    ADD CONSTRAINT fk_oauth2_authorization_registered_client_id
    FOREIGN KEY (registered_client_id) REFERENCES oauth2_registered_client (id) ON DELETE CASCADE;

ALTER TABLE oauth2_authorization_consent
    ADD CONSTRAINT fk_oauth2_authorization_consent_registered_client_id
    FOREIGN KEY (registered_client_id) REFERENCES oauth2_registered_client (id) ON DELETE CASCADE;

ALTER TABLE espi_application_info DROP FOREIGN KEY fk_client_id;
ALTER TABLE espi_application_info
    ADD CONSTRAINT fk_client_id
    FOREIGN KEY (client_id) REFERENCES oauth2_registered_client (client_id) ON DELETE CASCADE;

ALTER TABLE oauth2_consent_details DROP FOREIGN KEY fk_consent_client;
ALTER TABLE oauth2_consent_details
    ADD CONSTRAINT fk_consent_client
    FOREIGN KEY (registered_client_id) REFERENCES oauth2_registered_client (id) ON DELETE CASCADE;

ALTER TABLE oauth2_client_metrics DROP FOREIGN KEY fk_metrics_client;
ALTER TABLE oauth2_client_metrics
    ADD CONSTRAINT fk_metrics_client
    FOREIGN KEY (client_id) REFERENCES oauth2_registered_client (client_id) ON DELETE CASCADE;
