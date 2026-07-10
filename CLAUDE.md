# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Jackson Databind is the general-purpose data-binding library for the Jackson data processor. It provides functionality for converting between JSON and Java POJOs (Plain Old Java Objects), as well as a Tree Model API via JsonNode. While originally designed for JSON, it can work with other data formats through the Jackson ecosystem.

- **Base JDK**: Java 8 (`javac.target.version` = 1.8)
- **Build Tool**: Maven (via `./mvnw`)
- **Main Package**: `com.fasterxml.jackson.databind`

This file describes the **Jackson 2.x line**. It lives on every active 2.x branch and
is merged forward along with the code, so it must not hardcode a version number:
read the current one from `pom.xml`. For the 3.x line, see the copy on `3.x`.

## Branches and Merge-Forward

This is the single most important thing to get right in this repo.

Branches form an ordered chain, oldest first. Each is a real maintenance branch,
and the version on each is whatever its `pom.xml` says:

```
2.18 → 2.19 → 2.20 → 2.21 → 2.22 → 2.x → 3.x
```

- Numbered branches (`2.18` … `2.22`) are **patch** branches for already-released minors.
- `2.x` is the **next 2.x minor** under development.
- `3.x` is the next major. `master` is stale and is not a development branch — do not target it.

**Rules:**

1. **Land a fix on the oldest branch it applies to**, not on the newest. A bug present
   since 2.18 gets fixed on `2.18`. A fix for new-in-2.x behavior goes on `2.x`.
2. **Fixes are merged forward** along the chain (`git merge 2.20` while on `2.21`, etc.).
   Never cherry-pick or re-apply the same fix independently onto two branches — that
   creates conflicts on every subsequent merge-forward.
3. Which branch is "oldest applicable" depends on which branches are still open for
   maintenance; older ones are closed over time. Check recent commits on a branch before
   targeting it, and when in doubt, ask.
4. Security fixes are commonly backported further down the chain than ordinary bug fixes.

## Build and Test Commands

### Building the Project

```bash
# Full build with tests
./mvnw clean verify

# Build without tests (faster)
./mvnw clean install -DskipTests

# Build for specific Java version (profiles: java11, java17, java21)
./mvnw clean verify -Pjava17
```

### Running Tests

```bash
# Run all tests. Surefire is pinned to PrimarySuite, which selects the whole
# com.fasterxml.jackson.databind package tree except typepollution tests.
./mvnw test

# Run a specific test class (-Dtest overrides the pinned suite)
./mvnw test -Dtest=ObjectMapperTest

# Run a specific test method
./mvnw test -Dtest=ObjectMapperTest#testSomeMethod
```

Tests run 4 threads per class in parallel, so tests must not share mutable static state.

`MapperFootprintTest` and the `typepollution` package are excluded from the default
run; type-pollution tests run only under the `java17` profile.

### Other Useful Commands

```bash
# Check Android SDK compatibility (SDK 26+, via gummy-bears signatures)
./mvnw animal-sniffer:check

# Generate Javadocs
./mvnw javadoc:javadoc

# Run with ErrorProne static analysis
./mvnw clean verify -Perrorprone

# Generate code coverage report (jacoco)
./mvnw verify
# Report available at: target/site/jacoco/index.html
```

## Architecture Overview

### Core Components

Jackson databind is structured around three main responsibilities:

1. **Serialization (Java → JSON)**: Converting Java objects to JSON
2. **Deserialization (JSON → Java)**: Parsing JSON into Java objects
3. **Tree Model**: In-memory JSON representation via JsonNode

### Key Classes and Their Roles

- **`ObjectMapper`**: The main entry point for all databind operations. Thread-safe after configuration. Acts as factory for ObjectReader/ObjectWriter.
- **`ObjectReader`**: Immutable, configured reader for deserialization operations
- **`ObjectWriter`**: Immutable, configured writer for serialization operations
- **`JavaType`**: Represents Java type information with full generic type resolution
- **`JsonNode`**: Base class for tree model representation of JSON

### Package Structure

**`com.fasterxml.jackson.databind`** (root package)
- Core classes: ObjectMapper, ObjectReader, ObjectWriter
- Feature enums: SerializationFeature, DeserializationFeature, MapperFeature
- Base abstractions: JsonSerializer, JsonDeserializer

**`com.fasterxml.jackson.databind.ser`** (serialization)
- `BeanSerializer`: Main POJO serializer
- `BeanSerializerFactory`: Creates serializers for beans
- `BeanPropertyWriter`: Handles individual property serialization
- `std/`: Standard serializers for JDK types (collections, primitives, etc.)
- `impl/`: Implementation details

**`com.fasterxml.jackson.databind.deser`** (deserialization)
- `BeanDeserializer`: Main POJO deserializer
- `BeanDeserializerFactory`: Creates deserializers for beans
- `SettableBeanProperty`: Handles individual property deserialization
- `ValueInstantiator`: Controls object construction (constructors, factories, builders)
- `std/`: Standard deserializers for JDK types
- `impl/`: Implementation details

**`com.fasterxml.jackson.databind.introspect`** (reflection/introspection)
- `AnnotatedClass`, `AnnotatedMethod`, `AnnotatedField`: Reflection wrappers with annotation support
- `POJOPropertiesCollector`: Discovers properties from a class
- `BasicClassIntrospector`: Main introspection entry point
- `VisibilityChecker`: Controls which fields/methods are visible for serialization

**`com.fasterxml.jackson.databind.type`** (type system)
- Type hierarchy for representing Java types with full generics
- `TypeFactory`: Creates JavaType instances

**`com.fasterxml.jackson.databind.node`** (tree model)
- `ObjectNode`, `ArrayNode`, `TextNode`, etc.: Concrete JsonNode implementations

**`com.fasterxml.jackson.databind.jsontype`** (polymorphic types)
- Polymorphic type handling (type ids, type resolvers)
- `SubtypeResolver`: Resolves polymorphic subtypes

**`com.fasterxml.jackson.databind.cfg`** (configuration)
- Configuration objects and builders
- `MapperBuilder`: Modern builder pattern for ObjectMapper configuration

**`com.fasterxml.jackson.databind.module`** (extension mechanism)
- `Module` interface and implementations for extending Jackson

**`com.fasterxml.jackson.databind.util`** (utilities)
- Common utility classes used throughout databind

### Important Design Patterns

**Factory Pattern**: BeanSerializerFactory and BeanDeserializerFactory create serializers/deserializers

**Builder Pattern**: ObjectReader/ObjectWriter use builders for configuration. ValueInstantiator supports Builder-based deserialization.

**Introspection & Caching**: Heavy use of caching for introspected class information and created serializers/deserializers. Thread-safety is critical.

**Contextual Processing**: Serializers/deserializers can be contextual (ContextualSerializer/ContextualDeserializer) to handle different scenarios for the same Java type.

**Visitor Pattern**: JsonFormatVisitorWrapper for schema generation

## Test Organization

Tests are organized by feature area under `src/test/java/com/fasterxml/jackson/databind/`:

- Root package: Core ObjectMapper functionality
- `deser/`: Deserialization tests
- `ser/`: Serialization tests
- `introspect/`: Reflection/introspection tests
- `node/`: Tree model tests
- `type/`: Type system tests
- `jsontype/`: Polymorphic type tests
- `exc/`: Exception handling tests
- `convert/`: Conversion tests

**Test Suite**: `PrimarySuite` is the main JUnit 5 suite. It selects by *package*
(`@SelectPackages("com.fasterxml.jackson.databind")`), so a new test class under that
tree is picked up automatically — there is nothing to register.

**Conventions**:
- JUnit 5 (Jupiter) only: `org.junit.jupiter.api.Test`, `static org.junit.jupiter.api.Assertions.*`.
  Some older tests still use JUnit 4 idioms; do not copy them.
- Static-import shared helpers from `testutil.DatabindTestUtil` rather than reimplementing them.
- Annotate a test that reproduces a known-broken behavior with `@JacksonTestFailureExpected`.
  The interceptor inverts the outcome, so the test passes while the bug exists and starts
  failing once it is fixed — that is the signal to remove the annotation.

**JDK-Specific Tests** (compiled only under the matching profile):
- `src/test-jdk11/`: Java 11+ specific tests (`-Pjava11`)
- `src/test-jdk17/`: Java 17+ specific tests (`-Pjava17`)
- `src/test-jdk21/`: Java 21+ specific tests (`-Pjava21`)

## Common Development Patterns

### Adding a New Feature

1. Start with failing test in appropriate test package
2. Implement feature in relevant ser/deser/introspect package
3. Update relevant Feature enum if adding configuration option
4. Ensure thread-safety if modifying cached/shared state
5. Run full test suite across multiple JDK versions

### Fixing Deserialization Issues

Look at:
- `BeanDeserializer` and `BeanDeserializerFactory` for POJO deserialization
- Standard deserializers in `deser.std` for JDK types
- `ValueInstantiator` for object construction issues
- `SettableBeanProperty` for property setting issues

### Fixing Serialization Issues

Look at:
- `BeanSerializer` and `BeanSerializerFactory` for POJO serialization
- Standard serializers in `ser.std` for JDK types
- `BeanPropertyWriter` for property writing issues

### Type Resolution

Jackson's type system handles Java generics:
- Use `TypeFactory` to create `JavaType` instances
- `JavaType` preserves full generic type information
- Type resolution happens during introspection and (de)serializer creation

## Important Constraints

### No Additional Dependencies

Core components (annotations, streaming, databind) cannot add dependencies beyond:
- JDK itself (Java 8 for version 2.13+)
- Other Jackson core components

Extensions must be built as separate modules.

### Thread Safety

- `ObjectMapper` is thread-safe after initial configuration
- `ObjectReader` and `ObjectWriter` are fully immutable and thread-safe
- Serializers and deserializers must be thread-safe (they are cached and reused)
- Context classes (SerializationContext, DeserializationContext) are NOT thread-safe

### Android Compatibility

As of 2.14+, maintain compatibility with Android SDK 26+. Verify with:
```bash
./mvnw animal-sniffer:check
```

## CI and Release

- **CI**: GitHub Actions (`.github/workflows/main.yml`) builds on Java 8, 17, 21 and 25
  on `ubuntu-24.04`; Java 8 additionally on `windows-latest`. The Java 8 Ubuntu job is
  the release build. Also: CodeQL, CIFuzz, and downstream dependent builds.
- **Target Branch for PRs**: the oldest branch the change applies to — see
  [Branches and Merge-Forward](#branches-and-merge-forward). Not `master`.
- **PR Branch Naming**: `tatu-claude/<target-branch>/<issue-number>-<short-slug>`,
  e.g. `tatu-claude/2.21/6085-add-claude-md`. Omit the issue number if there is no issue.
- **Release Notes**: a user-visible change needs two entries, both added on the branch
  the fix lands on:
  - `release-notes/VERSION-2.x`, under the pending version:
    `#<issue>: <title>` followed by indented `(reported by @x)` / `(fix by @y)` lines.
  - `release-notes/CREDITS-2.x`, appended at the end:
    `Name (@handle)`, then ` * Reported[, fixed] #<issue>: <title>`, then `  [<version>]`.
- **Release Process**: coordinated across Jackson components via the `jackson-base` parent POM.
