package com.fasterxml.jackson.databind.jsonFormatVisitors;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;

public interface JsonObjectFormatVisitor extends JsonFormatVisitorWithSerializerProvider
{
    public void property(BeanProperty writer) throws JsonMappingException;
    public void property(String name, JsonFormatVisitable handler, JavaType propertyTypeHint) throws JsonMappingException;

    @Deprecated
    public void property(String name) throws JsonMappingException;

    public void optionalProperty(BeanProperty writer) throws JsonMappingException;
    public void optionalProperty(String name, JsonFormatVisitable handler,
            JavaType propertyTypeHint)
        throws JsonMappingException;

    @Deprecated
    public void optionalProperty(String name) throws JsonMappingException;

    /**
     * Default "empty" implementation, useful as the base to start on;
     * especially as it is guaranteed to implement all the method
     * of the interface, even if new methods are getting added.
     */
    public static class Base
        implements JsonObjectFormatVisitor
    {
        protected SerializerProvider _provider;

        public Base() { }
        public Base(SerializerProvider p) { _provider = p; }

        @Override
        public SerializerProvider getProvider() { return _provider; }

        @Override
        public void setProvider(SerializerProvider p) { _provider = p; }

        @Override
        public void property(BeanProperty writer) throws JsonMappingException { }

        @Override
        public void property(String name, JsonFormatVisitable handler,
                JavaType propertyTypeHint) throws JsonMappingException { }

        @Deprecated
        @Override
        public void property(String name) throws JsonMappingException { }

        @Override
        public void optionalProperty(BeanProperty writer)
                throws JsonMappingException { }

        @Override
        public void optionalProperty(String name, JsonFormatVisitable handler,
                JavaType propertyTypeHint) throws JsonMappingException { }

        @Deprecated
        @Override
        public void optionalProperty(String name) throws JsonMappingException { }
    }
}
