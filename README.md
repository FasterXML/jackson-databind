# Overview

This project contains the general-purpose data-binding functionality
and tree-model for Jackson. It builds on the
[core streaming parser/generator](/FasterXML/jackson-core) package,
and uses [Jackson Annotations](/FasterXML/jackson-annotations) for
configuration

While the original use case was JSON data-binding,
it can typically be used for other data formats as well, as long as
parser and generator implementations exist.
Naming of classes uses word 'JSON' in many places even though there is no
actual hard dependency to JSON format.

----

## Usage, general

### Maven, Java package

All annotations are in Java package `com.fasterxml.core.annotation`.
To use annotations, you need to use Maven dependency:

    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-annotations</artifactId>
      <version>2.0.0</version>
    </dependency>

or download jars from Maven repository or [Download page](wiki.fasterxml.com/JacksonDownload)

----

## Usage, simple

(TO BE WRITTEN)

### Full data-binding

(TO BE WRITTEN)

### JSON Trees

(TO BE WRITTEN)

### 'Simple' data-binding (Lists, Maps, String/Boolean/Number)

(TO BE WRITTEN)

----

Project contains versions 2.0 and above: source code for earlier (1.x) versions is available from [Codehaus](http://jackson.codehaus.org) SVN repository

Note that the main differences compared to 1.0 core jar are:

* Maven build instead of Ant
* Java package is now `com.fasterxml.jackson.databind` (instead of `org.codehaus.jackson.map`)

# Further reading

* [Jackson Project Home](http://wiki.fasterxml.com/JacksonHome)
* [Documentation](http://wiki.fasterxml.com/JacksonDocumentation)
 * [JavaDocs](http://wiki.fasterxml.com/JacksonJavaDocs)
* [Downloads](http://wiki.fasterxml.com/JacksonDownload)

