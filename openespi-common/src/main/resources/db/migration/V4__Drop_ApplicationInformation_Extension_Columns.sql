-- ==========================================================================================================
-- V4__Drop_ApplicationInformation_Extension_Columns.sql
--
-- Purpose: Remove extension columns from application_information table that are not in ESPI 4.0 XSD schema.
--          This migration aligns the database schema with the espi.xsd ApplicationInformation definition.
--
-- BREAKING CHANGE WARNING:
--   This migration PERMANENTLY deletes columns and data. Applications using these fields will break.
--   Extension fields removed:
--     1. kind
--     2. data_custodian_default_batch_resource
--     3. data_custodian_default_subscription_resource
--     4. data_custodian_third_party_selection_screen_uri
--     5. third_party_data_custodian_selection_screen_uri
--     6. third_party_login_screen_uri
--     7. third_party_application_name
--
-- Related: ApplicationInformationEntity.java and ApplicationInformationDto.java have been updated to
--          match XSD field sequence and remove extension fields.
--
-- Author: Claude Code (feature/fix-ApplicationInformation-structure)
-- Date: 2025-12-17
-- ==========================================================================================================

-- Drop extension columns from application_information table
-- Each column is dropped in a separate ALTER TABLE statement for H2 compatibility

ALTER TABLE application_information DROP COLUMN IF EXISTS kind;

ALTER TABLE application_information DROP COLUMN IF EXISTS data_custodian_default_batch_resource;

ALTER TABLE application_information DROP COLUMN IF EXISTS data_custodian_default_subscription_resource;

ALTER TABLE application_information DROP COLUMN IF EXISTS data_custodian_third_party_selection_screen_uri;

ALTER TABLE application_information DROP COLUMN IF EXISTS third_party_data_custodian_selection_screen_uri;

ALTER TABLE application_information DROP COLUMN IF EXISTS third_party_login_screen_uri;

ALTER TABLE application_information DROP COLUMN IF EXISTS third_party_application_name;

-- Note: All remaining columns match ESPI 4.0 XSD schema ApplicationInformation sequence (espi.xsd lines 62-246)