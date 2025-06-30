-- Initialize database for OpenESPI Authorization Server
-- This script sets up the initial database configuration

-- Ensure proper character set and collation
ALTER DATABASE espi_authserver CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Grant additional privileges for Flyway migrations
GRANT CREATE, ALTER, DROP, INDEX, REFERENCES ON espi_authserver.* TO 'espi_user'@'%';

-- Create additional database for DataCustodian integration testing
CREATE DATABASE IF NOT EXISTS espi_datacustodian CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON espi_datacustodian.* TO 'espi_user'@'%';

-- Optimize for ESPI OAuth2 operations
SET GLOBAL innodb_buffer_pool_size = 268435456; -- 256MB
SET GLOBAL query_cache_size = 67108864; -- 64MB
SET GLOBAL max_connections = 100;

-- Flush privileges to ensure all changes take effect
FLUSH PRIVILEGES;

-- Display configuration
SELECT 'Database initialization completed for OpenESPI Authorization Server' AS status;