package com.fasterxml.jackson.databind.jsonschema.factories;

import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonschema.types.JsonSchema;
import com.fasterxml.jackson.databind.jsonschema.types.NullSchema;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonNullFormatVisitor;

public class NullSchemaFactory extends SchemaFactory implements
		JsonNullFormatVisitor {

	protected SchemaFactory parent;
	protected NullSchema nullSchema;
	
	public NullSchemaFactory(SchemaFactory parent) {
		this.parent = parent;
		setProvider(parent.getProvider());
		nullSchema = new NullSchema();
	}

	/**
	 * @param provider
	 */
	public NullSchemaFactory(SerializerProvider provider) {
		parent = null;
		setProvider(provider);
		nullSchema = new NullSchema();
	}

	public JsonSchema getSchema() {
		return nullSchema;
	}

}
