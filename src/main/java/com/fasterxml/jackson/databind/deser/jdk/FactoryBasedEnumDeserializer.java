package com.fasterxml.jackson.databind.deser.jdk;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.bean.PropertyBasedCreator;
import com.fasterxml.jackson.databind.deser.bean.PropertyValueBuffer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Deserializer that uses a single-String static factory method
 * for locating Enum values by String id.
 */
class FactoryBasedEnumDeserializer
    extends StdDeserializer<Object>
{
    // Marker type; null if String expected; otherwise numeric wrapper
    protected final JavaType _inputType;
    protected final boolean _hasArgs;
    protected final AnnotatedMethod _factory;
    protected final ValueDeserializer<?> _deser;
    protected final ValueInstantiator _valueInstantiator;
    protected final SettableBeanProperty[] _creatorProps;

    /**
     * Lazily instantiated property-based creator.
     */
    private transient PropertyBasedCreator _propCreator;
    
    public FactoryBasedEnumDeserializer(Class<?> cls, AnnotatedMethod f, JavaType paramType,
            ValueInstantiator valueInstantiator, SettableBeanProperty[] creatorProps)
    {
        super(cls);
        _factory = f;
        _hasArgs = true;
        // We'll skip case of `String`, as well as no type (zero-args): 
        _inputType = paramType.hasRawClass(String.class) ? null : paramType;
        _deser = null;
        _valueInstantiator = valueInstantiator;
        _creatorProps = creatorProps;
    }

    /**
     * @since 2.8
     */
    public FactoryBasedEnumDeserializer(Class<?> cls, AnnotatedMethod f)
    {
        super(cls);
        _factory = f;
        _hasArgs = false;
        _inputType = null;
        _deser = null;
        _valueInstantiator = null;
        _creatorProps = null;
    }

    protected FactoryBasedEnumDeserializer(FactoryBasedEnumDeserializer base,
            ValueDeserializer<?> deser) {
        super(base._valueClass);
        _inputType = base._inputType;
        _factory = base._factory;
        _hasArgs = base._hasArgs;
        _valueInstantiator = base._valueInstantiator;
        _creatorProps = base._creatorProps;

        _deser = deser;
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property)
    {
        if ((_deser == null) && (_inputType != null) && (_creatorProps == null)) {
            return new FactoryBasedEnumDeserializer(this,
                    ctxt.findContextualValueDeserializer(_inputType, property));
        }
        return this;
    }

    @Override
    public Boolean supportsUpdate(DeserializationConfig config) {
        return Boolean.FALSE;
    }

    @Override
    public LogicalType logicalType() {
        return LogicalType.Enum;
    }

    @Override
    public boolean isCachable() { return true; }

    @Override
    public ValueInstantiator getValueInstantiator() { return _valueInstantiator; }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        Object value;
        if (_deser != null) {
            value = _deser.deserialize(p, ctxt);
        } else if (_hasArgs) {
            JsonToken curr = p.currentToken();

            // 30-Mar-2020, tatu: For properties-based one, MUST get JSON Object (before
            //   2.11, was just assuming match)
            if (_creatorProps != null) {
                if (!p.isExpectedStartObjectToken()) {
                    final JavaType targetType = getValueType(ctxt);
                    ctxt.reportInputMismatch(targetType,
"Input mismatch reading Enum %s: properties-based `@JsonCreator` (%s) expects JSON Object (JsonToken.START_OBJECT), got JsonToken.%s",
ClassUtil.getTypeDescription(targetType), _factory, p.currentToken());
                }
                if (_propCreator == null) {
                    _propCreator = PropertyBasedCreator.construct(ctxt, _valueInstantiator, _creatorProps,
                            ctxt.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES));
                }
                p.nextToken();
                return deserializeEnumUsingPropertyBased(p, ctxt, _propCreator);
            }

            // 30-Mar-2020, tatu: Single-arg delegating creators may go through
            //    here; although not 100% sure why they do not take the first branch
            if (curr == JsonToken.VALUE_STRING || curr == JsonToken.PROPERTY_NAME) {
                value = p.getText();
            } else if (curr == JsonToken.VALUE_NUMBER_INT) {
                value = p.getNumberValue();
            } else {
                value = p.getValueAsString();
            }
        } else { // zero-args; just skip whatever value there may be
            p.skipChildren();
            try {
                return _factory.call();
            } catch (Exception e) {
                Throwable t = ClassUtil.throwRootCauseIfJacksonE(e);
                return ctxt.handleInstantiationProblem(_valueClass, null, t);
            }
        }
        try {
            return _factory.callOnWith(_valueClass, value);
        } catch (Exception e) {
            Throwable t = ClassUtil.throwRootCauseIfJacksonE(e);
            // [databind#1642]:
            if (ctxt.isEnabled(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL) &&
                    t instanceof IllegalArgumentException) {
                return null;
            }
            return ctxt.handleInstantiationProblem(_valueClass, value, t);
        }
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer)
            throws JacksonException
    {
        if (_deser == null) { // String never has type info
            return deserialize(p, ctxt);
        }
        return typeDeserializer.deserializeTypedFromAny(p, ctxt);
    }
    
    // Method to deserialize the Enum using property based methodology
    protected Object deserializeEnumUsingPropertyBased(final JsonParser p, final DeserializationContext ctxt,
    		final PropertyBasedCreator creator) throws JacksonException
    {
        PropertyValueBuffer buffer = creator.startBuilding(p, ctxt, null);
    
        JsonToken t = p.currentToken();
        for (; t == JsonToken.PROPERTY_NAME; t = p.nextToken()) {
            String propName = p.currentName();
            p.nextToken(); // to point to value
    
            final SettableBeanProperty creatorProp = creator.findCreatorProperty(propName);
            if (buffer.readIdProperty(propName) && creatorProp == null) {
                continue;
            }
            if (creatorProp != null) {
                buffer.assignParameter(creatorProp, _deserializeWithErrorWrapping(p, ctxt, creatorProp));
                continue;
            }
            // 26-Nov-2020, tatu: ... what should we do here tho?
            p.skipChildren();
        }
        return creator.build(ctxt, buffer);
    }

    // ************ Got the below methods from BeanDeserializer ********************//

    protected final Object _deserializeWithErrorWrapping(JsonParser p, DeserializationContext ctxt,
            SettableBeanProperty prop) throws JacksonException
    {
        try {
            return prop.deserialize(p, ctxt);
        } catch (Exception e) {
            return wrapAndThrow(e, handledType(), prop.getName(), ctxt);
        }
    }

    protected Object wrapAndThrow(Throwable t, Object bean, String fieldName, DeserializationContext ctxt)
        throws DatabindException
    {
        throw DatabindException.wrapWithPath(throwOrReturnThrowable(t, ctxt), bean, fieldName);
    }

    private Throwable throwOrReturnThrowable(Throwable t, DeserializationContext ctxt)
        throws JacksonException
    {
        t = ClassUtil.getRootCause(t);
        // Errors to be passed as is
        ClassUtil.throwIfError(t);
        boolean wrap = (ctxt == null) || ctxt.isEnabled(DeserializationFeature.WRAP_EXCEPTIONS);
        if (!wrap) {
            ClassUtil.throwIfRTE(t);
        }
        return t;
    }
}
