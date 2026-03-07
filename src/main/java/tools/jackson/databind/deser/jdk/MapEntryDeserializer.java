package tools.jackson.databind.deser.jdk;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.deser.*;
import tools.jackson.databind.deser.std.ContainerDeserializerBase;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.type.LogicalType;
import tools.jackson.databind.util.ClassUtil;

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

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public MapEntryDeserializer(JavaType type,
            KeyDeserializer keyDeser, ValueDeserializer<Object> valueDeser,
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

    protected MapEntryDeserializer(MapEntryDeserializer src,
            KeyDeserializer keyDeser, ValueDeserializer<Object> valueDeser,
            TypeDeserializer valueTypeDeser)
    {
        super(src);
        _keyDeserializer = keyDeser;
        _valueDeserializer = valueDeser;
        _valueTypeDeserializer = valueTypeDeser;
    }

    /**
     * Factory method for constructing initial (non-contextual) instances.
     *
     * @since 3.1
     */
    @SuppressWarnings("unchecked")
    public static ValueDeserializer<Object> construct(DeserializationContext ctxt,
            JavaType entryType,
            boolean pojoWrappedFormat)
    {
        ValueDeserializer<?> deser = pojoWrappedFormat
                ? constructAsPOJO(ctxt, entryType)
                : constructDefault(ctxt, entryType);
        return (ValueDeserializer<Object>) deser;
    }

    /**
     * Factory method for initial instance using the default ("natural") format,
     * in which an Object with a single entry is expected.
     *
     * @since 3.1
     */
    protected static MapEntryDeserializer constructDefault(DeserializationContext ctxt,
            JavaType entryType)
    {
        final JavaType keyType = entryType.containedTypeOrUnknown(0);
        final JavaType valueType = entryType.containedTypeOrUnknown(1);
        // 28-Apr-2015, tatu: TypeFactory does it all for us already so
        // 04-Jan-2025, tatu: Or does is? None of tests fails if following was
        //    removed.
        TypeDeserializer vts = (TypeDeserializer) valueType.getTypeHandler();
        if (vts == null) {
            vts = ctxt.findTypeDeserializer(valueType);
        }
        @SuppressWarnings("unchecked")
        ValueDeserializer<Object> valueDeser = (ValueDeserializer<Object>) valueType.getValueHandler();
        KeyDeserializer keyDes = (KeyDeserializer) keyType.getValueHandler();
        return new MapEntryDeserializer(entryType, keyDes, valueDeser, vts);
    }

    /**
     * Factory method for initial instance using the alternative ("as POJO" or
     * "POJO-wrapped") format, in which an Object with 2 separate entries -- "key"
     * and "value" -- are expected.
     *
     * @since 3.1
     */
    protected static POJOWrappedDeserializer constructAsPOJO(DeserializationContext ctxt,
            JavaType entryType)
    {
        return new POJOWrappedDeserializer(entryType);
    }

    /**
     * Fluent factory method used to create a copy with slightly
     * different settings.
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
        return LogicalType.Map;
    }

    /*
    /**********************************************************************
    /* Validation, post-processing
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
        // [databind#1419]: Check if property has @JsonFormat(shape=POJO)
        if (Boolean.TRUE.equals(_shouldDeserializeAsPOJO(ctxt, property))) {
            return constructAsPOJO(ctxt, _containerType)
                    ._createContextual2(ctxt, property);
        }
        return _createContextual2(ctxt, property);
    }

    // Method called from "createContextual()"s after determining if
    // "shape-shifting" needed (and has been performed)
    protected ValueDeserializer<?> _createContextual2(DeserializationContext ctxt,
            BeanProperty property)
    {
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
        return withResolved(kd, vtd, vd);
    }

    protected static Boolean _shouldDeserializeAsPOJO(DeserializationContext ctxt,
            BeanProperty property)
    {
        if (property != null) {
            JsonFormat.Value format = property.findPropertyFormat(ctxt.getConfig(), Map.Entry.class);

            switch (format.getShape()) {
            case NATURAL:
                return false;
            case POJO:
                return true;
            default: // fall through
            }
        }
        return null;
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
                        "Cannot deserialize a `Map.Entry` out of empty Object");
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

    /*
    /**********************************************************************
    /* Alternate handlers
    /**********************************************************************
     */

    /**
     * @since 3.1
     */
    protected static class POJOWrappedDeserializer
        extends StdDeserializer<Map.Entry<Object, Object>>
    {
        protected final ValueDeserializer<Object> _keyDeserializer;
        protected final TypeDeserializer _keyTypeDeserializer;

        protected final ValueDeserializer<Object> _valueDeserializer;
        protected final TypeDeserializer _valueTypeDeserializer;

        /*
        /**********************************************************************
        /* Life-cycle
        /**********************************************************************
         */

        public POJOWrappedDeserializer(JavaType type)
        {
            super(type);
            _keyDeserializer = null;
            _keyTypeDeserializer = null;
            _valueDeserializer = null;
            _valueTypeDeserializer = null;
        }
    
        protected POJOWrappedDeserializer(POJOWrappedDeserializer src,
                ValueDeserializer<Object> keyDeser, TypeDeserializer valueTypeDeser,
                ValueDeserializer<Object> valueDeser, TypeDeserializer keyTypeDeser)
        {
            super(src);
            _keyDeserializer = keyDeser;
            _keyTypeDeserializer = keyTypeDeser;
            _valueDeserializer = valueDeser;
            _valueTypeDeserializer = valueTypeDeser;
        }

        /**
         * Fluent factory method used to create a copy with slightly
         * different settings.
         */
        @SuppressWarnings("unchecked")
        protected POJOWrappedDeserializer withResolved(ValueDeserializer<?> keyDeser,
                TypeDeserializer keyTypeDeser,
                ValueDeserializer<?> valueDeser, TypeDeserializer valueTypeDeser)
        {
            if ((_keyDeserializer == keyDeser)
                    && (_keyTypeDeserializer == keyTypeDeser)
                    && (_valueDeserializer == valueDeser)
                    && (_valueTypeDeserializer == valueTypeDeser)) {
                return this;
            }
            return new POJOWrappedDeserializer(this,
                    (ValueDeserializer<Object>) keyDeser, keyTypeDeser,
                    (ValueDeserializer<Object>) valueDeser, valueTypeDeser);
        }

        @Override
        public LogicalType logicalType() {
            return LogicalType.POJO;
        }

        /*
        /**********************************************************************
        /* Validation, post-processing
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
            // May override back to standard too:
            if (Boolean.FALSE.equals(_shouldDeserializeAsPOJO(ctxt, property))) {
                return constructAsPOJO(ctxt, _valueType)
                        ._createContextual2(ctxt, property);
            }
            return _createContextual2(ctxt, property);
        }

        // Method called from "createContextual()"s after determining if
        // "shape-shifting" needed (and has been performed)
        protected ValueDeserializer<?> _createContextual2(DeserializationContext ctxt,
                BeanProperty property)
        {
            ValueDeserializer<?> kd = _keyDeserializer;
            kd = findConvertingContentDeserializer(ctxt, property, kd);
            JavaType keyType = _valueType.containedTypeOrUnknown(0);
            if (kd == null) {
                kd = ctxt.findContextualValueDeserializer(keyType, property);
            } else { // if directly assigned, probably not yet contextual, so:
                kd = ctxt.handleSecondaryContextualization(kd, property, keyType);
            }
            TypeDeserializer ktd = _keyTypeDeserializer;
            if (ktd != null) {
                ktd = ktd.forProperty(property);
            }

            ValueDeserializer<?> vd = _valueDeserializer;
            vd = findConvertingContentDeserializer(ctxt, property, vd);
            JavaType valueType = _valueType.containedType(1);
            if (vd == null) {
                vd = ctxt.findContextualValueDeserializer(valueType, property);
            } else { // if directly assigned, probably not yet contextual, so:
                vd = ctxt.handleSecondaryContextualization(vd, property, valueType);
            }
            TypeDeserializer vtd = _valueTypeDeserializer;
            if (vtd != null) {
                vtd = vtd.forProperty(property);
            }
            return withResolved(kd, ktd, vd, vtd);
        }

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
            JsonToken t = p.currentToken();
            if (t == JsonToken.START_OBJECT) {
                t = p.nextToken();
            } else if (t != JsonToken.PROPERTY_NAME && t != JsonToken.END_OBJECT) {
                if (t == JsonToken.START_ARRAY) {
                    return _deserializeFromArray(p, ctxt);
                }
                return (Map.Entry<Object,Object>) ctxt.handleUnexpectedToken(_valueType, p);
            }

            Object key = null;
            Object value = null;

            // Read properties "key" and "value"
            while (t == JsonToken.PROPERTY_NAME) {
                String propName = p.currentName();
                t = p.nextToken(); // move to value

                if ("key".equals(propName)) {
                    try {
                        if (t == JsonToken.VALUE_NULL) {
                            key = _keyDeserializer.getNullValue(ctxt);
                        } else if (_keyTypeDeserializer != null) {
                            key = _keyDeserializer.deserializeWithType(p, ctxt, _keyTypeDeserializer);
                        } else {
                            key = _keyDeserializer.deserialize(p, ctxt);
                        }
                    } catch (Exception e) {
                        wrapAndThrow(ctxt, e, Map.Entry.class, propName);
                    }
                } else if ("value".equals(propName)) {
                    try {
                        if (t == JsonToken.VALUE_NULL) {
                            value = _valueDeserializer.getNullValue(ctxt);
                        } else if (_valueTypeDeserializer != null) {
                            value = _valueDeserializer.deserializeWithType(p, ctxt, _valueTypeDeserializer);
                        } else {
                            value = _valueDeserializer.deserialize(p, ctxt);
                        }
                    } catch (Exception e) {
                        wrapAndThrow(ctxt, e, Map.Entry.class, propName);
                    }
                } else {
                    // Unknown property: check if we should fail or skip
                    handleUnknownProperty(p, ctxt, _valueType, propName);
                }

                t = p.nextToken(); // move to next property or END_OBJECT
            }

            if (t != JsonToken.END_OBJECT) {
                ctxt.reportInputMismatch(this,
                        "Problem deserializing `Map.Entry`; unexpected content after Object value: "
                                +JsonToken.valueDescFor(t));
            }

            return new AbstractMap.SimpleEntry<>(key, value);
        }
        
        @Override
        public Map.Entry<Object,Object> deserialize(JsonParser p, DeserializationContext ctxt,
                Map.Entry<Object,Object> result) throws JacksonException
        {
            throw new IllegalStateException("Cannot update `Map.Entry` values");
        }

        @Override
        public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
                TypeDeserializer typeDeserializer)
            throws JacksonException
        {
            // In future could check current token... for now this should be enough:
            return typeDeserializer.deserializeTypedFromObject(p, ctxt);
        }

        // Copied from `ContainerDeserializerBase`
        protected <BOGUS> BOGUS wrapAndThrow(DeserializationContext ctxt,
                Throwable t, Object ref, String key) throws JacksonException
        {
            while (t instanceof InvocationTargetException && t.getCause() != null) {
                t = t.getCause();
            }
            ClassUtil.throwIfError(t);
            if (!ctxt.isEnabled(DeserializationFeature.WRAP_EXCEPTIONS)) {
                ClassUtil.throwIfRTE(t);
            }
            throw DatabindException.wrapWithPath(ctxt, t,
                        new JacksonException.Reference(ref, ClassUtil.nonNull(key, "N/A")));
        }
    }
}
