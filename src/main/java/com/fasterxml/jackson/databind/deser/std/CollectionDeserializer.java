package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.UnresolvedForwardReference;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.impl.ReadableObjectId.Referring;
import com.fasterxml.jackson.databind.deser.std.ContainerDeserializerBase;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

/**
 * Basic serializer that can take JSON "Array" structure and
 * construct a {@link java.util.Collection} instance, with typed contents.
 *<p>
 * Note: for untyped content (one indicated by passing Object.class
 * as the type), {@link UntypedObjectDeserializer} is used instead.
 * It can also construct {@link java.util.List}s, but not with specific
 * POJO types, only other containers and primitives/wrappers.
 */
@JacksonStdImpl
public class CollectionDeserializer
    extends ContainerDeserializerBase<Collection<Object>>
    implements ContextualDeserializer
{
    private static final long serialVersionUID = 3917273725180652224L;

    // // Configuration

    protected final JavaType _collectionType;
    
    /**
     * Value deserializer.
     */
    protected final JsonDeserializer<Object> _valueDeserializer;

    /**
     * If element instances have polymorphic type information, this
     * is the type deserializer that can handle it
     */
    protected final TypeDeserializer _valueTypeDeserializer;

    // // Instance construction settings:
    
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

    /**
     * Constructor for context-free instances, where we do not yet know
     * which property is using this deserializer.
     */
    public CollectionDeserializer(JavaType collectionType,
            JsonDeserializer<Object> valueDeser,
            TypeDeserializer valueTypeDeser, ValueInstantiator valueInstantiator)
    {
        this(collectionType, valueDeser, valueTypeDeser, valueInstantiator, null);
    }

    /**
     * Constructor used when creating contextualized instances.
     */
    protected CollectionDeserializer(JavaType collectionType,
            JsonDeserializer<Object> valueDeser, TypeDeserializer valueTypeDeser,
            ValueInstantiator valueInstantiator,
            JsonDeserializer<Object> delegateDeser)
    {
        super(collectionType);
        _collectionType = collectionType;
        _valueDeserializer = valueDeser;
        _valueTypeDeserializer = valueTypeDeser;
        _valueInstantiator = valueInstantiator;
        _delegateDeserializer = delegateDeser;
    }

    /**
     * Copy-constructor that can be used by sub-classes to allow
     * copy-on-write styling copying of settings of an existing instance.
     */
    protected CollectionDeserializer(CollectionDeserializer src)
    {
        super(src._collectionType);
        _collectionType = src._collectionType;
        _valueDeserializer = src._valueDeserializer;
        _valueTypeDeserializer = src._valueTypeDeserializer;
        _valueInstantiator = src._valueInstantiator;
        _delegateDeserializer = src._delegateDeserializer;
    }

    /**
     * Fluent-factory method call to construct contextual instance.
     */
    @SuppressWarnings("unchecked")
    protected CollectionDeserializer withResolved(JsonDeserializer<?> dd,
            JsonDeserializer<?> vd, TypeDeserializer vtd)
    {
        if ((dd == _delegateDeserializer) && (vd == _valueDeserializer) && (vtd == _valueTypeDeserializer)) {
            return this;
        }
        return new CollectionDeserializer(_collectionType,
                (JsonDeserializer<Object>) vd, vtd,
                _valueInstantiator, (JsonDeserializer<Object>) dd);
    }
    
    /*
    /**********************************************************
    /* Validation, post-processing (ResolvableDeserializer)
    /**********************************************************
     */

    /**
     * Method called to finalize setup of this deserializer,
     * when it is known for which property deserializer is needed
     * for.
     */
    @Override
    public CollectionDeserializer createContextual(DeserializationContext ctxt,
            BeanProperty property) throws JsonMappingException
    {
        // May need to resolve types for delegate-based creators:
        JsonDeserializer<Object> delegateDeser = null;
        if ((_valueInstantiator != null) && _valueInstantiator.canCreateUsingDelegate()) {
            JavaType delegateType = _valueInstantiator.getDelegateType(ctxt.getConfig());
            if (delegateType == null) {
                throw new IllegalArgumentException("Invalid delegate-creator definition for "+_collectionType
                        +": value instantiator ("+_valueInstantiator.getClass().getName()
                        +") returned true for 'canCreateUsingDelegate()', but null for 'getDelegateType()'");
            }
            delegateDeser = findDeserializer(ctxt, delegateType, property);
        }
        // also, often value deserializer is resolved here:
        JsonDeserializer<?> valueDeser = _valueDeserializer;
        // #125: May have a content converter
        valueDeser = findConvertingContentDeserializer(ctxt, property, valueDeser);
        if (valueDeser == null) {
            valueDeser = ctxt.findContextualValueDeserializer(
                    _collectionType.getContentType(), property);
        } else { // if directly assigned, probably not yet contextual, so:
            valueDeser = ctxt.handleSecondaryContextualization(valueDeser, property);
        }
        // and finally, type deserializer needs context as well
        TypeDeserializer valueTypeDeser = _valueTypeDeserializer;
        if (valueTypeDeser != null) {
            valueTypeDeser = valueTypeDeser.forProperty(property);
        }
        return withResolved(delegateDeser, valueDeser, valueTypeDeser);
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

    @Override
    public JsonDeserializer<Object> getContentDeserializer() {
        return _valueDeserializer;
    }
    
    /*
    /**********************************************************
    /* JsonDeserializer API
    /**********************************************************
     */
    
    @SuppressWarnings("unchecked")
    @Override
    public Collection<Object> deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        if (_delegateDeserializer != null) {
            return (Collection<Object>) _valueInstantiator.createUsingDelegate(ctxt,
                    _delegateDeserializer.deserialize(jp, ctxt));
        }
        /* [JACKSON-620]: empty String may be ok; bit tricky to check, however, since
         *  there is also possibility of "auto-wrapping" of single-element arrays.
         *  Hence we only accept empty String here.
         */
        if (jp.getCurrentToken() == JsonToken.VALUE_STRING) {
            String str = jp.getText();
            if (str.length() == 0) {
                return (Collection<Object>) _valueInstantiator.createFromString(ctxt, str);
            }
        }
        return deserialize(jp, ctxt, (Collection<Object>) _valueInstantiator.createUsingDefault(ctxt));
    }

    @Override
    public Collection<Object> deserialize(JsonParser jp, DeserializationContext ctxt,
            Collection<Object> result)
        throws IOException, JsonProcessingException
    {
        // Ok: must point to START_ARRAY (or equivalent)
        if (!jp.isExpectedStartArrayToken()) {
            return handleNonArray(jp, ctxt, result);
        }

        JsonDeserializer<Object> valueDes = _valueDeserializer;
        final TypeDeserializer typeDeser = _valueTypeDeserializer;
        CollectionReferringAccumulator referringAccumulator =
            (valueDes.getObjectIdReader() == null) ? null :
                new CollectionReferringAccumulator(_collectionType.getContentType().getRawClass(), result);

        JsonToken t;
        while ((t = jp.nextToken()) != JsonToken.END_ARRAY) {
            try {
                Object value;
                if (t == JsonToken.VALUE_NULL) {
                    value = valueDes.getNullValue();
                } else if (typeDeser == null) {
                    value = valueDes.deserialize(jp, ctxt);
                } else {
                    value = valueDes.deserializeWithType(jp, ctxt, typeDeser);
                }
                if (referringAccumulator != null) {
                    referringAccumulator.add(value);
                } else {
                    result.add(value);
                }
            } catch (UnresolvedForwardReference reference) {
                if (referringAccumulator == null) {
                    throw JsonMappingException
                            .from(jp, "Unresolved forward reference but no identity info.", reference);
                }
                Referring ref = referringAccumulator.handleUnresolvedReference(reference);
                reference.getRoid().appendReferring(ref);
            }
        }
        return result;
    }

    @Override
    public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws IOException, JsonProcessingException
    {
        // In future could check current token... for now this should be enough:
        return typeDeserializer.deserializeTypedFromArray(jp, ctxt);
    }

    /**
     * Helper method called when current token is no START_ARRAY. Will either
     * throw an exception, or try to handle value as if member of implicit
     * array, depending on configuration.
     */
    protected final Collection<Object> handleNonArray(JsonParser jp, DeserializationContext ctxt,
            Collection<Object> result)
        throws IOException, JsonProcessingException
    {
        // [JACKSON-526]: implicit arrays from single values?
        if (!ctxt.isEnabled(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)) {
            throw ctxt.mappingException(_collectionType.getRawClass());
        }
        JsonDeserializer<Object> valueDes = _valueDeserializer;
        final TypeDeserializer typeDeser = _valueTypeDeserializer;
        JsonToken t = jp.getCurrentToken();

        Object value;
        
        if (t == JsonToken.VALUE_NULL) {
            value = valueDes.getNullValue();
        } else if (typeDeser == null) {
            value = valueDes.deserialize(jp, ctxt);
        } else {
            value = valueDes.deserializeWithType(jp, ctxt, typeDeser);
        }
        result.add(value);
        return result;
    }

    public final static class CollectionReferringAccumulator {
        private final Class<?> _elementType;
        private final Collection<Object> _result;

        /**
         * A list of {@link CollectionReferring} to maintain ordering.
         */
        private List<CollectionReferring> _accumulator = new ArrayList<CollectionReferring>();

        public CollectionReferringAccumulator(Class<?> elementType, Collection<Object> result) {
            _elementType = elementType;
            _result = result;
        }

        public void add(Object value)
        {
            if (_accumulator.isEmpty()) {
                _result.add(value);
            } else {
                CollectionReferring ref = _accumulator.get(_accumulator.size() - 1);
                ref.next.add(value);
            }
        }

        public Referring handleUnresolvedReference(UnresolvedForwardReference reference)
        {
            CollectionReferring id = new CollectionReferring(this, reference, _elementType);
            _accumulator.add(id);
            return id;
        }

        public void resolveForwardReference(Object id, Object value) throws IOException
        {
            Iterator<CollectionReferring> iterator = _accumulator.iterator();
            // Resolve ordering after resolution of an id. This mean either:
            // 1- adding to the result collection in case of the first unresolved id.
            // 2- merge the content of the resolved id with its previous unresolved id.
            Collection<Object> previous = _result;
            while (iterator.hasNext()) {
                CollectionReferring ref = iterator.next();
                if (ref.hasId(id)) {
                    iterator.remove();
                    previous.add(value);
                    previous.addAll(ref.next);
                    return;
                }
                previous = ref.next;
            }

            throw new IllegalArgumentException("Trying to resolve a forward reference with id [" + id
                    + "] that wasn't previously seen as unresolved.");
        }
    }

    /**
     * Helper class to maintain processing order of value. The resolved
     * object associated with {@link #_id} comes before the values in
     * {@link #next}.
     */
    private final static class CollectionReferring extends Referring {
        private final CollectionReferringAccumulator _parent;
        public final List<Object> next = new ArrayList<Object>();
        
        private CollectionReferring(CollectionReferringAccumulator parent,
                UnresolvedForwardReference reference, Class<?> contentType)
        {
            super(reference, contentType);
            _parent = parent;
        }
        
        @Override
        public void handleResolvedForwardReference(Object id, Object value) throws IOException {
            _parent.resolveForwardReference(id, value);
        }
    }
}
