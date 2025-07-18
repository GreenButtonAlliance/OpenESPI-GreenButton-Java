-- 
-- Migration to remove incorrect readingTypeRef columns from usage_points table
-- Per ESPI standard, UsagePoint should reference ReadingType via ATOM links (rel="related")
-- not through embedded readingTypeRef fields in SummaryMeasurement objects
-- This resolves Hibernate 6.6 mapping validation issues
--

-- Drop indexes first to avoid constraint issues
DROP INDEX IF EXISTS idx_usage_points_estimated_load_reading_type_ref ON usage_points;
DROP INDEX IF EXISTS idx_usage_points_nominal_voltage_reading_type_ref ON usage_points;
DROP INDEX IF EXISTS idx_usage_points_rated_current_reading_type_ref ON usage_points;
DROP INDEX IF EXISTS idx_usage_points_rated_power_reading_type_ref ON usage_points;

-- Drop the incorrect readingTypeRef columns from usage_points table
ALTER TABLE usage_points DROP COLUMN IF EXISTS estimated_load_reading_type_ref;
ALTER TABLE usage_points DROP COLUMN IF EXISTS nominal_voltage_reading_type_ref;
ALTER TABLE usage_points DROP COLUMN IF EXISTS rated_current_reading_type_ref;
ALTER TABLE usage_points DROP COLUMN IF EXISTS rated_power_reading_type_ref;