package com.fasterxml.jackson.databind.jsonschema.factories;

import com.fasterxml.jackson.databind.jsonschema.types.NullSchema;
import com.fasterxml.jackson.databind.jsonschema.types.JsonSchema;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonNullFormatVisitor;

public class NullSchemaFactory extends SchemaFactory implements
		JsonNullFormatVisitor, SchemaFactoryDelegate {

	protected SchemaFactory parent;
	protected NullSchema nullSchema;
	
	public NullSchemaFactory(SchemaFactory parent) {
		super(parent.provider);
		this.parent = parent;
		nullSchema = new NullSchema();
	}

	public JsonSchema getSchema() {
		return nullSchema;
	}

}
