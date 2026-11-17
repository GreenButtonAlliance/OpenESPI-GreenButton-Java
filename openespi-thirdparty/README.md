[![CircleCI](https://circleci.com/gh/GreenButtonAlliance/OpenESPI-ThirdParty-java/tree/master.svg?style=svg)](https://circleci.com/gh/GreenButtonAlliance/OpenESPI-ThirdParty-java/tree/master)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=GreenButtonAlliance_OpenESPI-ThirdParty-java&metric=alert_status)](https://sonarcloud.io/dashboard?id=GreenButtonAlliance_OpenESPI-ThirdParty-java)


# OpenESPI-ThirdParty -- Archived January 29, 2024

NOTE: This repository has been archived and is no longer being maintained. The code is preserved here for historical 
reference and access to the code base.

The Open Energy Services Provider Interface (ESPI) Third Party Repository Providing implementations of the interface used to provide energy usage information to retail customers.

An operational sandbox with these services operating may be found at:
<a href="https://sandbox.greenbuttonalliance.org:8443/ThirdParty">Green Button Alliance Third Party Sandbox</a>

## Setup

Note: You need to download and install [OpenESPI-Common-Java](https://github.com/GreenButtonAlliance/OpenESPI-Common-java) into your local Maven repository to build this project.

First clone the project from github:

```bash
git clone https://github.com/GreenButtonAlliance/OpenESPI-ThirdParty-java.git
cd OpenESPI-ThirdParty-java/
```

Build and start tomcat7 using maven (note: you must have first built the OpenESPI-Common-java jar):

```bash
mvn tomcat7:run
```

Now the application should be available at [http://localhost:9000/](http://localhost:9000/).


## Building and running (monorepo)
```bash
# From the repository root: build the Third Party and the modules it depends on
mvn clean install -pl openespi-thirdparty -am

# Run it. The Spring profile is chosen at run time, not with mvn -P:
cd openespi-thirdparty
mvn spring-boot:run                                          # local (default): in-memory H2, no database server
mvn spring-boot:run -Dspring-boot.run.profiles=dev-mysql      # MySQL on localhost:3306 (database thirdparty, user espi)
mvn spring-boot:run -Dspring-boot.run.profiles=dev-postgresql # PostgreSQL on localhost:5432 (database thirdparty, user espi)
```

In every profile Flyway creates the schema from the openespi-common scripts (`db/migration` plus
`db/vendor/{h2,mysql,postgres}`), and Hibernate only validates it (`ddl-auto: validate`).

**Existing MySQL or PostgreSQL database from before #267:** Hibernate built it, so Flyway refuses it
(or fails on V1's tables). Drop and recreate it once, then start the Third Party:

```bash
mysql -u root -p -e "DROP DATABASE thirdparty; CREATE DATABASE thirdparty; GRANT ALL ON thirdparty.* TO 'espi'@'%';"
dropdb thirdparty && createdb -O espi thirdparty
```

The `local` profile listens on `127.0.0.1` only: it seeds published DEV ONLY accounts (`admin`/`admin`,
`customer`/`customer`) and enables the H2 console. To reach the Third Party from another machine or
from outside a container, use `dev-mysql` or `dev-postgresql`.


## IDE Setup

### Eclipse Setup

Open Eclipse and import a Maven project (File > Import... > Maven > Existing Maven Projects).

### Spring Tool Suite Setup

Open Spring Tool Suite and import a Maven project (File > Import... > Maven > Existing Maven Projects).

To Run from within STS:

right click on project and select RunOnServer


To run the DC and/or the TP:

do a maven build and install accordingly. Then the WAR files will be in the right position.

To Start server:

```bash
sudo /home/bitnami/springsource/vfabric-tc-server-developer-2.9.3.RELEASE/base-instance/bin/tcruntime-ctl.sh start
```
To Stop server:

```bash
sudo /home/bitnami/springsource/vfabric-tc-server-developer-2.9.3.RELEASE/base-instance/bin/tcruntime-ctl.sh stop
```

### IntelliJ Setup

Open IntelliJ and open the project (File > Open...).

## Testing

### Unit Tests

To run all Unit tests:

```bash
mvn test
```

Run a single test class:

```bash
mvn -Dtest=<TestClassName> test
mvn -Dtest=HomeControllerTests test
```

Run a single test in a single class:

```bash
mvn -Dtest=<TestClassName>#<testMethodName> testMethodName
mvn -Dtest=HomeControllerTests#index_whenNotLoggedIn_displaysHomeView test
```

### Cucumber Features

To run all Cucumber features:

```bash
mvn verify
```

