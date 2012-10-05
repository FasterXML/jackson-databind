package com.fasterxml.jackson.databind.jsonFormatVisitors;

import com.fasterxml.jackson.databind.JavaType;

public interface JsonFormatVisitorWrapper extends JsonFormatVisitorWithSerializerProvider
{
    public JsonObjectFormatVisitor expectObjectFormat(JavaType convertedType);
    public JsonArrayFormatVisitor expectArrayFormat(JavaType convertedType);
    public JsonStringFormatVisitor expectStringFormat(JavaType convertedType);
    public JsonNumberFormatVisitor expectNumberFormat(JavaType convertedType);
    public JsonIntegerFormatVisitor expectIntegerFormat(JavaType convertedType);
    public JsonBooleanFormatVisitor expectBooleanFormat(JavaType convertedType);
    public JsonNullFormatVisitor expectNullFormat(JavaType convertedType);
    public JsonAnyFormatVisitor expectAnyFormat(JavaType convertedType);
}
