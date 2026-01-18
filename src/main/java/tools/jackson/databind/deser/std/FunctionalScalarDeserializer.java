package tools.jackson.databind.deser.std;

import java.util.function.BiFunction;
import java.util.function.Function;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;

import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

/**
 * A general-purpose deserializer that uses a {@link Function} or {@link BiFunction}
 * to convert JSON scalar values (strings, numbers, booleans) into target type instances.
 * <p>
 * This deserializer is primarily designed for String-based conversions but also
 * supports other JSON scalar types via {@code getValueAsString()} coercion.
 * Non-scalar JSON values (arrays, objects, embedded objects) are rejected.
 * <p>
 * Usage examples:
 * <pre>
 * // Simple case - method reference
 * new FunctionalScalarDeserializer&lt;&gt;(Bar.class, Bar::of)
 *
 * // Full access case
 * new FunctionalScalarDeserializer&lt;&gt;(Bar.class, (p, ctx) -&gt;
 *     Bar.parse(p.getValueAsString(), ctx.getLocale()))
 * </pre>
 *
 * @param <T> Target type to deserialize into
 * @since 3.1
 */
public class FunctionalScalarDeserializer<T> extends StdScalarDeserializer<T> {

    protected final BiFunction<JsonParser, DeserializationContext, T> _function;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public FunctionalScalarDeserializer(Class<T> type,
                                        BiFunction<JsonParser, DeserializationContext, T> function) {
        super(type);
        _function = function;
    }

    public FunctionalScalarDeserializer(JavaType type,
                                        BiFunction<JsonParser, DeserializationContext, T> function) {
        super(type);
        _function = function;
    }

    public FunctionalScalarDeserializer(Class<T> type, Function<String, T> function) {
        super(type);
        _function = (p, ctx) -> function.apply(p.getValueAsString());
    }

    public FunctionalScalarDeserializer(JavaType type, Function<String, T> function) {
        super(type);
        _function = (p, ctx) -> function.apply(p.getValueAsString());
    }

    protected FunctionalScalarDeserializer(FunctionalScalarDeserializer<T> src) {
        super(src);
        _function = src._function;
    }

    @Override
    public LogicalType logicalType() {
        return LogicalType.OtherScalar;
    }

    /*
    /**********************************************************************
    /* Deserializer implementations
    /**********************************************************************
     */

    @SuppressWarnings("unchecked")
    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        String text = p.getValueAsString();

        if (text == null) {
            JsonToken t = p.currentToken();
            if (t == JsonToken.START_OBJECT) {
                text = ctxt.extractScalarFromObject(p, this, _valueClass);
                if (text == null) {
                    return (T) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
                }
            } else {
                // Non-scalar tokens (arrays, embedded objects, etc.) are not supported
                return (T) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
            }
        }

        if (text.isEmpty()) {
            return (T) _deserializeFromEmptyString(ctxt);
        }

        try {
            return _function.apply(p, ctxt);
        } catch (IllegalArgumentException e) {
            String msg = "not a valid textual representation";
            String m2 = e.getMessage();
            if (m2 != null) {
                msg = msg + ", problem: " + m2;
            }
            throw ctxt.weirdStringException(text, _valueClass, msg)
                    .withCause(e);
        }
    }

    /**
     * Handle empty String input according to {@link CoercionAction} configuration.
     */
    protected Object _deserializeFromEmptyString(DeserializationContext ctxt)
            throws JacksonException
    {
        CoercionAction act = ctxt.findCoercionAction(logicalType(), _valueClass,
                CoercionInputShape.EmptyString);

        if (act == CoercionAction.Fail) {
            ctxt.reportInputMismatch(this,
                    "Cannot coerce empty String (\"\") to %s (but could if enabling coercion using `CoercionConfig`)",
                    _coercedTypeDesc());
        }
        if (act == CoercionAction.AsNull) {
            return getNullValue(ctxt);
        }
        if (act == CoercionAction.AsEmpty) {
            return getEmptyValue(ctxt);
        }
        // TryConvert: delegate to overridable method for subclass customization
        return _deserializeFromEmptyStringDefault(ctxt);
    }

    /**
     * Handle empty String when {@link CoercionAction#TryConvert} is configured.
     * Subclasses may override to provide custom behavior.
     * Default implementation returns null.
     */
    protected Object _deserializeFromEmptyStringDefault(DeserializationContext ctxt)
            throws JacksonException {
        return getNullValue(ctxt);
    }
}
