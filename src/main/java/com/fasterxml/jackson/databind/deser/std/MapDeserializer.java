package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.deser.DeserializerCache;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.impl.PropertyBasedCreator;
import com.fasterxml.jackson.databind.deser.impl.PropertyValueBuffer;
import com.fasterxml.jackson.databind.deser.std.ContainerDeserializerBase;
import com.fasterxml.jackson.databind.introspect.AnnotatedWithParams;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.util.ArrayBuilders;

/**
 * Basic serializer that can take Json "Object" structure and
 * construct a {@link java.util.Map} instance, with typed contents.
 *<p>
 * Note: for untyped content (one indicated by passing Object.class
 * as the type), {@link UntypedObjectDeserializer} is used instead.
 * It can also construct {@link java.util.Map}s, but not with specific
 * POJO types, only other containers and primitives/wrappers.
 */
@JacksonStdImpl
public class MapDeserializer
    extends ContainerDeserializerBase<Map<Object,Object>>
    implements ResolvableDeserializer
{
    // // Configuration: typing, deserializers

    protected final JavaType _mapType;

    /**
     * Key deserializer used, if not null. If null, String from JSON
     * content is used as is.
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
    
    // // Instance construction settings:

    protected final ValueInstantiator _valueInstantiator;

    protected final boolean _hasDefaultCreator;
    
    /**
     * If the Map is to be instantiated using non-default constructor
     * or factory method
     * that takes one or more named properties as argument(s),
     * this creator is used for instantiation.
     */
    protected PropertyBasedCreator _propertyBasedCreator;    
    
    /**
     * Deserializer that is used iff delegate-based creator is
     * to be used for deserializing from JSON Object.
     */
    protected JsonDeserializer<Object> _delegateDeserializer;
    
    // // Any properties to ignore if seen?
    
    protected HashSet<String> _ignorableProperties;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public MapDeserializer(JavaType mapType, ValueInstantiator valueInstantiator,
            KeyDeserializer keyDeser, JsonDeserializer<Object> valueDeser,
            TypeDeserializer valueTypeDeser)
    {
        super(Map.class);
        _mapType = mapType;
        _keyDeserializer = keyDeser;
        _valueDeserializer = valueDeser;
        _valueTypeDeserializer = valueTypeDeser;
        _valueInstantiator = valueInstantiator;
        _hasDefaultCreator = valueInstantiator.canCreateUsingDefault();
    }

    /**
     * Copy-constructor that can be used by sub-classes to allow
     * copy-on-write styling copying of settings of an existing instance.
     */
    protected MapDeserializer(MapDeserializer src)
    {
        super(src._valueClass);
        _mapType = src._mapType;
        _keyDeserializer = src._keyDeserializer;
        _valueDeserializer = src._valueDeserializer;
        _valueTypeDeserializer = src._valueTypeDeserializer;
        _valueInstantiator = src._valueInstantiator;
        _propertyBasedCreator = src._propertyBasedCreator;
        _delegateDeserializer = src._delegateDeserializer;
        _hasDefaultCreator = src._hasDefaultCreator;
        // should we make a copy here?
        _ignorableProperties = src._ignorableProperties;
    }

    public void setIgnorableProperties(String[] ignorable)
    {
        _ignorableProperties = (ignorable == null || ignorable.length == 0) ?
            null : ArrayBuilders.arrayToSet(ignorable);
    }

    /*
    /**********************************************************
    /* Validation, post-processing (ResolvableDeserializer)
    /**********************************************************
     */

    /**
     * Method called to finalize setup of this deserializer,
     * after deserializer itself has been registered. This
     * is needed to handle recursive and transitive dependencies.
     */
    @Override
    public void resolve(DeserializationConfig config, DeserializerCache provider)
        throws JsonMappingException
    {
        // May need to resolve types for delegate- and/or property-based creators:
        if (_valueInstantiator.canCreateUsingDelegate()) {
            JavaType delegateType = _valueInstantiator.getDelegateType(config);
            if (delegateType == null) {
                throw new IllegalArgumentException("Invalid delegate-creator definition for "+_mapType
                        +": value instantiator ("+_valueInstantiator.getClass().getName()
                        +") returned true for 'canCreateUsingDelegate()', but null for 'getDelegateType()'");
            }
            AnnotatedWithParams delegateCreator = _valueInstantiator.getDelegateCreator();
            // Need to create a temporary property to allow contextual deserializers:
            // Note: unlike BeanDeserializer, we don't have an AnnotatedClass around; hence no annotations passed
            BeanProperty.Std property = new BeanProperty.Std(null,
                    delegateType, null, delegateCreator);
            _delegateDeserializer = findDeserializer(config, provider, delegateType, property);
        }
        if (_valueInstantiator.canCreateFromObjectWith()) {
            SettableBeanProperty[] creatorProps = _valueInstantiator.getFromObjectArguments(config);
            _propertyBasedCreator = new PropertyBasedCreator(_valueInstantiator, creatorProps);
            for (SettableBeanProperty prop : creatorProps) {
                if (!prop.hasValueDeserializer()) {
                    _propertyBasedCreator.assignDeserializer(prop, findDeserializer(config, provider, prop.getType(), prop));
                }
            }
        }
    }
    
    /*
    /**********************************************************
    /* ContainerDeserializerBase API
    /**********************************************************
     */

    @Override
    public JavaType getContentType() {
        return _mapType.getContentType();
    }

    @Override
    public JsonDeserializer<Object> getContentDeserializer() {
        return _valueDeserializer;
    }
    
    /*
    /**********************************************************
    /* JsonDeserializer API
    /**********************************************************
     */

    @Override
    @SuppressWarnings("unchecked")
    public Map<Object,Object> deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        if (_propertyBasedCreator != null) {
            return _deserializeUsingCreator(jp, ctxt);
        }
        if (_delegateDeserializer != null) {
            return (Map<Object,Object>) _valueInstantiator.createUsingDelegate(ctxt,
                    _delegateDeserializer.deserialize(jp, ctxt));
        }
        if (!_hasDefaultCreator) {
            throw ctxt.instantiationException(getMapClass(), "No default constructor found");
        }
        // Ok: must point to START_OBJECT, FIELD_NAME or END_OBJECT
        JsonToken t = jp.getCurrentToken();
        if (t != JsonToken.START_OBJECT && t != JsonToken.FIELD_NAME && t != JsonToken.END_OBJECT) {
            // [JACKSON-620] (empty) String may be ok however:
            if (t == JsonToken.VALUE_STRING) {
                return (Map<Object,Object>) _valueInstantiator.createFromString(ctxt, jp.getText());
            }
            throw ctxt.mappingException(getMapClass());
        }
        final Map<Object,Object> result = (Map<Object,Object>) _valueInstantiator.createUsingDefault(ctxt);
        _readAndBind(jp, ctxt, result);
        return result;
    }

    @Override
    public Map<Object,Object> deserialize(JsonParser jp, DeserializationContext ctxt,
            Map<Object,Object> result)
        throws IOException, JsonProcessingException
    {
        // Ok: must point to START_OBJECT or FIELD_NAME
        JsonToken t = jp.getCurrentToken();
        if (t != JsonToken.START_OBJECT && t != JsonToken.FIELD_NAME) {
            throw ctxt.mappingException(getMapClass());
        }
        _readAndBind(jp, ctxt, result);
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
    
    /*
    /**********************************************************
    /* Other public accessors
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    public final Class<?> getMapClass() { return (Class<Map<Object,Object>>) _mapType.getRawClass(); }

    @Override public JavaType getValueType() { return _mapType; }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    protected final void _readAndBind(JsonParser jp, DeserializationContext ctxt,
                                      Map<Object,Object> result)
        throws IOException, JsonProcessingException
    {
        JsonToken t = jp.getCurrentToken();
        if (t == JsonToken.START_OBJECT) {
            t = jp.nextToken();
        }
        final KeyDeserializer keyDes = _keyDeserializer;
        final JsonDeserializer<Object> valueDes = _valueDeserializer;
        final TypeDeserializer typeDeser = _valueTypeDeserializer;
        for (; t == JsonToken.FIELD_NAME; t = jp.nextToken()) {
            // Must point to field name
            String fieldName = jp.getCurrentName();
            Object key = keyDes.deserializeKey(fieldName, ctxt);
            // And then the value...
            t = jp.nextToken();
            if (_ignorableProperties != null && _ignorableProperties.contains(fieldName)) {
                jp.skipChildren();
                continue;
            }
            // Note: must handle null explicitly here; value deserializers won't
            Object value;            
            if (t == JsonToken.VALUE_NULL) {
                value = null;
            } else if (typeDeser == null) {
                value = valueDes.deserialize(jp, ctxt);
            } else {
                value = valueDes.deserializeWithType(jp, ctxt, typeDeser);
            }
            /* !!! 23-Dec-2008, tatu: should there be an option to verify
             *   that there are no duplicate field names? (and/or what
             *   to do, keep-first or keep-last)
             */
            result.put(key, value);
        }
    }

    @SuppressWarnings("unchecked") 
    public Map<Object,Object> _deserializeUsingCreator(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        final PropertyBasedCreator creator = _propertyBasedCreator;
        PropertyValueBuffer buffer = creator.startBuilding(jp, ctxt);

        JsonToken t = jp.getCurrentToken();
        if (t == JsonToken.START_OBJECT) {
            t = jp.nextToken();
        }
        final JsonDeserializer<Object> valueDes = _valueDeserializer;
        final TypeDeserializer typeDeser = _valueTypeDeserializer;
        for (; t == JsonToken.FIELD_NAME; t = jp.nextToken()) {
            String propName = jp.getCurrentName();
            t = jp.nextToken(); // to get to value
            if (_ignorableProperties != null && _ignorableProperties.contains(propName)) {
                jp.skipChildren(); // and skip it (in case of array/object)
                continue;
            }
            // creator property?
            SettableBeanProperty prop = creator.findCreatorProperty(propName);
            if (prop != null) {
                // Last property to set?
                Object value = prop.deserialize(jp, ctxt);
                if (buffer.assignParameter(prop.getPropertyIndex(), value)) {
                    jp.nextToken();
                    Map<Object,Object> result;
                    try {
                        result = (Map<Object,Object>)creator.build(ctxt, buffer);
                    } catch (Exception e) {
                        wrapAndThrow(e, _mapType.getRawClass());
                        return null;
                    }
                    _readAndBind(jp, ctxt, result);
                    return result;
                }
                continue;
            }
            // other property? needs buffering
            String fieldName = jp.getCurrentName();
            Object key = _keyDeserializer.deserializeKey(fieldName, ctxt);
            Object value;            
            if (t == JsonToken.VALUE_NULL) {
                value = null;
            } else if (typeDeser == null) {
                value = valueDes.deserialize(jp, ctxt);
            } else {
                value = valueDes.deserializeWithType(jp, ctxt, typeDeser);
            }
            buffer.bufferMapProperty(key, value);
        }
        // end of JSON object?
        // if so, can just construct and leave...
        try {
            return (Map<Object,Object>)creator.build(ctxt, buffer);
        } catch (Exception e) {
            wrapAndThrow(e, _mapType.getRawClass());
            return null;
        }
    }

    // note: copied form BeanDeserializer; should try to share somehow...
    protected void wrapAndThrow(Throwable t, Object ref)
        throws IOException
    {
        // to handle StackOverflow:
        while (t instanceof InvocationTargetException && t.getCause() != null) {
            t = t.getCause();
        }
        // Errors and "plain" IOExceptions to be passed as is
        if (t instanceof Error) {
            throw (Error) t;
        }
        // ... except for mapping exceptions
        if (t instanceof IOException && !(t instanceof JsonMappingException)) {
            throw (IOException) t;
        }
        throw JsonMappingException.wrapWithPath(t, ref, null);
    }

}
