package com.fasterxml.jackson.databind.jsonFormatVisitors;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Interface for visitor callbacks, when type in question can be any of
 * legal JSON types.
 *<p>
 * In most cases it will make more sense to extend {@link JsonFormatVisitorWrapper.Base}
 * instead of directly implementing this interface.
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

    /**
     * Empty "no-op" implementation of {@link JsonFormatVisitorWrapper}, suitable for
     * sub-classing. Does implement {@link #setProvider(SerializerProvider)} and
     * {@link #getProvider()} as expected; other methods simply return null
     * and do nothing.
     *
     * @since 2.5
     */
    public static class Base implements JsonFormatVisitorWrapper {
        protected SerializerProvider _provider;

        public Base() { }

        public Base(SerializerProvider p) {
            _provider = p;
        }

        @Override
        public SerializerProvider getProvider() {
            return _provider;
        }

        @Override
        public void setProvider(SerializerProvider p) {
            _provider = p;
        }

        @Override
        public JsonObjectFormatVisitor expectObjectFormat(JavaType type)
                throws JsonMappingException {
            return null;
        }

        @Override
        public JsonArrayFormatVisitor expectArrayFormat(JavaType type)
                  throws JsonMappingException {
            return null;
        }

        @Override
        public JsonStringFormatVisitor expectStringFormat(JavaType type)
                throws JsonMappingException {
            return null;
        }

        @Override
        public JsonNumberFormatVisitor expectNumberFormat(JavaType type)
                throws JsonMappingException {
            return null;
        }

        @Override
        public JsonIntegerFormatVisitor expectIntegerFormat(JavaType type)
                throws JsonMappingException {
            return null;
        }

        @Override
        public JsonBooleanFormatVisitor expectBooleanFormat(JavaType type)
                throws JsonMappingException {
            return null;
        }

        @Override
        public JsonNullFormatVisitor expectNullFormat(JavaType type)
                throws JsonMappingException {
            return null;
        }

        @Override
        public JsonAnyFormatVisitor expectAnyFormat(JavaType type)
                throws JsonMappingException {
            return null;
        }

        @Override
        public JsonMapFormatVisitor expectMapFormat(JavaType type)
                throws JsonMappingException {
            return null;
        }
   }
}
