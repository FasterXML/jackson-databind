package com.fasterxml.jackson.databind.jsonschema.factories;

import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonschema.types.NumberSchema;
import com.fasterxml.jackson.databind.jsonschema.types.ValueTypeSchema;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonNumberFormatVisitor;

public class NumberSchemaFactory extends ValueTypeSchemaFactory implements
		JsonNumberFormatVisitor {

	protected NumberSchema numberSchema;
	
	public NumberSchemaFactory(SchemaFactory parent) {
		super(parent);
		this.parent = parent;
		numberSchema = new NumberSchema();
	}

	/**
	 * @param provider
	 */
	public NumberSchemaFactory(SerializerProvider provider) {
		super(provider);
		numberSchema = new NumberSchema();
	}

	@Override
	protected ValueTypeSchema getValueSchema() {
		return numberSchema;
	}
	

}
