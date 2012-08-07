package com.fasterxml.jackson.databind.jsonschema.factories;

import java.util.Set;

import com.fasterxml.jackson.databind.jsonschema.types.JsonSchema;
import com.fasterxml.jackson.databind.jsonschema.types.JsonValueFormat;
import com.fasterxml.jackson.databind.jsonschema.types.ValueTypeSchema;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonValueFormatVisitor;

public abstract class ValueTypeSchemaFactory extends SchemaFactory implements
		SchemaFactoryDelegate, JsonValueFormatVisitor {

	protected SchemaFactory parent; 
	
	protected ValueTypeSchemaFactory(SchemaFactory parent) {
		this.parent = parent;
		setProvider(parent.getProvider());
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
