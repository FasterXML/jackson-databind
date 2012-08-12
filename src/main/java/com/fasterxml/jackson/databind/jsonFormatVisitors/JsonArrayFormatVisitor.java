package com.fasterxml.jackson.databind.jsonFormatVisitors;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsonschema.types.SchemaType;

public interface JsonArrayFormatVisitor extends JsonFormatVisitorWithSerializerProvider {

	void itemsFormat(JavaType contentType);

	void itemsFormat(SchemaType format);

}
