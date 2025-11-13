package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import tools.jackson.core.*;

import tools.jackson.databind.JacksonSerializable;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.util.ClassUtil;

/**
 * Value node that contains a wrapped POJO, to be serialized as
 * a JSON constructed through data mapping (usually done by
 * calling {@link tools.jackson.databind.ObjectMapper}).
 */
public class POJONode
    extends ValueNode
{
    private static final long serialVersionUID = 3L;

    protected final Object _value;

    public POJONode(Object v) { _value = v; }

    /*
    /**********************************************************************
    /* Base class overrides
    /**********************************************************************
     */

    @Override
    protected String _valueDesc() {
        return "{POJO of type "+ClassUtil.classNameOf(_value)+"}";
    }

    @Override
    public JsonNodeType getNodeType() {
        return JsonNodeType.POJO;
    }

    @Override
    public JsonToken asToken() { return JsonToken.VALUE_EMBEDDED_OBJECT; }

    @Override
    public boolean isEmbeddedValue() { return true; }

    /*
    /**********************************************************************
    /* Overridden JsonNode methods, scalar access, non-numeric
    /**********************************************************************
     */

    @Override
    protected Boolean _asBoolean()
    {
        if (_value == null) {
            return Boolean.FALSE;
        }
        if (_value instanceof Boolean B) {
            return B;
        }
        return null;
    }

    @Override
    protected String _asString() {
        if (_value instanceof String str) {
             return str;
        }
        // 21-Mar-2025, tatu: [databind#5034] Should we consider RawValue too?
        //    (for now, won't)
        return null;
    }

    /**
     * As it is possible that some implementations embed byte[] as POJONode
     * (despite optimal being {@link BinaryNode}), let's add support for exposing
     * binary data here too.
     */
    @Override
    public byte[] binaryValue()
    {
        if (_value instanceof byte[] byteArray) {
            return byteArray;
        }
        return super.binaryValue();
    }

    /*
    /**********************************************************************
    /* Overridden JsonNode methods, scalar access, numeric
    /**********************************************************************
     */

    // `shortValue()` (etc) fine as defaults (fail); but need to override `asShort()`

    @Override
    public short asShort() {
        // First, `null` same as `NullNode`
        if (_value == null) {
            return 0;
        }

        // Next, check if the value is NOT a Number
        if (!(_value instanceof Number)) {
            // report coercion fail
            super.asShort();
        }

        // Then, extract from Number
        Long L = _extractAsLong();
        if (L == null || L < Short.MIN_VALUE || L > Short.MAX_VALUE) {
            // report range fail
            _reportShortCoercionRangeFail("asShort()");
        }
        return L.shortValue();
    }

    @Override
    public short asShort(short defaultValue) {
        // First, `null` same as `NullNode`
        if (_value == null) {
            return 0;
        }

        // Next, check if the value is NOT a Number
        if (!(_value instanceof Number)) {
            return defaultValue;
        }

        // Then, extract from Number
        Long L = _extractAsLong();
        if (L == null || L < Short.MIN_VALUE || L > Short.MAX_VALUE) {
            return defaultValue;
        }
        return L.shortValue();
    }

    @Override
    public Optional<Short> asShortOpt() {
        // First, `null` same as `NullNode`
        if (_value == null) {
            return Optional.of((short) 0);
        }

        // Next, check if the value is NOT a Number
        if (!(_value instanceof Number)) {
            return Optional.empty();
        }

        // Then, extract from Number
        Long L = _extractAsLong();
        if (L == null || L < Short.MIN_VALUE || L > Short.MAX_VALUE) {
            return Optional.empty();
        }
        return Optional.of(L.shortValue());
    }

    // `intValue()` (etc) fine as defaults (fail); but need to override `asInt()`

    @Override
    public int asInt() {
        // First, `null` same as `NullNode`
        if (_value == null) {
            return 0;
        }

        // Next, check if the value is NOT a Number
        if (!(_value instanceof Number)) {
            // report coercion fail
            super.asInt();
        }

        // Then, extract from Number
        Long L = _extractAsLong();
        if (L == null || L < Integer.MIN_VALUE || L > Integer.MAX_VALUE) {
            // report range fail
            _reportIntCoercionRangeFail("asInt()");
        }
        return L.intValue();
    }

    @Override
    public int asInt(int defaultValue) {
        // First, `null` same as `NullNode`
        if (_value == null) {
            return 0;
        }

        // Next, check if the value is NOT a Number
        if (!(_value instanceof Number)) {
            return defaultValue;
        }

        // Then, extract from Number
        Long L = _extractAsLong();
        if (L == null || L < Integer.MIN_VALUE || L > Integer.MAX_VALUE) {
            return defaultValue;
        }
        return L.intValue();
    }

    @Override
    public OptionalInt asIntOpt() {
        // First, `null` same as `NullNode`
        if (_value == null) {
            return OptionalInt.of(0);
        }

        // Next, check if the value is NOT a Number
        if (!(_value instanceof Number)) {
            return OptionalInt.empty();
        }

        // Then, extract from Number
        Long L = _extractAsLong();
        if (L == null || L < Integer.MIN_VALUE || L > Integer.MAX_VALUE) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(L.intValue());
    }

    // `longValue()` (etc) fine as defaults (fail); but need to override `asLong()`

    @Override
    public long asLong() {
        // First, `null` same as `NullNode`
        if (_value == null) {
            return 0L;
        }

        // Next, check if the value is NOT a Number
        if (!(_value instanceof Number)) {
            // report coercion fail
            super.asLong();
        }

        // Then, extract from Number
        Long L = _extractAsLong();
        if (L == null) {
            // report range fail
            _reportLongCoercionRangeFail("asLong()");
        }
        return L;
    }

    @Override
    public long asLong(long defaultValue) {
        // First, `null` same as `NullNode`
        if (_value == null) {
            return 0L;
        }

        // Next, report coercion fail if the value is NOT a Number
        if (!(_value instanceof Number)) {
            return defaultValue;
        }

        // Then, extract from Number
        Long L = _extractAsLong();
        if (L == null) {
            return defaultValue;
        }
        return L;
    }

    @Override
    public OptionalLong asLongOpt() {
        // First, `null` same as `NullNode`
        if (_value == null) {
            return OptionalLong.of(0L);
        }

        // Next, report coercion fail if the value is NOT a Number
        if (!(_value instanceof Number)) {
            return OptionalLong.empty();
        }

        // Then, extract from Number
        Long L = _extractAsLong();
        if (L == null) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(L);
    }

    // `bigIntegerValue()` (etc) fine as defaults (fail); but need to override `asBigInteger()`

    @Override
    public BigInteger asBigInteger() {
        // First, `null` same as `NullNode`
        if (_value == null) {
            return BigInteger.ZERO;
        }

        // Next, check if the value is NOT a Number
        if (!(_value instanceof Number)) {
            // report coercion fail
            super.asBigInteger();
        }

        // Then, extract from Number
        return _extractAsBigInteger();
    }

    @Override
    public BigInteger asBigInteger(BigInteger defaultValue) {
        // First, `null` same as `NullNode`
        if (_value == null) {
            return BigInteger.ZERO;
        }

        // Next, check if the value is NOT a Number
        if (!(_value instanceof Number)) {
            return defaultValue;
        }

        // Then, extract from Number
        return _extractAsBigInteger();
    }

    @Override
    public Optional<BigInteger> asBigIntegerOpt() {
        // First, `null` same as `NullNode`
        if (_value == null) {
            return Optional.of(BigInteger.ZERO);
        }

        // Next, check if the value is NOT a Number
        if (!(_value instanceof Number)) {
            return Optional.empty();
        }

        // Then, extract from Number
        return Optional.of(_extractAsBigInteger());
    }

    // `floatValue()` (etc) fine as defaults (fail); but need to override `asFloat()`

    @Override
    public float asFloat() {
        // First, `null` same as `NullNode`
        if (_value == null) {
            return 0.0f;
        }

        // Next, check if the value is NOT a Number
        if (!(_value instanceof Number)) {
            // report coercion fail
            super.asFloat();
        }

        // Then, extract from Number
        Float F = _extractAsFloat();
        if (F == null) {
            // report range fail
            _reportFloatCoercionRangeFail("asFloat()");
        }
        return F;
    }

    @Override
    public float asFloat(float defaultValue) {
        // First, `null` same as `NullNode`
        if (_value == null) {
            return 0.0f;
        }

        // Next, check if the value is NOT a Number
        if (!(_value instanceof Number)) {
            return defaultValue;
        }

        // Then, extract from Number
        Float F = _extractAsFloat();
        if (F == null) {
            return defaultValue;
        }
        return F;
    }

    @Override
    public Optional<Float> asFloatOpt() {
        // First, `null` same as `NullNode`
        if (_value == null) {
            return Optional.of(0.0f);
        }

        // Next, check if the value is NOT a Number
        if (!(_value instanceof Number)) {
            return Optional.empty();
        }

        // Then, extract from Number
        Float F = _extractAsFloat();
        if (F == null) {
            return Optional.empty();
        }
        return Optional.of(F);
    }

    // `doubleValue()` (etc) fine as defaults (fail); but need to override `asDouble()`

    @Override
    public double asDouble() {
        // First, `null` same as `NullNode`
        if (_value == null) {
            return 0.0d;
        }

        // Next, check if the value is NOT a Number
        if (!(_value instanceof Number)) {
            // report coercion fail
            super.asDouble();
        }

        // Then, extract from Number
        Double D = _extractAsDouble();
        if (D == null) {
            // report range fail
            _reportDoubleCoercionRangeFail("asDouble()");
        }
        return D;
    }

    @Override
    public double asDouble(double defaultValue) {
        // First, `null` same as `NullNode`
        if (_value == null) {
            return 0.0d;
        }

        // Next, check if the value is NOT a Number
        if (!(_value instanceof Number)) {
            return defaultValue;
        }

        // Then, extract from Number
        Double D = _extractAsDouble();
        if (D == null) {
            return defaultValue;
        }
        return D;
    }

    @Override
    public OptionalDouble asDoubleOpt() {
        // First, `null` same as `NullNode`
        if (_value == null) {
            return OptionalDouble.of(0.0d);
        }

        // Next, check if the value is NOT a Number
        if (!(_value instanceof Number)) {
            return OptionalDouble.empty();
        }

        // Then, extract from Number
        Double D = _extractAsDouble();
        if (D == null) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(D);
    }

    // `decimalValue()` (etc) fine as defaults (fail); but need to override `asDecimal()`

    @Override
    public BigDecimal asDecimal() {
        // First, `null` same as `NullNode`
        if (_value == null) {
            return BigDecimal.ZERO;
        }

        // Next, check if the value is NOT a Number
        if (!(_value instanceof Number)) {
            // report coercion fail
            super.asDecimal();
        }

        // Then, extract from Number
        return _extractAsBigDecimal();
    }

    @Override
    public BigDecimal asDecimal(BigDecimal defaultValue) {
        // First, `null` same as `NullNode`
        if (_value == null) {
            return BigDecimal.ZERO;
        }

        // Next, check if the value is NOT a Number
        if (!(_value instanceof Number)) {
            return defaultValue;
        }

        // Then, extract from Number
        return _extractAsBigDecimal();
    }

    @Override
    public Optional<BigDecimal> asDecimalOpt() {
        // First, `null` same as `NullNode`
        if (_value == null) {
            return Optional.of(BigDecimal.ZERO);
        }

        // Next, check if the value is NOT a Number
        if (!(_value instanceof Number)) {
            return Optional.empty();
        }

        // Then, extract from Number
        return Optional.of(_extractAsBigDecimal());
    }

    // extract Long from Number, or return null if range check fails
    protected Long _extractAsLong() {
        if (_value instanceof Number N) {
            if (N instanceof BigInteger big) {
                if (big.compareTo(BI_MIN_LONG) >= 0 && big.compareTo(BI_MAX_LONG) <= 0) {
                    return big.longValue();
                }
            } else if (N instanceof BigDecimal dec) {
                if (dec.compareTo(BD_MIN_LONG) >= 0 && dec.compareTo(BD_MAX_LONG) <= 0) {
                    return dec.longValue();
                }
            } else if (N instanceof Double D) {
                if (D >= Long.MIN_VALUE && D <= Long.MAX_VALUE) {
                    return D.longValue();
                }
            } else if (N instanceof Float F) {
                if (F >= Long.MIN_VALUE && F <= Long.MAX_VALUE) {
                    return F.longValue();
                }
            } else {
                return N.longValue();
            }
        }
        return null;
    }

    // extract BigInteger from Number, or return null if range check fails
    protected BigInteger _extractAsBigInteger() {
        if (_value instanceof Number N) {
            if (N instanceof BigInteger big) {
                return big;
            } else if (N instanceof BigDecimal dec) {
                return dec.toBigInteger();
            } else {
                return BigInteger.valueOf(N.longValue());
            }
        }
        return null;
    }

    // extract Float from Number, or return null if range check fails
    protected Float _extractAsFloat() {
        if (_value instanceof Number N) {
            if (N instanceof Float F) {
                return F;
            }
            float f = N.floatValue();
            if (Float.isFinite(f)) {
                return f;
            }
        }
        return null;
    }

    // extract Double from Number, or return null if range check fails
    protected Double _extractAsDouble() {
        if (_value instanceof Number N) {
            if (N instanceof Double D) {
                return D;
            }
            double d = N.doubleValue();
            if (Double.isFinite(d)) {
                return d;
            }
        }
        return null;
    }

    // extract BigDecimal from Number, or return null if range check fails
    protected BigDecimal _extractAsBigDecimal() {
        if (_value instanceof Number N) {
            if (N instanceof BigDecimal dec) {
                return dec;
            } else if (N instanceof BigInteger big) {
                return new BigDecimal(big);
            } else if (N instanceof Long || N instanceof Integer || N instanceof Short || N instanceof Byte) {
                return BigDecimal.valueOf(N.longValue());
            }
            // Use doubleValue() as a last resort for Float & Double
            try {
                return BigDecimal.valueOf(N.doubleValue());
            } catch (IllegalArgumentException e) {
                // got an NaN
            }
        }
        return null;
    }
    
    /*
    /**********************************************************************
    /* Public API, serialization
    /**********************************************************************
     */

    @Override
    public final void serialize(JsonGenerator gen, SerializationContext ctxt) throws JacksonException
    {
        if (_value == null) {
            ctxt.defaultSerializeNullValue(gen);
        } else if (_value instanceof JacksonSerializable) {
            ((JacksonSerializable) _value).serialize(gen, ctxt);
        } else {
            // 25-May-2018, tatu: [databind#1991] do not call via generator but through context;
            //    this to preserve contextual information
            ctxt.writeValue(gen, _value);
        }
    }

    /*
    /**********************************************************************
    /* Extended API
    /**********************************************************************
     */

    /**
     * Method that can be used to access the POJO this node wraps.
     */
    public Object getPojo() { return _value; }

    /*
    /**********************************************************************
    /* Overridden standard methods
    /**********************************************************************
     */

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o instanceof POJONode pojoNode) {
            return _pojoEquals(pojoNode);
        }
        return false;
    }

    protected boolean _pojoEquals(POJONode other)
    {
        if (_value == null) {
            return other._value == null;
        }
        return _value.equals(other._value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(_value);
    }
}
