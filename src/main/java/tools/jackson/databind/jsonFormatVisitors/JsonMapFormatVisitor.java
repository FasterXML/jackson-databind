package tools.jackson.databind.jsonFormatVisitors;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationContext;

public interface JsonMapFormatVisitor extends WithGettableSerializationContext
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
        protected final SerializationContext _context;

        public Base(SerializationContext p) { _context = p; }

        @Override
        public SerializationContext getContext() { return _context; }

        @Override
        public void keyFormat(JsonFormatVisitable handler, JavaType keyType) { }
        @Override
        public void valueFormat(JsonFormatVisitable handler, JavaType valueType) { }
    }
}
