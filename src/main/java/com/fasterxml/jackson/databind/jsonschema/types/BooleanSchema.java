package com.fasterxml.jackson.databind.jsonschema.types;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * This class represents a {@link JsonSchema} of type boolean
 * @author jphelan
 *
 */
public class BooleanSchema extends ValueTypeSchema {
	
	@JsonIgnore
	private final SchemaType type = SchemaType.BOOLEAN;
	
	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.JsonSchema#isBooleanSchema()
	 */
	@Override
	public boolean isBooleanSchema() { return true; }
	
	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.JsonSchema#getType()
	 */
	@Override
	public SchemaType getType() {
		return type;
	}
	
	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.JsonSchema#asBooleanSchema()
	 */
	@Override
	public BooleanSchema asBooleanSchema() { return this; }
}