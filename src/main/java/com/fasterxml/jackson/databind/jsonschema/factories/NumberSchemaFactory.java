package com.fasterxml.jackson.databind.jsonschema.factories;

import com.fasterxml.jackson.databind.jsonschema.types.NumberSchema;
import com.fasterxml.jackson.databind.jsonschema.types.ValueTypeSchema;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonNumberFormatVisitor;

public class NumberSchemaFactory extends ValueTypeSchemaFactory implements
		JsonNumberFormatVisitor, SchemaFactoryDelegate {

	protected SchemaFactory parent;
	protected NumberSchema numberSchema;
	
	public NumberSchemaFactory(SchemaFactory parent) {
		super(parent.provider);
		this.parent = parent;
		numberSchema = new NumberSchema();
	}

	@Override
	protected ValueTypeSchema getValueSchema() {
		return numberSchema;
	}
	

}
