package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.Collection;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.introspect.AnnotatedWithParams;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

/**
 * Specifically optimized version for {@link java.util.Collection}s
 * that contain String values; reason is that this is a very common
 * type and we can make use of the fact that Strings are final.
 */
@JacksonStdImpl
public final class StringCollectionDeserializer
    extends ContainerDeserializerBase<Collection<String>>
    implements ContextualDeserializer
{
    private static final long serialVersionUID = 1L;

    // // Configuration

    protected final JavaType _collectionType;
    
    /**
     * Value deserializer to use, if NOT the standard one
     * (if it is, will be null).
     */
    protected final JsonDeserializer<String> _valueDeserializer;

    // // Instance construction settings:
    
    /**
     * Instantiator used in case custom handling is needed for creation.
     */
    protected final ValueInstantiator _valueInstantiator;

    /**
     * Deserializer that is used iff delegate-based creator is
     * to be used for deserializing from JSON Object.
     */
    protected final JsonDeserializer<Object> _delegateDeserializer;

    // NOTE: no PropertyBasedCreator, as JSON Arrays have no properties

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    public StringCollectionDeserializer(JavaType collectionType,
            JsonDeserializer<?> valueDeser, ValueInstantiator valueInstantiator)
    {
        this(collectionType, valueInstantiator, null, valueDeser);
    }

    @SuppressWarnings("unchecked")
    protected StringCollectionDeserializer(JavaType collectionType,
            ValueInstantiator valueInstantiator, JsonDeserializer<?> delegateDeser,
            JsonDeserializer<?> valueDeser)
    {
        super(collectionType);
        _collectionType = collectionType;
        _valueDeserializer = (JsonDeserializer<String>) valueDeser;
        _valueInstantiator = valueInstantiator;
        _delegateDeserializer = (JsonDeserializer<Object>) delegateDeser;
    }

    protected StringCollectionDeserializer withResolved(JsonDeserializer<?> delegateDeser,
            JsonDeserializer<?> valueDeser)
    {
        if ((_valueDeserializer == valueDeser) && (_delegateDeserializer == delegateDeser)) {
            return this;
        }
        return new StringCollectionDeserializer(_collectionType,
                _valueInstantiator, delegateDeser, valueDeser);
    }

    /*
    /**********************************************************
    /* Validation, post-processing
    /**********************************************************
     */
    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property) throws JsonMappingException
    {
        // May need to resolve types for delegate-based creators:
        JsonDeserializer<Object> delegate = null;
        if (_valueInstantiator != null) {
            AnnotatedWithParams delegateCreator = _valueInstantiator.getDelegateCreator();
            if (delegateCreator != null) {
                JavaType delegateType = _valueInstantiator.getDelegateType(ctxt.getConfig());
                delegate = findDeserializer(ctxt, delegateType, property);
            }
        }
        JsonDeserializer<?> valueDeser = _valueDeserializer;
        if (valueDeser == null) {
            // #125: May have a content converter
            valueDeser = findConvertingContentDeserializer(ctxt, property, valueDeser);
            if (valueDeser == null) {
            // And we may also need to get deserializer for String
                valueDeser = ctxt.findContextualValueDeserializer( _collectionType.getContentType(), property);
            }
        } else { // if directly assigned, probably not yet contextual, so:
            valueDeser = ctxt.handleSecondaryContextualization(valueDeser, property);
        } 
        if (isDefaultDeserializer(valueDeser)) {
            valueDeser = null;
        }
        return withResolved(delegate, valueDeser);
    }
    
    /*
    /**********************************************************
    /* ContainerDeserializerBase API
    /**********************************************************
     */

    @Override
    public JavaType getContentType() {
        return _collectionType.getContentType();
    }

    @SuppressWarnings("unchecked")
    @Override
    public JsonDeserializer<Object> getContentDeserializer() {
        JsonDeserializer<?> deser = _valueDeserializer;
        return (JsonDeserializer<Object>) deser;
    }
    
    /*
    /**********************************************************
    /* JsonDeserializer API
    /**********************************************************
     */
    
    @SuppressWarnings("unchecked")
    @Override
    public Collection<String> deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException
    {
        if (_delegateDeserializer != null) {
            return (Collection<String>) _valueInstantiator.createUsingDelegate(ctxt,
                    _delegateDeserializer.deserialize(jp, ctxt));
        }
        final Collection<String> result = (Collection<String>) _valueInstantiator.createUsingDefault(ctxt);
        return deserialize(jp, ctxt, result);
    }

    @Override
    public Collection<String> deserialize(JsonParser jp, DeserializationContext ctxt,
            Collection<String> result)
        throws IOException
    {
        // Ok: must point to START_ARRAY
        if (!jp.isExpectedStartArrayToken()) {
            return handleNonArray(jp, ctxt, result);
        }

        if (_valueDeserializer != null) {
            return deserializeUsingCustom(jp, ctxt, result, _valueDeserializer);
        }
        JsonToken t;

        try {
            while ((t = jp.nextToken()) != JsonToken.END_ARRAY) {
                String value;
                if (t == JsonToken.VALUE_STRING) {
                    value = jp.getText();
                } else if (t == JsonToken.VALUE_NULL) {
                    value = null;
                } else {
                    value = _parseString(jp, ctxt);
                }
                result.add(value);
            }
        } catch (Exception e) {
            throw JsonMappingException.wrapWithPath(e, result, result.size());
        }
        return result;
    }
    
    private Collection<String> deserializeUsingCustom(JsonParser jp, DeserializationContext ctxt,
            Collection<String> result, final JsonDeserializer<String> deser) throws IOException
    {
        JsonToken t;
        while ((t = jp.nextToken()) != JsonToken.END_ARRAY) {
            String value;

            if (t == JsonToken.VALUE_NULL) {
                value = deser.getNullValue();
            } else {
                value = deser.deserialize(jp, ctxt);
            }
            result.add(value);
        }
        return result;
    }
    
    @Override
    public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException {
        // In future could check current token... for now this should be enough:
        return typeDeserializer.deserializeTypedFromArray(jp, ctxt);
    }

    /**
     * Helper method called when current token is no START_ARRAY. Will either
     * throw an exception, or try to handle value as if member of implicit
     * array, depending on configuration.
     */
    private final Collection<String> handleNonArray(JsonParser jp, DeserializationContext ctxt, Collection<String> result) throws IOException
    {
        // [JACKSON-526]: implicit arrays from single values?
        if (!ctxt.isEnabled(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)) {
            throw ctxt.mappingException(_collectionType.getRawClass());
        }
        // Strings are one of "native" (intrinsic) types, so there's never type deserializer involved
        JsonDeserializer<String> valueDes = _valueDeserializer;
        JsonToken t = jp.getCurrentToken();

        String value;
        
        if (t == JsonToken.VALUE_NULL) {
            value = (valueDes == null) ? null : valueDes.getNullValue();
        } else {
            value = (valueDes == null) ? _parseString(jp, ctxt) : valueDes.deserialize(jp, ctxt);
        }
        result.add(value);
        return result;
    }
}
