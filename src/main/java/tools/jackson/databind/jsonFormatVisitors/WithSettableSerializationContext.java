package tools.jackson.databind.jsonFormatVisitors;

import tools.jackson.databind.SerializationContext;

/**
 * In Jackson 2.x was named {@code JsonFormatVisitorWithSerializationContext}.
 */
public interface WithSettableSerializationContext
    extends WithGettableSerializationContext
{
    @Override
    public SerializationContext getContext();

    public void setContext(SerializationContext ctxt);
}
