package com.fasterxml.jackson.databind.jsonFormatVisitors;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Interface for visitor callbacks, when type in question can be any of
 * legal JSON types.
 */
public interface JsonFormatVisitorWrapper extends JsonFormatVisitorWithSerializerProvider
{
    /**
     * @param type Declared type of visited property (or List element) in Java
     */
    public JsonObjectFormatVisitor expectObjectFormat(JavaType type) throws JsonMappingException;

    /**
     * @param type Declared type of visited property (or List element) in Java
     */
    public JsonArrayFormatVisitor expectArrayFormat(JavaType type) throws JsonMappingException;

    /**
     * @param type Declared type of visited property (or List element) in Java
     */
    public JsonStringFormatVisitor expectStringFormat(JavaType type) throws JsonMappingException;

    /**
     * @param type Declared type of visited property (or List element) in Java
     */
    public JsonNumberFormatVisitor expectNumberFormat(JavaType type) throws JsonMappingException;

    /**
     * @param type Declared type of visited property (or List element) in Java
     */
    public JsonIntegerFormatVisitor expectIntegerFormat(JavaType type) throws JsonMappingException;

    /**
     * @param type Declared type of visited property (or List element) in Java
     */
    public JsonBooleanFormatVisitor expectBooleanFormat(JavaType type) throws JsonMappingException;

    /**
     * @param type Declared type of visited property (or List element) in Java
     */
    public JsonNullFormatVisitor expectNullFormat(JavaType type) throws JsonMappingException;

    /**
     * @param type Declared type of visited property (or List element) in Java
     */
    public JsonAnyFormatVisitor expectAnyFormat(JavaType type) throws JsonMappingException;

    /**
     * Method called when type is of Java {@link java.util.Map} type, and will
     * be serialized as a JSON Object.
     * 
     * @since 2.2
     */
    public JsonMapFormatVisitor expectMapFormat(JavaType type) throws JsonMappingException;
}
