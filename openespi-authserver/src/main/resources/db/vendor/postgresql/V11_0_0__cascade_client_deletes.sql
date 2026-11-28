-- #344 review: deleting a registered client deletes every row that refers to it, in the database.
--
-- Each table below has a foreign key to oauth2_registered_client with no ON DELETE action, so a client
-- that had been used could not be deleted until the application deleted those rows first, from a list
-- of tables it had to keep in step with the schema. With ON DELETE CASCADE the client's DELETE removes
-- its authorizations (so its tokens no longer work), consents, ESPI application info, consent details
-- and metrics. The certificate tables (V6) already cascade. oauth2_audit_log has no foreign key and
-- keeps its rows: they are history.
--
-- Each key is re-checked against every row while its tables are locked: on a database with many
-- authorizations, run this migration in a maintenance window.

ALTER TABLE oauth2_authorization DROP CONSTRAINT fk_oauth2_authorization_registered_client_id;
ALTER TABLE oauth2_authorization
    ADD CONSTRAINT fk_oauth2_authorization_registered_client_id
    FOREIGN KEY (registered_client_id) REFERENCES oauth2_registered_client(id) ON DELETE CASCADE;

ALTER TABLE oauth2_authorization_consent DROP CONSTRAINT fk_oauth2_authorization_consent_registered_client_id;
ALTER TABLE oauth2_authorization_consent
    ADD CONSTRAINT fk_oauth2_authorization_consent_registered_client_id
    FOREIGN KEY (registered_client_id) REFERENCES oauth2_registered_client(id) ON DELETE CASCADE;

ALTER TABLE espi_application_info DROP CONSTRAINT fk_espi_application_info_client_id;
ALTER TABLE espi_application_info
    ADD CONSTRAINT fk_espi_application_info_client_id
    FOREIGN KEY (client_id) REFERENCES oauth2_registered_client(client_id) ON DELETE CASCADE;

ALTER TABLE oauth2_consent_details DROP CONSTRAINT fk_consent_client;
ALTER TABLE oauth2_consent_details
    ADD CONSTRAINT fk_consent_client
    FOREIGN KEY (registered_client_id) REFERENCES oauth2_registered_client(id) ON DELETE CASCADE;

ALTER TABLE oauth2_client_metrics DROP CONSTRAINT fk_metrics_client;
ALTER TABLE oauth2_client_metrics
    ADD CONSTRAINT fk_metrics_client
    FOREIGN KEY (client_id) REFERENCES oauth2_registered_client(client_id) ON DELETE CASCADE;
