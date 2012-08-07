package com.fasterxml.jackson.databind.jsonschema.factories;

import com.fasterxml.jackson.databind.jsonschema.types.IntegerSchema;
import com.fasterxml.jackson.databind.jsonschema.types.ValueTypeSchema;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonIntegerFormatVisitor;

public class IntegerSchemaFactory extends ValueTypeSchemaFactory implements
		JsonIntegerFormatVisitor, SchemaFactoryDelegate {

	protected IntegerSchema integerSchema;
	
	public IntegerSchemaFactory(SchemaFactory parent) {
		super(parent);
		integerSchema = new IntegerSchema();
	}

	public ValueTypeSchema getValueSchema() {
		return integerSchema;
	}

}
