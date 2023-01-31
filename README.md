# Overview

This project contains the general-purpose data-binding functionality
and tree-model for [Jackson Data Processor](../../../jackson).
It builds on [Streaming API](../../../jackson-core) (stream parser/generator) package,
and uses [Jackson Annotations](../../../jackson-annotations) for configuration.
Project is licensed under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).

While the original use case for Jackson was JSON data-binding, it can now be used to read content
encoded in other data formats as well, as long as parser and generator implementations exist.
Naming of classes uses word 'JSON' in many places even though there is no actual hard dependency to JSON format.

## Status

| Type | Status |
| ---- | ------ |
| Build (CI) | [![Build (github)](https://github.com/FasterXML/jackson-databind/actions/workflows/main.yml/badge.svg)](https://github.com/FasterXML/jackson-databind/actions/workflows/main.yml) |
| Artifact | [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.fasterxml.jackson.core/jackson-databind/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.fasterxml.jackson.core/jackson-databind) |
| OSS Sponsorship | [![Tidelift](https://tidelift.com/badges/package/maven/com.fasterxml.jackson.core:jackson-databind)](https://tidelift.com/subscription/pkg/maven-com-fasterxml-jackson-core-jackson-databind?utm_source=maven-com-fasterxml-jackson-core-jackson-databind&utm_medium=referral&utm_campaign=readme) |
| Javadocs | [![Javadoc](https://javadoc.io/badge/com.fasterxml.jackson.core/jackson-databind.svg)](http://www.javadoc.io/doc/com.fasterxml.jackson.core/jackson-databind) |
| Code coverage (2.15) | [![codecov.io](https://codecov.io/github/FasterXML/jackson-databind/coverage.svg?branch=2.15)](https://codecov.io/github/FasterXML/jackson-databind?branch=2.15) |

# Get it!

## Maven

Functionality of this package is contained in Java package `com.fasterxml.jackson.databind`, and can be used using following Maven dependency:

```xml
<properties>
  ...
  <!-- Use the latest version whenever possible. -->
  <jackson.version>2.14.2</jackson.version>
  ...
</properties>

<dependencies>
  ...
  <dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>${jackson.version}</version>
  </dependency>
  ...
</dependencies>
```

Package also depends on `jackson-core` and `jackson-annotations` packages, but when using build tools
like Maven or Gradle, dependencies are automatically included.
You may, however, want to use [jackson-bom](../../../jackson-bom) to ensure compatible versions
of dependencies.
If not using build tool that can handle dependencies using project's `pom.xml`, you will need to download
and include these 2 jars explicitly.

## Non-Maven dependency resolution

For use cases that do not automaticall resolve dependencies from Maven repositories, you can still
download jars from [Central Maven repository](https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/).

Databind jar is also a functional OSGi bundle, with proper import/export declarations, so it can be use on OSGi container as is.

Jackson 2.10 and above include `module-info.class` definitions so the jar is also a proper Java Module (JPMS).

Jackson 2.12 and above include additional Gradle 6 Module Metadata for version alignment with Gradle.

-----
## Compatibility

### JDK

Jackson-databind package baseline JDK requirements are as follows:

* Versions 2.0 - 2.7 require JDK 6
* Versions 2.8 - 2.12 require JDK 7 to run (but 2.11 - 2.12 require JDK 8 to build)
* Versions 2.13 and above require JDK 8

### Android

List is incomplete due to recent addition of compatibility checker.

* 2.13: Android SDK 24+
* 2.14: Android SDK 26+

for information on Android SDK versions to Android Release names see [https://en.wikipedia.org/wiki/Android_version_history]

-----

# Use It!

More comprehensive documentation can be found from [Jackson-docs](../../../jackson-docs) repository; as well as from [Wiki](../../wiki) of this project.
But here are brief introductionary tutorials, in recommended order of reading.

## 1 minute tutorial: POJOs to JSON and back

The most common usage is to take piece of JSON, and construct a Plain Old Java Object ("POJO") out of it. So let's start there. With simple 2-property POJO like this:

```java
// Note: can use getters/setters as well; here we just use public fields directly:
public class MyValue {
  public String name;
  public int age;
  // NOTE: if using getters/setters, can keep fields `protected` or `private`
}
```

we will need a `com.fasterxml.jackson.databind.ObjectMapper` instance, used for all data-binding, so let's construct one:

```java
ObjectMapper mapper = new ObjectMapper(); // create once, reuse
```

The default instance is fine for our use -- we will learn later on how to configure mapper instance if necessary. Usage is simple:

```java
MyValue value = mapper.readValue(new File("data.json"), MyValue.class);
// or:
value = mapper.readValue(new URL("http://some.com/api/entry.json"), MyValue.class);
// or:
value = mapper.readValue("{\"name\":\"Bob\", \"age\":13}", MyValue.class);
```

And if we want to write JSON, we do the reverse:

```java
mapper.writeValue(new File("result.json"), myResultObject);
// or:
byte[] jsonBytes = mapper.writeValueAsBytes(myResultObject);
// or:
String jsonString = mapper.writeValueAsString(myResultObject);
```

So far so good?

## 3 minute tutorial: Generic collections, Tree Model

Beyond dealing with simple Bean-style POJOs, you can also handle JDK `List`s, `Map`s:

```java
Map<String, Integer> scoreByName = mapper.readValue(jsonSource, Map.class);
List<String> names = mapper.readValue(jsonSource, List.class);

// and can obviously write out as well
mapper.writeValue(new File("names.json"), names);
```

as long as JSON structure matches, and types are simple.
If you have POJO values, you need to indicate actual type (note: this is NOT needed for POJO properties with `List` etc types):

```java
Map<String, ResultValue> results = mapper.readValue(jsonSource,
   new TypeReference<Map<String, ResultValue>>() { } );
// why extra work? Java Type Erasure will prevent type detection otherwise
```

(note: no extra effort needed for serialization, regardless of generic types)

But wait! There is more!

While dealing with `Map`s, `List`s and other "simple" Object types (Strings, Numbers, Booleans) can be simple, Object traversal can be cumbersome.
This is where Jackson's [Tree model](https://github.com/FasterXML/jackson-databind/wiki/JacksonTreeModel) can come in handy:

```java
// can be read as generic JsonNode, if it can be Object or Array; or,
// if known to be Object, as ObjectNode, if array, ArrayNode etc:
ObjectNode root = mapper.readTree("stuff.json");
String name = root.get("name").asText();
int age = root.get("age").asInt();

// can modify as well: this adds child Object as property 'other', set property 'type'
root.with("other").put("type", "student");
String json = mapper.writeValueAsString(root);

// with above, we end up with something like as 'json' String:
// {
//   "name" : "Bob", "age" : 13,
//   "other" : {
//      "type" : "student"
//   }
// }
```

Tree Model can be more convenient than data-binding, especially in cases where structure is highly dynamic, or does not map nicely to Java classes.

## 5 minute tutorial: Streaming parser, generator

As convenient as data-binding (to/from POJOs) can be; and as flexible as Tree model can be, there is one more canonical processing model available: incremental (aka "streaming") model.
It is the underlying processing model that data-binding and Tree Model both build upon, but it is also exposed to users who want ultimate performance and/or control over parsing or generation details.

For in-depth explanation, look at [Jackson Core component](https://github.com/FasterXML/jackson-core).
But let's look at a simple teaser to whet your appetite.

```java
ObjectMapper mapper = ...;
// First: write simple JSON output
File jsonFile = new File("test.json");
// note: method added in Jackson 2.11 (earlier would need to use
// mapper.getFactory().createGenerator(...)
JsonGenerator g = f.createGenerator(jsonFile, JsonEncoding.UTF8);
// write JSON: { "message" : "Hello world!" }
g.writeStartObject();
g.writeStringField("message", "Hello world!");
g.writeEndObject();
g.close();

// Second: read file back
try (JsonParser p = mapper.createParser(jsonFile)) {
  JsonToken t = p.nextToken(); // Should be JsonToken.START_OBJECT
  t = p.nextToken(); // JsonToken.FIELD_NAME
  if ((t != JsonToken.FIELD_NAME) || !"message".equals(p.getCurrentName())) {
   // handle error
  }
  t = p.nextToken();
  if (t != JsonToken.VALUE_STRING) {
   // similarly
  }
  String msg = p.getText();
  System.out.printf("My message to you is: %s!\n", msg);
}
```

## 10 minute tutorial: configuration

There are two entry-level configuration mechanisms you are likely to use:
[Features](https://github.com/FasterXML/jackson-databind/wiki/JacksonFeatures) and [Annotations](https://github.com/FasterXML/jackson-annotations).

### Commonly used Features

Here are examples of configuration features that you are most likely to need to know about.

Let's start with higher-level data-binding configuration.

```java
// SerializationFeature for changing how JSON is written

// to enable standard indentation ("pretty-printing"):
mapper.enable(SerializationFeature.INDENT_OUTPUT);
// to allow serialization of "empty" POJOs (no properties to serialize)
// (without this setting, an exception is thrown in those cases)
mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
// to write java.util.Date, Calendar as number (timestamp):
mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

// DeserializationFeature for changing how JSON is read as POJOs:

// to prevent exception when encountering unknown property:
mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
// to allow coercion of JSON empty String ("") to null Object value:
mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
```

In addition, you may need to change some of low-level JSON parsing, generation details:

```java
// JsonParser.Feature for configuring parsing settings:

// to allow C/C++ style comments in JSON (non-standard, disabled by default)
// (note: with Jackson 2.5, there is also `mapper.enable(feature)` / `mapper.disable(feature)`)
mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
// to allow (non-standard) unquoted field names in JSON:
mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
// to allow use of apostrophes (single quotes), non standard
mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

// JsonGenerator.Feature for configuring low-level JSON generation:

// to force escaping of non-ASCII characters:
mapper.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true);
```

Full set of features are explained on [Jackson Features](https://github.com/FasterXML/jackson-databind/wiki/JacksonFeatures) page.

### Annotations: changing property names

The simplest annotation-based approach is to use `@JsonProperty` annotation like so:

```java
public class MyBean {
   private String _name;

   // without annotation, we'd get "theName", but we want "name":
   @JsonProperty("name")
   public String getTheName() { return _name; }

   // note: it is enough to add annotation on just getter OR setter;
   // so we can omit it here
   public void setTheName(String n) { _name = n; }
}
```

There are other mechanisms to use for systematic naming changes: see [Custom Naming Convention](https://github.com/FasterXML/jackson-databind/wiki/JacksonCustomNamingConvention) for details.

Note, too, that you can use [Mix-in Annotations](https://github.com/FasterXML/jackson-databind/wiki/JacksonMixinAnnotations) to associate all annotations.

### Annotations: Ignoring properties

There are two main annotations that can be used to to ignore properties: `@JsonIgnore` for individual properties; and `@JsonIgnoreProperties` for per-class definition

```java
// means that if we see "foo" or "bar" in JSON, they will be quietly skipped
// regardless of whether POJO has such properties
@JsonIgnoreProperties({ "foo", "bar" })
public class MyBean
{
   // will not be written as JSON; nor assigned from JSON:
   @JsonIgnore
   public String internal;

   // no annotation, public field is read/written normally
   public String external;

   @JsonIgnore
   public void setCode(int c) { _code = c; }

   // note: will also be ignored because setter has annotation!
   public int getCode() { return _code; }
}
```

As with renaming, note that annotations are "shared" between matching fields, getters and setters: if only one has `@JsonIgnore`, it affects others.
But it is also possible to use "split" annotations, to for example:

```java
public class ReadButDontWriteProps {
   private String _name;
   @JsonProperty public void setName(String n) { _name = n; }
   @JsonIgnore public String getName() { return _name; }
}
```

in this case, no "name" property would be written out (since 'getter' is ignored); but if "name" property was found from JSON, it would be assigned to POJO property!

For a more complete explanation of all possible ways of ignoring properties when writing out JSON, check ["Filtering properties"](http://www.cowtowncoder.com/blog/archives/2011/02/entry_443.html) article.

### Annotations: using custom constructor

Unlike many other data-binding packages, Jackson does not require you to define "default constructor" (constructor that does not take arguments).
While it will use one if nothing else is available, you can easily define that an argument-taking constructor is used:

```java
public class CtorBean
{
  public final String name;
  public final int age;

  @JsonCreator // constructor can be public, private, whatever
  private CtorBean(@JsonProperty("name") String name,
    @JsonProperty("age") int age)
  {
      this.name = name;
      this.age = age;
  }
}
```

Constructors are especially useful in supporting use of
[Immutable objects](http://www.cowtowncoder.com/blog/archives/2010/08/entry_409.html).

Alternatively, you can also define "factory methods":

```java
public class FactoryBean
{
    // fields etc omitted for brevity

    @JsonCreator
    public static FactoryBean create(@JsonProperty("name") String name) {
      // construct and return an instance
    }
}
```

Note that use of a "creator method" (`@JsonCreator` with `@JsonProperty` annotated arguments) does not preclude use of setters: you
can mix and match properties from constructor/factory method with ones that
are set via setters or directly using fields.

## Tutorial: fancier stuff, conversions

One useful (but not very widely known) feature of Jackson is its ability
to do arbitrary POJO-to-POJO conversions. Conceptually you can think of conversions as sequence of 2 steps: first, writing a POJO as JSON, and second, binding that JSON into another kind of POJO. Implementation just skips actual generation of JSON, and uses more efficient intermediate representation.

Conversions work between any compatible types, and invocation is as simple as:

```java
ResultType result = mapper.convertValue(sourceObject, ResultType.class);
```

and as long as source and result types are compatible -- that is, if to-JSON, from-JSON sequence would succeed -- things will "just work".
But here are couple of potentially useful use cases:

```java
// Convert from List<Integer> to int[]
List<Integer> sourceList = ...;
int[] ints = mapper.convertValue(sourceList, int[].class);
// Convert a POJO into Map!
Map<String,Object> propertyMap = mapper.convertValue(pojoValue, Map.class);
// ... and back
PojoType pojo = mapper.convertValue(propertyMap, PojoType.class);
// decode Base64! (default byte[] representation is base64-encoded String)
String base64 = "TWFuIGlzIGRpc3Rpbmd1aXNoZWQsIG5vdCBvbmx5IGJ5IGhpcyByZWFzb24sIGJ1dCBieSB0aGlz";
byte[] binary = mapper.convertValue(base64, byte[].class);
```

Basically, Jackson can work as a replacement for many Apache Commons components, for tasks like base64 encoding/decoding, and handling of "dyna beans" (Maps to/from POJOs).

## Tutorial: Builder design pattern + Jackson
The Builder design pattern is a creational design pattern and can be used to create complex objects step by step.
If we have an object that needs multiple checks on other dependencies, In such cases, it is preferred to use builder design pattern.

Let's consider the person structure, which has some optional fields

```java
public class Person {
    private final String name;
    private final Integer age;
 
    // getters
}
```

Let’s see how we can employ its power in deserialization. First of all, let’s declare a private all-arguments constructor, and a Builder class.
```java
private Person(String name, Integer age) {
    this.name = name;
    this.age = age;
}
 
static class Builder {
    String name;
    Integer age;
    
    Builder withName(String name) {
        this.name = name;
        return this;
    }
    
    Builder withAge(Integer age) {
        this.age = age;
        return this;
    }
    
    public Person build() {
        return new Person(name, age);
    } 
}
```
First of all, we need to mark our class with `@JsonDeserialize` annotation, passing a builder parameter with a fully qualified domain name of a builder class.
After that, we need to annotate the builder class itself as `@JsonPOJOBuilder`.

```java
@JsonDeserialize(builder = Person.Builder.class)
public class Person {
    //...
    
    @JsonPOJOBuilder
    static class Builder {
        //...
    }
}
```

A simple unit test will be:

```java
String json = "{\"name\":\"Hassan\",\"age\":23}";
Person person = new ObjectMapper().readValue(json, Person.class);
 
assertEquals("Hassan", person.getName());
assertEquals(23, person.getAge().intValue());
```

If your builder pattern implementation uses other prefixes for methods or uses other names than build() for the builder method Jackson also provide a handy way for you.

For example, if you have a builder class that uses the "set" prefix for its methods and use the create() method instead of build() for building the whole class, you have to annotate your class like:
```java
@JsonPOJOBuilder(buildMethodName = "create", withPrefix = "set")
static class Builder {
    String name;
    Integer age;
    
    Builder setName(String name) {
        this.name = name;
        return this;
    }
    
    Builder setAge(Integer age) {
        this.age = age;
        return this;
    }
    
    public Person create() {
        return new Person(name, age);
    } 
}
```

To deserialize JSON fields under a different name than their object counterparts,
the @JsonProperty annotation can be used within the builder on the appropriate fields.

```java
@JsonPOJOBuilder(buildMethodName = "create", withPrefix = "set")
static class Builder {
    @JsonProperty("known_as")
    String name;
    Integer age;
    //...
}
```
This will deserialize the JSON property `known_as` into the builder field `name`. If a mapping like this is not provided (and further annotations aren't supplied to handle this), an `Unrecognized field "known_as"` exception will be thrown during deserialization if the field is provided anyways.

If you wish to refer to properties with more than one alias for deserialization, the `@JsonAlias` annotation can be used.

```java
@JsonPOJOBuilder(buildMethodName = "create", withPrefix = "set")
static class Builder {
    @JsonProperty("known_as")
    @JsonAlias({"identifier", "first_name"})
    String name;
    Integer age;
    //...
}
```
This will deserialize JSON fields with `known_as`, as well as `identifer` and `first_name` into `name`. Rather than an array of entries, a single alias can be used by specifying a string as such `JsonAlias("identifier")`.  
Note: to use the `@JsonAlias` annotation, a `@JsonProperty` annotation must also be used.




Overall, Jackson library is very powerful in deserializing objects using builder pattern.
 
# Contribute!

We would love to get your contribution, whether it's in form of bug reports, Requests for Enhancement (RFE), documentation, or code patches.

See [CONTRIBUTING](https://github.com/FasterXML/jackson/blob/master/CONTRIBUTING.md) for details on things like:

* Community, ways to interact (mailing lists, gitter)
* Issue tracking ([GitHub Issues](https://github.com/FasterXML/jackson-databind/issues))
* Paperwork: CLA (just once before the first merged contribution)

## Limitation on Dependencies by Core Components

One additional limitation exists for so-called core components (streaming api, jackson-annotations and jackson-databind): no additional dependencies are allowed beyond:

* Core components may rely on any methods included in the supported JDK
    * Minimum Java version is Java 7 for Jackson 2.7 - 2.12 of `jackson-databind` and most non-core components
    * Minimum Java version is Java 8 for Jackson 2.13 and later
* Jackson-databind (this package) depends on the other two (annotations, streaming).

This means that anything that has to rely on additional APIs or libraries needs to be built as an extension,
usually a Jackson module.

## Branches

`master` branch is for developing the next major Jackson version -- 3.0 -- but there
are active maintenance branches in which much of development happens:

* `2.15` is the branch for "next" minor version to release (as of November 2022)
* `2.14` is the current stable minor 2.x version
* `2.13` is for selected backported fixes

Older branches are usually not maintained after being declared as closed
on [Jackson Releases](https://github.com/FasterXML/jackson/wiki/Jackson-Releases) page,
but exist just in case a rare emergency patch is needed.
All released versions have matching git tags (`jackson-dataformats-binary-2.12.3`).

-----

## Differences from Jackson 1.x

Project contains versions 2.0 and above: source code for last (1.x) release, 1.9, is available at
[Jackson-1](../../../jackson-1) repo.

Main differences compared to 1.x "mapper" jar are:

* Maven build instead of Ant
* Java package is now `com.fasterxml.jackson.databind` (instead of `org.codehaus.jackson.map`)

-----

## Support

### Community support

Jackson components are supported by the Jackson community through mailing lists, Gitter forum, Github issues. See [Participation, Contributing](../../../jackson#participation-contributing) for full details.


### Enterprise support

Available as part of the [Tidelift](https://tidelift.com/subscription/pkg/maven-com-fasterxml-jackson-core-jackson-databind) Subscription.

The maintainers of `jackson-databind` and thousands of other packages are working with Tidelift to deliver commercial support and maintenance for the open source dependencies you use to build your applications. Save time, reduce risk, and improve code health, while paying the maintainers of the exact dependencies you use. [Learn more.](https://tidelift.com/subscription/pkg/maven-com-fasterxml-jackson-core-jackson-databind?utm_source=maven-com-fasterxml-jackson-core-jackson-databind&utm_medium=referral&utm_campaign=enterprise&utm_term=repo)

-----

## Further reading

* [Overall Jackson Docs](../../../jackson-docs)
* [Project wiki page](https://github.com/FasterXML/jackson-databind/wiki)

Related:

* [Core annotations](https://github.com/FasterXML/jackson-annotations) package defines annotations commonly used for configuring databinding details
* [Core parser/generator](https://github.com/FasterXML/jackson-core) package defines low-level incremental/streaming parsers, generators
* [Jackson Project Home](../../../jackson) has links to all modules
* [Jackson Docs](../../../jackson-docs) is project's documentation hub

