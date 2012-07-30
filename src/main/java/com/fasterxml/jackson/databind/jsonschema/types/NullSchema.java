package com.fasterxml.jackson.databind.jsonschema.types;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * value must be null;
 */
public class NullSchema extends SimpleTypeSchema {
	
	@JsonProperty(required = true)
	public final SchemaType type = SchemaType.NULL;
	
	@Override
	public boolean isNullSchema() { return true; }
	
	@Override
	public NullSchema asNullSchema() { return this; }
}