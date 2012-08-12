package com.fasterxml.jackson.databind.jsonschema.types;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * This class represents a {@link JsonSchema} as a null type
 * @author jphelan
 */
public class NullSchema extends SimpleTypeSchema {
	
	@JsonIgnore
	private final SchemaType type = SchemaType.NULL;
	
	@Override
	public NullSchema asNullSchema() { return this; }
	
	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.JsonSchema#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return (obj instanceof NullSchema && super.equals(obj));
	}
	
	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.JsonSchema#getType()
	 */
	@Override
	public SchemaType getType() {
		return type;
	}
	
	@Override
	public boolean isNullSchema() { return true; }
	
}