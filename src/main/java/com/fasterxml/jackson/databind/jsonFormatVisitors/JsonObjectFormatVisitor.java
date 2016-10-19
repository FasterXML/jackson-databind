package com.fasterxml.jackson.databind.jsonFormatVisitors;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Visitor called when properties of a type that maps to JSON Object
 * are being visited: this usually means POJOs, but sometimes other
 * types use it too (like {@link java.util.EnumMap}).
 */
public interface JsonObjectFormatVisitor extends JsonFormatVisitorWithSerializerProvider
{
    /**
     * Callback method called when a POJO property is being traversed.
     */
    public void property(BeanProperty writer) throws JsonMappingException;

    /**
     * Callback method called when a non-POJO property (typically something
     * like an Enum entry of {@link java.util.EnumMap} type) is being
     * traversed. With POJOs, {@link #property(BeanProperty)} is called instead.
     */
    public void property(String name, JsonFormatVisitable handler, JavaType propertyTypeHint) throws JsonMappingException;

    public void optionalProperty(BeanProperty writer) throws JsonMappingException;
    public void optionalProperty(String name, JsonFormatVisitable handler,
            JavaType propertyTypeHint)
        throws JsonMappingException;

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
        public void property(BeanProperty prop) throws JsonMappingException { }

        @Override
        public void property(String name, JsonFormatVisitable handler,
                JavaType propertyTypeHint) throws JsonMappingException { }

        @Override
        public void optionalProperty(BeanProperty prop) throws JsonMappingException { }

        @Override
        public void optionalProperty(String name, JsonFormatVisitable handler,
                JavaType propertyTypeHint) throws JsonMappingException { }
    }
}
