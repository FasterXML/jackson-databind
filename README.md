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

## Differences from Jackson 1.x

Project contains versions 2.0 and above: source code for earlier (1.x) versions is available from [Codehaus](http://jackson.codehaus.org) SVN repository

Note that the main differences compared to 1.0 core jar are:

* Maven build instead of Ant
* Java package is now `com.fasterxml.jackson.databind` (instead of `org.codehaus.jackson.map`)


----

## Usage, general

### Maven, Java package

Functionality of this package is contained in 
Java package `com.fasterxml.core.databind`.
To use databinding, you need to use following Maven dependency:

    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
      <version>2.0.0</version>
    </dependency>

or download jars from Maven repository or [Download page](wiki.fasterxml.com/JacksonDownload).
Core jar is a functional OSGi bundle, with proper import/export declarations.

Since package also depends on '''jackson-core''' and '''jackson-databind''' packages, you will need to download these if not using Maven; and you may also want to add them as Maven dependency to ensure that compatible versions are used.
If so:

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

# Further reading

* [Jackson Project Home](http://wiki.fasterxml.com/JacksonHome)
* [Documentation](http://wiki.fasterxml.com/JacksonDocumentation)
 * [JavaDocs](http://wiki.fasterxml.com/JacksonJavaDocs)
* [Downloads](http://wiki.fasterxml.com/JacksonDownload)

