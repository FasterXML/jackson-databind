package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.LogicalType;

/**
 * Basic serializer that can take JSON "Object" structure and
 * construct a {@link java.util.Map} instance, with typed contents.
 *<p>
 * Note: for untyped content (one indicated by passing Object.class
 * as the type), {@link UntypedObjectDeserializer} is used instead.
 * It can also construct {@link java.util.Map}s, but not with specific
 * POJO types, only other containers and primitives/wrappers.
 */
@JacksonStdImpl
public class MapEntryDeserializer
    extends ContainerDeserializerBase<Map.Entry<Object,Object>>
    implements ContextualDeserializer
{
    private static final long serialVersionUID = 1;

    // // Configuration: typing, deserializers

    /**
     * Key deserializer to use; either passed via constructor
     * (when indicated by annotations), or resolved when
     * {@link #createContextual} is called;
     */
    protected final KeyDeserializer _keyDeserializer;

    /**
     * Value deserializer.
     */
    protected final JsonDeserializer<Object> _valueDeserializer;

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

    public MapEntryDeserializer(JavaType type,
            KeyDeserializer keyDeser, JsonDeserializer<Object> valueDeser,
            TypeDeserializer valueTypeDeser)
    {
        super(type);
        if (type.containedTypeCount() != 2) { // sanity check
            throw new IllegalArgumentException("Missing generic type information for "+type);
        }
        _keyDeserializer = keyDeser;
        _valueDeserializer = valueDeser;
        _valueTypeDeserializer = valueTypeDeser;
    }

    /**
     * Copy-constructor that can be used by sub-classes to allow
     * copy-on-write styling copying of settings of an existing instance.
     */
    protected MapEntryDeserializer(MapEntryDeserializer src)
    {
        super(src);
        _keyDeserializer = src._keyDeserializer;
        _valueDeserializer = src._valueDeserializer;
        _valueTypeDeserializer = src._valueTypeDeserializer;
    }

    protected MapEntryDeserializer(MapEntryDeserializer src,
            KeyDeserializer keyDeser, JsonDeserializer<Object> valueDeser,
            TypeDeserializer valueTypeDeser)
    {
        super(src);
        _keyDeserializer = keyDeser;
        _valueDeserializer = valueDeser;
        _valueTypeDeserializer = valueTypeDeser;
    }

    /**
     * Fluent factory method used to create a copy with slightly
     * different settings. When sub-classing, MUST be overridden.
     */
    @SuppressWarnings("unchecked")
    protected MapEntryDeserializer withResolved(KeyDeserializer keyDeser,
            TypeDeserializer valueTypeDeser, JsonDeserializer<?> valueDeser)
    {

        if ((_keyDeserializer == keyDeser) && (_valueDeserializer == valueDeser)
                && (_valueTypeDeserializer == valueTypeDeser)) {
            return this;
        }
        return new MapEntryDeserializer(this,
                keyDeser, (JsonDeserializer<Object>) valueDeser, valueTypeDeser);
    }

    @Override // since 2.12
    public LogicalType logicalType() {
        // Slightly tricky, could consider POJO too?
        return LogicalType.Map;
    }

    /*
    /**********************************************************
    /* Validation, post-processing (ResolvableDeserializer)
    /**********************************************************
     */

    /**
     * Method called to finalize setup of this deserializer,
     * when it is known for which property deserializer is needed for.
     */
    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property) throws JsonMappingException
    {
        KeyDeserializer kd = _keyDeserializer;
        if (kd == null) {
            kd = ctxt.findKeyDeserializer(_containerType.containedType(0), property);
        } else {
            if (kd instanceof ContextualKeyDeserializer) {
                kd = ((ContextualKeyDeserializer) kd).createContextual(ctxt, property);
            }
        }
        JsonDeserializer<?> vd = _valueDeserializer;
        vd = findConvertingContentDeserializer(ctxt, property, vd);
        JavaType contentType = _containerType.containedType(1);
        if (vd == null) {
            vd = ctxt.findContextualValueDeserializer(contentType, property);
        } else { // if directly assigned, probably not yet contextual, so:
            vd = ctxt.handleSecondaryContextualization(vd, property, contentType);
        }
        TypeDeserializer vtd = _valueTypeDeserializer;
        if (vtd != null) {
            vtd = vtd.forProperty(property);
        }
        return withResolved(kd, vtd, vd);
    }

    /*
    /**********************************************************
    /* ContainerDeserializerBase API
    /**********************************************************
     */

    @Override
    public JavaType getContentType() {
        return _containerType.containedType(1);
    }

    @Override
    public JsonDeserializer<Object> getContentDeserializer() {
        return _valueDeserializer;
    }

    // 31-May-2020, tatu: Should probably define but we don't have it yet
//    public ValueInstantiator getValueInstantiator() { }

    /*
    /**********************************************************
    /* JsonDeserializer API
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    @Override
    public Map.Entry<Object,Object> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        // Ok: must point to START_OBJECT, FIELD_NAME or END_OBJECT
        JsonToken t = p.currentToken();
        if (t == JsonToken.START_OBJECT) {
            t = p.nextToken();
        } else if (t != JsonToken.FIELD_NAME && t != JsonToken.END_OBJECT) {
            // Empty array, or single-value wrapped in array?
            if (t == JsonToken.START_ARRAY) {
                return _deserializeFromArray(p, ctxt);
            }
            return (Map.Entry<Object,Object>) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
        }
        if (t != JsonToken.FIELD_NAME) {
            if (t == JsonToken.END_OBJECT) {
                return ctxt.reportInputMismatch(this,
                        "Cannot deserialize a Map.Entry out of empty JSON Object");
            }
            return (Map.Entry<Object,Object>) ctxt.handleUnexpectedToken(handledType(), p);
        }

        final KeyDeserializer keyDes = _keyDeserializer;
        final JsonDeserializer<Object> valueDes = _valueDeserializer;
        final TypeDeserializer typeDeser = _valueTypeDeserializer;

        final String keyStr = p.currentName();
        Object key = keyDes.deserializeKey(keyStr, ctxt);
        Object value = null;
        // And then the value...
        t = p.nextToken();
        try {
            // Note: must handle null explicitly here; value deserializers won't
            if (t == JsonToken.VALUE_NULL) {
                value = valueDes.getNullValue(ctxt);
            } else if (typeDeser == null) {
                value = valueDes.deserialize(p, ctxt);
            } else {
                value = valueDes.deserializeWithType(p, ctxt, typeDeser);
            }
        } catch (Exception e) {
            wrapAndThrow(ctxt, e, Map.Entry.class, keyStr);
        }

        // Close, but also verify that we reached the END_OBJECT
        t = p.nextToken();
        if (t != JsonToken.END_OBJECT) {
            if (t == JsonToken.FIELD_NAME) { // most likely
                ctxt.reportInputMismatch(this,
                        "Problem binding JSON into Map.Entry: more than one entry in JSON (second field: '%s')",
                        p.currentName());
            } else {
                // how would this occur?
                ctxt.reportInputMismatch(this,
                        "Problem binding JSON into Map.Entry: unexpected content after JSON Object entry: "+t);
            }
            return null;
        }
        return new AbstractMap.SimpleEntry<Object,Object>(key, value);
    }

    @Override
    public Map.Entry<Object,Object> deserialize(JsonParser p, DeserializationContext ctxt,
            Map.Entry<Object,Object> result) throws IOException
    {
        throw new IllegalStateException("Cannot update Map.Entry values");
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws IOException
    {
        // In future could check current token... for now this should be enough:
        return typeDeserializer.deserializeTypedFromObject(p, ctxt);
    }
}
