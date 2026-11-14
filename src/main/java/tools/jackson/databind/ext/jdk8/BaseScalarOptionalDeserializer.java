package tools.jackson.databind.ext.jdk8;

import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdScalarDeserializer;
import tools.jackson.databind.util.AccessPattern;

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

    /**
     * Empty {@code OptionalInt}/{@code OptionalLong}/{@code OptionalDouble} are
     * non-null constants, so {@link AccessPattern#CONSTANT} (not {@code ALWAYS_NULL};
     * see [databind#6437]).
     *<p>
     * NOTE: sub-classes that override {@link #getNullValue} must also override
     * this method if the value is not constant: see
     * {@link StdScalarDeserializer#getNullAccessPattern()}.
     */
    @Override
    public AccessPattern getNullAccessPattern() {
        return AccessPattern.CONSTANT;
    }
}
