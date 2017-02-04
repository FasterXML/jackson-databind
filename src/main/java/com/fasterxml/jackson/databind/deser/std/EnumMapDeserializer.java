package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.NullValueProvider;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

/**
 * Deserializer for {@link EnumMap} values.
 * <p>
 * Note: casting within this class is all messed up -- just could not figure out a way
 * to properly deal with recursive definition of "EnumMap&lt;K extends Enum&lt;K>, V>
 */
@SuppressWarnings({ "unchecked", "rawtypes" }) 
public class EnumMapDeserializer
    extends ContainerDeserializerBase<EnumMap<?,?>>
    implements ContextualDeserializer
{
    private static final long serialVersionUID = 1;

    protected final Class<?> _enumClass;

    protected KeyDeserializer _keyDeserializer;

    protected JsonDeserializer<Object> _valueDeserializer;

    /**
     * If value instances have polymorphic type information, this
     * is the type deserializer that can handle it
     */
    protected final TypeDeserializer _valueTypeDeserializer;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    /**
     * @since 2.9
     */
    public EnumMapDeserializer(JavaType mapType, KeyDeserializer keyDeser,
            JsonDeserializer<?> valueDeser, TypeDeserializer vtd,
            NullValueProvider nuller)
    {
        super(mapType, nuller, null);
        _enumClass = mapType.getKeyType().getRawClass();
        _keyDeserializer = keyDeser;
        _valueDeserializer = (JsonDeserializer<Object>) valueDeser;
        _valueTypeDeserializer = vtd;
    }

    @Deprecated // since 2.9
    public EnumMapDeserializer(JavaType mapType, KeyDeserializer keyDeser,
            JsonDeserializer<?> valueDeser, TypeDeserializer vtd)
    {
        this(mapType, keyDeser, valueDeser, vtd, null);
    }
    
    public EnumMapDeserializer withResolved(KeyDeserializer keyDeserializer,
            JsonDeserializer<?> valueDeserializer, TypeDeserializer valueTypeDeser,
            NullValueProvider nuller)
    {
        if ((keyDeserializer == _keyDeserializer) && (nuller == _nullProvider)
                && (valueDeserializer == _valueDeserializer) && (valueTypeDeser == _valueTypeDeserializer)) {
            return this;
        }
        return new EnumMapDeserializer(_containerType, keyDeserializer,
                valueDeserializer, _valueTypeDeserializer, nuller);
    }
    
    /**
     * Method called to finalize setup of this deserializer,
     * when it is known for which property deserializer is needed for.
     */
    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException
    {
        // note: instead of finding key deserializer, with enums we actually
        // work with regular deserializers (less code duplication; but not
        // quite as clean as it ought to be)
        KeyDeserializer keyDeser = _keyDeserializer;
        if (keyDeser == null) {
            keyDeser = ctxt.findKeyDeserializer(_containerType.getKeyType(), property);
        }
        JsonDeserializer<?> valueDeser = _valueDeserializer;
        final JavaType vt = _containerType.getContentType();
        if (valueDeser == null) {
            valueDeser = ctxt.findContextualValueDeserializer(vt, property);
        } else { // if directly assigned, probably not yet contextual, so:
            valueDeser = ctxt.handleSecondaryContextualization(valueDeser, property, vt);
        }
        TypeDeserializer vtd = _valueTypeDeserializer;
        if (vtd != null) {
            vtd = vtd.forProperty(property);
        }
        return withResolved(keyDeser, valueDeser, vtd, findContentNullProvider(ctxt, property, valueDeser));
    }

    /**
     * Because of costs associated with constructing Enum resolvers,
     * let's cache instances by default.
     */
    @Override
    public boolean isCachable() {
        // Important: do NOT cache if polymorphic values
        return (_valueDeserializer == null)
                && (_keyDeserializer == null)
                && (_valueTypeDeserializer == null);
    }

    /*
    /**********************************************************
    /* ContainerDeserializerBase API
    /**********************************************************
     */

    @Override
    public JsonDeserializer<Object> getContentDeserializer() {
        return _valueDeserializer;
    }

    // Must override since we do not expose ValueInstantiator
    @Override // since 2.9
    public Object getEmptyValue(DeserializationContext ctxt) throws JsonMappingException {
        return constructMap();
    }

    /*
    /**********************************************************
    /* Actual deserialization
    /**********************************************************
     */
    
    @Override
    public EnumMap<?,?> deserialize(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        // Ok: must point to START_OBJECT
        if (p.getCurrentToken() != JsonToken.START_OBJECT) {
            return _deserializeFromEmpty(p, ctxt);
        }
        EnumMap result = constructMap();
        final JsonDeserializer<Object> valueDes = _valueDeserializer;
        final TypeDeserializer typeDeser = _valueTypeDeserializer;

        while ((p.nextToken()) == JsonToken.FIELD_NAME) {
            String keyName = p.getCurrentName(); // just for error message
            // but we need to let key deserializer handle it separately, nonetheless
            Enum<?> key = (Enum<?>) _keyDeserializer.deserializeKey(keyName, ctxt);
            if (key == null) {
                if (!ctxt.isEnabled(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)) {
                    return (EnumMap<?,?>) ctxt.handleWeirdStringValue(_enumClass, keyName,
                            "value not one of declared Enum instance names for %s",
                            _containerType.getKeyType());
                }
                /* 24-Mar-2012, tatu: Null won't work as a key anyway, so let's
                 *  just skip the entry then. But we must skip the value as well, if so.
                 */
                p.nextToken();
                p.skipChildren();
                continue;
            }
            // And then the value...
            JsonToken t = p.nextToken();
            // note: MUST check for nulls separately: deserializers will
            // not handle them (and maybe fail or return bogus data)
            Object value;

            try {
                if (t == JsonToken.VALUE_NULL) {
                    if (_skipNullValues) {
                        continue;
                    }
                    value = _nullProvider.getNullValue(ctxt);
                } else if (typeDeser == null) {
                    value =  valueDes.deserialize(p, ctxt);
                } else {
                    value = valueDes.deserializeWithType(p, ctxt, typeDeser);
                }
            } catch (Exception e) {
                return wrapAndThrow(e, result, keyName);
            }
            result.put(key, value);
        }
        return result;
    }

    @Override
    public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt, TypeDeserializer typeDeserializer)
        throws IOException
    {
        // In future could check current token... for now this should be enough:
        return typeDeserializer.deserializeTypedFromObject(jp, ctxt);
    }
    
    protected EnumMap<?,?> constructMap() {
        return new EnumMap(_enumClass);
    }
}

