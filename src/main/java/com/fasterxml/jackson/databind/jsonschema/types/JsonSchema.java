package com.fasterxml.jackson.databind.jsonschema.types;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * The type wraps the json schema specification at :
 * <a href="http://tools.ietf.org/id/draft-zyp-json-schema-03.txt"> Json JsonSchema
 * Draft </a> <blockquote> JSON (JavaScript Object Notation) JsonSchema defines the
 * media type "application/schema+json", a JSON based format for defining the
 * structure of JSON data. JSON JsonSchema provides a contract for what JSON data is
 * required for a given application and how to interact with it. JSON JsonSchema is
 * intended to define validation, documentation, hyperlink navigation, and
 * interaction control of JSON data. </blockquote>
 * 
 * <blockquote> JSON (JavaScript Object Notation) JsonSchema is a JSON media type
 * for defining the structure of JSON data. JSON JsonSchema provides a contract for
 * what JSON data is required for a given application and how to interact with
 * it. JSON JsonSchema is intended to define validation, documentation, hyperlink
 * navigation, and interaction control of JSON data. </blockquote>
 * 
 * An example JSON JsonSchema provided by the JsonSchema draft:
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
@JsonTypeInfo(use = Id.CUSTOM, include = As.PROPERTY, property = "type")
@JsonTypeIdResolver(JsonSchema.JsonSchemaIdResolver.class)
public abstract class JsonSchema {

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
	 * This attribute defines a URI of a JSON JsonSchema that is the schema of the
	 * current schema. When this attribute is defined, a validator SHOULD use
	 * the schema referenced by the value's URI (if known and available) when
	 * resolving Hyper JsonSchema (Section 6) links (Section 6.1).
	 * 
	 * A validator MAY use this attribute's value to determine which version of
	 * JSON JsonSchema the current schema is written in, and provide the appropriate
	 * validation features and behavior. Therefore, it is RECOMMENDED that all
	 * schema authors include this attribute in their schemas to prevent
	 * conflicts with future JSON JsonSchema specification changes.
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
	private JsonSchema[] disallow;
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
	private JsonSchema[] extendsextends;
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

	
	/**
	 * Attempt to return this JsonSchema as an {@link AnySchema}
	 * @return this as an AnySchema if possible, or null otherwise
	 */
	public AnySchema asAnySchema() {
		return null;
	}

	/**
	 * Attempt to return this JsonSchema as an {@link ArraySchema}
	 * @return this as an ArraySchema if possible, or null otherwise
	 */
	public ArraySchema asArraySchema() {
		return null;
	}

	/**
	 * Attempt to return this JsonSchema as a {@link BooleanSchema}
	 * @return this as a BooleanSchema if possible, or null otherwise
	 */
	public BooleanSchema asBooleanSchema() {
		return null;
	}

	/**
	 * Attempt to return this JsonSchema as a {@link ContainerTypeSchema}
	 * @return this as an ContainerTypeSchema if possible, or null otherwise
	 */
	public ContainerTypeSchema asContainerSchema() {
		return null;
	}

	/**
	 * Attempt to return this JsonSchema as an {@link IntegerSchema}
	 * @return this as an IntegerSchema if possible, or null otherwise
	 */
	public IntegerSchema asIntegerSchema() {
		return null;
	}

	/**
	 * Attempt to return this JsonSchema as a {@link NullSchema}
	 * @return this as a NullSchema if possible, or null otherwise
	 */
	public NullSchema asNullSchema() {
		return null;
	}

	/**
	 * Attempt to return this JsonSchema as a {@link NumberSchema}
	 * @return this as a NumberSchema if possible, or null otherwise
	 */
	public NumberSchema asNumberSchema() {
		return null;
	}

	/**
	 * Attempt to return this JsonSchema as an {@link ObjectSchema}
	 * @return this as an ObjectSchema if possible, or null otherwise
	 */
	public ObjectSchema asObjectSchema() {
		return null;
	}

	/**
	 * Attempt to return this JsonSchema as a {@link SimpleTypeSchema}
	 * @return this as a SimpleTypeSchema if possible, or null otherwise
	 */
	public SimpleTypeSchema asSimpleTypeSchema() {
		return null;
	}
	
	/**
	 * Attempt to return this JsonSchema as a {@link StringSchema}
	 * @return this as a StringSchema if possible, or null otherwise
	 */
	public StringSchema asStringSchema() {
		return null;
	}

	/**
	 * Attempt to return this JsonSchema as an {@link UnionTypeSchema}
	 * @return this as a UnionTypeSchema if possible, or null otherwise
	 */
	public UnionTypeSchema asUnionTypeSchema() {
		return null;
	}

	/**
	 * Attempt to return this JsonSchema as a {@link ValueTypeSchema}
	 * @return this as a ValueTypeSchema if possible, or null otherwise
	 */
	public ValueTypeSchema asValueSchemaSchema() {
		return null;
	}

	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof JsonSchema) {
			JsonSchema that = ((JsonSchema)obj);
			return that.getType() == getType() &&
					getRequired() == null ? that.getRequired() == null : 
						getRequired().equals(that.getRequired()) &&
					get$ref() == null ? that.get$ref() == null : 
						get$ref().equals(that.get$ref()) &&
					get$schema() == null ? that.get$schema() == null : 
						get$schema().equals(that.get$schema()) &&
					getDisallow() == null ? that.getDisallow() == null : 
						getDisallow().equals(that.getDisallow()) &&
					getExtends() == null ? that.getExtends() == null : 
						getExtends().equals(that.getExtends());
		} 
		return false;
	}
	
	/**
	 * {@link JsonSchema#$ref}
	 * 
	 * @return the $ref
	 */
	public String get$ref() {
		return $ref;
	}

	/**
	 * {@link JsonSchema#$schema}
	 * 
	 * @return the $schema
	 */
	public String get$schema() {
		return $schema;
	}

	/**
	 * {@link JsonSchema#disallow}
	 * 
	 * @return the disallow
	 */
	public JsonSchema[] getDisallow() {
		return disallow;
	}

	/**
	 * {@link JsonSchema#extendsextends}
	 * 
	 * @return the extendsextends
	 */
	@JsonGetter("extends")
	public JsonSchema[] getExtends() {
		return extendsextends;
	}

	/**
	 * {@link JsonSchema#id}
	 * 
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * {@link JsonSchema#required}
	 * 
	 * @return the required
	 */
	public Boolean getRequired() {
		return required;
	}

	@JsonIgnore
	public abstract SchemaType getType();


	/**
	 * determine if this JsonSchema is an {@link AnySchema}.
	 *
	 * @return true if this JsonSchema is an AnySchema, false otherwise
	 */
	@JsonIgnore
	public boolean isAnySchema() {
		return false;
	}

	/**
	 * determine if this JsonSchema is an {@link ArraySchema}.
	 *
	 * @return true if this JsonSchema is an ArraySchema, false otherwise
	 */
	@JsonIgnore
	public boolean isArraySchema() {
		return false;
	}

	/**
	 * determine if this JsonSchema is an {@link BooleanSchema}.
	 *
	 * @return true if this JsonSchema is an BooleanSchema, false otherwise
	 */
	@JsonIgnore
	public boolean isBooleanSchema() {
		return false;
	}

	/**
	 * determine if this JsonSchema is an {@link ContainerTypeSchema}.
	 *
	 * @return true if this JsonSchema is an ContainerTypeSchema, false otherwise
	 */
	@JsonIgnore
	public boolean isContainerTypeSchema() {
		return false;
	}

	/**
	 * determine if this JsonSchema is an {@link IntegerSchema}.
	 *
	 * @return true if this JsonSchema is an IntegerSchema, false otherwise
	 */
	@JsonIgnore
	public boolean isIntegerSchema() {
		return false;
	}

	/**
	 * determine if this JsonSchema is an {@link NullSchema}.
	 *
	 * @return true if this JsonSchema is an NullSchema, false otherwise
	 */
	@JsonIgnore
	public boolean isNullSchema() {
		return false;
	}

	/**
	 * determine if this JsonSchema is an {@link NumberSchema}.
	 *
	 * @return true if this JsonSchema is an NumberSchema, false otherwise
	 */
	@JsonIgnore
	public boolean isNumberSchema() {
		return false;
	}

	/**
	 * determine if this JsonSchema is an {@link ObjectSchema}.
	 *
	 * @return true if this JsonSchema is an ObjectSchema, false otherwise
	 */
	@JsonIgnore
	public boolean isObjectSchema() {
		return false;
	}

	/**
	 * determine if this JsonSchema is an {@link SimpleTypeSchema}.
	 *
	 * @return true if this JsonSchema is an SimpleTypeSchema, false otherwise
	 */
	@JsonIgnore
	public boolean isSimpleTypeSchema() {
		return false;
	}

	/**
	 * determine if this JsonSchema is an {@link StringSchema}.
	 *
	 * @return true if this JsonSchema is an StringSchema, false otherwise
	 */
	@JsonIgnore
	public boolean isStringSchema() {
		return false;
	}

	/**
	 * determine if this JsonSchema is an {@link UnionTypeSchema}.
	 *
	 * @return true if this JsonSchema is an UnionTypeSchema, false otherwise
	 */
	@JsonIgnore
	public boolean isUnionTypeSchema() {
		return false;
	}


	/**
	 * determine if this JsonSchema is an {@link ValueTypeSchema}.
	 *
	 * @return true if this JsonSchema is an ValueTypeSchema, false otherwise
	 */
	@JsonIgnore
	public boolean isValueTypeSchema() {
		return false;
	}

	/**
	 * {@link JsonSchema#$ref}
	 * 
	 * @param $ref
	 *            the $ref to set
	 */
	public void set$ref(String $ref) {
		this.$ref = $ref;
	}

	/**
	 * {@link JsonSchema#$schema}
	 * 
	 * @param $schema
	 *            the $schema to set
	 */
	public void set$schema(String $schema) {
		this.$schema = $schema;
	}

	/**
	 * {@link JsonSchema#disallow}
	 * 
	 * @param disallow
	 *            the disallow to set
	 */
	public void setDisallow(JsonSchema[] disallow) {
		this.disallow = disallow;
	}

	/**
	 * {@link JsonSchema#extendsextends}
	 * 
	 * @param extendsextends
	 *            the extendsextends to set
	 */
	@JsonSetter("extends")
	public void setExtends(JsonSchema[] extendsextends) {
		this.extendsextends = extendsextends;
	}

	/**
	 * {@link JsonSchema#id}
	 * 
	 * @param id
	 *            the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * {@link JsonSchema#required}
	 * 
	 * @param required
	 *            the required to set
	 */
	public void setRequired(Boolean required) {
		this.required = required;
	}

	/**
	 * Create a schema which verifies only that an object is of the given format.
	 * @param format the format to expect
	 * @return the schema verifying the given format
	 */
	public static JsonSchema minimalForFormat(SchemaType format) {
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
	
	public static class JsonSchemaIdResolver implements TypeIdResolver {


		/* (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.jsontype.TypeIdResolver#idFromValue(java.lang.Object)
		 */
		public String idFromValue(Object value) {
			if ( value instanceof JsonSchema) {
				return ((JsonSchema)value).getType().value();
			} else {
				return null;
			}
		}

		/* (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.jsontype.TypeIdResolver#idFromValueAndType(java.lang.Object, java.lang.Class)
		 */
		public String idFromValueAndType(Object value, Class<?> suggestedType) {
			return idFromValue(value);
		}
		
		private static JavaType any = TypeFactory.defaultInstance().constructType(AnySchema.class);
		private static JavaType array = TypeFactory.defaultInstance().constructType(ArraySchema.class);
		private static JavaType booleanboolean = TypeFactory.defaultInstance().constructType(BooleanSchema.class);
		private static JavaType integer = TypeFactory.defaultInstance().constructType(IntegerSchema.class);
		private static JavaType nullnull = TypeFactory.defaultInstance().constructType(NullSchema.class);
		private static JavaType number = TypeFactory.defaultInstance().constructType(NumberSchema.class);
		private static JavaType object = TypeFactory.defaultInstance().constructType(ObjectSchema.class);
		private static JavaType string = TypeFactory.defaultInstance().constructType(StringSchema.class);
		
		/* (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.jsontype.TypeIdResolver#typeFromId(java.lang.String)
		 */
		public JavaType typeFromId(String id) {
			switch (SchemaType.forValue(id)) {
			case ANY: 		return any;
			case ARRAY: 	return array;
			case BOOLEAN:	return booleanboolean;
			case INTEGER:	return integer;
			case NULL:		return nullnull;
			case NUMBER:	return number;
			case OBJECT:	return object;
			case STRING:	return string;
			default:
				return null;
			}
		}

		/* (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.jsontype.TypeIdResolver#getMechanism()
		 */
		public Id getMechanism() {
			return Id.CUSTOM;
		}

		/* (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.jsontype.TypeIdResolver#init(com.fasterxml.jackson.databind.JavaType)
		 */
		public void init(JavaType baseType) { }

		/* (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.jsontype.TypeIdResolver#idFromBaseType()
		 */
		public String idFromBaseType() {
			return null;
		}
		
	}
}