package com.fasterxml.jackson.databind.jsonschema.types;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;


public abstract class ContainerTypeSchema extends SimpleTypeSchema {
	/**
	 * This provides an enumeration of all possible values that are valid
	   for the instance property.  This MUST be an array, and each item in
	   the array represents a possible value for the instance value.  If
	   this attribute is defined, the instance value MUST be one of the
	   values in the array in order for the schema to be valid.  Comparison
	   of enum values uses the same algorithm as defined in "uniqueItems"
	   (Section 5.15).
	 */
	@JsonProperty(required = true)
	private Set<JsonNode> enums;
	
	@Override
	public boolean isContainerTypeSchema() { return true; }
	
	@Override
	public ContainerTypeSchema asContainerSchema() { return this; }
}