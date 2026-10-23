# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Behavioral

1. Don’t assume. Don’t hide confusion. Surface tradeoffs.
2. Minimum code that solves the problem. Limit speculative additions.
3. Touch only what you must, clean up only your own mess -- but do suggest additional related fixes.
4. Define success criteria. Loop until verified.

## Project Overview

This is **jackson-databind**, the general-purpose data-binding functionality and tree-model for Jackson Data Processor. It builds on the Streaming API (jackson-core) and uses jackson-annotations for configuration. This is the 3.x branch (Jackson 3.0+) which requires JDK 17+ and uses the `tools.jackson` package namespace (2.x used `com.fasterxml.jackson`).

**Key characteristics:**
- ~790 test files with comprehensive test coverage
- Thread-safe mapper instances (as of Jackson 3.0)
- Maven-based build system with Maven wrapper (`./mvnw`)
- Supports multiple JDK versions (17, 21, 25) with special test profiles

## Common Development Commands

### Building and Testing

```bash
# Full build with tests
./mvnw clean verify

# Build without tests (faster)
./mvnw clean install -DskipTests

# Run tests only
./mvnw test

# Run a specific test class
./mvnw test -Dtest=ClassName

# Run a specific test method
./mvnw test -Dtest=ClassName#methodName

# Generate test report
./create-test-report.sh  # runs: mvn surefire-report:report

# Verify Android SDK compatibility
./mvnw animal-sniffer:check

# JDK 21+ test sources: the `java21` profile auto-activates on JDK 21+,
# so no flag is normally needed (`-Pjava21` just forces it on)
```

### Code Quality

```bash
# Run with ErrorProne static analysis
./mvnw verify -Perrorprone

# Generate code coverage report
./mvnw test jacoco:report
# Report will be in target/site/jacoco/

# Check dependencies
./mvnw dependency:tree
```

## Code Architecture

### Core Components Hierarchy

1. **ObjectMapper** (`ObjectMapper.java`): The main entry point for all Jackson databind operations
   - Thread-safe and fully immutable (as of 3.0)
   - Uses builder pattern for construction (`JsonMapper.builder()` for JSON)
   - Contains caches for serializers/deserializers

2. **Serialization Path** (`ser/` package):
   - `SerializerFactory` / `BeanSerializerFactory`: Creates serializers
   - `ValueSerializer`: Base class for all serializers
   - `BeanSerializer`: Handles POJO serialization
   - `BeanPropertyWriter`: Writes individual bean properties
   - `SerializationContext`: Context for serialization process

3. **Deserialization Path** (`deser/` package):
   - `DeserializerFactory` / `BeanDeserializerFactory`: Creates deserializers
   - `ValueDeserializer`: Base class for all deserializers
   - `BeanDeserializer` (in `deser/bean/`) / `BeanDeserializerBuilder`: Handles POJO deserialization
   - `SettableBeanProperty`: Represents a settable bean property
   - `DeserializationContext`: Context for deserialization process

4. **Type System** (`type/` package):
   - `JavaType` (in root `databind` package): Represents Java types with full generic information
   - `TypeFactory`: Creates JavaType instances
   - Critical for handling generics correctly

5. **Introspection** (`introspect/` package):
   - `AnnotatedClass`, `AnnotatedMethod`, `AnnotatedField`: Represents annotated members
   - `AnnotationIntrospector` (in root `databind` package): Processes annotations to configure behavior
   - Handles reflection and metadata extraction

6. **Configuration**:
   - `MapperConfig` (in `cfg/`): Base configuration
   - `SerializationConfig` / `DeserializationConfig` (in root `databind` package): Specific configurations
   - `MapperFeature`, `SerializationFeature`, `DeserializationFeature` (in root `databind` package): Feature flags
   - `PackageVersion` (in `cfg/`): Generated file containing version information

7. **Tree Model**:
   - `JsonNode` (in root `databind` package): Abstract base for all node types
   - `ObjectNode`, `ArrayNode`, `StringNode`, etc. (in `node/` package): Concrete node types.
     Note: 2.x's `TextNode` is named `StringNode` in 3.x
   - Alternative to POJO binding for dynamic structures

### Package Organization

- `tools.jackson.databind` - Core classes (ObjectMapper, configs, features)
- `tools.jackson.databind.ser` - Serialization infrastructure
- `tools.jackson.databind.deser` - Deserialization infrastructure
- `tools.jackson.databind.type` - Type system and TypeFactory
- `tools.jackson.databind.introspect` - Reflection and metadata
- `tools.jackson.databind.node` - Tree model (JsonNode hierarchy)
- `tools.jackson.databind.annotation` - Databind-specific annotations
- `tools.jackson.databind.json` - JSON-specific mapper (JsonMapper)
- `tools.jackson.databind.jsontype` - Polymorphic type handling
- `tools.jackson.databind.jsonFormatVisitors` - Schema generation visitors
- `tools.jackson.databind.exc` - Exception types
- `tools.jackson.databind.util` - Utility classes
- `tools.jackson.databind.module` - Module system
- `tools.jackson.databind.ext` - External type integrations

### Important Design Patterns

1. **Builder Pattern**: ObjectMapper uses immutable builder pattern (3.x change)
2. **Factory Pattern**: SerializerFactory and DeserializerFactory create handlers
3. **Caching**: Serializers/deserializers are cached for performance
4. **Context Objects**: SerializationContext and DeserializationContext carry state
5. **Visitor Pattern**: JsonFormatVisitorWrapper for schema generation

## Testing

### Test Structure

Tests are organized by functional area under `src/test/java/tools/jackson/databind/`:
- `deser/` - Deserialization tests
- `ser/` - Serialization tests
- `node/` - Tree model tests
- `type/` - Type system tests
- `introspect/` - Introspection tests
- `jsontype/` - Polymorphic type handling tests
- `convert/` - Conversion tests
- `format/` - Format-specific tests
- `mixins/` - Mixin annotation tests
- `module/` - Module system tests
- `objectid/` - Object identity tests
- `records/` - Java Records support tests
- `views/` - JSON Views tests
- `struct/` - Structural type tests
- `seq/` - Sequence (streaming read/write) tests
- `misc/` - Miscellaneous tests
- `cfg/`, `contextual/`, `access/`, `exc/`, `ext/`, `interop/`, `json/`, `jsonschema/`, `util/` - Other functional areas
- `tofix/` - Known failing tests (deferred fixes)
- `testutil/` - Test utilities and base classes (`@JacksonTestFailureExpected` lives in `testutil/failure/`)

### Test Utilities

Use `DatabindTestUtil` class (in `testutil/` package) which extends `JacksonTestUtilBase`:
- Provides common assertion methods
- Sample JSON documents and constants
- Helper methods for ObjectMapper creation
- JUnit 5 based (migrated from JUnit 4)

### `@JacksonTestFailureExpected`

Tests for known bugs that have not yet been fixed should be placed in the `tofix/` package and annotated with `@JacksonTestFailureExpected` (in addition to `@Test`). This annotation inverts the test outcome via `JacksonTestFailureExpectedInterceptor`:
- If the test **throws an exception** (the expected behavior for an unfixed bug), the test **passes**.
- If the test **passes without error** (meaning the bug was fixed), the test **fails** with a `JacksonTestShouldFailException` — signaling that the annotation (and possibly the `tofix/` placement) should be removed.

This ensures known-failing tests don't break the build, while automatically detecting when a fix makes them pass so they can be promoted to regular tests.

### JDK-Specific Tests

- `src/test-jdk21/java/` - Tests that require JDK 21+ features
- These are only compiled/run when building with JDK 21+

### Testing, misc

- Prefer text blocks (""" separator) over other mechanisms (like "a2q" or backslash escaping)
- Avoid "test" prefix in methods (legacy code has these)

## Version and Compatibility Notes

- **JDK Baseline**: Jackson 3.x requires JDK 17 minimum
- **Android SDK**: Jackson 3.0 requires Android SDK 34+
- **Package Namespace**:
  - Jackson 1.x: `org.codehaus.jackson.map`
  - Jackson 2.x: `com.fasterxml.jackson.databind`
  - Jackson 3.x: `tools.jackson.databind` (current)
- **Dependencies**:
  - `tools.jackson.core:jackson-core` (streaming API)
  - `com.fasterxml.jackson.core:jackson-annotations` (still 2.x groupId and `com.fasterxml.jackson.annotation` package)

## Branch Strategy

Active 3.x lines (three roles):
- `3.x` - **Development branch** (a.k.a. mainline / integration branch): ongoing work toward the next minor (3.3.0-SNAPSHOT). Exactly one.
- `3.2` - **Latest release branch**: the latest released minor (3.2.x), the line users are steered toward. Exactly one per major.
- `3.1`, `3.0` - **Maintenance branches**: older released minors still getting fixes. Zero or more.

The asymmetry: one development branch, one latest-release branch, and zero-or-more maintenance branches behind it. As releases progress a branch ages down — when 3.3 ships, `3.2` becomes a maintenance branch and `3.3` becomes the latest release branch.

Fixes are **merged forward**: they land on the oldest affected released line and are merged up one step at a time toward `3.x` (e.g. `Merge branch '3.1' into 3.2`, then `Merge branch '3.2' into 3.x`).

**Long-Term Support (LTS)** is an orthogonal designation, *not* reflected in branch names: a released line marked LTS keeps getting fixes well past the point a normal maintenance branch would be retired. Current LTS lines: `3.1`, `2.21`, `2.18`. A branch's LTS status is independent of its role above — `3.1` kept its LTS status after aging down from latest release branch to maintenance branch.

Legacy 2.x lines:
- `2.x` - Development branch, next minor 2.x version (2.23.0-SNAPSHOT)
- `2.22` - Latest release branch of 2.x
- `2.21`, `2.18` - Maintenance branches (both LTS)

## Maven Configuration

- Parent POM: `tools.jackson:jackson-base`, version-locked to the project version, which tracks the branch (`3.1.6-SNAPSHOT` on `3.1`, `3.3.0-SNAPSHOT` on `3.x`, ...)
- Generated file: `PackageVersion.java`, into `tools.jackson.databind.cfg` (via maven-replacer-plugin)
- Special profiles:
  - `java21` - Enables JDK 21+ test sources (`src/test-jdk21/java/`); auto-activated by `<jdk>[21,)</jdk>`
  - `errorprone` - Enables ErrorProne static analysis
  - `release` - Skips tests for release builds

### Required JVM Arguments

Tests require these JVM arguments (defined in pom.xml):
```
--add-opens=java.base/java.lang=tools.jackson.databind
--add-opens=java.base/java.util=tools.jackson.databind
```

## Code Style and Patterns

### When modifying serializers/deserializers:

1. Serializers should extend `ValueSerializer<T>` and override `serialize()`
2. Deserializers should extend `ValueDeserializer<T>` and override `deserialize()`
3. Check if contextual configuration is needed (override `createContextual()`, defined directly on `ValueSerializer`/`ValueDeserializer` in 3.x -- the 2.x `ContextualSerializer`/`ContextualDeserializer` interfaces are gone; `ContextualKeyDeserializer` remains for key deserializers)
4. Consider caching implications - deserializers are heavily cached
5. Handle null values appropriately

### When adding features:

1. Add feature flag to appropriate enum (`MapperFeature`, `SerializationFeature`, `DeserializationFeature`)
2. Update configuration classes to handle the feature
3. Add comprehensive tests in appropriate test package
4. Consider backward compatibility with 2.x if relevant

### When fixing bugs:

1. Find or create test that reproduces the issue (in `tofix/` package if deferred)
2. Fix should typically be in factory, serializer, or deserializer layer
3. Ensure fix doesn't break existing tests

## Important Implementation Details

- **Thread Safety**: ObjectMapper is fully thread-safe and immutable in 3.x
- **Caching**: Root-level deserializers are always cached with full generic type info
- **Type Handling**: Use `TypeFactory` for creating `JavaType` instances with generics
- **Builder Pattern**: Always use builder for ObjectMapper construction in 3.x
- **Annotations**: Jackson annotations (2.x) are still in the `com.fasterxml.jackson.annotation` package (published under the `com.fasterxml.jackson.core` groupId)
