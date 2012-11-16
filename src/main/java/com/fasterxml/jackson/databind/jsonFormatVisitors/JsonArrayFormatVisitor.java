package com.fasterxml.jackson.databind.jsonFormatVisitors;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;

public interface JsonArrayFormatVisitor extends JsonFormatVisitorWithSerializerProvider
{
	void itemsFormat(JavaType contentType) throws JsonMappingException;
	void itemsFormat(JsonFormatTypes format) throws JsonMappingException;
}

