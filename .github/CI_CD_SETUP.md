# CI/CD Setup Guide

This document explains the Continuous Integration and Continuous Deployment (CI/CD) setup for the OpenESPI GreenButton Java monorepo.

## Overview

The project uses **GitHub Actions** for CI/CD with **SonarCloud** integration for code quality analysis.

## Workflows

### 1. Main CI/CD Pipeline (`.github/workflows/ci.yml`)

**Triggers:**
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop` branches

**Jobs:**

#### build-and-test
- Runs on Ubuntu with Java 21
- Sets up MySQL 8.0 and PostgreSQL 15 services
- Builds all modules
- Runs unit tests for each module separately (authserver temporarily excluded)
- Runs integration tests with TestContainers
- Generates JaCoCo coverage reports
- Uploads test results and coverage reports as artifacts

**Note:** The `openespi-authserver` module is temporarily excluded from CI/CD until implementation is complete.

####sonarcloud
- Depends on successful `build-and-test` job
- Runs SonarCloud analysis with code coverage
- Uploads results to SonarCloud dashboard
- Checks quality gates

#### security-scan
- Runs OWASP Dependency Check
- Scans for known vulnerabilities in dependencies
- Uploads security reports as artifacts

### 2. Pull Request Checks (`.github/workflows/pr-checks.yml`)

**Triggers:**
- Pull request opened, synchronized, or reopened

**Jobs:**

#### pr-validation
- Validates PR title follows conventional commits format
- Checks code formatting with Spotless
- Runs quick tests on core modules
- Security vulnerability scan with CVSS threshold

#### sonarcloud-pr
- Runs SonarCloud analysis specific to the PR
- Provides inline PR comments with code quality issues
- Only runs for PRs from the same repository (not forks)

## SonarCloud Configuration

### Required Secrets

Add these secrets in GitHub repository settings (`Settings > Secrets and variables > Actions`):

1. **SONAR_TOKEN**
   - Generate from [SonarCloud Account](https://sonarcloud.io/account/security)
   - Go to SonarCloud → My Account → Security → Generate Token
   - Add as repository secret in GitHub

2. **GITHUB_TOKEN**
   - Automatically provided by GitHub Actions
   - No manual configuration needed

### SonarCloud Project Setup

1. **Import Project to SonarCloud:**
   ```bash
   # Visit https://sonarcloud.io
   # Click "+" → Analyze new project
   # Select "GreenButtonAlliance/OpenESPI-GreenButton-Java"
   # Choose "With GitHub Actions"
   ```

2. **Configure Organization:**
   - Organization key: `greenbuttonalliance`
   - Project key: `GreenButtonAlliance_OpenESPI-GreenButton-Java`

3. **Quality Gate:**
   - Uses SonarCloud default quality gate
   - Can be customized in SonarCloud project settings

### Configuration Files

- **`sonar-project.properties`** - Main SonarCloud configuration
  - Defines project structure
  - Configures coverage paths
  - Sets exclusions
  - Defines module structure

- **`pom.xml`** - Maven configuration
  - SonarCloud Maven plugin version
  - JaCoCo coverage plugin
  - SonarCloud properties

## Code Coverage

### JaCoCo Configuration

- Minimum coverage: 50% line coverage
- Reports generated in: `**/target/site/jacoco/`
- XML reports uploaded to SonarCloud

### Viewing Coverage

1. **Locally:**
   ```bash
   mvn clean test jacoco:report
   open openespi-common/target/site/jacoco/index.html
   ```

2. **SonarCloud:**
   - Visit https://sonarcloud.io/project/overview?id=GreenButtonAlliance_OpenESPI-GreenButton-Java
   - View coverage metrics and trends

## Running CI/CD Locally

### Run Tests Locally
```bash
# All tests
mvn clean test

# Specific module
mvn test -pl openespi-common

# With coverage
mvn clean test jacoco:report
```

### Run SonarCloud Analysis Locally
```bash
# Set SONAR_TOKEN environment variable
export SONAR_TOKEN=your-token-here

# Run analysis
mvn clean verify sonar:sonar \
  -Dsonar.projectKey=GreenButtonAlliance_OpenESPI-GreenButton-Java \
  -Dsonar.organization=greenbuttonalliance \
  -Dsonar.host.url=https://sonarcloud.io
```

### Run Integration Tests
```bash
# Requires Docker running
mvn verify -Pintegration-tests
```

## Database Services in CI

GitHub Actions workflows include MySQL and PostgreSQL services for testing:

- **MySQL 8.0**: Port 3306
  - Database: `test_db`
  - User: `root`
  - Password: `root`

- **PostgreSQL 15**: Port 5432
  - Database: `test_db`
  - User: `postgres`
  - Password: `postgres`

## Troubleshooting

### Tests Failing in CI but Pass Locally

1. **Check Java version:**
   - CI uses Java 21 (Temurin distribution)
   - Verify local Java version: `java -version`

2. **Database issues:**
   - CI uses MySQL 8.0 and PostgreSQL 15
   - Check local database versions match

3. **Environment variables:**
   - Review `SPRING_PROFILES_ACTIVE` settings
   - Check application-test.yml configuration

### SonarCloud Analysis Fails

1. **Token issues:**
   - Verify `SONAR_TOKEN` secret is set correctly
   - Check token hasn't expired

2. **Quality gate failures:**
   - Review SonarCloud dashboard for issues
   - Check coverage requirements met (minimum 50%)

3. **Build failures:**
   - Ensure `mvn clean verify` succeeds locally
   - Check all tests pass before SonarCloud analysis

### Security Scan Issues

1. **OWASP Dependency Check:**
   - Review `target/dependency-check-report.html`
   - Update vulnerable dependencies
   - Use `mvn versions:display-dependency-updates`

## Migration from CircleCI

The old CircleCI configuration (`openespi-thirdparty/.circleci/config.yml`) is deprecated:

**Issues with old config:**
- Used Java 8 (outdated)
- Tests were skipped
- Only covered ThirdParty module
- Hardcoded SonarCloud token (security risk)

**Migration benefits:**
- Modern Java 21
- All modules tested
- Secure token management
- Full monorepo support
- Better integration with GitHub

### Deprecation Notice

The CircleCI configuration should be removed after verifying GitHub Actions workflows are working correctly.

## Best Practices

1. **Never commit tokens:**
   - Always use GitHub Secrets
   - Never hardcode in workflow files

2. **Run tests before push:**
   ```bash
   mvn clean test
   ```

3. **Check coverage locally:**
   ```bash
   mvn clean test jacoco:report
   ```

4. **Review SonarCloud before merging:**
   - Check quality gate status
   - Review new issues in PR

5. **Keep dependencies updated:**
   - Run `mvn versions:display-dependency-updates` regularly
   - Address security vulnerabilities promptly

## Support

- **GitHub Actions**: https://docs.github.com/en/actions
- **SonarCloud**: https://docs.sonarcloud.io
- **JaCoCo**: https://www.jacoco.org/jacoco/trunk/doc/
- **Issues**: https://github.com/GreenButtonAlliance/OpenESPI-GreenButton-Java/issues