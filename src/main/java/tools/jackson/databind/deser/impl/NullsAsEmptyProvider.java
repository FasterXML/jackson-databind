package tools.jackson.databind.deser.impl;

import tools.jackson.databind.*;
import tools.jackson.databind.deser.NullValueProvider;
import tools.jackson.databind.exc.InvalidNullException;
import tools.jackson.databind.util.AccessPattern;

/**
 * Simple {@link NullValueProvider} that will always throw a
 * {@link InvalidNullException} when a null is encountered.
 */
public class NullsAsEmptyProvider
    implements NullValueProvider, java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    protected final ValueDeserializer<?> _deserializer;

    public NullsAsEmptyProvider(ValueDeserializer<?> deser) {
        _deserializer = deser;
    }

    @Override
    public AccessPattern getNullAccessPattern() {
        return AccessPattern.DYNAMIC;
    }

    @Override
    public Object getNullValue(DeserializationContext ctxt) {
        return _deserializer.getEmptyValue(ctxt);
    }
}
