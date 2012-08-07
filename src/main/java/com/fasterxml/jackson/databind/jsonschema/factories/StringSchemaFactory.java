package com.fasterxml.jackson.databind.jsonschema.factories;

import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonschema.types.StringSchema;
import com.fasterxml.jackson.databind.jsonschema.types.ValueTypeSchema;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonStringFormatVisitor;

public class StringSchemaFactory extends ValueTypeSchemaFactory implements JsonStringFormatVisitor,
		SchemaFactoryDelegate {

	protected StringSchema stringSchema;
	
	public StringSchemaFactory(SchemaFactory parent) {
		super(parent);
		stringSchema = new StringSchema();
	}

	/**
	 * @param provider
	 */
	public StringSchemaFactory(SerializerProvider provider) {
		super(provider);
		stringSchema = new StringSchema();
	}

	public ValueTypeSchema getValueSchema() {
		return stringSchema;
	}

}
