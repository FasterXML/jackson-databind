package tools.jackson.databind.deser.std;

import java.net.MalformedURLException;
import java.net.UnknownHostException;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.deser.jdk.UUIDDeserializer;
import tools.jackson.databind.type.LogicalType;
import tools.jackson.databind.util.ClassUtil;

/**
 * Base class for building simple scalar value deserializers that accept
 * String values.
 */
public abstract class FromStringDeserializer<T> extends StdScalarDeserializer<T>
{
    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected FromStringDeserializer(Class<?> vc) {
        super(vc);
    }

    protected FromStringDeserializer(JavaType type) {
        super(type);
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
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException
    {
        // Let's get textual value, possibly via coercion from other scalar types
        String text = p.getValueAsString();
        if (text == null) {
            JsonToken t = p.currentToken();
            if (t != JsonToken.START_OBJECT) {
                return (T) _deserializeFromOther(p, ctxt, t);
            }
            // 29-Jun-2020, tatu: New! "Scalar from Object" (mostly for XML)
            text = ctxt.extractScalarFromObject(p, this, _valueClass);
        }
        if (text.isEmpty()) {
            // 09-Jun-2020, tatu: Commonly `null` but may coerce to "empty" as well
            return (T) _deserializeFromEmptyString(ctxt);
        }
        if (_shouldTrim()) {
            final String old = text;
            text = text.trim();
            if (text != old) {
                if (text.isEmpty()) {
                    return (T) _deserializeFromEmptyString(ctxt);
                }
            }
        }
        Exception cause = null;
        try {
            // 19-May-2017, tatu: Used to require non-null result (assuming `null`
            //    indicated error; but that seems wrong. Should be able to return
            //    `null` as value.
            return _deserialize(text, ctxt);
        } catch (IllegalArgumentException | MalformedURLException | UnknownHostException e) {
            cause = e;
        }
        // note: `cause` can't be null
        String msg = "not a valid textual representation";
        String m2 = cause.getMessage();
        if (m2 != null) {
            msg = msg + ", problem: "+m2;
        }
        // 05-May-2016, tatu: Unlike most usage, this seems legit, so...
        throw ctxt.weirdStringException(text, _valueClass, msg)
            .withCause(cause);
    }

    /**
     * Main method from trying to deserialize actual value from non-empty
     * String.
     */
    protected abstract T _deserialize(String value, DeserializationContext ctxt)
        throws JacksonException, MalformedURLException, UnknownHostException;

    protected boolean _shouldTrim() {
        return true;
    }

    protected Object _deserializeFromOther(JsonParser p, DeserializationContext ctxt,
            JsonToken t) throws JacksonException
    {
        // [databind#381]
        if (t == JsonToken.START_ARRAY) {
            return _deserializeFromArray(p, ctxt);
        }
        if (t == JsonToken.VALUE_EMBEDDED_OBJECT) {
            // Trivial cases; null to null, instance of type itself returned as is
            Object ob = p.getEmbeddedObject();
            if (ob == null) {
                return null;
            }
            if (_valueClass.isAssignableFrom(ob.getClass())) {
                return ob;
            }
            return _deserializeEmbedded(ob, ctxt);
        }
        return ctxt.handleUnexpectedToken(getValueType(ctxt), p);
    }

    /**
     * Overridable method to allow coercion from embedded value that is neither
     * {@code null} nor directly assignable to target type.
     * Used, for example, by {@link UUIDDeserializer} to coerce from {@code byte[]}.
     */
    protected T _deserializeEmbedded(Object ob, DeserializationContext ctxt) throws JacksonException {
        // default impl: error out
        ctxt.reportInputMismatch(this,
                "Don't know how to convert embedded Object of type %s into %s",
                ClassUtil.classNameOf(ob), _valueClass.getName());
        return null;
    }

    protected Object _deserializeFromEmptyString(DeserializationContext ctxt) throws JacksonException {
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
        // 09-Jun-2020, tatu: semantics for `TryConvert` are bit interesting due to
        //    historical reasons
        return _deserializeFromEmptyStringDefault(ctxt);
    }

    protected Object _deserializeFromEmptyStringDefault(DeserializationContext ctxt)
        throws JacksonException
    {
        // by default, "as-null", but overridable by sub-classes
        return getNullValue(ctxt);
    }
}
