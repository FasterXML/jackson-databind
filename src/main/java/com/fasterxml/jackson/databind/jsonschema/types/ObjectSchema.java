package com.fasterxml.jackson.databind.jsonschema.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public class ObjectSchema extends ContainerTypeSchema {

	/**
	 * This attribute defines a schema for all properties that are not
	 * explicitly defined in an object type definition. If specified, the value
	 * MUST be a schema or a boolean. If false is provided, no additional
	 * properties are allowed beyond the properties defined in the schema. The
	 * default value is an empty schema which allows any value for additional
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
	 This attribute is an object that defines the schema for a set of property
	 * names of an object instance. The name of each property of this
	 * attribute's object is a regular expression pattern in the ECMA 262/Perl 5
	 * format, while the value is a schema. If the pattern matches the name of a
	 * property on the instance object, the value of the instance's property
	 * MUST be valid against the pattern name's schema value.
	 */
	@JsonProperty
	private Map<String, Schema> patternProperties;

	/**
	 * This attribute is an object with property definitions that define the
	 * valid values of instance object property values. When the instance value
	 * is an object, the property values of the instance object MUST conform to
	 * the property definitions in this object. In this object, each property
	 * definition's value MUST be a schema, and the property's name MUST be the
	 * name of the instance property that it defines. The instance property
	 * value MUST be valid according to the schema from the property definition.
	 * Properties are considered unordered, the order of the instance properties
	 * MAY be in any order.
	 */
	@JsonProperty
	private Map<String, Schema> properties;

	@JsonProperty(required = true)
	public final SchemaType type = SchemaType.OBJECT;

	// instance initializer block
	{
		dependencies = new ArrayList<Dependency>();
		patternProperties = new HashMap<String, Schema>();
		properties = new HashMap<String, Schema>();
	}

	public boolean addSchemaDependency(String depender, Schema parentMustMatch) {
		return dependencies
				.add(new SchemaDependency(depender, parentMustMatch));
	}

	public boolean addSimpleDependency(String depender, String dependsOn) {
		return dependencies.add(new SimpleDependency(depender, dependsOn));
	}

	@Override
	public ObjectSchema asObjectSchema() {
		return this;
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
	public Map<String, Schema> getPatternProperties() {
		return patternProperties;
	}

	/**
	 * {@link ObjectSchema#properties}
	 * 
	 * @return the properties
	 */
	public Map<String, Schema> getProperties() {
		return properties;
	}

	@Override
	public boolean isObjectSchema() {
		return true;
	}

	public void putOptionalProperty(String name, Schema schema) {
		// just don't put anything in the property list
	}

	public Schema putPatternProperty(String regex, Schema value) {
		return patternProperties.put(regex, value);
	}

	public Schema putProperty(String name, Schema value) {
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
	public void setPatternProperties(Map<String, Schema> patternProperties) {
		this.patternProperties = patternProperties;
	}

	/**
	 * {@link ObjectSchema#properties}
	 * 
	 * @param properties
	 *            the properties to set
	 */
	public void setProperties(Map<String, Schema> properties) {
		this.properties = properties;
	}

	public static abstract class AdditionalProperties {
	}

	public static abstract class Dependency {
	}

	public static class NoAdditionalProperties extends AdditionalProperties {
		public final Boolean schema = false;

		protected NoAdditionalProperties() {
		}

		@JsonValue
		public Boolean value() {
			return schema;
		}

		public static final NoAdditionalProperties instance = new NoAdditionalProperties();
	}

	public static class SchemaAdditionalProperties extends AdditionalProperties {

		@JsonProperty
		private Schema schema;

		public SchemaAdditionalProperties(Schema schema) {
			this.schema = schema;
		}
	}

	/**
	 * Schema Dependency If the dependency value is a schema, then the instance
	 * object MUST be valid against the schema.
	 */
	public static class SchemaDependency extends Dependency {

		@JsonProperty(required = true)
		private String depender;

		@JsonProperty(required = true)
		private Schema parentMustMatch;

		public SchemaDependency(String depender, Schema parentMustMatch) {
			this.depender = depender;
			this.parentMustMatch = parentMustMatch;
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
	}

}