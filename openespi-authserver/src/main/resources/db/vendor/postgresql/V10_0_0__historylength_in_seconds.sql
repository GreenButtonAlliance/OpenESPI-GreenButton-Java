-- #284: HistoryLength in an ESPI scope is a number of SECONDS (REQ.21.6.1.2.1, REQ.21.4.2.1.3.3).
-- The V1 and V3 seed data used month and day counts (HistoryLength=13 with BlockDuration=monthly,
-- HistoryLength=7 with BlockDuration=daily), which a REQ.21 Data Custodian reads as 13 or 7 seconds.
-- V1 and V3 are left as applied (Flyway checksums); this migration converts the seeded rows:
--   HistoryLength=13 -> 34128000 (395 days, about 13 months)
--   HistoryLength=7  -> 604800   (7 days)
-- (?![0-9]) keeps longer values (e.g. HistoryLength=130) unchanged. Audit log rows are history and stay as written.

UPDATE oauth2_registered_client
SET scopes = regexp_replace(
        regexp_replace(scopes, 'HistoryLength=13(?![0-9])', 'HistoryLength=34128000', 'g'),
        'HistoryLength=7(?![0-9])', 'HistoryLength=604800', 'g')
WHERE scopes ~ 'HistoryLength=(13|7)(?![0-9])';

UPDATE espi_application_info
SET scope = regexp_replace(
        regexp_replace(scope, 'HistoryLength=13(?![0-9])', 'HistoryLength=34128000', 'g'),
        'HistoryLength=7(?![0-9])', 'HistoryLength=604800', 'g')
WHERE scope ~ 'HistoryLength=(13|7)(?![0-9])';

UPDATE oauth2_consent_details
SET scopes_consented = regexp_replace(
        regexp_replace(scopes_consented, 'HistoryLength=13(?![0-9])', 'HistoryLength=34128000', 'g'),
        'HistoryLength=7(?![0-9])', 'HistoryLength=604800', 'g')
WHERE scopes_consented ~ 'HistoryLength=(13|7)(?![0-9])';
