# Multi-stage Dockerfile for OpenESPI Authorization Server
# Optimized for production deployment with NAESB ESPI 4.0 compliance

# ===================================================================
# Build Stage - Create the application JAR
# ===================================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

# Set working directory
WORKDIR /app

# Copy Maven wrapper and POM files
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Make Maven wrapper executable
RUN chmod +x ./mvnw

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src/ src/

# Build the application
RUN ./mvnw clean package -DskipTests -B

# Extract JAR layers for better caching
RUN java -Djarmode=layertools -jar target/*.jar extract

# ===================================================================
# Runtime Stage - Create the final image
# ===================================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Create app user for security
RUN addgroup -g 1001 -S espi && \
    adduser -u 1001 -S espi -G espi

# Install required packages
RUN apk add --no-cache \
    curl \
    tzdata \
    ca-certificates \
    && rm -rf /var/cache/apk/*

# Set timezone
ENV TZ=UTC

# Create application directories
RUN mkdir -p /app/config /app/logs /app/certificates /app/temp && \
    chown -R espi:espi /app

# Set working directory
WORKDIR /app

# Copy JAR layers from builder stage
COPY --from=builder --chown=espi:espi app/dependencies/ ./
COPY --from=builder --chown=espi:espi app/spring-boot-loader/ ./
COPY --from=builder --chown=espi:espi app/snapshot-dependencies/ ./
COPY --from=builder --chown=espi:espi app/application/ ./

# Copy configuration files
COPY --chown=espi:espi docker/config/ /app/config/

# Switch to app user
USER espi

# Expose application port and management port
EXPOSE 8080 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

# Default JVM options for production
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Application configuration
ENV SPRING_PROFILES_ACTIVE=docker
ENV MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus
ENV MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=when-authorized

# Entry point
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]

# Metadata
LABEL maintainer="Green Button Alliance <info@greenbuttonalliance.org>"
LABEL description="OpenESPI Authorization Server - NAESB ESPI 4.0 Compliant OAuth2/OIDC Server"
LABEL version="1.0.0"
LABEL org.opencontainers.image.source="https://github.com/GreenButtonAlliance/OpenESPI-AuthorizationServer-java"
LABEL org.opencontainers.image.vendor="Green Button Alliance"
LABEL org.opencontainers.image.title="OpenESPI Authorization Server"
LABEL org.opencontainers.image.description="NAESB ESPI 4.0 compliant OAuth2/OIDC Authorization Server"