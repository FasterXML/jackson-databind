package com.fasterxml.jackson.databind.jsonschema.types;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * value must be null;
 */
public class NullSchema extends SimpleTypeSchema {
	
	@JsonProperty(required = true)
	private final SchemaType type = SchemaType.NULL;
	
	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.Schema#getType()
	 */
	@Override
	public SchemaType getType() {
		return type;
	}
	
	@Override
	public boolean isNullSchema() { return true; }
	
	@Override
	public NullSchema asNullSchema() { return this; }
}