-- #344 review: deleting a registered client deletes every row that refers to it, in the database.
--
-- The H2 schema had no foreign keys to oauth2_registered_client, so a deleted client's authorizations
-- (and their tokens), consents and ESPI application info could outlive it. They now have the same
-- cascading keys as MySQL and PostgreSQL (H2 has no consent details or metrics tables, #145). Rows
-- that already refer to no client are deleted first, or the keys could not be added: Spring
-- Authorization Server cannot load such an authorization (its client is gone), so no token in one
-- still works. oauth2_audit_log has no foreign key and keeps its rows: they are history.

DELETE FROM oauth2_authorization
WHERE registered_client_id NOT IN (SELECT id FROM oauth2_registered_client);

DELETE FROM oauth2_authorization_consent
WHERE registered_client_id NOT IN (SELECT id FROM oauth2_registered_client);

DELETE FROM espi_application_info
WHERE client_id NOT IN (SELECT client_id FROM oauth2_registered_client);

ALTER TABLE oauth2_authorization
    ADD CONSTRAINT fk_oauth2_authorization_registered_client_id
    FOREIGN KEY (registered_client_id) REFERENCES oauth2_registered_client (id) ON DELETE CASCADE;

ALTER TABLE oauth2_authorization_consent
    ADD CONSTRAINT fk_oauth2_authorization_consent_registered_client_id
    FOREIGN KEY (registered_client_id) REFERENCES oauth2_registered_client (id) ON DELETE CASCADE;

ALTER TABLE espi_application_info
    ADD CONSTRAINT fk_espi_application_info_client_id
    FOREIGN KEY (client_id) REFERENCES oauth2_registered_client (client_id) ON DELETE CASCADE;
