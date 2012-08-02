package com.fasterxml.jackson.databind.jsonschema.visitors;


import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;

public interface JsonFormatVisitor {

	public JsonObjectFormatVisitor objectFormat(JavaType type);
	public JsonArrayFormatVisitor arrayFormat(JavaType elementType);
	public JsonStringFormatVisitor stringFormat();
	public JsonNumberFormatVisitor numberFormat();
	public JsonIntegerFormatVisitor integerFormat();
	public JsonBooleanFormatVisitor booleanFormat();
	public JsonNullFormatVisitor nullFormat();
	public JsonAnyFormatVisitor anyFormat();
	
	public SerializerProvider getProvider();

}
