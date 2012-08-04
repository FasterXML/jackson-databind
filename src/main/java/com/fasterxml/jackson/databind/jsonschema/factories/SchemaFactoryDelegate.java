package com.fasterxml.jackson.databind.jsonschema.factories;

import com.fasterxml.jackson.databind.jsonschema.types.JsonSchema;

public interface SchemaFactoryDelegate {

	public JsonSchema getSchema();
}
