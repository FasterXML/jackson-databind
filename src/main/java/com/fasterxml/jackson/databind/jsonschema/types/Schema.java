package com.fasterxml.jackson.databind.jsonschema.types;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * <a href="http://tools.ietf.org/id/draft-zyp-json-schema-03.txt"> Json Schema
 * Draft </a> <blockquote> JSON (JavaScript Object Notation) Schema defines the
 * media type "application/schema+json", a JSON based format for defining the
 * structure of JSON data. JSON Schema provides a contract for what JSON data is
 * required for a given application and how to interact with it. JSON Schema is
 * intended to define validation, documentation, hyperlink navigation, and
 * interaction control of JSON data. </blockquote>
 * 
 * <blockquote> JSON (JavaScript Object Notation) Schema is a JSON media type
 * for defining the structure of JSON data. JSON Schema provides a contract for
 * what JSON data is required for a given application and how to interact with
 * it. JSON Schema is intended to define validation, documentation, hyperlink
 * navigation, and interaction control of JSON data. </blockquote>
 * 
 * An example JSON Schema provided by the Schema draft:
 * 
 * <pre>
 * 	{
 * 	  "name":"Product",
 * 	  "properties":{
 * 	    "id":{
 * 	      "type":"number",
 * 	      "description":"Product identifier",
 * 	      "required":true
 * 	    },
 * 	    "name":{
 * 	      "description":"Name of the product",
 * 	      "type":"string",
 * 	      "required":true
 * 	    },
 * 	    "price":{
 * 	      "required":true,
 * 	      "type": "number",
 * 	      "minimum":0,
 * 	      "required":true
 * 	    },
 * 	    "tags":{
 * 	      "type":"array",
 * 	      "items":{
 * 	        "type":"string"
 * 	      }
 * 	    }
 * 	  },
 * 	  "links":[
 * 	    {
 * 	      "rel":"full",
 * 	      "href":"{id}"
 * 	    },
 * 	    {
 * 	      "rel":"comments",
 * 	      "href":"comments/?id={id}"
 * 	    }
 * 	  ]
 * 	}
 * </pre>
 * 
 * @author jphelan
 */
@JsonInclude(Include.NON_EMPTY)
public abstract class Schema {

	/**
	 * This attribute defines a URI of a schema that contains the full
	 * representation of this schema. When a validator encounters this
	 * attribute, it SHOULD replace the current schema with the schema
	 * referenced by the value's URI (if known and available) and re- validate
	 * the instance. This URI MAY be relative or absolute, and relative URIs
	 * SHOULD be resolved against the URI of the current schema.
	 */
	@JsonProperty
	private String $ref;

	/**
	 * This attribute defines a URI of a JSON Schema that is the schema of the
	 * current schema. When this attribute is defined, a validator SHOULD use
	 * the schema referenced by the value's URI (if known and available) when
	 * resolving Hyper Schema (Section 6) links (Section 6.1).
	 * 
	 * A validator MAY use this attribute's value to determine which version of
	 * JSON Schema the current schema is written in, and provide the appropriate
	 * validation features and behavior. Therefore, it is RECOMMENDED that all
	 * schema authors include this attribute in their schemas to prevent
	 * conflicts with future JSON Schema specification changes.
	 */
	@JsonProperty
	private String $schema;

	/**
	 * This attribute takes the same values as the "type" attribute, however if
	 * the instance matches the type or if this value is an array and the
	 * instance matches any type or schema in the array, then this instance is
	 * not valid.
	 */
	@JsonProperty
	private Schema[] disallow;
	/**
	 * The value of this property MUST be another schema which will provide a
	 * base schema which the current schema will inherit from. The inheritance
	 * rules are such that any instance that is valid according to the current
	 * schema MUST be valid according to the referenced schema. This MAY also be
	 * an array, in which case, the instance MUST be valid for all the schemas
	 * in the array. A schema that extends another schema MAY define additional
	 * attributes, constrain existing attributes, or add other constraints.
	 * 
	 * Conceptually, the behavior of extends can be seen as validating an
	 * instance against all constraints in the extending schema as well as the
	 * extended schema(s). More optimized implementations that merge schemas are
	 * possible, but are not required. An example of using "extends":
	 * 
	 * { "description":"An adult", "properties":{"age":{"minimum": 21}},
	 * "extends":"person" } { "description":"Extended schema",
	 * "properties":{"deprecated":{"type": "boolean"}},
	 * "extends":"http://json-schema.org/draft-03/schema" }
	 */
	@JsonIgnore
	private Schema[] extendsextends;
	/**
	 * This attribute defines the current URI of this schema (this attribute is
	 * effectively a "self" link). This URI MAY be relative or absolute. If the
	 * URI is relative it is resolved against the current URI of the parent
	 * schema it is contained in. If this schema is not contained in any parent
	 * schema, the current URI of the parent schema is held to be the URI under
	 * which this schema was addressed. If id is missing, the current URI of a
	 * schema is defined to be that of the parent schema. The current URI of the
	 * schema is also used to construct relative references such as for $ref.
	 */
	@JsonProperty
	private String id;
	/**
	 * This attribute indicates if the instance must have a value, and not be
	 * undefined. This is false by default, making the instance optional.
	 */
	@JsonProperty
	private Boolean required = null;

	public AnySchema asAnySchema() {
		return null;
	}

	public ArraySchema asArraySchema() {
		return null;
	}

	public BooleanSchema asBooleanSchema() {
		return null;
	}

	public ContainerTypeSchema asContainerSchema() {
		return null;
	}

	public IntegerSchema asIntegerSchema() {
		return null;
	}

	public NullSchema asNullSchema() {
		return null;
	}

	public NumberSchema asNumberSchema() {
		return null;
	}

	public ObjectSchema asObjectSchema() {
		return null;
	}

	public SimpleTypeSchema asSimpleTypeSchema() {
		return null;
	}

	public StringSchema asStringSchema() {
		return null;
	}

	public UnionTypeSchema asUnionTypeSchema() {
		return null;
	}

	public ValueTypeSchema asValueSchemaSchema() {
		return null;
	}

	/**
	 * {@link Schema#$ref}
	 * 
	 * @return the $ref
	 */
	public String get$ref() {
		return $ref;
	}

	/**
	 * {@link Schema#$schema}
	 * 
	 * @return the $schema
	 */
	public String get$schema() {
		return $schema;
	}

	/**
	 * {@link Schema#disallow}
	 * 
	 * @return the disallow
	 */
	public Schema[] getDisallow() {
		return disallow;
	}

	/**
	 * {@link Schema#extendsextends}
	 * 
	 * @return the extendsextends
	 */
	@JsonGetter("extends")
	public Schema[] getExtends() {
		return extendsextends;
	}

	/**
	 * {@link Schema#id}
	 * 
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * {@link Schema#required}
	 * 
	 * @return the required
	 */
	public Boolean getRequired() {
		return required;
	}

	public abstract SchemaType getType();

	@JsonIgnore
	public boolean isAnySchema() {
		return false;
	}

	@JsonIgnore
	public boolean isArraySchema() {
		return false;
	}

	@JsonIgnore
	public boolean isBooleanSchema() {
		return false;
	}

	@JsonIgnore
	public boolean isContainerTypeSchema() {
		return false;
	}

	@JsonIgnore
	public boolean isIntegerSchema() {
		return false;
	}

	@JsonIgnore
	public boolean isNullSchema() {
		return false;
	}

	@JsonIgnore
	public boolean isNumberSchema() {
		return false;
	}

	@JsonIgnore
	public boolean isObjectSchema() {
		return false;
	}

	@JsonIgnore
	public boolean isSimpleTypeSchema() {
		return false;
	}

	@JsonIgnore
	public boolean isStringSchema() {
		return false;
	}

	@JsonIgnore
	public boolean isUnionTypeSchema() {
		return false;
	}

	@JsonIgnore
	public boolean isValueTypeSchema() {
		return false;
	}

	/**
	 * {@link Schema#$ref}
	 * 
	 * @param $ref
	 *            the $ref to set
	 */
	public void set$ref(String $ref) {
		this.$ref = $ref;
	}

	/**
	 * {@link Schema#$schema}
	 * 
	 * @param $schema
	 *            the $schema to set
	 */
	public void set$schema(String $schema) {
		this.$schema = $schema;
	}

	/**
	 * {@link Schema#disallow}
	 * 
	 * @param disallow
	 *            the disallow to set
	 */
	public void setDisallow(Schema[] disallow) {
		this.disallow = disallow;
	}

	/**
	 * {@link Schema#extendsextends}
	 * 
	 * @param extendsextends
	 *            the extendsextends to set
	 */
	@JsonSetter("extends")
	public void setExtends(Schema[] extendsextends) {
		this.extendsextends = extendsextends;
	}

	/**
	 * {@link Schema#id}
	 * 
	 * @param id
	 *            the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * {@link Schema#required}
	 * 
	 * @param required
	 *            the required to set
	 */
	public void setRequired(Boolean required) {
		this.required = required;
	}

	public static Schema minimalForFormat(SchemaType format) {
		switch (format) {
		case ARRAY:
			return new ArraySchema();
		case OBJECT:
			return new ObjectSchema();
		case BOOLEAN:
			return new BooleanSchema();
		case INTEGER:
			return new IntegerSchema();
		case NUMBER:
			return new NumberSchema();
		case STRING:
			return new StringSchema();
		case NULL:
			return new NullSchema();

		default:
			return new AnySchema();
		}
	}

}