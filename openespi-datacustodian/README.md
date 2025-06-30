[![CI/CD Pipeline](https://github.com/GreenButtonAlliance/OpenESPI-Common-java/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/GreenButtonAlliance/OpenESPI-Common-java/actions/workflows/ci.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=GreenButtonAlliance_OpenESPI-Common-java&metric=alert_status)](https://sonarcloud.io/dashboard?id=GreenButtonAlliance_OpenESPI-Common-java)


# OpenESPI-DataCustodian

This is the DataCustodian module of the OpenESPI GreenButton Java monorepo. It is a modern Spring Boot 3.5 OAuth2 resource server written in Java 21, providing Green Button energy data services.

This module implements the NAESB ESPI 4.0 specification for retail customers and authorized third parties to access energy usage data through standardized RESTful APIs.

An operational sandbox with these services operating may be found at:
<a href="https://sandbox.greenbuttonalliance.org:8443">sandbox.greenbuttonalliance.org:8443</a>

## Setup

As part of the OpenESPI GreenButton Java monorepo, this module is built together with other modules:

```bash
# Clone the monorepo (contains this module)
git clone https://github.com/GreenButtonAlliance/OpenESPI-GreenButton-Java.git
cd OpenESPI-GreenButton-Java

# Build all modules including openespi-datacustodian
mvn clean install

# Build only this module and its dependencies
mvn clean install -pl openespi-datacustodian -am

# Run the DataCustodian Spring Boot application
cd openespi-datacustodian && mvn spring-boot:run

# Access the application at http://localhost:8080
```

## IDE Setup

### Eclipse Setup

Open Eclipse and import the root monorepo as a Maven project (File > Import... > Maven > Existing Maven Projects). All modules including openespi-datacustodian will be imported automatically.

### Spring Tool Suite Setup

Open Spring Tool Suite and import the root monorepo as a Maven project (File > Import... > Maven > Existing Maven Projects).

### IntelliJ Setup

Open IntelliJ and open the root monorepo directory (File > Open...). IntelliJ will automatically detect all Maven modules.

## Testing

### Unit Tests
```bash
# Run tests for this module only
mvn clean test -pl openespi-datacustodian

# Run tests with dependencies
mvn clean test -pl openespi-datacustodian -am

# Run all tests in the monorepo
mvn clean test
```

### Integration Testing

Integration testing is performed using the [Green Button CMD Test Harness](https://github.com/greenbuttonalliance/OpenESPI-GreenButtonCMDTest.git). See the [README](https://github.com/greenbuttonalliance/OpenESPI-GreenButtonCMDTest/blob/master/README.md) for instructions.

## API Documentation

The DataCustodian provides the following REST APIs:
- **Green Button Download My Data (DMD)** - Customer energy data download
- **Green Button Connect My Data (CMD)** - Third-party authorization and data access
- **OAuth2 Resource Server** - Protected API endpoints with token validation
- **ESPI 4.0 Resource APIs** - Usage points, meter readings, interval blocks

For API reference documentation, start the application and visit `/swagger-ui` or `/api-docs`.
