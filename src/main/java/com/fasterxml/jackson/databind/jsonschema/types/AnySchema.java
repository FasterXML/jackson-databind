package com.fasterxml.jackson.databind.jsonschema.types;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;


public class AnySchema extends SimpleTypeSchema {
	
	/**
	 * This provides an enumeration of all possible values that are valid
	   for the instance property.  This MUST be an array, and each item in
	   the array represents a possible value for the instance value.  If
	   this attribute is defined, the instance value MUST be one of the
	   values in the array in order for the schema to be valid.  Comparison
	   of enum values uses the same algorithm as defined in "uniqueItems"
	   (Section 5.15).
	 */
	@JsonProperty
	private Set<String> enums;
	
	@JsonProperty(required = true)
	public final SchemaType type = SchemaType.ANY;
	
	//instance initializer block
	{
		enums = new HashSet<String>();
	}
	
	@Override
	public AnySchema asAnySchema() { return this; }
	
	@Override
	public boolean isAnySchema() { return true; }
	
	public void setEnums(Set<String> enums) {
		this.enums = enums;
	}
}