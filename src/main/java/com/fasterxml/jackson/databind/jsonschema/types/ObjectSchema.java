package com.fasterxml.jackson.databind.jsonschema.types;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class ObjectSchema extends ContainerTypeSchema {
	public static final TextNode type = TextNode.valueOf(SchemaType.OBJECT.toString());
	
	/*
	 *  This attribute is an object with property definitions that define the
	   valid values of instance object property values.  When the instance
	   value is an object, the property values of the instance object MUST
	   conform to the property definitions in this object.  In this object,
	   each property definition's value MUST be a schema, and the property's
	   name MUST be the name of the instance property that it defines.  The
	   instance property value MUST be valid according to the schema from
	   the property definition.  Properties are considered unordered, the
	   order of the instance properties MAY be in any order.
	 */
	private Map<String, Schema> properties;
	public Schema putProperty(String name, Schema value) {
		return properties.put(name, value);
	}
	
	/*
	 * 
	   This attribute is an object that defines the schema for a set of
	   property names of an object instance.  The name of each property of
	   this attribute's object is a regular expression pattern in the ECMA
	   262/Perl 5 format, while the value is a schema.  If the pattern
	   matches the name of a property on the instance object, the value of
	   the instance's property MUST be valid against the pattern name's
	   schema value.
	 */
	private Map<String, Schema> patternProperties;
	public Schema putPatternProperty(String regex, Schema value) {
		return patternProperties.put(regex, value);
	}
	
	/*
	 * This attribute defines a schema for all properties that are not
	   explicitly defined in an object type definition.  If specified, the
	   value MUST be a schema or a boolean.  If false is provided, no
	   additional properties are allowed beyond the properties defined in
	   the schema.  The default value is an empty schema which allows any
	   value for additional properties.
	 */
	private AdditionalProperties additionalProperties;
	public void rejectAdditionalProperties() {
		additionalProperties = NoAdditionalProperties.instance;
	}
	
	public static abstract class AdditionalProperties {}
	public static class NoAdditionalProperties extends AdditionalProperties {
		public static final NoAdditionalProperties instance = new NoAdditionalProperties();
		protected NoAdditionalProperties() {}
	}
	public static class SchemaAdditionalProperties extends AdditionalProperties{
		private Schema schema;
		public SchemaAdditionalProperties(Schema schema) {
			this.schema = schema;
		}
	}
	/*
	 * This attribute is an object that defines the requirements of a
	   property on an instance object.  If an object instance has a property
	   with the same name as a property in this attribute's object, then the
	   instance must be valid against the attribute's property value
	 */
	private List<Dependency> dependencies;
	public boolean addSimpleDependency(String depender, String dependsOn) {
		return dependencies.add(new SimpleDependency(depender, dependsOn));
	}
	public boolean addSchemaDependency(String depender, Schema parentMustMatch) {
		return dependencies.add(new SchemaDependency(depender, parentMustMatch));
	}
	
	public static abstract class Dependency {}

	/*
	 * Simple Dependency  If the dependency value is a string, then the
      instance object MUST have a property with the same name as the
      dependency value.  If the dependency value is an array of strings,
      then the instance object MUST have a property with the same name
      as each string in the dependency value's array.
	 */
	public static class SimpleDependency extends Dependency {
		private String depender;
		private String dependsOn;
		
		public SimpleDependency(String depender, String dependsOn) {
			this.depender = depender;
			this.dependsOn = dependsOn;
		}
	}
	
	/*
	 * Schema Dependency  If the dependency value is a schema, then the
  		instance object MUST be valid against the schema.
	 */
	public static class SchemaDependency extends Dependency {
		private String depender;
		private Schema parentMustMatch;
		public SchemaDependency(String depender, Schema parentMustMatch) {
			this.depender = depender;
			this.parentMustMatch = parentMustMatch;
		}
	}
}