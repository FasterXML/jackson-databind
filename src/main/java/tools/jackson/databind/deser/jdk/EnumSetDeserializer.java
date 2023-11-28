package tools.jackson.databind.deser.jdk;

import java.util.*;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.NullValueProvider;
import tools.jackson.databind.deser.impl.NullsConstantProvider;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.type.LogicalType;
import tools.jackson.databind.util.AccessPattern;

/**
 * Standard deserializer for {@link EnumSet}s.
 * <p>
 * Note: casting within this class is all messed up -- just could not figure out a way
 * to properly deal with recursive definition of "EnumSet&lt;K extends Enum&lt;K&gt;, V&gt;
 */
@SuppressWarnings("rawtypes")
public class EnumSetDeserializer
    extends StdDeserializer<EnumSet<?>>
{
    protected final JavaType _enumType;

    protected ValueDeserializer<Enum<?>> _enumDeserializer;

    /**
     * If element instances have polymorphic type information, this
     * is the type deserializer that can handle it.
     */
    protected final TypeDeserializer _valueTypeDeserializer;

    /**
     * Handler we need for dealing with nulls.
     */
    protected final NullValueProvider _nullProvider;

    /**
     * Marker flag set if the <code>_nullProvider</code> indicates that all null
     * content values should be skipped (instead of being possibly converted).
     */
    protected final boolean _skipNullValues;

    /**
     * Specific override for this instance (from proper, or global per-type overrides)
     * to indicate whether single value may be taken to mean an unwrapped one-element array
     * or not. If null, left to global defaults.
     */
    protected final Boolean _unwrapSingle;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    @SuppressWarnings("unchecked")
    public EnumSetDeserializer(JavaType enumType, ValueDeserializer<?> deser,
            TypeDeserializer valueTypeDeser)
    {
        super(EnumSet.class);
        _enumType = enumType;
        // sanity check
        if (!enumType.isEnumType()) {
            throw new IllegalArgumentException("Type "+enumType+" not Java Enum type");
        }
        _enumDeserializer = (ValueDeserializer<Enum<?>>) deser;
        _valueTypeDeserializer = valueTypeDeser;
        _unwrapSingle = null;
        _nullProvider = null;
        _skipNullValues = false;
    }

    @SuppressWarnings("unchecked" )
    protected EnumSetDeserializer(EnumSetDeserializer base,
            ValueDeserializer<?> deser, TypeDeserializer valueTypeDeser,
            NullValueProvider nuller, Boolean unwrapSingle) {
        super(base);
        _enumType = base._enumType;
        _enumDeserializer = (ValueDeserializer<Enum<?>>) deser;
        _valueTypeDeserializer = base._valueTypeDeserializer;
        _nullProvider = nuller;
        _skipNullValues = NullsConstantProvider.isSkipper(nuller);
        _unwrapSingle = unwrapSingle;
    }

    public EnumSetDeserializer withDeserializer(ValueDeserializer<?> deser) {
        if (_enumDeserializer == deser) {
            return this;
        }
        return new EnumSetDeserializer(this, deser, _valueTypeDeserializer,
                _nullProvider, _unwrapSingle);
    }

    public EnumSetDeserializer withResolved(ValueDeserializer<?> deser,
            TypeDeserializer valueTypeDeser,
            NullValueProvider nuller, Boolean unwrapSingle) {
        if ((Objects.equals(_unwrapSingle, unwrapSingle))
                && (_enumDeserializer == deser)
                && (_valueTypeDeserializer == valueTypeDeser)
                && (_nullProvider == deser)) {
            return this;
        }
        return new EnumSetDeserializer(this, deser, _valueTypeDeserializer,
                nuller, unwrapSingle);
    }

    /*
    /**********************************************************************
    /* Basic metadata
    /**********************************************************************
     */

    /**
     * Because of costs associated with constructing Enum resolvers,
     * let's cache instances by default.
     */
    @Override
    public boolean isCachable() {
        // One caveat: content deserializer should prevent caching
        if ((_enumType.getValueHandler() != null)
                // Another: polymorphic deserialization
                || (_valueTypeDeserializer != null)) {
            return false;
        }
        return true;
    }
    
    @Override // since 2.12
    public LogicalType logicalType() {
        return LogicalType.Collection;
    }

    @Override
    public Boolean supportsUpdate(DeserializationConfig config) {
        return Boolean.TRUE;
    }

    @Override
    public Object getEmptyValue(DeserializationContext ctxt) {
        return constructSet();
    }

    @Override
    public AccessPattern getEmptyAccessPattern() {
        return AccessPattern.DYNAMIC;
    }

    /*
    /**********************************************************************
    /* Contextualization
    /**********************************************************************
     */

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property)
    {
        // 07-May-2020, tatu: Is the argument `EnumSet.class` correct here?
        //    In a way seems like it should rather refer to value class... ?
        //    (as it's individual value of element type, not Container)...
        final Boolean unwrapSingle = findFormatFeature(ctxt, property, EnumSet.class,
                JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        ValueDeserializer<?> deser = _enumDeserializer;
        if (deser == null) {
            deser = ctxt.findContextualValueDeserializer(_enumType, property);
        } else { // if directly assigned, probably not yet contextual, so:
            deser = ctxt.handleSecondaryContextualization(deser, property, _enumType);
        }
        // and finally, type deserializer needs context as well
        TypeDeserializer valueTypeDeser = _valueTypeDeserializer;
        if (valueTypeDeser != null) {
            valueTypeDeser = valueTypeDeser.forProperty(property);
        }
        return withResolved(deser, valueTypeDeser,
                findContentNullProvider(ctxt, property, deser), unwrapSingle);
    }

    /*
    /**********************************************************************
    /* ValueDeserializer API
    /**********************************************************************
     */

    @Override
    public EnumSet<?> deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException
    {
        EnumSet result = constructSet();
        // Ok: must point to START_ARRAY (or equivalent)
        if (!p.isExpectedStartArrayToken()) {
            return handleNonArray(p, ctxt, result);
        }
        return _deserialize(p, ctxt, result);
    }

    @Override
    public EnumSet<?> deserialize(JsonParser p, DeserializationContext ctxt,
            EnumSet<?> result) throws JacksonException
    {
        // Ok: must point to START_ARRAY (or equivalent)
        if (!p.isExpectedStartArrayToken()) {
            return handleNonArray(p, ctxt, result);
        }
        return _deserialize(p, ctxt, result);
    }

    @SuppressWarnings("unchecked")
    protected final EnumSet<?> _deserialize(JsonParser p, DeserializationContext ctxt,
            EnumSet result) throws JacksonException
    {
        JsonToken t;
        final TypeDeserializer typeDeser = _valueTypeDeserializer;

        try {
            while ((t = p.nextToken()) != JsonToken.END_ARRAY) {
                // What to do with nulls? Fail or ignore? Fail, for now (note: would fail if we
                // passed it to EnumDeserializer, too, but in general nulls should never be passed
                // to non-container deserializers)
                Enum<?> value;
                if (t == JsonToken.VALUE_NULL) {
                    if (_skipNullValues) {
                        continue;
                    }
                    value = (Enum<?>) _nullProvider.getNullValue(ctxt);
                } else if (typeDeser == null) {
                    value = _enumDeserializer.deserialize(p, ctxt);
                } else {
                    value = (Enum<?>) _enumDeserializer.deserializeWithType(p, ctxt, typeDeser);
                }
                if (value != null) {
                    result.add(value);
                }
            }
        } catch (Exception e) {
            throw DatabindException.wrapWithPath(e, result, result.size());
        }
        return result;
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws JacksonException
    {
        return typeDeserializer.deserializeTypedFromArray(p, ctxt);
    }

    @SuppressWarnings("unchecked")
    private EnumSet constructSet()
    {
        return EnumSet.noneOf((Class<Enum>) _enumType.getRawClass());
    }

    @SuppressWarnings("unchecked")
    protected EnumSet<?> handleNonArray(JsonParser p, DeserializationContext ctxt,
            EnumSet result)
        throws JacksonException
    {
        boolean canWrap = (_unwrapSingle == Boolean.TRUE) ||
                ((_unwrapSingle == null) &&
                        ctxt.isEnabled(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY));

        if (!canWrap) {
            return (EnumSet<?>) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
        }
        // First: since `null`s not allowed, slightly simpler...
        if (p.hasToken(JsonToken.VALUE_NULL)) {
            return (EnumSet<?>) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
        }
        try {
            Enum<?> value = _enumDeserializer.deserialize(p, ctxt);
            if (value != null) {
                result.add(value);
            }
        } catch (Exception e) {
            throw DatabindException.wrapWithPath(e, result, result.size());
        }
        return result;
    }
}
