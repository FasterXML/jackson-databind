package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

/**
 * Deserializer for {@link EnumMap} values.
 * <p>
 * Note: casting within this class is all messed up -- just could not figure out a way
 * to properly deal with recursive definition of "EnumMap<K extends Enum<K>, V>
 * 
 * @author tsaloranta
 */
@SuppressWarnings({ "unchecked", "rawtypes" }) 
public class EnumMapDeserializer
    extends StdDeserializer<EnumMap<?,?>>
    implements ContextualDeserializer
{
    private static final long serialVersionUID = 1518773374647478964L;

    protected final JavaType _mapType;
    
    protected final Class<?> _enumClass;

    protected JsonDeserializer<Enum<?>> _keyDeserializer;

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
     * @deprecated Since 2.1.3 -- use variant that takes one more argument.
     */
    @Deprecated
    public EnumMapDeserializer(JavaType mapType,
            JsonDeserializer<?> keyDeserializer, JsonDeserializer<?> valueDeser) {
        this(mapType, keyDeserializer, valueDeser, null);
    }
    
    public EnumMapDeserializer(JavaType mapType,
            JsonDeserializer<?> keyDeserializer, JsonDeserializer<?> valueDeser,
            TypeDeserializer valueTypeDeser)
    {
        super(EnumMap.class);
        _mapType = mapType;
        _enumClass = mapType.getKeyType().getRawClass();
        _keyDeserializer = (JsonDeserializer<Enum<?>>) keyDeserializer;
        _valueDeserializer = (JsonDeserializer<Object>) valueDeser;
        _valueTypeDeserializer = valueTypeDeser;
    }

    /**
     * @deprecated Since 2.1.3 -- use variant that takes one more argument.
     */
    @Deprecated
    public EnumMapDeserializer withResolved(JsonDeserializer<?> keyDeserializer,
            JsonDeserializer<?> valueDeserializer)
    {
        return withResolved(keyDeserializer, valueDeserializer, null);
    } 
    
    public EnumMapDeserializer withResolved(JsonDeserializer<?> keyDeserializer,
            JsonDeserializer<?> valueDeserializer, TypeDeserializer valueTypeDeser)
    {
        if ((keyDeserializer == _keyDeserializer)
                && (valueDeserializer == _valueDeserializer)
                && (valueTypeDeser == _valueTypeDeserializer)) {
            return this;
        }
        return new EnumMapDeserializer(_mapType,
                keyDeserializer, valueDeserializer, _valueTypeDeserializer);
    }
    
    /**
     * Method called to finalize setup of this deserializer,
     * when it is known for which property deserializer is needed for.
     */
    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property) throws JsonMappingException
    {
        // note: instead of finding key deserializer, with enums we actually
        // work with regular deserializers (less code duplication; but not
        // quite as clean as it ought to be)
        JsonDeserializer<?> kd = _keyDeserializer;
        if (kd == null) {
            kd = ctxt.findContextualValueDeserializer(_mapType.getKeyType(), property);
        }
        JsonDeserializer<?> vd = _valueDeserializer;
        if (vd == null) {
            vd = ctxt.findContextualValueDeserializer(_mapType.getContentType(), property);
        } else { // if directly assigned, probably not yet contextual, so:
            if (vd instanceof ContextualDeserializer) {
                vd = ((ContextualDeserializer) vd).createContextual(ctxt, property);
            }
        }
        TypeDeserializer vtd = _valueTypeDeserializer;
        if (vtd != null) {
            vtd = vtd.forProperty(property);
        }
        return withResolved(kd, vd, vtd);
    }
    
    /**
     * Because of costs associated with constructing Enum resolvers,
     * let's cache instances by default.
     */
    @Override
    public boolean isCachable() { return true; }
    
    /*
    /**********************************************************
    /* Actual deserialization
    /**********************************************************
     */

    @Override
    public EnumMap<?,?> deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        // Ok: must point to START_OBJECT
        if (jp.getCurrentToken() != JsonToken.START_OBJECT) {
            throw ctxt.mappingException(EnumMap.class);
        }
        EnumMap result = constructMap();
        final JsonDeserializer<Object> valueDes = _valueDeserializer;
        final TypeDeserializer typeDeser = _valueTypeDeserializer;

        while ((jp.nextToken()) != JsonToken.END_OBJECT) {
            Enum<?> key = _keyDeserializer.deserialize(jp, ctxt);
            if (key == null) {
                if (!ctxt.isEnabled(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)) {
                    String value = null;
                    try { // bit ugly, but will have to do; works with usual scalars
                        if (jp.hasCurrentToken()) {
                            value = jp.getText();
                        }
                    } catch (Exception e) { }
                    throw ctxt.weirdStringException(value, _enumClass, "value not one of declared Enum instance names");
                }
                /* 24-Mar-2012, tatu: Null won't work as a key anyway, so let's
                 *  just skip the entry then. But we must skip the value then.
                 */
                jp.nextToken();
                jp.skipChildren();
                continue;
            }
            // And then the value...
            JsonToken t = jp.nextToken();
            /* note: MUST check for nulls separately: deserializers will
             * not handle them (and maybe fail or return bogus data)
             */
            Object value;
            
            if (t == JsonToken.VALUE_NULL) {
                value = null;
            } else if (typeDeser == null) {
                value =  valueDes.deserialize(jp, ctxt);
            } else {
                value = valueDes.deserializeWithType(jp, ctxt, typeDeser);
            }
            result.put(key, value);
        }
        return result;
    }

    @Override
    public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws IOException, JsonProcessingException
    {
        // In future could check current token... for now this should be enough:
        return typeDeserializer.deserializeTypedFromObject(jp, ctxt);
    }
    
    private EnumMap<?,?> constructMap() {
        return new EnumMap(_enumClass);
    }
}
