Inspect the Spring Data JPA interface 
`org.greenbuttonalliance.espi.common.repositories.usage.AuthorizationRepository` and its references. The 
Id property has been updated from a Long value to a UUID. Refactor references to use the UUID value.


Inspect the Spring Data JPA interface
`org.greenbuttonalliance.espi.common.repositories.usage.ElectricPowerQualitySummaryRepository` and its references. The
Id property has been updated from a Long value to a UUID. Refactor references to use the UUID value. Note, expect
other compile errors due to a larger refactoring effort.

Inspect the Spring Data JPA interface
`org.greenbuttonalliance.espi.common.repositories.usage.UsageSummaryRepository` and its references. The
Id property has been updated from a Long value to a UUID. Refactor references to use the UUID value. Note, expect
other compile errors due to a larger refactoring effort.

Inspect the Spring Data JPA interface
`org.greenbuttonalliance.espi.common.repositories.usage.MeterReadingRepository` and its references. The
Id property has been updated from a Long value to a UUID. Refactor references to use the UUID value. Note, expect
other compile errors due to a larger refactoring effort.

Inspect the Spring Data JPA interfaces
`org.greenbuttonalliance.espi.common.repositories.usage.RetailCustomerRepository` and
`org.greenbuttonalliance.espi.common.repositories.usage.ReadingTypeRepository` and their references. The
Id property has been updated from a Long value to a UUID. Refactor references to use the UUID value. Note, expect
other compile errors due to a larger refactoring effort.

Inspect the Spring Data JPA interfaces
`org.greenbuttonalliance.espi.common.repositories.usage.IntervalBlockRepository` and
`org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository` and their references. The
Id property has been updated from a Long value to a UUID. Refactor references to use the UUID value. Note, expect
other compile errors due to a larger refactoring effort.



Inspect the Spring Data JPA interfaces
`org.greenbuttonalliance.espi.common.repositories.usage.SubscriptionRepository` and
`org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository` and their references. The
Id property has been updated from a Long value to a UUID. Refactor references to use the UUID value. Note, expect
other compile errors due to a larger refactoring effort.

Inspect the Spring Data JPA interfaces
`org.greenbuttonalliance.espi.thirdparty.repository.MeterReadingRESTRepository` its references. The
Id property has been updated from a Long value to a UUID. Refactor references to use the UUID value. Note, expect
other compile errors due to a larger refactoring effort.

Inspect the controllers `NotificationController`, `RetailCustomerController` and `ModernAuthorizationController`
for errors, fix errors.

During the compile process, there are multiple warnings from mapstruct mappers having unmapped properties. 
Inspect the compile warnings and resolve problems. Verify there are no mapstruct warnings from the compile process.

Inspect the CustomerAccount JPA entity. Add a boolean property called `isPrePay` to the entity. Add flyway migrations
for mysql and postgres to add the column to the corresponding database table. Inspect the corresponding DTO class, add
the `isPrePay` property. Verify the mapstruct mapper is correct.

Inspect the Spring Data JPA Repository CustomerRepository. The id related JPA entity has been refactored from a long
to a UUID. Update the CustomerRepository for this change. Inspect usages of the query method findByUuid and refactor 
to use findById. Remove the findByUuid method from the repository.

Inspect the JPA Entity `AggregatedNodeRefEntity`. Refactor this entity to extend the class `IndentifiedObject`. Verify JPA
annotations are correct. Inspect references and update as needed.

Inspect the JPA Entity `LineItemEntity`. Refactor this entity to extend the class `IndentifiedObject`. Verify JPA
annotations are correct. Inspect references and update as needed.

Inspect the JPA Entity `PnodeRefEntity`. Refactor this entity to extend the class `IndentifiedObject`. Verify JPA
annotations are correct. Inspect references and update as needed.

Inspect the JPA Entity `ServiceDeliveryPointEntity`. Refactor this entity to extend the class `IndentifiedObject`. Verify JPA
annotations are correct. Inspect references and update as needed.

Inspect the JPA Entity `IntervalReadingEntity`. Refactor this entity to extend the class `IndentifiedObject`. Verify JPA
annotations are correct. Inspect references and update as needed.

Inspect the JPA Entity `ReadingQualityEntity`. Refactor this entity to extend the class `IndentifiedObject`. Verify JPA
annotations are correct. Inspect references and update as needed.

Inspect the JPA Entity `BatchListEntity`. Refactor this entity to extend the class `IndentifiedObject`. Verify JPA
annotations are correct. Inspect references and update as needed.

In the module `openespi-datacustodian`, create two new profiles using application-test.yml as the reference.
Do the following:
1. Add a new file called application-test-postgres.yml with ddl-auto set to validate, flyaway enabled true, and
   h2 database url configured to enable postgres compatability.
2. Add a new file called application-test-mysql.yml with ddl-auto set to validate, flyaway enabled true, and
   h2 database url configured to enable mysql compatability.
3. Create a new test like `DataCustodianApplicationTest` but configured to use the `test-postgres` profile.
4. Create a new test like `DataCustodianApplicationTest` but configured to use the `test-mysql` profile.
NOTE: There are known errors in the flyway migration scripts, the new tests are expected to fail.



In the module `openespi-common` inspect the JPA entities in the package `org.greenbuttonalliance.espi.common.domain`.
Also inspect the database migration scripts in `src/main/resources/db/migration`. There are migration scripts for mysql
and postgres. Your goal is to refactor to a single migration script for the initial database creation for either mysql or
postgres. Consolidate any alter table statements into the initial create table statement. Determine the proper order of 
table and constraint creation. Expect the migration to fail, if a constraint references a resource that has not been 
created.

In the module `openespi-datacustodian` there are two tests which will verify the flyway migration scripts are correct.
Use the test `DataCustodianApplicationMySqlTest` to verify the mysql flyway migration scripts, and the test 
`DataCustodianApplicationPostgresTest` to verify the postgres flyway migration scripts.

followup prompt:

In the module `openespi-common` inspect the JPA entities in the package `org.greenbuttonalliance.espi.common.domain`.
Also inspect the database migration scripts in `src/main/resources/db/migration`. There are migration scripts for mysql
and postgres. Your goal is to refactor to a single migration script for the initial database creation for either mysql or
postgres. Consolidate any alter table statements into the initial create table statement. Determine the proper order of
table and constraint creation. Expect the migration to fail, if a constraint references a resource that has not been
created.

In the module `openespi-datacustodian` there are two tests which will verify the flyway migration scripts are correct.
Use the test `DataCustodianApplicationMySqlTest` to verify the mysql flyway migration scripts, and the test
`DataCustodianApplicationPostgresTest` to verify the postgres flyway migration scripts.

The tests are still failing on schema validation errors. Run the tests, inspect the output, and fix errors.
The errors are reported as `nested exception is org.hibernate.tool.schema.spi.SchemaManagementException`

For example, the test output of: 

```text
09:18:34.366 [main] ERROR o.s.o.j.LocalContainerEntityManagerFactoryBean - Failed to initialize JPA EntityManagerFactory: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing table [aggregated_node_refs]
09:18:34.366 [main] WARN  o.s.w.c.s.GenericWebApplicationContext - Exception encountered during context initialization - cancelling refresh attempt: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'entityManagerFactory' defined in class path resource [org/springframework/boot/autoconfigure/orm/jpa/HibernateJpaConfiguration.class]: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing table [aggregated_node_refs]
```
This indicates a missing table of `aggregated_node_refs`. To fix this, inspect the corresponding JPA entity, `AggregatedNodeRefEntity` 
and add a create table statement for it.

Only update the migration scripts in the `openespi-common` module.

DO NOT ADD MIGRATION SCRIPTS TO THE MODULE `openespi-datacustodian`

Favor using the IDE over maven to execute the tests.

## v2 - refactored to common module

In the module `openespi-common` inspect the JPA entities in the package `org.greenbuttonalliance.espi.common.domain`.
Also inspect the database migration scripts in `src/main/resources/db/migration`. There are migration scripts for mysql
and postgres. Your goal is to refactor to a single migration script for the initial database creation for either mysql or
postgres. Consolidate any alter table statements into the initial create table statement. Determine the proper order of
table and constraint creation. Expect the migration to fail, if a constraint references a resource that has not been
created.

In the same module there are two tests which will verify the flyway migration scripts are correct.
Use the test `DataCustodianApplicationMySqlTest` to verify the mysql flyway migration scripts, and the test
`DataCustodianApplicationPostgresTest` to verify the postgres flyway migration scripts.

The tests are still failing on schema validation errors. Run the tests, inspect the output, and fix errors.
The errors are reported as `nested exception is org.hibernate.tool.schema.spi.SchemaManagementException`

For example, the test output of:

```text
09:18:34.366 [main] ERROR o.s.o.j.LocalContainerEntityManagerFactoryBean - Failed to initialize JPA EntityManagerFactory: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing table [aggregated_node_refs]
09:18:34.366 [main] WARN  o.s.w.c.s.GenericWebApplicationContext - Exception encountered during context initialization - cancelling refresh attempt: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'entityManagerFactory' defined in class path resource [org/springframework/boot/autoconfigure/orm/jpa/HibernateJpaConfiguration.class]: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing table [aggregated_node_refs]
```
This indicates a missing table of `aggregated_node_refs`. To fix this, inspect the corresponding JPA entity, `AggregatedNodeRefEntity`
and add a create table statement for it.

Favor using the IDE over maven to execute the tests.


-------------------
In the module `openespi-common` inspect the JPA entities in the package `org.greenbuttonalliance.espi.common.domain`.
Also inspect the database migration scripts in `src/main/resources/db/migration`. This is the default configuration
for a Spring Boot project.

In the same module there are two tests which will verify the flyway migration scripts are correct.
Use the test `DataCustodianApplicationMySqlTest` to verify the mysql flyway migration scripts, and the test
`DataCustodianApplicationPostgresTest` to verify the postgres flyway migration scripts.

Your goal is to refactor to a single migration script for the initial database creation for either mysql or
postgres. Consolidate any alter table statements into the initial create table statement. Determine the proper order of
table and constraint creation. Expect the migration to fail, if a constraint references a resource that has not been
created.

Identify the JPA Entities, and analyze the dependency tree.

In the sequence of the dependency tree, starting with entities without dependencies, 
create a refactoring plan.

For each entity, analyze its properties. Then inspect the flyway migration scripts for correctness. There are 
two sets of migration scripts, one for mysql, the second for postgres. Verify the necessary SQL is in the 
migration script for each database. Verify the SQL syntax and datatypes are correct for the target database.

Create a detailed enumerated task list according to your analysis. For each entity to be verified in the proper 
sequence, create a detailed task. Task items should have a placeholder [ ] for marking as done [x] upon 
task completion. Write the task list to `.junie/tasks-19.md` file. 

---------
Analyze and complete the task list `.junie/tasks-19.md`.