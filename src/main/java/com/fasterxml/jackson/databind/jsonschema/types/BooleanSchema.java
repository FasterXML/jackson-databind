package com.fasterxml.jackson.databind.jsonschema.types;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BooleanSchema extends ValueTypeSchema {
	
	@JsonProperty(required = true)
	public final SchemaType type = SchemaType.BOOLEAN;
	
	@Override
	public boolean isBooleanSchema() { return true; }
	
	@Override
	public BooleanSchema asBooleanSchema() { return this; }
}