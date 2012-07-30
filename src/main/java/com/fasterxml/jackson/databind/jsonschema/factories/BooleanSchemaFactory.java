package com.fasterxml.jackson.databind.jsonschema.factories;

import com.fasterxml.jackson.databind.jsonschema.types.BooleanSchema;
import com.fasterxml.jackson.databind.jsonschema.types.ValueTypeSchema;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonBooleanFormatVisitor;

public class BooleanSchemaFactory extends ValueTypeSchemaFactory implements
		SchemaFactoryDelegate, JsonBooleanFormatVisitor {

	protected SchemaFactory parent;
	protected BooleanSchema booleanSchema;
	
	public BooleanSchemaFactory(SchemaFactory parent) {
		super(parent.mapper);
		this.parent = parent;
		booleanSchema = new BooleanSchema();
	}

	public ValueTypeSchema getValueSchema() {
		return booleanSchema;
	}

}
