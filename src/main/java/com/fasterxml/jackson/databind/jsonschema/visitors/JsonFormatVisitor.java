package com.fasterxml.jackson.databind.jsonschema.visitors;

import java.lang.reflect.Type;

import com.fasterxml.jackson.databind.SerializerProvider;

public interface JsonFormatVisitor {

	public JsonObjectFormatVisitor objectFormat(Type type);
	public JsonArrayFormatVisitor arrayFormat(Type elementType);
	public JsonStringFormatVisitor stringFormat();
	public JsonNumberFormatVisitor numberFormat();
	public JsonIntegerFormatVisitor integerFormat();
	public JsonBooleanFormatVisitor booleanFormat();
	public JsonNullFormatVisitor nullFormat();
	public JsonAnyFormatVisitor anyFormat();
	
	public SerializerProvider getProvider();

}
