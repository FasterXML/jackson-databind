package com.fasterxml.jackson.databind.jsonschema.types;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BooleanSchema extends ValueTypeSchema {
	
	@JsonProperty(required = true)
	private final SchemaType type = SchemaType.BOOLEAN;
	
	@Override
	public boolean isBooleanSchema() { return true; }
	
	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.Schema#getType()
	 */
	@Override
	public SchemaType getType() {
		return type;
	}
	
	@Override
	public BooleanSchema asBooleanSchema() { return this; }
}