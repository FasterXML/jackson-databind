package tools.jackson.databind.jsonFormatVisitors;

import tools.jackson.databind.SerializationContext;

/**
 * @since 3.1
 */
public interface WithGettableSerializationContext {
    public SerializationContext getContext();
}
