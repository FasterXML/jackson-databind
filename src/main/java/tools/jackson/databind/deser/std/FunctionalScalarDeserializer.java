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
import tools.jackson.databind.util.ExceptionUtil;

/**
 * A general-purpose deserializer that uses a {@link Function} or {@link BiFunction}
 * to convert JSON scalar values (strings, numbers, booleans) into target type instances.
 * <p>
 * This deserializer is primarily designed for String-based conversions but also
 * supports other JSON scalar types via {@code getValueAsString()} coercion.
 * Non-scalar JSON values (arrays, objects, embedded objects) are rejected.
 * <p>
 * <b>Error handling:</b>
 * <ul>
 *   <li>{@link JacksonException} thrown by user code is propagated as-is.</li>
 *   <li>Other exceptions are wrapped in
 *       {@link tools.jackson.databind.exc.InvalidFormatException}.</li>
 *   <li>If {@link tools.jackson.databind.DeserializationFeature#WRAP_EXCEPTIONS}
 *       is disabled, {@link RuntimeException} is thrown as-is without wrapping.</li>
 * </ul>
 * <p>
 * Usage examples:
 * <pre>
 * // Simple case - method reference
 * new FunctionalScalarDeserializer<>(Bar.class, Bar::of)
 *
 * // Full access case
 * new FunctionalScalarDeserializer<>(Bar.class, (p, ctx) ->
 *     Bar.parse(p.getValueAsString(), ctx.getLocale()))
 * </pre>
 *
 * @param <T> Target type to deserialize into
 *
 * @since 3.1
 */
public class FunctionalScalarDeserializer<T> extends StdScalarDeserializer<T>
{
    protected final BiFunction<JsonParser, DeserializationContext, T> _biFunction;
    protected final Function<String, T> _stringFunction;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public FunctionalScalarDeserializer(Class<T> type,
            BiFunction<JsonParser, DeserializationContext, T> function) {
        super(type);
        _biFunction = function;
        _stringFunction = null;
    }

    public FunctionalScalarDeserializer(JavaType type,
            BiFunction<JsonParser, DeserializationContext, T> function) {
        super(type);
        _biFunction = function;
        _stringFunction = null;
    }

    public FunctionalScalarDeserializer(Class<T> type, Function<String, T> function) {
        super(type);
        _biFunction = null;
        _stringFunction = function;
    }

    public FunctionalScalarDeserializer(JavaType type, Function<String, T> function) {
        super(type);
        _biFunction = null;
        _stringFunction = function;
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
    public T deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        // BiFunction: invoke directly without any pre-processing
        if (_biFunction != null) {
            try {
                return _biFunction.apply(p, ctxt);
            } catch (Exception e) {
                return _handleException(p, ctxt, e);
            }
        }

        // Function<String, T>: extract text and pass to function
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
            return _stringFunction.apply(text);
        } catch (Exception e) {
            return _handleException(text, ctxt, e);
        }
    }

    private T _handleException(JsonParser p, DeserializationContext ctxt,
            Exception e)
        throws JacksonException
    {
        if (e instanceof JacksonException je) {
            throw je;
        }
        return _handleException(p.getValueAsString(), ctxt, e);
    }

    private T _handleException(String text, DeserializationContext ctxt, Exception e)
        throws JacksonException
    {
        e = ExceptionUtil.rethrowIfNoWrap(ctxt, e);

        String msg = "not a valid textual representation";
        String m2 = e.getMessage();
        if (m2 != null) {
            msg = msg + ", problem: " + m2;
        }
        throw ctxt.weirdStringException(text, _valueClass, msg)
                .withCause(e);
    }

    /**
     * Handle empty String input according to {@link CoercionAction} configuration.
     */
    private Object _deserializeFromEmptyString(DeserializationContext ctxt)
        throws JacksonException
    {
        CoercionAction act = ctxt.findCoercionAction(logicalType(), _valueClass,
                CoercionInputShape.EmptyString);

        if (act == CoercionAction.Fail) {
            ctxt.reportInputMismatch(this,
                    "Cannot coerce empty String (\"\") to %s (but could if enabling coercion using `CoercionConfig`)",
                    _coercedTypeDesc());
        }
        if (act == CoercionAction.AsEmpty) {
            return getEmptyValue(ctxt);
        }
        // if (act == CoercionAction.AsNull) etc
        return getNullValue(ctxt);
    }
}
