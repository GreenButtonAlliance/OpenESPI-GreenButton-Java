Goal of the update is to improve the test coverage of the JPA Test Entities. Inspect the module openespi-common, in 
the package org.greenbuttonalliance.espi.common.domain identify the JPA Entities. For each entity, create a 
@DataJPA test using the active profile of `test` to use the H2 in memory database. 

Create tests for:
- CRUD Operations
- All JPA Respository query methods. 
- Test persistence of child relations. 
- Test validation constraints.