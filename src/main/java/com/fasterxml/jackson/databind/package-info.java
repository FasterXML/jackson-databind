/**
Contains basic mapper (conversion) functionality that
allows for converting between regular streaming JSON content and
Java objects (POJOs or Tree Model: support for both is via
{@link com.fasterxml.jackson.databind.ObjectMapper},
{@link com.fasterxml.jackson.databind.ObjectReader} and
{@link com.fasterxml.jackson.databind.ObjectWriter},
 classes.
<p>
The main starting point for operations is {@link com.fasterxml.jackson.databind.ObjectMapper},
which can be used either directly (via multiple overloaded
<code>readValue</code>,
<code>readTree</code>,
<code>writeValue</code> and
<code>writeTree</code> methods, or it can be used as a configurable factory for constructing
fully immutable, thread-safe and reusable {@link com.fasterxml.jackson.databind.ObjectReader}
and {@link com.fasterxml.jackson.databind.ObjectWriter} objects.
<p>
In addition to simple reading and writing of JSON as POJOs or JSON trees (represented as
{@link com.fasterxml.jackson.databind.JsonNode}, and configurability needed to change aspects
of reading/writing, mapper contains additional functionality such as:
<ul>
 <li>Value conversions using {@link com.fasterxml.jackson.databind.ObjectMapper#convertValue(Object, Class)},
  {@link com.fasterxml.jackson.databind.ObjectMapper#valueToTree(Object)} and
  {@link com.fasterxml.jackson.databind.ObjectMapper#treeToValue(com.fasterxml.jackson.core.TreeNode, Class)} methods.
  </li>
 <li>Type introspection needed for things like generation of Schemas (like JSON Schema, Avro Schema, or protoc
   definitions), using {@link com.fasterxml.jackson.databind.ObjectMapper#acceptJsonFormatVisitor(Class, com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper)}
   (note: actual handles are usually provided by various Jackson modules: mapper simply initiates calling of
   callbacks, based on serializers registered)
  </li>
</ul>
<p>
For more usage, refer to
{@link com.fasterxml.jackson.databind.ObjectMapper},
{@link com.fasterxml.jackson.databind.ObjectReader} and
{@link com.fasterxml.jackson.databind.ObjectWriter}
Javadocs.
*/

package com.fasterxml.jackson.databind;
