# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

You are a senior developer and expert in Spring Boot.
When you add or modify a file you must run mvn spotless:apply to ensure code formatting and style consistency.

## Project Overview

This is a Spring Boot Starter project for configuring multiple RestClient instances with declarative YAML configuration. It enables applications to define multiple REST clients with individual settings for base URLs, timeouts, and interceptors.

## Architecture

### Configuration-Driven RestClient Management

The project uses Spring Boot's configuration properties mechanism to externalize RestClient configuration:

- **RestClientProperties** (`config/properties/RestClientProperties.java`): Binds YAML configuration under `rest-clients` prefix
- **Configuration structure**: Each client has a name, base-url, connect-timeout, read-timeout, and an ordered list of interceptors
- **@ConfigurationPropertiesScan**: Enabled in the main application class to auto-discover configuration properties

### Key Configuration Pattern

```yaml
rest-clients:
  clients:
    - name: "client1"
      base-url: "http://localhost:8081"
      connect-timeout: 5000  # default if not specified
      read-timeout: 10000    # default if not specified
      interceptors:
        - bean-name: "loggingInterceptor"
          order: 1
```

### Annotation Processors

The project uses two annotation processors configured in maven-compiler-plugin:
1. **spring-boot-configuration-processor**: Generates metadata for IDE autocomplete in application.yaml
2. **lombok**: Provides code generation for getters/setters/builders

## Commands

### Build and Compile
```bash
mvn clean compile          # Clean and compile, generates configuration metadata
mvn clean package          # Build JAR
```

### Code Formatting (Spotless)
```bash
mvn spotless:check         # Check code formatting (Java, POM, YAML)
mvn spotless:apply         # Auto-format all files
mvn verify                 # Runs spotless:check automatically
```

### Testing
```bash
mvn test                   # Run all tests
mvn test -Dtest=ClassName  # Run specific test class
```

## Code Style

- **Java**: Google Java Format style enforced by Spotless
- **Imports**: Automatically ordered and unused imports removed
- **POM**: Dependencies sorted by scope, groupId, artifactId
- **YAML**: Formatted using Jackson

## Terminology

In Spring RestClient context, use **"interceptor"** (not "middleware") to refer to `ClientHttpRequestInterceptor` implementations.
