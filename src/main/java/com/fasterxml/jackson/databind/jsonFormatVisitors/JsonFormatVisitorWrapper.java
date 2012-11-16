package com.fasterxml.jackson.databind.jsonFormatVisitors;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;

public interface JsonFormatVisitorWrapper extends JsonFormatVisitorWithSerializerProvider
{
    public JsonObjectFormatVisitor expectObjectFormat(JavaType convertedType) throws JsonMappingException;
    public JsonArrayFormatVisitor expectArrayFormat(JavaType convertedType) throws JsonMappingException;
    public JsonStringFormatVisitor expectStringFormat(JavaType convertedType) throws JsonMappingException;
    public JsonNumberFormatVisitor expectNumberFormat(JavaType convertedType) throws JsonMappingException;
    public JsonIntegerFormatVisitor expectIntegerFormat(JavaType convertedType) throws JsonMappingException;
    public JsonBooleanFormatVisitor expectBooleanFormat(JavaType convertedType) throws JsonMappingException;
    public JsonNullFormatVisitor expectNullFormat(JavaType convertedType) throws JsonMappingException;
    public JsonAnyFormatVisitor expectAnyFormat(JavaType convertedType) throws JsonMappingException;
}
