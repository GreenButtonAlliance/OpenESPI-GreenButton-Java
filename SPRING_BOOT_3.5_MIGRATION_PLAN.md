# OpenESPI Green Button Alliance - Spring Boot 3.5 Migration Plan

## Executive Summary

This document outlines the comprehensive migration plan for upgrading the OpenESPI (Open Energy Services Provider Interface) ecosystem from legacy Spring Framework to Spring Boot 3.5. The migration addresses critical security vulnerabilities, architectural modernization, and NAESB ESPI 4.0 compliance requirements.

**Critical Finding**: The openespi-datacustodian module requires immediate completion of its Spring Boot 3.5 migration with security hardening. The openespi-thirdparty module will be treated as a separate rewrite project.

## Current State Assessment

### Architecture Overview
- **openespi-common**: Service layer and JPA mapping (Spring Boot 3.5 core migration complete)
- **openespi-datacustodian**: Data custodian REST API (PRIMARY FOCUS - migration 75% complete)
- **openespi-authserver**: OAuth2 Authorization Server (modern Spring Authorization Server)
- **openespi-thirdparty**: Third-party application interface (legacy - deprioritized for complete rewrite)

### Migration Status
- ✅ **openespi-common**: Core Spring Boot 3.5 migration completed, security hardening needed
- 🔄 **openespi-datacustodian**: Controller modernization 75% complete - PRIMARY FOCUS
- ✅ **openespi-authserver**: Modern implementation complete, security improvements needed
- 📋 **openespi-thirdparty**: Legacy module - scheduled for complete rewrite (separate project)

## Critical Security Issues Identified

### URGENT (Fix within 7 days)
1. **Disabled Authentication** in openespi-datacustodian
   - OAuth2 Resource Server configuration incomplete in SecurityConfiguration.java
   - **Risk**: Unauthenticated access to energy data APIs
   - **Impact**: CRITICAL - Core functionality exposed

2. **Repository Migration Incomplete**
   - ~25% of datacustodian controllers still using legacy patterns
   - Compilation errors preventing application startup
   - **Risk**: Application instability, inconsistent data access

3. **Hardcoded Database Credentials**
   - Production credentials in application-dev-mysql.yml
   - **Risk**: Database compromise, credential exposure

### HIGH PRIORITY (Fix within 30 days)
- Insecure CORS configuration allowing any origin in datacustodian
- Missing security headers (HSTS, X-Frame-Options) in SecurityConfiguration
- Incomplete jakarta namespace migration in openespi-common
- {noop} password encoding in authserver production configs
- System.out.printf logging exposing customer data in common services

## Migration Plan

### Phase 1: Emergency Security Fixes (Weeks 1-2)

#### Week 1: Complete DataCustodian Migration
**Priority: CRITICAL**

1. **Complete Repository Migration in DataCustodian**
   - Fix remaining 2 controller compilation errors (ScopeSelectionController, UsagePointController)
   - Complete ApplicationInformationEntity type mismatches
   - Test all ESPI REST endpoints for functionality

2. **Enable OAuth2 Resource Server Security**
   ```java
   @EnableWebSecurity
   public class SecurityConfiguration {
       @Bean
       public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
           http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
           return http.build();
       }
   }
   ```

3. **Database Security Emergency Fixes**
   - Externalize all hardcoded database credentials
   - Secure H2 console access in development environments

#### Week 2: Security Hardening DataCustodian & Common
1. **CORS Security Configuration**
   - Replace insecure wildcard CORS with specific allowed origins
   - Implement Spring Security CORS configuration
   - Remove legacy CORSFilter.java

2. **Complete Jakarta Namespace Migration**
   ```bash
   # Complete remaining javax -> jakarta in openespi-common
   find openespi-common -name "*.java" -exec sed -i 's/javax\./jakarta\./g' {} \;
   ```

3. **Input Validation & Logging Security**
   - Add @Valid annotations to all REST controller parameters
   - Replace System.out.printf with proper logging framework
   - Add SQL injection prevention to @Query parameters

### Phase 2: Architecture Modernization (Weeks 3-8)

#### Weeks 3-4: Service Layer & Security Enhancement
1. **Complete Common Module Security**
   - Encrypt sensitive data in ApplicationInformationEntity and AuthorizationEntity
   - Implement proper password hashing with @JsonIgnore for RetailCustomerEntity
   - Add @PreAuthorize security annotations to service methods

2. **Replace Deprecated Dependencies**
   - Replace deprecated OAuth2 dependencies with Spring Security 6.x
   - Update MediaType.APPLICATION_JSON_UTF8 usage
   - Replace com.sun.syndication dependency in BatchRESTController

3. **AuthServer Security Improvements**
   ```java
   @Bean
   public PasswordEncoder passwordEncoder() {
       return new BCryptPasswordEncoder(12);
   }
   ```

#### Weeks 5-6: Security Implementation
1. **Comprehensive Security Headers**
   ```java
   http.headers(headers -> headers
       .frameOptions(DENY)
       .contentTypeOptions(nosniff())
       .httpStrictTransportSecurity(hstsConfig -> hstsConfig
           .maxAgeInSeconds(31536000)
           .includeSubdomains(true)
           .preload(true)));
   ```

2. **RBAC Implementation**
   - Implement @PreAuthorize annotations
   - Define ESPI-specific roles and permissions
   - Add method-level security

3. **Input Validation Framework**
   ```java
   @RestController
   @Validated
   public class UsagePointController {
       @PostMapping
       public ResponseEntity<?> create(@Valid @RequestBody UsagePointDto dto) {
           // Implementation
       }
   }
   ```

#### Weeks 7-8: Data Protection & Encryption
1. **Sensitive Data Encryption**
   - Encrypt client secrets in database
   - Implement field-level encryption for tokens
   - Add @JsonIgnore for sensitive fields

2. **Audit Logging Implementation**
   - Add comprehensive audit trail
   - Implement security event logging
   - Configure log analysis and monitoring

### Phase 3: Code Quality & Testing (Weeks 9-12)

#### Weeks 9-10: Testing Infrastructure
1. **TestContainers Integration**
   ```java
   @Testcontainers
   class IntegrationTest {
       @Container
       static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
           .withDatabaseName("espi_test")
           .withUsername("test")
           .withPassword("test");
   }
   ```

2. **Security Testing Suite**
   - OAuth2 flow integration tests
   - Authorization boundary tests
   - ESPI compliance validation tests

#### Weeks 11-12: Performance & Monitoring
1. **Database Performance Optimization**
   - Add proper indexes on query fields
   - Implement connection pooling optimization
   - Fix N+1 query problems with @BatchSize

2. **Application Monitoring**
   ```yaml
   management:
     endpoints:
       web:
         exposure:
           include: health,metrics,info
     endpoint:
       health:
         show-details: when_authorized
   ```

### Phase 4: Production Readiness (Weeks 13-16)

#### Weeks 13-14: ESPI 4.0 Compliance
1. **NAESB ESPI 4.0 Standard Implementation**
   - Opaque token format enforcement
   - HTTPS-only communication
   - Standard-compliant scope handling

2. **Green Button Data Format Validation**
   - XML schema validation
   - ATOM feed compliance
   - Energy data integrity checks

#### Weeks 15-16: Deployment & Documentation
1. **Production Deployment Pipeline**
   ```yaml
   # GitHub Actions workflow
   name: Deploy Production
   on:
     push:
       branches: [main]
   jobs:
     security-scan:
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@v3
         - name: Security Scan
           run: ./gradlew dependencyCheckAnalyze
   ```

2. **Operational Documentation**
   - Security runbook updates
   - Incident response procedures
   - Monitoring and alerting setup

## Resource Allocation

### Team Requirements
- **Security Engineer**: Full-time for Phases 1-2
- **Senior Java Developer**: Full-time for all phases
- **DevOps Engineer**: Part-time for infrastructure updates
- **QA Engineer**: Full-time for Phase 3

### Infrastructure Requirements
- Development environment with Spring Boot 3.5
- Security scanning tools (SonarQube, OWASP Dependency Check)
- Staging environment matching production
- Load testing infrastructure

## Risk Mitigation

### High-Risk Components
1. **OpenESPI-ThirdParty Module**
   - **Risk**: Complete rewrite required
   - **Mitigation**: Parallel development track, feature flags

2. **Database Schema Changes**
   - **Risk**: Data migration complexity
   - **Mitigation**: Comprehensive backup strategy, rollback procedures

3. **OAuth2 Flow Compatibility**
   - **Risk**: Client application breakage
   - **Mitigation**: Backward compatibility layer, phased rollout

### Rollback Strategy
```bash
# Emergency rollback procedure
git checkout stable-branch
./deploy-rollback.sh production
```

## Success Metrics

### Security Metrics
- ✅ Zero critical vulnerabilities (Target: Week 2)
- ✅ 100% HTTPS enforcement (Target: Week 4)
- ✅ Complete audit trail coverage (Target: Week 8)

### Performance Metrics
- 🎯 <200ms API response time (95th percentile)
- 🎯 >99.9% uptime during migration
- 🎯 Zero data loss incidents

### Compliance Metrics
- ✅ NAESB ESPI 4.0 compliance certification
- ✅ Security audit passing grade
- ✅ Green Button Alliance certification renewal

## Timeline Summary

| Phase | Duration | Key Deliverables | Risk Level |
|-------|----------|------------------|------------|
| 1 | Weeks 1-2 | Critical security fixes | HIGH |
| 2 | Weeks 3-8 | Architecture modernization | MEDIUM |
| 3 | Weeks 9-12 | Testing & quality | LOW |
| 4 | Weeks 13-16 | Production deployment | MEDIUM |

**Total Duration**: 16 weeks (4 months)
**Go-Live Target**: End of Month 4

## Immediate Next Steps (Next 48 Hours)

1. **URGENT**: Complete remaining 2 datacustodian controller fixes (ScopeSelectionController, UsagePointController)
2. **URGENT**: Enable OAuth2 Resource Server security in datacustodian SecurityConfiguration.java
3. **HIGH**: Externalize hardcoded database credentials from configuration files
4. **HIGH**: Test datacustodian application startup and basic ESPI functionality

## Dependencies and Blockers

### External Dependencies
- Green Button Alliance certification timeline
- Third-party client application compatibility testing
- Production environment access for security team

### Potential Blockers
- Remaining compilation errors preventing datacustodian startup
- OAuth2 integration complexity with authserver module
- ESPI compliance testing requirements for Green Button certification

**Note**: The openespi-thirdparty module has been deprioritized as it requires a complete rewrite and should be treated as a separate project phase.

## Communication Plan

### Stakeholder Updates
- **Weekly**: Technical team standup
- **Bi-weekly**: Management progress report
- **Monthly**: Stakeholder steering committee

### Documentation Updates
- Migration progress tracking
- Security posture improvements
- Technical debt reduction metrics

---

**Document Version**: 1.0  
**Last Updated**: 2025-07-02  
**Next Review**: 2025-07-09  
**Approved By**: [Pending Technical Review]

**Emergency Contact**: Security Team (security@greenbuttonalliance.org)