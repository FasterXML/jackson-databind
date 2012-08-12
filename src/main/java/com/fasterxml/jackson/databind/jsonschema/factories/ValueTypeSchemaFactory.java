package com.fasterxml.jackson.databind.jsonschema.factories;

import java.util.Set;

import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonValueFormatVisitor;
import com.fasterxml.jackson.databind.jsonschema.types.JsonSchema;
import com.fasterxml.jackson.databind.jsonschema.types.JsonValueFormat;
import com.fasterxml.jackson.databind.jsonschema.types.ValueTypeSchema;

public abstract class ValueTypeSchemaFactory extends SchemaFactory implements
	JsonValueFormatVisitor {

	protected SchemaFactory parent; 
	
	protected ValueTypeSchemaFactory(SchemaFactory parent) {
		this.parent = parent;
		setProvider(parent.getProvider());
	}

	/**
	 * @param provider
	 */
	public ValueTypeSchemaFactory(SerializerProvider provider) {
		parent = null;
		setProvider(provider);
	}

	public JsonSchema getSchema() {
		return getValueSchema();
	}
	protected abstract ValueTypeSchema getValueSchema();
	
	public void format(JsonValueFormat format) {
		getValueSchema().setFormat(format);

	}

	public void enumTypes(Set<String> enums) {
		getValueSchema().setEnums(enums);

	}

}
