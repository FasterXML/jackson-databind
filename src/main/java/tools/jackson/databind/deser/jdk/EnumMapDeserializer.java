package tools.jackson.databind.deser.jdk;

import java.util.*;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.*;
import tools.jackson.databind.deser.bean.PropertyBasedCreator;
import tools.jackson.databind.deser.bean.PropertyValueBuffer;
import tools.jackson.databind.deser.std.ContainerDeserializerBase;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.type.LogicalType;

/**
 * Deserializer for {@link EnumMap} values.
 * <p>
 * Note: casting within this class is all messed up -- just could not figure out a way
 * to properly deal with recursive definition of {@code EnumMap<K extends Enum<K>, V>}
 */
@SuppressWarnings({ "unchecked", "rawtypes" }) 
public class EnumMapDeserializer
    extends ContainerDeserializerBase<EnumMap<?,?>>
{
    protected final Class<?> _enumClass;

    protected KeyDeserializer _keyDeserializer;

    protected ValueDeserializer<Object> _valueDeserializer;

    /**
     * If value instances have polymorphic type information, this
     * is the type deserializer that can handle it
     */
    protected final TypeDeserializer _valueTypeDeserializer;

    // // Instance construction settings:

    protected final ValueInstantiator _valueInstantiator;

    /**
     * Deserializer that is used iff delegate-based creator is
     * to be used for deserializing from JSON Object.
     */
    protected ValueDeserializer<Object> _delegateDeserializer;

    /**
     * If the Map is to be instantiated using non-default constructor
     * or factory method
     * that takes one or more named properties as argument(s),
     * this creator is used for instantiation.
     */
    protected PropertyBasedCreator _propertyBasedCreator;    

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public EnumMapDeserializer(JavaType mapType, ValueInstantiator valueInst,
            KeyDeserializer keyDeser, ValueDeserializer<?> valueDeser, TypeDeserializer vtd,
            NullValueProvider nuller)
    {
        super(mapType, nuller, null);
        _enumClass = mapType.getKeyType().getRawClass();
        _keyDeserializer = keyDeser;
        _valueDeserializer = (ValueDeserializer<Object>) valueDeser;
        _valueTypeDeserializer = vtd;
        _valueInstantiator = valueInst;
    }

    protected EnumMapDeserializer(EnumMapDeserializer base,
            KeyDeserializer keyDeser, ValueDeserializer<?> valueDeser, TypeDeserializer vtd,
            NullValueProvider nuller)
    {
        super(base, nuller, base._unwrapSingle);
        _enumClass = base._enumClass;
        _keyDeserializer = keyDeser;
        _valueDeserializer = (ValueDeserializer<Object>) valueDeser;
        _valueTypeDeserializer = vtd;

        _valueInstantiator = base._valueInstantiator;
        _delegateDeserializer = base._delegateDeserializer;
        _propertyBasedCreator = base._propertyBasedCreator;
    }

    public EnumMapDeserializer withResolved(KeyDeserializer keyDeserializer,
            ValueDeserializer<?> valueDeserializer, TypeDeserializer valueTypeDeser,
            NullValueProvider nuller)
    {
        if ((keyDeserializer == _keyDeserializer) && (nuller == _nullProvider)
                && (valueDeserializer == _valueDeserializer) && (valueTypeDeser == _valueTypeDeserializer)) {
            return this;
        }
        return new EnumMapDeserializer(this,
                keyDeserializer, valueDeserializer, valueTypeDeser, nuller);
    }

    /*
    /**********************************************************************
    /* Validation, post-processing (ResolvableDeserializer)
    /**********************************************************************
     */

    @Override
    public void resolve(DeserializationContext ctxt)
    {
        // May need to resolve types for delegate- and/or property-based creators:
        if (_valueInstantiator != null) {
            if (_valueInstantiator.canCreateUsingDelegate()) {
                JavaType delegateType = _valueInstantiator.getDelegateType(ctxt.getConfig());
                if (delegateType == null) {
                    ctxt.reportBadDefinition(_containerType, String.format(
"Invalid delegate-creator definition for %s: value instantiator (%s) returned true for 'canCreateUsingDelegate()', but null for 'getDelegateType()'",
                            _containerType,
                            _valueInstantiator.getClass().getName()));
                }
                /* Theoretically should be able to get CreatorProperty for delegate
                 * parameter to pass; but things get tricky because DelegateCreator
                 * may contain injectable values. So, for now, let's pass nothing.
                 */
                _delegateDeserializer = findDeserializer(ctxt, delegateType, null);
            } else if (_valueInstantiator.canCreateUsingArrayDelegate()) {
                JavaType delegateType = _valueInstantiator.getArrayDelegateType(ctxt.getConfig());
                if (delegateType == null) {
                    ctxt.reportBadDefinition(_containerType, String.format(
"Invalid delegate-creator definition for %s: value instantiator (%s) returned true for 'canCreateUsingArrayDelegate()', but null for 'getArrayDelegateType()'",
                            _containerType,
                            _valueInstantiator.getClass().getName()));
                }
                _delegateDeserializer = findDeserializer(ctxt, delegateType, null);
            } else if (_valueInstantiator.canCreateFromObjectWith()) {
                SettableBeanProperty[] creatorProps = _valueInstantiator.getFromObjectArguments(ctxt.getConfig());
                _propertyBasedCreator = PropertyBasedCreator.construct(ctxt, _valueInstantiator, creatorProps,
                        ctxt.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES));
            }
        }
    }

    /**
     * Method called to finalize setup of this deserializer,
     * when it is known for which property deserializer is needed for.
     */
    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
    {
        // note: instead of finding key deserializer, with enums we actually
        // work with regular deserializers (less code duplication; but not
        // quite as clean as it ought to be)
        KeyDeserializer keyDeser = _keyDeserializer;
        if (keyDeser == null) {
            keyDeser = ctxt.findKeyDeserializer(_containerType.getKeyType(), property);
        }
        ValueDeserializer<?> valueDeser = _valueDeserializer;
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

    @Override // since 2.12
    public LogicalType logicalType() {
        return LogicalType.Map;
    }

    /*
    /**********************************************************************
    /* ContainerDeserializerBase API
    /**********************************************************************
     */

    @Override
    public ValueDeserializer<Object> getContentDeserializer() {
        return _valueDeserializer;
    }

    @Override
    public ValueInstantiator getValueInstantiator() {
        return _valueInstantiator;
    }

    @Override
    public Object getEmptyValue(DeserializationContext ctxt) {
        return constructMap(ctxt);
    }

    /*
    /**********************************************************************
    /* Actual deserialization
    /**********************************************************************
     */
    
    @Override
    public EnumMap<?,?> deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        if (_propertyBasedCreator != null) {
            return _deserializeUsingProperties(p, ctxt);
        }
        if (_delegateDeserializer != null) {
            return (EnumMap<?,?>) _valueInstantiator.createUsingDelegate(ctxt,
                    _delegateDeserializer.deserialize(p, ctxt));
        }

        switch (p.currentTokenId()) {
        case JsonTokenId.ID_START_OBJECT:
        case JsonTokenId.ID_END_OBJECT:
        case JsonTokenId.ID_PROPERTY_NAME:
            return deserialize(p, ctxt, constructMap(ctxt));
        case JsonTokenId.ID_STRING:
            // (empty) String may be ok however; or single-String-arg ctor
            return _deserializeFromString(p, ctxt);
        case JsonTokenId.ID_START_ARRAY:
            // Empty array, or single-value wrapped in array?
            return _deserializeFromArray(p, ctxt);
        default:
        }
        return (EnumMap<?,?>) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
    }

    @Override
    public EnumMap<?,?> deserialize(JsonParser p, DeserializationContext ctxt,
            EnumMap result)
        throws JacksonException
    {
        // [databind#631]: Assign current value, to be accessible by custom deserializers
        p.assignCurrentValue(result);

        final ValueDeserializer<Object> valueDes = _valueDeserializer;
        final TypeDeserializer typeDeser = _valueTypeDeserializer;

        String keyStr;
        if (p.isExpectedStartObjectToken()) {
            keyStr = p.nextName();
        } else {
            JsonToken t = p.currentToken();
            if (t != JsonToken.PROPERTY_NAME) {
                if (t == JsonToken.END_OBJECT) {
                    return result;
                }
                ctxt.reportWrongTokenException(this, JsonToken.PROPERTY_NAME, null);
            }
            keyStr = p.currentName();
        }

        for (; keyStr != null; keyStr = p.nextName()) {
            // but we need to let key deserializer handle it separately, nonetheless
            Enum<?> key = (Enum<?>) _keyDeserializer.deserializeKey(keyStr, ctxt);
            JsonToken t = p.nextToken();
            if (key == null) {
                if (!ctxt.isEnabled(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)) {
                    return (EnumMap<?,?>) ctxt.handleWeirdStringValue(_enumClass, keyStr,
                            "value not one of declared Enum instance names for %s",
                            _containerType.getKeyType());
                }
                // 24-Mar-2012, tatu: Null won't work as a key anyway, so let's
                //  just skip the entry then. But we must skip the value as well, if so.
                p.skipChildren();
                continue;
            }
            // And then the value...
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
                return wrapAndThrow(ctxt, e, result, keyStr);
            }
            result.put(key, value);
        }
        return result;
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws JacksonException
    {
        // In future could check current token... for now this should be enough:
        return typeDeserializer.deserializeTypedFromObject(p, ctxt);
    }

    protected EnumMap<?,?> constructMap(DeserializationContext ctxt) {
        if (_valueInstantiator == null) {
            return new EnumMap(_enumClass);
        }
        if (!_valueInstantiator.canCreateUsingDefault()) {
            return (EnumMap<?,?>) ctxt.handleMissingInstantiator(handledType(),
                    getValueInstantiator(), null,
                    "no default constructor found");
        }
        return (EnumMap<?,?>) _valueInstantiator.createUsingDefault(ctxt);
    }

    public EnumMap<?,?> _deserializeUsingProperties(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        final PropertyBasedCreator creator = _propertyBasedCreator;
        // null -> no ObjectIdReader for EnumMaps
        PropertyValueBuffer buffer = creator.startBuilding(p, ctxt, null);

        String keyName;
        if (p.isExpectedStartObjectToken()) {
            keyName = p.nextName();
        } else if (p.hasToken(JsonToken.PROPERTY_NAME)) {
            keyName = p.currentName();
        } else {
            keyName = null;
        }

        for (; keyName != null; keyName = p.nextName()) {
            JsonToken t = p.nextToken(); // to get to value
            // creator property?
            SettableBeanProperty prop = creator.findCreatorProperty(keyName);
            if (prop != null) {
                // Last property to set?
                if (buffer.assignParameter(prop, prop.deserialize(p, ctxt))) {
                    p.nextToken(); // from value to END_OBJECT or PROPERTY_NAME
                    EnumMap<?,?> result;
                    try {
                        result = (EnumMap<?,?>)creator.build(ctxt, buffer);
                    } catch (Exception e) {
                        return wrapAndThrow(ctxt, e, _containerType.getRawClass(), keyName);
                    }
                    return deserialize(p, ctxt, result);
                }
                continue;
            }
            // other property? needs buffering
            // but we need to let key deserializer handle it separately, nonetheless
            Enum<?> key = (Enum<?>) _keyDeserializer.deserializeKey(keyName, ctxt);
            if (key == null) {
                if (!ctxt.isEnabled(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)) {
                    return (EnumMap<?,?>) ctxt.handleWeirdStringValue(_enumClass, keyName,
                            "value not one of declared Enum instance names for %s",
                            _containerType.getKeyType());
                }
                // 24-Mar-2012, tatu: Null won't work as a key anyway, so let's
                //  just skip the entry then. But we must skip the value as well, if so.
                p.nextToken();
                p.skipChildren();
                continue;
            }
            Object value; 

            try {
                if (t == JsonToken.VALUE_NULL) {
                    if (_skipNullValues) {
                        continue;
                    }
                    value = _nullProvider.getNullValue(ctxt);
                } else if (_valueTypeDeserializer == null) {
                    value = _valueDeserializer.deserialize(p, ctxt);
                } else {
                    value = _valueDeserializer.deserializeWithType(p, ctxt, _valueTypeDeserializer);
                }
            } catch (Exception e) {
                wrapAndThrow(ctxt, e, _containerType.getRawClass(), keyName);
                return null;
            }
            buffer.bufferMapProperty(key, value);
        }
        // end of JSON object?
        // if so, can just construct and leave...
        try {
            return (EnumMap<?,?>)creator.build(ctxt, buffer);
        } catch (Exception e) {
            wrapAndThrow(ctxt, e, _containerType.getRawClass(), keyName);
            return null;
        }
    }
}
