package com.fasterxml.jackson.databind.jsonschema.types;

import java.lang.reflect.Type;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonschema.JsonSchema;
import com.fasterxml.jackson.databind.jsonschema.SchemaAware;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.ser.BeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;

/**
 * <a href="http://tools.ietf.org/id/draft-zyp-json-schema-03.txt"> Json Schema Draft </a>
 *  <blockquote>
   	JSON (JavaScript Object Notation) Schema defines the media type
	"application/schema+json", a JSON based format for defining the
	structure of JSON data.  JSON Schema provides a contract for what
	JSON data is required for a given application and how to interact
	with it.  JSON Schema is intended to define validation,
	documentation, hyperlink navigation, and interaction control of JSON
	data.
	</blockquote>
	
	<blockquote>
	JSON (JavaScript Object Notation) Schema is a JSON media type for
	defining the structure of JSON data.  JSON Schema provides a contract
	for what JSON data is required for a given application and how to
	interact with it.  JSON Schema is intended to define validation,
	documentation, hyperlink navigation, and interaction control of JSON
	data.
	</blockquote>
	
	An example JSON Schema provided by the Schema draft:
	<pre>
	{
	  "name":"Product",
	  "properties":{
	    "id":{
	      "type":"number",
	      "description":"Product identifier",
	      "required":true
	    },
	    "name":{
	      "description":"Name of the product",
	      "type":"string",
	      "required":true
	    },
	    "price":{
	      "required":true,
	      "type": "number",
	      "minimum":0,
	      "required":true
	    },
	    "tags":{
	      "type":"array",
	      "items":{
	        "type":"string"
	      }
	    }
	  },
	  "links":[
	    {
	      "rel":"full",
	      "href":"{id}"
	    },
	    {
	      "rel":"comments",
	      "href":"comments/?id={id}"
	    }
	  ]
	}
	</pre>

 * @author jphelan
 */
public abstract class Schema { 
	 public JsonNode asJson() {
		 
		 return null;
	 }
	
	/**
	 * 	Attempt to add the output of the given {@link BeanPropertyWriter} in the given {@link ObjectNode}.
	 * 	Otherwise, add the default schema {@link JsonNode} in place of the writer's output
	 * 
	 * @param writer Bean property serializer to use to create schema value
	 * @param propertiesNode Node which the given property would exist within
	 * @param provider Provider that can be used for accessing dynamic aspects of serialization
	 * 	processing
	 * 	
	 *  {@link BeanPropertyFilter#depositSchemaProperty(BeanPropertyWriter, ObjectNode, SerializerProvider)}
	 */
	public static void depositSchemaProperty(BeanPropertyWriter writer, ObjectNode propertiesNode, SerializerProvider provider) {
		JavaType propType = writer.getSerializationType();
	
		// 03-Dec-2010, tatu: SchemaAware REALLY should use JavaType, but alas it doesn't...
		Type hint = (propType == null) ? writer.getGenericPropertyType() : propType.getRawClass();
		JsonNode schemaNode;
		// Maybe it already has annotated/statically configured serializer?
		JsonSerializer<Object> ser = writer.getSerializer();
	
		try {
			if (ser == null) { // nope
				Class<?> serType = writer.getRawSerializationType();
				if (serType == null) {
					serType = writer.getPropertyType();
				}
				ser = provider.findValueSerializer(serType, writer);
			}
			boolean isOptional = !BeanSerializerBase.isPropertyRequired(writer, provider);
			if (ser instanceof SchemaAware) {
				schemaNode =  ((SchemaAware) ser).getSchema(provider, hint, isOptional) ;
			} else {  
				schemaNode = JsonSchema.getDefaultSchemaNode(); 
			}
		} catch (JsonMappingException e) {
			schemaNode = JsonSchema.getDefaultSchemaNode(); 
			//TODO: log error
		}
		propertiesNode.put(writer.getName(), schemaNode);
	}

	/*
	 * This attribute indicates if the instance must have a value, and not
	   be undefined.  This is false by default, making the instance
	   optional.
	 */
	private BooleanNode required = BooleanNode.FALSE;// default = false;
	
	/*
	 *  This attribute defines the current URI of this schema (this attribute
	   is effectively a "self" link).  This URI MAY be relative or absolute.
	   If the URI is relative it is resolved against the current URI of the
	   parent schema it is contained in.  If this schema is not contained in
	   any parent schema, the current URI of the parent schema is held to be
	   the URI under which this schema was addressed.  If id is missing, the
	   current URI of a schema is defined to be that of the parent schema.
	   The current URI of the schema is also used to construct relative
	   references such as for $ref.
	 */
	private TextNode id;
	
	/*
	 * This attribute defines a URI of a schema that contains the full
	   representation of this schema.  When a validator encounters this
	   attribute, it SHOULD replace the current schema with the schema
	   referenced by the value's URI (if known and available) and re-
	   validate the instance.  This URI MAY be relative or absolute, and
	   relative URIs SHOULD be resolved against the URI of the current
	   schema.
	 */
	private TextNode $ref; 
	
	/*
	 * This attribute defines a URI of a JSON Schema that is the schema of
	   the current schema.  When this attribute is defined, a validator
	   SHOULD use the schema referenced by the value's URI (if known and
	   available) when resolving Hyper Schema (Section 6) links
	   (Section 6.1).
	
	   A validator MAY use this attribute's value to determine which version
	   of JSON Schema the current schema is written in, and provide the
	   appropriate validation features and behavior.  Therefore, it is
	   RECOMMENDED that all schema authors include this attribute in their
	   schemas to prevent conflicts with future JSON Schema specification
	   changes.

	 */
	private TextNode $schema;
	
	/*
	 * The value of this property MUST be another schema which will provide
	   a base schema which the current schema will inherit from.  The
	   inheritance rules are such that any instance that is valid according
	   to the current schema MUST be valid according to the referenced
	   schema.  This MAY also be an array, in which case, the instance MUST
	   be valid for all the schemas in the array.  A schema that extends
	   another schema MAY define additional attributes, constrain existing
	   attributes, or add other constraints.
	
	   Conceptually, the behavior of extends can be seen as validating an
	   instance against all constraints in the extending schema as well as
	   the extended schema(s).  More optimized implementations that merge
	   schemas are possible, but are not required.  An example of using
	   "extends":
	
	   {
	     "description":"An adult",
	     "properties":{"age":{"minimum": 21}},
	     "extends":"person"
	   }
	   {
	     "description":"Extended schema",
	     "properties":{"deprecated":{"type": "boolean"}},
	     "extends":"http://json-schema.org/draft-03/schema"
	   }
	 */
	private Schema[] extendsextends;

	/*
	 * This attribute takes the same values as the "type" attribute, however
	   if the instance matches the type or if this value is an array and the
	   instance matches any type or schema in the array, then this instance
	   is not valid.
	 */
	private Schema[] disallow;

}