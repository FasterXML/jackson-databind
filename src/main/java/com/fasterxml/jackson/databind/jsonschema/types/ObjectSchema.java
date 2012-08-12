package com.fasterxml.jackson.databind.jsonschema.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * This type represents a {@link JsonSchema} as an object type
 * @author jphelan
 *
 */
public class ObjectSchema extends ContainerTypeSchema {

	/**
	 * This attribute defines a jsonSchema for all properties that are not
	 * explicitly defined in an object type definition. If specified, the value
	 * MUST be a jsonSchema or a boolean. If false is provided, no additional
	 * properties are allowed beyond the properties defined in the jsonSchema. The
	 * default value is an empty jsonSchema which allows any value for additional
	 * properties.
	 */
	@JsonProperty
	private AdditionalProperties additionalProperties;

	/**
	 * This attribute is an object that defines the requirements of a property
	 * on an instance object. If an object instance has a property with the same
	 * name as a property in this attribute's object, then the instance must be
	 * valid against the attribute's property value
	 */
	@JsonProperty
	private List<Dependency> dependencies;

	/**
	 * 
	 This attribute is an object that defines the jsonSchema for a set of property
	 * names of an object instance. The name of each property of this
	 * attribute's object is a regular expression pattern in the ECMA 262/Perl 5
	 * format, while the value is a jsonSchema. If the pattern matches the name of a
	 * property on the instance object, the value of the instance's property
	 * MUST be valid against the pattern name's jsonSchema value.
	 */
	@JsonProperty
	private Map<String, JsonSchema> patternProperties;

	/**
	 * This attribute is an object with property definitions that define the
	 * valid values of instance object property values. When the instance value
	 * is an object, the property values of the instance object MUST conform to
	 * the property definitions in this object. In this object, each property
	 * definition's value MUST be a jsonSchema, and the property's name MUST be the
	 * name of the instance property that it defines. The instance property
	 * value MUST be valid according to the jsonSchema from the property definition.
	 * Properties are considered unordered, the order of the instance properties
	 * MAY be in any order.
	 */
	@JsonProperty
	private Map<String, JsonSchema> properties;

	@JsonIgnore
	private final SchemaType type = SchemaType.OBJECT;

	// instance initializer block
	{
		dependencies = new ArrayList<Dependency>();
		patternProperties = new HashMap<String, JsonSchema>();
		properties = new HashMap<String, JsonSchema>();
	}

	public boolean addSchemaDependency(String depender, JsonSchema parentMustMatch) {
		return dependencies
				.add(new SchemaDependency(depender, parentMustMatch));
	}

	public boolean addSimpleDependency(String depender, String dependsOn) {
		return dependencies.add(new SimpleDependency(depender, dependsOn));
	}

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.JsonSchema#asObjectSchema()
	 */
	@Override
	public ObjectSchema asObjectSchema() {
		return this;
	}

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.JsonSchema#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ObjectSchema) {
			ObjectSchema that = (ObjectSchema) obj;
			return getAdditionalProperties() == null ? that.getAdditionalProperties() == null :
						getAdditionalProperties().equals(that.getAdditionalProperties()) &&
					getDependencies() == null ? that.getDependencies() == null :
						getDependencies().equals(that.getDependencies()) &&
					getPatternProperties() == null ? that.getPatternProperties() == null :
						getPatternProperties().equals(that.getPatternProperties()) &&
					getProperties() == null ? that.getProperties() == null :
						getProperties().equals(that.getProperties()) &&
					super.equals(obj);
		} else {
			return false;
		}

	}
	
	/**
	 * {@link ObjectSchema#additionalProperties}
	 * 
	 * @return the additionalProperties
	 */
	public AdditionalProperties getAdditionalProperties() {
		return additionalProperties;
	}

	/**
	 * {@link ObjectSchema#dependencies}
	 * 
	 * @return the dependencies
	 */
	public List<Dependency> getDependencies() {
		return dependencies;
	}

	/**
	 * {@link ObjectSchema#patternProperties}
	 * 
	 * @return the patternProperties
	 */
	public Map<String, JsonSchema> getPatternProperties() {
		return patternProperties;
	}

	/**
	 * {@link ObjectSchema#properties}
	 * 
	 * @return the properties
	 */
	public Map<String, JsonSchema> getProperties() {
		return properties;
	}

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.JsonSchema#getType()
	 */
	@Override
	public SchemaType getType() {
		return type;
	}
	
	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.JsonSchema#isObjectSchema()
	 */
	@Override
	public boolean isObjectSchema() {
		return true;
	}

	public void putOptionalProperty(String name, JsonSchema jsonSchema) {
		properties.put(name, jsonSchema);
	}

	public JsonSchema putPatternProperty(String regex, JsonSchema value) {
		return patternProperties.put(regex, value);
	}

	public JsonSchema putProperty(String name, JsonSchema value) {
		value.setRequired(true);
		return properties.put(name, value);
	}

	public void rejectAdditionalProperties() {
		additionalProperties = NoAdditionalProperties.instance;
	}

	/**
	 * {@link ObjectSchema#additionalProperties}
	 * 
	 * @param additionalProperties
	 *            the additionalProperties to set
	 */
	public void setAdditionalProperties(
			AdditionalProperties additionalProperties) {
		this.additionalProperties = additionalProperties;
	}

	/**
	 * {@link ObjectSchema#dependencies}
	 * 
	 * @param dependencies
	 *            the dependencies to set
	 */
	public void setDependencies(List<Dependency> dependencies) {
		this.dependencies = dependencies;
	}

	/**
	 * {@link ObjectSchema#patternProperties}
	 * 
	 * @param patternProperties
	 *            the patternProperties to set
	 */
	public void setPatternProperties(Map<String, JsonSchema> patternProperties) {
		this.patternProperties = patternProperties;
	}

	/**
	 * {@link ObjectSchema#properties}
	 * 
	 * @param properties
	 *            the properties to set
	 */
	public void setProperties(Map<String, JsonSchema> properties) {
		this.properties = properties;
	}

	public static abstract class AdditionalProperties {
		
		@JsonCreator
		public AdditionalProperties jsonCreator() {
			//KNOWN ISSUE: pending https://github.com/FasterXML/jackson-databind/issues/43
			return null;
		}
	}

	public static abstract class Dependency {
		@JsonCreator
		public Dependency jsonCreator() {
			//KNOWN ISSUE: pending https://github.com/FasterXML/jackson-databind/issues/43
			return null;
		}
	}

	public static class NoAdditionalProperties extends AdditionalProperties {
		public final Boolean schema = false;

		protected NoAdditionalProperties() {
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			return obj instanceof NoAdditionalProperties;
		}

		@JsonValue
		public Boolean value() {
			return schema;
		}

		public static final NoAdditionalProperties instance = new NoAdditionalProperties();
	}

	public static class SchemaAdditionalProperties extends AdditionalProperties {

		@JsonProperty
		private JsonSchema jsonSchema;

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			return obj instanceof SchemaAdditionalProperties &&
					getJsonSchema() == null ? ((SchemaAdditionalProperties)obj).getJsonSchema() == null :
						getJsonSchema().equals(((SchemaAdditionalProperties)obj).getJsonSchema());
		}
		
		/**
		 * {@link ObjectSchema.SchemaAdditionalProperties#jsonSchema}
		 * @return the jsonSchema
		 */
		public JsonSchema getJsonSchema() {
			return jsonSchema;
		}
		
		public SchemaAdditionalProperties(JsonSchema jsonSchema) {
			this.jsonSchema = jsonSchema;
		}
	}

	/**
	 * JsonSchema Dependency If the dependency value is a jsonSchema, then the instance
	 * object MUST be valid against the jsonSchema.
	 */
	public static class SchemaDependency extends Dependency {

		@JsonProperty(required = true)
		private String depender;

		@JsonProperty(required = true)
		private JsonSchema parentMustMatch;

		public SchemaDependency(String depender, JsonSchema parentMustMatch) {
			this.depender = depender;
			this.parentMustMatch = parentMustMatch;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof SchemaDependency) {
				SchemaDependency that = (SchemaDependency) obj;
				return getDepender() == null ? that.getDepender() == null :
						getDepender().equals(that.getDepender()) &&
					getParentMustMatch() == null ? that.getParentMustMatch() == null :
						getParentMustMatch().equals(that.getParentMustMatch());
			} else {
				return false;
			}
		}
		
		/**
		 * {@link ObjectSchema.SchemaDependency#depender}
		 * @return the depender
		 */
		public String getDepender() {
			return depender;
		}
		
		/**
		 * {@link ObjectSchema.SchemaDependency#parentMustMatch}
		 * @return the parentMustMatch
		 */
		public JsonSchema getParentMustMatch() {
			return parentMustMatch;
		}
	}

	/**
	 * Simple Dependency If the dependency value is a string, then the instance
	 * object MUST have a property with the same name as the dependency value.
	 * If the dependency value is an array of strings, then the instance object
	 * MUST have a property with the same name as each string in the dependency
	 * value's array.
	 */
	public static class SimpleDependency extends Dependency {

		@JsonProperty(required = true)
		private String depender;

		@JsonProperty(required = true)
		private String dependsOn;

		public SimpleDependency(String depender, String dependsOn) {
			this.depender = depender;
			this.dependsOn = dependsOn;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof SchemaDependency) {
				SimpleDependency that = (SimpleDependency) obj;
				return getDepender() == null ? that.getDepender() == null :
						getDepender().equals(that.getDepender()) &&
					getDependsOn() == null ? that.getDependsOn() == null :
						getDependsOn().equals(that.getDependsOn());
			} else {
				return false;
			}
		}
		
		/**
		 * {@link ObjectSchema.SimpleDependency#depender}
		 * @return the depender
		 */
		public String getDepender() {
			return depender;
		}
		
		/**
		 * {@link ObjectSchema.SimpleDependency#dependsOn}
		 * @return the dependsOn
		 */
		public String getDependsOn() {
			return dependsOn;
		}
	}

}