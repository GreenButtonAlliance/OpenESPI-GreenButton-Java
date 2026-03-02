Status of Controller Migration

- Application Information - completed
- Customer Information - completed
- Customer Account - completed
- Electric Power Quality Summary - completed
- Interval Block - completed
- Meter Reading - completed
- Usage Point - completed
- Reading Type - completed
- Usage Point - completed
- Usage Summary - completed

Generally where the controller implement subscription or retail customer queries, stubbed out implementations added 
until mapping is complete.

Customer APIs not documented in the API spec, so not sure what the expected payloads are.

Open Issues:
- Batch Controller - not clear what the expected payloads are.
- Local Time Parameters - not clear what the expected payloads are.
- Service Status - not clear what the expected payloads are.
- Retail Customer - not clear what the expected payloads are.
- Time Configuration - not clear what the expected payloads are, or required functionality.
- Customer controllers had reference to `@accountSecurityService.hasAccessToAccount(authentication, #customerAccountId)`
  I was unable to find the implementation of this method.

Next Steps:
- Finish Remaining controllers.
- Consolidate SQL Migration Scripts.
- Complete subscription and retail customer functionality.
- Add Integration Tests for Postgres and MySQL.
  - Will need to load realistic test data.
- Improve testing of returned payloads.


---

Prompt to migrate the controllers to use the new design patterns:

Inspect the controllers UseagePointController, MeterReadingController, and ReadingTypeRESTController and their 
corresponding tests. These classes implement best practices for returning the required payloads. Note 
usage of the StreamingResponseBody.

Your task is to implement the same functionality in the RetailCustomerRESTController to return CustomerDto. 
The reference controllers use type specific implementations of the BaseExportService to process the response to the proper 
XML format. To complete this task, you will need to implement the BaseExportService for the CustomerDto.

The RetailCustomerRESTController is legacy code which needs to be refactored to support the new
design patterns. Update RetailCustomerRESTController to use the new design patterns and add proper test coverage.
