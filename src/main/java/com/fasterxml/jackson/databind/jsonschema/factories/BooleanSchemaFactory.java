package com.fasterxml.jackson.databind.jsonschema.factories;

import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonBooleanFormatVisitor;
import com.fasterxml.jackson.databind.jsonschema.types.BooleanSchema;
import com.fasterxml.jackson.databind.jsonschema.types.ValueTypeSchema;

public class BooleanSchemaFactory extends ValueTypeSchemaFactory implements
	JsonBooleanFormatVisitor {

	protected BooleanSchema booleanSchema;
	
	public BooleanSchemaFactory(SchemaFactory parent) {
		super(parent);
		booleanSchema = new BooleanSchema();
	}

	/**
	 * @param provider
	 */
	public BooleanSchemaFactory(SerializerProvider provider) {
		super(provider);
		booleanSchema = new BooleanSchema();
	}

	public ValueTypeSchema getValueSchema() {
		return booleanSchema;
	}

}
