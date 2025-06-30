# OpenESPI Authorization Server Docker Image
# Multi-stage build for optimized production image

# Build stage
FROM eclipse-temurin:21-jdk AS builder

LABEL maintainer="Green Button Alliance <info@greenbuttonalliance.org>"
LABEL org.opencontainers.image.title="OpenESPI Authorization Server Builder"
LABEL org.opencontainers.image.description="Build stage for OpenESPI OAuth2 Authorization Server"
LABEL org.opencontainers.image.version="1.0.0-SNAPSHOT"
LABEL org.opencontainers.image.source="https://github.com/GreenButtonAlliance/OpenESPI-AuthorizationServer-java"

WORKDIR /app

# Copy Maven wrapper and configuration
COPY mvnw ./
COPY mvnw.cmd ./
COPY .mvn .mvn
COPY pom.xml ./

# Download dependencies (for better layer caching)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests -P prod && \
    java -Djarmode=layertools -jar target/OpenESPI-AuthorizationServer-*.jar extract

# Runtime stage
FROM eclipse-temurin:21-jre-jammy AS runtime

LABEL maintainer="Green Button Alliance <info@greenbuttonalliance.org>"
LABEL org.opencontainers.image.title="OpenESPI Authorization Server"
LABEL org.opencontainers.image.description="Green Button Alliance OpenESPI OAuth2 Authorization Server with Spring Boot 3.5"
LABEL org.opencontainers.image.version="1.0.0-SNAPSHOT"
LABEL org.opencontainers.image.source="https://github.com/GreenButtonAlliance/OpenESPI-AuthorizationServer-java"
LABEL org.opencontainers.image.licenses="Apache-2.0"

# Create non-root user for security
RUN groupadd -r openespi && useradd -r -g openespi -m -d /home/openespi openespi

# Install necessary packages and clean up
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        curl \
        ca-certificates && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /home/openespi

# Copy application layers from builder stage for optimal caching
COPY --from=builder --chown=openespi:openespi app/dependencies/ ./
COPY --from=builder --chown=openespi:openespi app/spring-boot-loader/ ./
COPY --from=builder --chown=openespi:openespi app/snapshot-dependencies/ ./
COPY --from=builder --chown=openespi:openespi app/application/ ./

# Switch to non-root user
USER openespi

# Expose port
EXPOSE 9999

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:9999/actuator/health || exit 1

# Set JVM options for container environment
ENV JAVA_OPTS="-server \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:G1HeapRegionSize=32m \
    -XX:+UseStringDeduplication \
    -XX:+OptimizeStringConcat \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.jmx.enabled=false"

# Default environment variables
ENV SPRING_PROFILES_ACTIVE=docker
ENV ESPI_TOKEN_FORMAT=opaque
ENV SERVER_PORT=9999

# Entry point using Spring Boot's layered JAR approach
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]