package tools.jackson.databind.deser.jdk;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.deser.*;
import tools.jackson.databind.deser.std.ContainerDeserializerBase;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.type.LogicalType;

/**
 * Basic serializer that can take JSON "Object" structure and
 * construct a {@link java.util.Map.Entry} instance, with typed contents.
 *<p>
 * Note: for untyped content (one indicated by passing Object.class
 * as the type), {@link UntypedObjectDeserializer} is used instead.
 * It can also construct {@link java.util.Map.Entry}s, but not with specific
 * POJO types, only other containers and primitives/wrappers.
 */
@JacksonStdImpl
public class MapEntryDeserializer
    extends ContainerDeserializerBase<Map.Entry<Object,Object>>
{
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
    protected final ValueDeserializer<Object> _valueDeserializer;

    /**
     * If value instances have polymorphic type information, this
     * is the type deserializer that can handle it
     */
    protected final TypeDeserializer _valueTypeDeserializer;

    /**
     * Flag set when we should deserialize as POJO with "key" and "value"
     * properties, instead of the default Map.Entry format.
     *
     * @since 3.1 (for [databind#1419])
     */
    protected final boolean _deserializeAsPOJO;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public MapEntryDeserializer(JavaType type,
            KeyDeserializer keyDeser, ValueDeserializer<Object> valueDeser,
            TypeDeserializer valueTypeDeser)
    {
        this(type, keyDeser, valueDeser, valueTypeDeser, false);
    }

    protected MapEntryDeserializer(JavaType type,
            KeyDeserializer keyDeser, ValueDeserializer<Object> valueDeser,
            TypeDeserializer valueTypeDeser, boolean deserializeAsPOJO)
    {
        super(type);
        if (type.containedTypeCount() != 2) { // sanity check
            throw new IllegalArgumentException("Missing generic type information for "+type);
        }
        _keyDeserializer = keyDeser;
        _valueDeserializer = valueDeser;
        _valueTypeDeserializer = valueTypeDeser;
        _deserializeAsPOJO = deserializeAsPOJO;
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
        _deserializeAsPOJO = src._deserializeAsPOJO;
    }

    protected MapEntryDeserializer(MapEntryDeserializer src,
            KeyDeserializer keyDeser, ValueDeserializer<Object> valueDeser,
            TypeDeserializer valueTypeDeser)
    {
        super(src);
        _keyDeserializer = keyDeser;
        _valueDeserializer = valueDeser;
        _valueTypeDeserializer = valueTypeDeser;
        _deserializeAsPOJO = src._deserializeAsPOJO;
    }

    /**
     * Fluent factory method used to create a copy with slightly
     * different settings. When sub-classing, MUST be overridden.
     */
    @SuppressWarnings("unchecked")
    protected MapEntryDeserializer withResolved(KeyDeserializer keyDeser,
            TypeDeserializer valueTypeDeser, ValueDeserializer<?> valueDeser)
    {

        if ((_keyDeserializer == keyDeser) && (_valueDeserializer == valueDeser)
                && (_valueTypeDeserializer == valueTypeDeser)) {
            return this;
        }
        return new MapEntryDeserializer(this,
                keyDeser, (ValueDeserializer<Object>) valueDeser, valueTypeDeser);
    }

    @Override // since 2.12
    public LogicalType logicalType() {
        // Slightly tricky, could consider POJO too?
        return LogicalType.Map;
    }

    /*
    /**********************************************************************
    /* Validation, post-processing (ResolvableDeserializer)
    /**********************************************************************
     */

    /**
     * Method called to finalize setup of this deserializer,
     * when it is known for which property deserializer is needed for.
     */
    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property)
    {
        // [databind#1419]: Check if property has @JsonFormat(shape=OBJECT/POJO)
        boolean deserializeAsPOJO = _deserializeAsPOJO;
        if (property != null) {
            JsonFormat.Value format = ctxt.getAnnotationIntrospector()
                    .findFormat(ctxt.getConfig(), property.getMember());
            if (format != null) {
                if ((format.getShape() == JsonFormat.Shape.POJO)
                        || (format.getShape() == JsonFormat.Shape.OBJECT)) {
                    deserializeAsPOJO = true;
                }
            }
        }

        KeyDeserializer kd = _keyDeserializer;
        if (kd == null) {
            kd = ctxt.findKeyDeserializer(_containerType.containedType(0), property);
        } else {
            if (kd instanceof ContextualKeyDeserializer ckd) {
                kd = ckd.createContextual(ctxt, property);
            }
        }
        ValueDeserializer<?> vd = _valueDeserializer;
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

        MapEntryDeserializer deser = withResolved(kd, vtd, vd);
        if (deserializeAsPOJO != _deserializeAsPOJO) {
            return new MapEntryDeserializer(_containerType, kd,
                    (ValueDeserializer<Object>) vd, vtd, deserializeAsPOJO);
        }
        return deser;
    }

    /*
    /**********************************************************************
    /* ContainerDeserializerBase API
    /**********************************************************************
     */

    @Override
    public JavaType getContentType() {
        return _containerType.containedType(1);
    }

    @Override
    public ValueDeserializer<Object> getContentDeserializer() {
        return _valueDeserializer;
    }

    // 31-May-2020, tatu: Should probably define but we don't have it yet
//    public ValueInstantiator getValueInstantiator() { }

    /*
    /**********************************************************************
    /* ValueDeserializer API
    /**********************************************************************
     */

    @SuppressWarnings("unchecked")
    @Override
    public Map.Entry<Object,Object> deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        // [databind#1419]: If deserializing as POJO with "key" and "value" properties
        if (_deserializeAsPOJO) {
            return _deserializeAsPOJO(p, ctxt);
        }

        // Ok: must point to START_OBJECT, PROPERTY_NAME or END_OBJECT
        JsonToken t = p.currentToken();
        if (t == JsonToken.START_OBJECT) {
            t = p.nextToken();
        } else if (t != JsonToken.PROPERTY_NAME && t != JsonToken.END_OBJECT) {
            // Empty array, or single-value wrapped in array?
            if (t == JsonToken.START_ARRAY) {
                return _deserializeFromArray(p, ctxt);
            }
            return (Map.Entry<Object,Object>) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
        }
        if (t != JsonToken.PROPERTY_NAME) {
            if (t == JsonToken.END_OBJECT) {
                return ctxt.reportInputMismatch(this,
                        "Cannot deserialize a Map.Entry out of empty JSON Object");
            }
            return (Map.Entry<Object,Object>) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
        }

        final KeyDeserializer keyDes = _keyDeserializer;
        final ValueDeserializer<Object> valueDes = _valueDeserializer;
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
            if (t == JsonToken.PROPERTY_NAME) { // most likely
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

    /**
     * Helper method to deserialize Map.Entry as POJO with "key" and "value" properties.
     *
     * @since 3.1 (for [databind#1419])
     */
    protected Map.Entry<Object,Object> _deserializeAsPOJO(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        JsonToken t = p.currentToken();
        if (t == JsonToken.START_OBJECT) {
            t = p.nextToken();
        } else if (t != JsonToken.PROPERTY_NAME && t != JsonToken.END_OBJECT) {
            if (t == JsonToken.START_ARRAY) {
                return _deserializeFromArray(p, ctxt);
            }
            return (Map.Entry<Object,Object>) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
        }

        final KeyDeserializer keyDes = _keyDeserializer;
        final ValueDeserializer<Object> valueDes = _valueDeserializer;
        final TypeDeserializer typeDeser = _valueTypeDeserializer;

        Object key = null;
        Object value = null;

        // Read properties "key" and "value"
        while (t == JsonToken.PROPERTY_NAME) {
            String propName = p.currentName();
            t = p.nextToken(); // move to value

            if ("key".equals(propName)) {
                // Deserialize key
                if (t == JsonToken.VALUE_NULL) {
                    key = keyDes.deserializeKey(null, ctxt);
                } else if (t.isScalarValue()) {
                    key = keyDes.deserializeKey(p.getText(), ctxt);
                } else {
                    ctxt.reportInputMismatch(this,
                            "Can not deserialize Map.Entry key from non-scalar JSON value");
                }
            } else if ("value".equals(propName)) {
                // Deserialize value
                try {
                    if (t == JsonToken.VALUE_NULL) {
                        value = valueDes.getNullValue(ctxt);
                    } else if (typeDeser == null) {
                        value = valueDes.deserialize(p, ctxt);
                    } else {
                        value = valueDes.deserializeWithType(p, ctxt, typeDeser);
                    }
                } catch (Exception e) {
                    wrapAndThrow(ctxt, e, Map.Entry.class, propName);
                }
            } else {
                // Unknown property: check if we should fail or skip
                handleUnknownProperty(p, ctxt, _containerType.getRawClass(), propName);
            }

            t = p.nextToken(); // move to next property or END_OBJECT
        }

        if (t != JsonToken.END_OBJECT) {
            ctxt.reportInputMismatch(this,
                    "Problem binding JSON into Map.Entry: unexpected content: "+t);
        }

        return new AbstractMap.SimpleEntry<Object,Object>(key, value);
    }

    @Override
    public Map.Entry<Object,Object> deserialize(JsonParser p, DeserializationContext ctxt,
            Map.Entry<Object,Object> result) throws JacksonException
    {
        throw new IllegalStateException("Cannot update Map.Entry values");
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws JacksonException
    {
        // In future could check current token... for now this should be enough:
        return typeDeserializer.deserializeTypedFromObject(p, ctxt);
    }
}
