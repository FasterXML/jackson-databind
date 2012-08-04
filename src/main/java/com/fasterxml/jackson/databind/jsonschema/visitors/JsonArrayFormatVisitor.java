package com.fasterxml.jackson.databind.jsonschema.visitors;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsonschema.JsonFormatVisitorAware;
import com.fasterxml.jackson.databind.jsonschema.types.SchemaType;

public interface JsonArrayFormatVisitor {

	void itemsFormat(JavaType contentType);

	void itemsFormat(SchemaType format);

	void itemsFormat(JsonFormatVisitorAware toVisit);
	
}
