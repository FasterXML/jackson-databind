package com.fasterxml.jackson.databind.jsonschema.factories;

import com.fasterxml.jackson.databind.jsonschema.types.Schema;

public interface SchemaFactoryDelegate {

	public Schema getSchema();
}
