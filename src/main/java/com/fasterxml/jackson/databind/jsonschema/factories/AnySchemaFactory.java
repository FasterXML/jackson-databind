package com.fasterxml.jackson.databind.jsonschema.factories;

import com.fasterxml.jackson.databind.jsonschema.types.AnySchema;
import com.fasterxml.jackson.databind.jsonschema.types.Schema;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonAnyFormatVisitor;

public class AnySchemaFactory extends SchemaFactory implements
		JsonAnyFormatVisitor, SchemaFactoryDelegate {

	protected SchemaFactory parent;
	protected AnySchema anySchema;
	
	public AnySchemaFactory(SchemaFactory parent) {
		super(parent.provider);
		this.parent = parent;
		anySchema = new AnySchema();
	}

	public Schema getSchema() {
		return anySchema;
	}
	

}
