# What is it?

This project contains the general-purpose data-binding functionality
and tree-model for Jackson. It builds on the
[core streaming parser/generator](/FasterXML/jackson-core) package,
and uses [Jackson Annotations](/FasterXML/jackson-annotations) for
configuration

While the original use case for Jackson was JSON data-binding,
it can now be used for other data formats as well, as long as
parser and generator implementations exist.
Naming of classes uses word 'JSON' in many places even though there is no
actual hard dependency to JSON format.

### Differences from Jackson 1.x

Project contains versions 2.0 and above: source code for earlier (1.x) versions is available from [Codehaus](http://jackson.codehaus.org) SVN repository

Note that the main differences compared to 1.0 core jar are:

* Maven build instead of Ant
* Java package is now `com.fasterxml.jackson.databind` (instead of `org.codehaus.jackson.map`)

-----

# Get it!

## Maven

Functionality of this package is contained in Java package `com.fasterxml.core.databind`, and can be used using following Maven dependency:

    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
      <version>2.0.0</version>
    </dependency>

Since package also depends on '''jackson-core''' and '''jackson-databind''' packages, you will need to download these if not using Maven; and you may also want to add them as Maven dependency to ensure that compatible versions are used.
If so, also add:

    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-annotations</artifactId>
      <version>2.0.0</version>
    </dependency>
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-core</artifactId>
      <version>2.0.0</version>
    </dependency>

## Non-Maven

For non-Maven use cases, you download jars from [Central Maven repository](http://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/) or [Download page](jackson-databind/wiki/JacksonDownload).

Databind jar is also a functional OSGi bundle, with proper import/export declarations, so it can be use on OSGi container as is.

-----

# Use It!

First things first: for a more in-depth coverage, read the [15 minute tutorial](jackson-databind/wiki/Jackson15MinuteTutorial).
But here is a little sampler of usage.

## 1 minute tutorial: POJOs to JSON and back

The most common usage is to take piece of JSON, and construct a Plain Old Java Object ("POJO") out of it. Like so:

    ObjectMapper mapper = new ObjectMapper(); // create once, reuse
    MyValue value = mapper.readValue(new File("data.json"), MyValue.class);
    // or:
    value = mapper.readValue(new URL("http://some.com/api/entry.json"), MyValue.class);
    // or:
    value = mapper.readValue("{\"name\":\"Bob\", \"age\":13}", MyValue.class);

followed by the reverse; taking a value, and writing it out as JSON:

    mapper.writeValue(new File("result.json"), myResultObject);

    // or:
    byte[] jsonBytes = mapper.writeValueAsBytes(myResultObject);

    // or:
    String jsonString = mapper.writeValueAsString(myResultObject);

## 2 minute tutorial: Generic collections, Tree Model

But beyond dealing with simple Bean-style pojos, you can also handle JDK `List`s, `Map`s:

    Map<String, Integer> scoreByName = mapper.readValue(jsonSource, Map.class);
    List<String> names = mapper.readValue(jsonSource, List.class);

    // and can obviously write out as well
    mapper.writeValue(new File("names.json"), names);

as long as JSON structure matches, and types are simple. If you have POJO values, you need to indicate actual type (note: this is NOT needed for POJO properties with `List` etc types):

    Map<String, ResultValue> results = mapper.readValue(jsonSource,
       new TypeReference<String, ResultValue>() { } );
    // why extra work? Java Type Erasure will prevent type detection otherwise

But wait! There is more!

While dealing with `Map`s, `List`s and other "simple" Object types (Strings, Numbers, Booleans) can be simple, Object traversal can be cumbersome.
This is where Jackson's [Tree model](jackson-databind/wiki/JacksonTreeModel) can come in handy:

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
    //   {
    // }

## 3 minute tutorial: Streaming parser, generator

As convenient as data-binding (to/from POJOs) can be; and as flexible as Tree model can be, there is one more canonical processing model available: incremental (aka "streaming") model.
It is the underlying processing model that data-binding and Tree Model both build upon, but it is also exposed to users who want ultimate performance and/or control over parsing or generation details.

For in-depth explanation, look at [Jackson Core component](jackson-core).
But let's look at a simple teaser to whet your appetite:

(TO BE COMPLETED)

## 5 minute tutorial: customizations

There are two entry-level configuration mechanisms you are likely to use:
[Features](jackson-databind/wiki/JacksonFeatures) and [Annotations](jackson-annotations).

As with all features so far, these use cases are meant to get you started; for more in-detail coverage, check out 
[Jackson Features](jackson-databind/wiki/JacksonFeatures) and [Jackson Annotations](jackson-annotations) pages.

### Most commonly used Features

Here are examples of configuration features that most users need first.

Let's start with higher-level data-binding configuration:

    // SerializationFeature for changing how JSON is written

    // to enable standard indentation ("pretty-printing"):
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    // to allow "empty" POJOs (no properties to serialize), prevent exception:
    mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    // to write java.util.Date, Calendar as number (timestamp):
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // DeserializationFeature for changing how JSON is read as POJOs:

    // to prevent exception when encountering unknown property:
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    // to allow coercion of JSON empty String ("") to null Object value:
    mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);

In addition, you may need to change some of low-level JSON parsing, generation details:

    // JsonParser.Feature for configuring parsing settings:
 
    // to allow C/C++ style comments in JSON (non-standard, disabled by default)
    mapper.enable(JsonParser.Feature.ALLOW_COMMENTS);
    // to allow (non-standard) unquoted field names in JSON:
    mapper.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
    // to allow use of apostrophes (single quotes), non standard
    mapper.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);

    // JsonGenerator.Feature for configuring low-level JSON generation:

    // to force escaping of non-ASCII characters:
    mapper.enable(JsonGenerator.Feature.ESCAPE_NON_ASCII);

### Changing property names

(TO BE COMPLETED)

### Ignoring properties

(TO BE COMPLETED)


----

# Further reading

* [Jackson Project Home](http://wiki.fasterxml.com/JacksonHome)
* [Documentation](http://wiki.fasterxml.com/JacksonDocumentation)
 * [JavaDocs](http://wiki.fasterxml.com/JacksonJavaDocs)
* [Downloads](http://wiki.fasterxml.com/JacksonDownload)

