package com.fasterxml.jackson.databind.jsonschema.types;

import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

public abstract class SimpleTypeSchema extends Schema {
	

	/*
	 * This provides an enumeration of all possible values that are valid
	   for the instance property.  This MUST be an array, and each item in
	   the array represents a possible value for the instance value.  If
	   this attribute is defined, the instance value MUST be one of the
	   values in the array in order for the schema to be valid.  Comparison
	   of enum values uses the same algorithm as defined in "uniqueItems"
	   (Section 5.15).
	 */
	private Set<JsonNode> enumenum;
	
	/*
	 * This attribute defines the default value of the instance when the
		instance is undefined.
	 */
	private JsonNode defaultdefault;
	
	/*
	 * This attribute is a string that provides a short description of the
		instance property.
	 */
	private TextNode title;
	
	/*
	 * This attribute is a string that provides a full description of the of
		purpose the instance property.
	 */
	private TextNode description;
}