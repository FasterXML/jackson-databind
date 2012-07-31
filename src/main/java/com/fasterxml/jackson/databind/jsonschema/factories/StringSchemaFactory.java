package com.fasterxml.jackson.databind.jsonschema.factories;

import com.fasterxml.jackson.databind.jsonschema.types.StringSchema;
import com.fasterxml.jackson.databind.jsonschema.types.ValueTypeSchema;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonStringFormatVisitor;

public class StringSchemaFactory extends ValueTypeSchemaFactory implements JsonStringFormatVisitor,
		SchemaFactoryDelegate {

	protected StringSchema stringSchema;
	protected SchemaFactory parent;
	
	public StringSchemaFactory(SchemaFactory parent) {
		super(parent.provider);
		this.parent = parent;
		stringSchema = new StringSchema();
	}

	public ValueTypeSchema getValueSchema() {
		return stringSchema;
	}

}
