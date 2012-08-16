package com.fasterxml.jackson.databind.jsonFormatVisitors;

import com.fasterxml.jackson.databind.JavaType;

public interface JsonArrayFormatVisitor extends JsonFormatVisitorWithSerializerProvider {

	void itemsFormat(JavaType contentType);

	void itemsFormat(JsonFormatTypes format);

}
