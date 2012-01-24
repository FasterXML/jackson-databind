package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
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
    implements ResolvableDeserializer
{
    protected final JavaType _mapType;
    
    protected final BeanProperty _property;
    
    protected final Class<?> _enumClass;

    protected final JsonDeserializer<Enum<?>> _keyDeserializer;

    protected JsonDeserializer<Object> _valueDeserializer;

    public EnumMapDeserializer(JavaType mapType, BeanProperty prop,
            JsonDeserializer<?> keyDeserializer, JsonDeserializer<Object> valueDeser)
    {
        super(EnumMap.class);
        _mapType = mapType;
        _property = prop;
        _enumClass = mapType.getKeyType().getRawClass();
        _keyDeserializer = (JsonDeserializer<Enum<?>>) keyDeserializer;
        _valueDeserializer = valueDeser;
    }

    /**
     * Because of costs associated with constructing Enum resolvers,
     * let's cache instances by default.
     */
    @Override
    public boolean isCachable() { return true; }
    
    @Override
    public EnumMap<?,?> deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        // Ok: must point to START_OBJECT
        if (jp.getCurrentToken() != JsonToken.START_OBJECT) {
            throw ctxt.mappingException(EnumMap.class);
        }
        EnumMap result = constructMap();

        while ((jp.nextToken()) != JsonToken.END_OBJECT) {
            Enum<?> key = _keyDeserializer.deserialize(jp, ctxt);
            if (key == null) {
                throw ctxt.weirdStringException(_enumClass, "value not one of declared Enum instance names");
            }
            // And then the value...
            JsonToken t = jp.nextToken();
            /* note: MUST check for nulls separately: deserializers will
             * not handle them (and maybe fail or return bogus data)
             */
            Object value = (t == JsonToken.VALUE_NULL) ?
                null :  _valueDeserializer.deserialize(jp, ctxt);
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

    @Override
    public void resolve(DeserializationContext ctxt) throws JsonMappingException
    {
        if (_valueDeserializer == null) {
            // 'null' -> arrays have no referring fields
            _valueDeserializer = ctxt.findValueDeserializer(_mapType.getContentType(), _property);
        }
    }
    
    private EnumMap<?,?> constructMap() {
        return new EnumMap(_enumClass);
    }
}
