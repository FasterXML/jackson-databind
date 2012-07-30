package com.fasterxml.jackson.databind.jsonschema.factories;

import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsonschema.types.JsonValueFormat;
import com.fasterxml.jackson.databind.jsonschema.types.Schema;
import com.fasterxml.jackson.databind.jsonschema.types.ValueTypeSchema;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonValueFormatVisitor;

public abstract class ValueTypeSchemaFactory extends SchemaFactory implements
		SchemaFactoryDelegate, JsonValueFormatVisitor {

	protected ValueTypeSchemaFactory(ObjectMapper mapper) {
		super(mapper);
	}

	public Schema getSchema() {
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
