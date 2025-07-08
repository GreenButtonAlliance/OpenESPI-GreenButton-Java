# GitHub Packages Setup for OpenESPI

This document explains how to set up GitHub Packages for publishing and consuming OpenESPI-Common artifacts.

## Overview

OpenESPI-Common is published to GitHub Packages, allowing other modules (datacustodian, thirdparty) to resolve dependencies remotely instead of requiring local builds.

## Publishing to GitHub Packages

### Prerequisites

1. **GitHub Personal Access Token** with `write:packages` permission
2. **Maven settings.xml** configured with GitHub credentials

### Configure Maven Settings

Add to your `~/.m2/settings.xml`:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_GITHUB_TOKEN</password>
    </server>
  </servers>
</settings>
```

### Publishing Commands

```bash
# From openespi-common directory
mvn clean deploy

# Or build and install locally first
mvn clean install
mvn deploy
```

## Consuming from GitHub Packages

### Repository Configuration

Other modules are already configured to consume from GitHub Packages:

```xml
<repositories>
    <repository>
        <id>github</id>
        <name>GitHub Green Button Alliance Apache Maven Packages</name>
        <url>https://maven.pkg.github.com/greenbuttonalliance/openespi-greenbutton-java</url>
        <releases>
            <enabled>true</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```

### Authentication for Consumption

Even for public repositories, GitHub Packages requires authentication. Add the same GitHub credentials to your `~/.m2/settings.xml` as shown above.

## Version Management

- **Release versions**: `3.5.0-RC1`, `3.5.0`, etc.
- **Snapshot versions**: `3.5.1-SNAPSHOT`
- All consuming modules reference `${openespi-common.version}` property

## Benefits

1. **Remote Builds**: No need to build openespi-common locally
2. **CI/CD Integration**: Automated publishing in pipelines
3. **Version Control**: Proper artifact versioning
4. **Team Collaboration**: Shared artifacts across developers

## Troubleshooting

### Authentication Issues
- Verify GitHub token has `write:packages` permission
- Check `~/.m2/settings.xml` server configuration
- Ensure server `<id>github</id>` matches repository ID

### Build Issues
- Clear local Maven cache: `rm -rf ~/.m2/repository/org/greenbuttonalliance`
- Force update dependencies: `mvn clean compile -U`
- Check repository URL is correct

### Package Visibility
- GitHub Packages inherits repository visibility
- Private repos require authentication for all access
- Public repos require authentication for publishing only

## Alternative: Sonatype/Maven Central

The configuration preserves commented Sonatype settings for future Maven Central publishing:

```xml
<!-- Alternative: Sonatype Maven Repository configuration (commented out) -->
<!--
<repository>
    <id>sonatype-release</id>
    <name>Sonatype Maven Release Repository</name>
    <url>https://oss.sonatype.org/content/repositories/releases</url>
</repository>
-->
```

To switch back to Sonatype, uncomment these sections and comment out GitHub Packages configuration.