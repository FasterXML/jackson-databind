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

    // [databind#6437]: empty OptionalInt/Long/Double are non-null constants.
    // ALWAYS_NULL would claim this provider only ever returns Java null.
    // NOTE: sub-classes that override `getNullValue()` to return a value that
    // is not constant (varies per call or depends on context) MUST also
    // override this method to return `AccessPattern.DYNAMIC`: callers may
    // cache the value of a CONSTANT provider, or call it with `null` context.
    @Override
    public AccessPattern getNullAccessPattern() {
        return AccessPattern.CONSTANT;
    }
}
