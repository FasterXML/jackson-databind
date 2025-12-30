package tools.jackson.databind.deser.jdk;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.core.*;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.deser.NullValueProvider;
import tools.jackson.databind.deser.ValueInstantiator;
import tools.jackson.databind.deser.std.ContainerDeserializerBase;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.AnnotatedWithParams;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.type.LogicalType;

/**
 * Specifically optimized version for {@link java.util.Collection}s
 * that contain String values; reason is that this is a very common
 * type and we can make use of the fact that Strings are final.
 */
@JacksonStdImpl
public final class StringCollectionDeserializer
    extends ContainerDeserializerBase<Collection<String>>
{
    // // Configuration

    /**
     * Value deserializer to use, if NOT the standard one
     * (if it is, will be null).
     */
    private final ValueDeserializer<String> _valueDeserializer;

    // // Instance construction settings:

    /**
     * Instantiator used in case custom handling is needed for creation.
     */
    private final ValueInstantiator _valueInstantiator;

    /**
     * Deserializer that is used iff delegate-based creator is
     * to be used for deserializing from JSON Object.
     */
    private final ValueDeserializer<Object> _delegateDeserializer;

    /**
     * Annotations defined on the actual Collection class; retained to avoid
     * re-introspection overhead during {@link #createContextual} calls.
     *
     * @since 3.1
     */
    protected transient final AnnotatedClass _classInfo;

    // NOTE: no PropertyBasedCreator, as JSON Arrays have no properties

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    // @since 3.1
    public StringCollectionDeserializer(JavaType collectionType,
            ValueDeserializer<?> valueDeser, ValueInstantiator valueInstantiator,
            AnnotatedClass classInfo)
    {
        this(collectionType, valueInstantiator, null, valueDeser, valueDeser, null, classInfo);
    }

    // @since 3.1
    @SuppressWarnings("unchecked")
    protected StringCollectionDeserializer(JavaType collectionType,
            ValueInstantiator valueInstantiator, ValueDeserializer<?> delegateDeser,
            ValueDeserializer<?> valueDeser,
            NullValueProvider nuller, Boolean unwrapSingle, AnnotatedClass classInfo)
    {
        super(collectionType, nuller, unwrapSingle);
        _valueDeserializer = (ValueDeserializer<String>) valueDeser;
        _valueInstantiator = valueInstantiator;
        _delegateDeserializer = (ValueDeserializer<Object>) delegateDeser;
        _classInfo = classInfo;
    }

    /**
     * Factory method called by {@code BasicDeserializerFactory} to create
     * an instance
     *
     * @since 3.1
     */
    public static StringCollectionDeserializer create(JavaType collectionType,
            BeanDescription.Supplier beanDescRef,
            ValueDeserializer<Object> valueDeser, ValueInstantiator valueInstantiator)
    {
        return new StringCollectionDeserializer(collectionType,
                valueDeser, valueInstantiator, beanDescRef.getClassInfo());
    }

    protected StringCollectionDeserializer withResolved(ValueDeserializer<?> delegateDeser,
            ValueDeserializer<?> valueDeser,
            NullValueProvider nuller, Boolean unwrapSingle)
    {
        if ((Objects.equals(_unwrapSingle, unwrapSingle)) && (_nullProvider == nuller)
                && (_valueDeserializer == valueDeser) && (_delegateDeserializer == delegateDeser)) {
            return this;
        }
        return new StringCollectionDeserializer(_containerType, _valueInstantiator,
                delegateDeser, valueDeser, nuller, unwrapSingle, _classInfo);
    }

    @Override
    public boolean isCachable() {
        // 26-Mar-2015, tatu: Important: prevent caching if custom deserializers via annotations
        //    are involved
        return (_valueDeserializer == null) && (_delegateDeserializer == null);
    }

    @Override
    public LogicalType logicalType() {
        return LogicalType.Collection;
    }

    /*
    /**********************************************************************
    /* Validation, post-processing
    /**********************************************************************
     */

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property)
    {
        // May need to resolve types for delegate-based creators:
        ValueDeserializer<Object> delegate = null;
        if (_valueInstantiator != null) {
            // [databind#2324]: check both array-delegating and delegating
            AnnotatedWithParams delegateCreator = _valueInstantiator.getArrayDelegateCreator();
            if (delegateCreator != null) {
                JavaType delegateType = _valueInstantiator.getArrayDelegateType(ctxt.getConfig());
                delegate = findDeserializer(ctxt, delegateType, property);
            } else if ((delegateCreator = _valueInstantiator.getDelegateCreator()) != null) {
                JavaType delegateType = _valueInstantiator.getDelegateType(ctxt.getConfig());
                delegate = findDeserializer(ctxt, delegateType, property);
            }
        }
        ValueDeserializer<?> valueDeser = _valueDeserializer;
        final JavaType valueType = _containerType.getContentType();
        if (valueDeser == null) {
            // [databind#125]: May have a content converter
            valueDeser = findConvertingContentDeserializer(ctxt, property, valueDeser);
            if (valueDeser == null) {
            // And we may also need to get deserializer for String
                valueDeser = ctxt.findContextualValueDeserializer(valueType, property);
            }
        } else { // if directly assigned, probably not yet contextual, so:
            valueDeser = ctxt.handleSecondaryContextualization(valueDeser, property, valueType);
        }
        // 11-Dec-2015, tatu: Should we pass basic `Collection.class`, or more refined? Mostly
        //   comes down to "List vs Collection" I suppose... for now, pass Collection
        Boolean unwrapSingle = findFormatFeature(ctxt, property, Collection.class,
                _classInfo,
                JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        NullValueProvider nuller = findContentNullProvider(ctxt, property, valueDeser);
        if (isDefaultDeserializer(valueDeser)) {
            valueDeser = null;
        }
        return withResolved(delegate, valueDeser, nuller, unwrapSingle);
    }

    /*
    /**********************************************************************
    /* ContainerDeserializerBase API
    /**********************************************************************
     */

    @SuppressWarnings("unchecked")
    @Override
    public ValueDeserializer<Object> getContentDeserializer() {
        ValueDeserializer<?> deser = _valueDeserializer;
        return (ValueDeserializer<Object>) deser;
    }

    @Override
    public ValueInstantiator getValueInstantiator() {
        return _valueInstantiator;
    }

    /*
    /**********************************************************************
    /* ValueDeserializer impl
    /**********************************************************************
     */

    @Override
    public Collection<String> deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        if (_delegateDeserializer != null) {
            return castToCollection(_valueInstantiator.createUsingDelegate(ctxt,
                    _delegateDeserializer.deserialize(p, ctxt)));
        }
        final Collection<String> result = castToCollection(_valueInstantiator.createUsingDefault(ctxt));
        return deserialize(p, ctxt, result);
    }

    @Override
    public Collection<String> deserialize(JsonParser p, DeserializationContext ctxt,
            Collection<String> result)
        throws JacksonException
    {
        // Ok: must point to START_ARRAY
        if (!p.isExpectedStartArrayToken()) {
            return handleNonArray(p, ctxt, result);
        }

        if (_valueDeserializer != null) {
            return deserializeUsingCustom(p, ctxt, result, _valueDeserializer);
        }
        try {
            while (true) {
                // First the common case:
                String value = p.nextStringValue();
                if (value != null) {
                    result.add(value);
                    continue;
                }
                JsonToken t = p.currentToken();
                if (t == JsonToken.END_ARRAY) {
                    break;
                }
                if (t == JsonToken.VALUE_NULL) {
                    if (_skipNullValues) {
                        continue;
                    }
                } else {
                    value = _parseString(p, ctxt, _nullProvider);
                }

                if (value == null) {
                    value = (String) _nullProvider.getNullValue(ctxt);

                    if (value == null && _skipNullValues) {
                        continue;
                    }
                }

                result.add(value);
            }
        } catch (Exception e) {
            throw DatabindException.wrapWithPath(ctxt, e,
                    new JacksonException.Reference(result, result.size()));
        }
        return result;
    }

    private Collection<String> deserializeUsingCustom(JsonParser p, DeserializationContext ctxt,
            Collection<String> result, final ValueDeserializer<String> deser) throws JacksonException
    {
        try {
            while (true) {
                /* 30-Dec-2014, tatu: This may look odd, but let's actually call method
                 *   that suggest we are expecting a String; this helps with some formats,
                 *   notably XML. Note, however, that while we can get String, we can't
                 *   assume that's what we use due to custom deserializer
                 */
                String value;
                if (p.nextStringValue() == null) {
                    JsonToken t = p.currentToken();
                    if (t == JsonToken.END_ARRAY) {
                        break;
                    }
                    // Ok: no need to convert Strings, but must recognize nulls
                    if (t == JsonToken.VALUE_NULL) {
                        if (_skipNullValues) {
                            continue;
                        }
                        value = null;
                    } else {
                        value = deser.deserialize(p, ctxt);
                    }
                } else {
                    value = deser.deserialize(p, ctxt);
                }

                if (value == null) {
                    value = (String) _nullProvider.getNullValue(ctxt);

                    if (value == null && _skipNullValues) {
                        continue;
                    }
                }

                result.add(value);
            }
        } catch (Exception e) {
            throw DatabindException.wrapWithPath(ctxt, e,
                    new JacksonException.Reference(result, result.size()));
        }
        return result;
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer) throws JacksonException {
        // In future could check current token... for now this should be enough:
        return typeDeserializer.deserializeTypedFromArray(p, ctxt);
    }

    /**
     * Helper method called when current token is not START_ARRAY. Will either
     * throw an exception, or try to handle value as if member of implicit
     * array, depending on configuration.
     */
    private final Collection<String> handleNonArray(JsonParser p, DeserializationContext ctxt,
            Collection<String> result) throws JacksonException
    {
        // implicit arrays from single values?
        boolean canWrap = (_unwrapSingle == Boolean.TRUE) ||
                ((_unwrapSingle == null) &&
                        ctxt.isEnabled(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY));
        if (!canWrap) {
            if (p.hasToken(JsonToken.VALUE_STRING)) {
                return _deserializeFromString(p, ctxt);
            }
            return castToCollection(ctxt.handleUnexpectedToken(_containerType, p));
        }
        // Strings are one of "native" (intrinsic) types, so there's never type deserializer involved
        ValueDeserializer<String> valueDes = _valueDeserializer;
        JsonToken t = p.currentToken();

        String value;

        if (t == JsonToken.VALUE_NULL) {
            // 03-Feb-2017, tatu: Does this work?
            if (_skipNullValues) {
                return result;
            }
            value = null;
        } else {
            if (p.hasToken(JsonToken.VALUE_STRING)) {
                String textValue = p.getString();
                // https://github.com/FasterXML/jackson-dataformat-xml/issues/513
                if (textValue.isEmpty()) {
                    final CoercionAction act = ctxt.findCoercionAction(logicalType(), handledType(),
                            CoercionInputShape.EmptyString);
                    if (act != CoercionAction.Fail) {
                        return castToCollection(_deserializeFromEmptyString(p, ctxt, act, handledType(),
                                "empty String (\"\")"));
                    }
                } else if (_isBlank(textValue)) {
                    final CoercionAction act = ctxt.findCoercionFromBlankString(logicalType(), handledType(),
                            CoercionAction.Fail);
                    if (act != CoercionAction.Fail) {
                        return castToCollection(_deserializeFromEmptyString(p, ctxt, act, handledType(),
                                "blank String (all whitespace)"));
                    }
                }
                // if coercion failed, we can still add it to a list
            }
            value = (valueDes == null) ? _parseString(p, ctxt, _nullProvider) : valueDes.deserialize(p, ctxt);
        }

        if (value == null) {
            value = (String) _nullProvider.getNullValue(ctxt);

            if (value == null && _skipNullValues) {
                return result;
            }
        }

        result.add(value);
        return result;
    }

    // Used to avoid type pollution: see
    //   https://micronaut-projects.github.io/micronaut-test/latest/guide/#typePollution
    // for details
    //
    // @since 2.18
    @SuppressWarnings("unchecked")
    private static Collection<String> castToCollection(Object o) {
        if (o != null) {
            // fast path for specific classes to avoid type pollution:
            // https://micronaut-projects.github.io/micronaut-test/latest/guide/#typePollution
            if (o.getClass() == ArrayList.class) {
                return (ArrayList<String>) o;
            }
            if (o.getClass() == HashSet.class) {
                return (HashSet<String>) o;
            }
        }
        return (Collection<String>) o;
    }
}
