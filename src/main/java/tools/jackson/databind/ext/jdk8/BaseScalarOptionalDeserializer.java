package tools.jackson.databind.ext.jdk8;

import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdScalarDeserializer;

public abstract class BaseScalarOptionalDeserializer<T>
    extends StdScalarDeserializer<T>
{
    protected final T _empty;

    protected BaseScalarOptionalDeserializer(Class<T> cls, T empty) {
        super(cls);
        _empty = empty;
    }

    @Override
    public T getNullValue(DeserializationContext ctxt) {
        return _empty;
    }
}
