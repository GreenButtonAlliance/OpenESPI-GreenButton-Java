Currently the flyway migrations are failing because of database specific data types.

The JPA Entities UsagePointEntity and TimeConfigurationEntity both have properties
which are a byte array (`byte[]`). The database types for persisting a byte array
are different for h2, mySql and Postgres. 

Refactor the database migration scripts to use the following file structor:

```
resources/db/
├── migration/             # Non-vendor specific migations
├── vendor/                # vendor specific migrations
│   ├── h2/                # H2 Migration Scripts
│   ├── mysql/             # Mysql Migration Scripts
│   ├── postgres/          # Postgres Migration Scripts
```

1. In the directory `resources/db/migration` create a flyway migration script called `v1_basetables.sql`. Add the 
   database tables from  `resources/db/migration/mysql/V1__Create_Complete_Schema_Mysql.sql` to `v1_basetables.sql`.

2. The database tables `time_configurations` and `usage_points` require vendor specific migtrations. Remove
   the create table statements from `v1_basetables.sql` and add vendor specific migration scripts for H2, Mysql, and Postgres
   using the convention `resources/db/{vendor name}/v2_{vendor name}_basetables.sql`.

3. Update the Spring Boot application properties file for default (h2 database), dev-mysql for mysql, and dev-postgresql
   for postgres. Each should use flyway migrations and use hibernate to validate the schema. Set the proper flyway
   schema locations for each vendor.

4. Update the tests `DataCustodianApplicationMysqlTest.java` and `DataCustodianApplicationPostgresTest.java` to use
   test containers and use `@DynamicPropertySource` to configure the database. Remove the redundant configuration settings
   from their respective `application.properties` file.

5. Add a new test using an H2 in memory database DataCustodianApplicationH2Test.java to test the H2 flyway migration
   when no active profile is set, or the test application profile is active. 

