package tools.jackson.databind.jsonFormatVisitors;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationContext;

public interface JsonMapFormatVisitor extends JsonFormatVisitorWithSerializationContext
{
    /**
     * Visit method called to indicate type of keys of the Map type
     * being visited
     */
    public void keyFormat(JsonFormatVisitable handler, JavaType keyType);

    /**
     * Visit method called after {@link #keyFormat} to allow visiting of
     * the value type
     */
    public void valueFormat(JsonFormatVisitable handler, JavaType valueType);

    /**
     * Default "empty" implementation, useful as the base to start on;
     * especially as it is guaranteed to implement all the method
     * of the interface, even if new methods are getting added.
     */
    public static class Base
        implements JsonMapFormatVisitor
    {
        protected SerializationContext _provider;

        public Base() { }
        public Base(SerializationContext p) { _provider = p; }

        @Override
        public SerializationContext getContext() { return _provider; }

        @Override
        public void setContext(SerializationContext p) { _provider = p; }

        @Override
        public void keyFormat(JsonFormatVisitable handler, JavaType keyType) { }
        @Override
        public void valueFormat(JsonFormatVisitable handler, JavaType valueType) { }
    }
}
