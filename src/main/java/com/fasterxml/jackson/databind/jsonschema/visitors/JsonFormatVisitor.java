package com.fasterxml.jackson.databind.jsonschema.visitors;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;

public interface JsonFormatVisitor {

	public JsonObjectFormatVisitor objectFormat(JavaType convertedType);
	public JsonArrayFormatVisitor arrayFormat(JavaType convertedType);
	public JsonStringFormatVisitor stringFormat(JavaType convertedType);
	public JsonNumberFormatVisitor numberFormat(JavaType convertedType);
	public JsonIntegerFormatVisitor integerFormat(JavaType convertedType);
	public JsonBooleanFormatVisitor booleanFormat(JavaType convertedType);
	public JsonNullFormatVisitor nullFormat(JavaType convertedType);
	public JsonAnyFormatVisitor anyFormat(JavaType convertedType);
	
	public SerializerProvider getProvider();
	public abstract void setProvider(SerializerProvider provider);

}
