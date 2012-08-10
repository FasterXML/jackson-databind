package com.fasterxml.jackson.databind.jsonschema.factories;

import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonAnyFormatVisitor;
import com.fasterxml.jackson.databind.jsonschema.types.AnySchema;
import com.fasterxml.jackson.databind.jsonschema.types.JsonSchema;

public class AnySchemaFactory extends SchemaFactory implements
		JsonAnyFormatVisitor {

	protected SchemaFactory parent;
	protected AnySchema anySchema;
	
	public AnySchemaFactory(SchemaFactory parent) {
		this.parent = parent;
		setProvider(parent.getProvider());
		anySchema = new AnySchema();
	}

	/**
	 * @param provider
	 */
	public AnySchemaFactory(SerializerProvider provider) {
		parent = null;
		setProvider(provider);
		anySchema = new AnySchema();
	}

	public JsonSchema getSchema() {
		return anySchema;
	}
	

}
