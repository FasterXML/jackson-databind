package com.fasterxml.jackson.databind.jsonFormatVisitors;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;

public interface JsonArrayFormatVisitor extends JsonFormatVisitorWithSerializerProvider
{
    /**
     * Visit method called for structured types, as well as possibly
     * for leaf types (especially if handled by custom serializers).
     *
     * @param handler Serializer used, to allow for further callbacks
     * @param elementType Type of elements in JSON array value
     */
    void itemsFormat(JsonFormatVisitable handler, JavaType elementType)
        throws JsonMappingException;

    /**
     * Visit method that is called if the content type is a simple
     * scalar type like {@link JsonFormatTypes#STRING} (but not
     * for structured types like {@link JsonFormatTypes#OBJECT} since
     * they would be missing type information).
     */
    void itemsFormat(JsonFormatTypes format)
        throws JsonMappingException;

    /**
     * Default "empty" implementation, useful as the base to start on;
     * especially as it is guaranteed to implement all the method
     * of the interface, even if new methods are getting added.
     */
    public static class Base implements JsonArrayFormatVisitor {
        protected SerializerProvider _provider;

        public Base() { }
        public Base(SerializerProvider p) { _provider = p; }

        @Override
        public SerializerProvider getProvider() { return _provider; }

        @Override
        public void setProvider(SerializerProvider p) { _provider = p; }

        @Override
        public void itemsFormat(JsonFormatVisitable handler, JavaType elementType)
            throws JsonMappingException { }

        @Override
        public void itemsFormat(JsonFormatTypes format)
            throws JsonMappingException { }
    }

}

