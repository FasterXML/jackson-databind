package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

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
        if (_value instanceof byte[]) {
            return (byte[]) _value;
        }
        return super.binaryValue();
    }

    /*
    /**********************************************************************
    /* Overridden JsonNode methods, scalar access, numeric
    /**********************************************************************
     */
    
    @Override
    public int asInt(int defaultValue)
    {
        if (_value instanceof Number N) {
            return N.intValue();
        }
        return defaultValue;
    }

    @Override
    public long asLong(long defaultValue)
    {
        if (_value instanceof Number N) {
            return N.longValue();
        }
        return defaultValue;
    }

    // `bigIntegerValue()` (etc) fine as defaults (fail); but need to override `asBigInteger()`

    @Override
    public BigInteger asBigInteger() {
        BigInteger big = _extractAsBigInteger();
        return (big == null) ? super.asBigInteger() : big;
    }

    @Override
    public BigInteger asBigInteger(BigInteger defaultValue) {
        BigInteger big = _extractAsBigInteger();
        return (big == null) ? defaultValue : big;
    }

    @Override
    public Optional<BigInteger> asBigIntegerOpt() {
        BigInteger big = _extractAsBigInteger();
        return (big == null) ? Optional.empty() : Optional.of(big);
    }

    // `doubleValue()` (etc) fine as defaults (fail); but need to override `asDouble()`

    @Override
    public double asDouble()
    {
        Double d = _extractAsDouble();
        return (d == null) ? super.asDouble() : d;
    }

    @Override
    public double asDouble(double defaultValue)
    {
        Double d = _extractAsDouble();
        return (d == null) ? defaultValue : d;
    }

    @Override
    public OptionalDouble asDoubleOpt() {
        Double d = _extractAsDouble();
        return (d == null) ? OptionalDouble.empty() : OptionalDouble.of(d);
    }

    // `decimalValue()` (etc) fine as defaults (fail); but need to override `asDecimal()`

    @Override
    public BigDecimal asDecimal() {
        BigDecimal dec = _extractAsBigDecimal();
        if (dec == null) {
            return super.asDecimal();
        }
        return dec;
    }

    @Override
    public BigDecimal asDecimal(BigDecimal defaultValue) {
        BigDecimal dec = _extractAsBigDecimal();
        if (dec == null) {
            return defaultValue;
        }
        return dec;
    }

    @Override
    public Optional<BigDecimal> asDecimalOpt() {
        BigDecimal dec = _extractAsBigDecimal();
        return (dec == null) ? Optional.empty() : Optional.of(dec);
    }

    // Consider only Integral numbers
    protected BigInteger _extractAsBigInteger() {
        // First, `null` same as `NullNode`
        if (_value == null) {
            return BigInteger.ZERO;
        }
        // Next, coercions from Numbers
        if (_value instanceof BigInteger big) {
            return big;
        }
        if (_value instanceof Number N) {
            if (N instanceof Long || N instanceof Integer || N instanceof Short || N instanceof Byte) {
                return BigInteger.valueOf(N.longValue());
            }
        }
        return null;
    }

    protected Double _extractAsDouble() {
        if (_value instanceof Number N) {
            if (_value instanceof Double D) {
                return D;
            }
            // 24-Mar-2025, tatu: Should probably check for NaN from overflow
            //    from "too big" `BigDecimal` or `BigInteger`. But will do for now
            return N.doubleValue();
        }
        return null;
    }
    
    protected BigDecimal _extractAsBigDecimal() {
        // First, `null` same as `NullNode`
        if (_value == null) {
            return BigDecimal.ZERO;
        }
        // Next, coercions from Numbers
        if (_value instanceof BigDecimal dec) {
            return dec;
        }
        if (_value instanceof BigInteger I) {
            return new BigDecimal(I);
        }
        if (_value instanceof Number N) {
            if (N instanceof Long || N instanceof Integer || N instanceof Short || N instanceof Byte) {
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
        if (o instanceof POJONode) {
            return _pojoEquals((POJONode) o);
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
