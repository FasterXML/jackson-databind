package tools.jackson.databind.deser.bean;

import java.util.Set;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.*;
import tools.jackson.databind.deser.impl.ObjectIdReader;
import tools.jackson.databind.introspect.AnnotatedMethod;
import tools.jackson.databind.util.ClassUtil;
import tools.jackson.databind.util.NameTransformer;

public class BeanAsArrayBuilderDeserializer
    extends BeanDeserializerBase
{
    /**
     * Deserializer we delegate operations that we cannot handle.
     */
    final protected BeanDeserializerBase _delegate;

    /**
     * Properties in order expected to be found in JSON array.
     */
    final protected SettableBeanProperty[] _orderedProperties;

    final protected AnnotatedMethod _buildMethod;

    /**
     * Type that the builder will produce, target type; as opposed to
     * `handledType()` which refers to Builder class.
     */
    protected final JavaType _targetType;

    /*
    /**********************************************************************
    /* Life-cycle, construction, initialization
    /**********************************************************************
     */

    /**
     * Main constructor used both for creating new instances (by
     * {@link BeanDeserializer#asArrayDeserializer}) and for
     * creating copies with different delegate.
     */
    public BeanAsArrayBuilderDeserializer(BeanDeserializerBase delegate,
            JavaType targetType,
            SettableBeanProperty[] ordered,
            AnnotatedMethod buildMethod)
    {
        super(delegate);
        _delegate = delegate;
        _targetType = targetType;
        _orderedProperties = ordered;
        _buildMethod = buildMethod;
    }

    @Override
    public ValueDeserializer<Object> unwrappingDeserializer(DeserializationContext ctxt,
            NameTransformer unwrapper)
    {
        // We can't do much about this; could either replace _delegate with unwrapping instance,
        // or just replace this one. Latter seems more sensible.
        return _delegate.unwrappingDeserializer(ctxt, unwrapper);
    }

    @Override
    public BeanDeserializerBase withObjectIdReader(ObjectIdReader oir) {
        return new BeanAsArrayBuilderDeserializer(_delegate.withObjectIdReader(oir),
                _targetType, _orderedProperties, _buildMethod);
    }

    @Override
    public BeanDeserializerBase withByNameInclusion(Set<String> ignorableProps,
            Set<String> includableProps) {
        return new BeanAsArrayBuilderDeserializer(_delegate.withByNameInclusion(ignorableProps, includableProps),
                _targetType, _orderedProperties, _buildMethod);
    }

    @Override
    public BeanDeserializerBase withIgnoreAllUnknown(boolean ignoreUnknown) {
        return new BeanAsArrayBuilderDeserializer(_delegate.withIgnoreAllUnknown(ignoreUnknown),
                _targetType, _orderedProperties, _buildMethod);
    }

    @Override
    public BeanDeserializerBase withBeanProperties(BeanPropertyMap props) {
        return new BeanAsArrayBuilderDeserializer(_delegate.withBeanProperties(props),
                _targetType, _orderedProperties, _buildMethod);
    }

    @Override
    protected BeanDeserializerBase asArrayDeserializer() {
        return this;
    }

    @Override
    protected void initNameMatcher(DeserializationContext ctxt) { }

    /*
    /**********************************************************************
    /* Overrides
    /**********************************************************************
     */

    @Override
    public Boolean supportsUpdate(DeserializationConfig config) {
        // 26-Oct-2016, tatu: No, we can't merge Builder-based POJOs as of now
        return Boolean.FALSE;
    }

    /*
    /**********************************************************************
    /* ValueDeserializer implementation
    /**********************************************************************
     */

    protected final Object finishBuild(DeserializationContext ctxt, Object builder)
        throws JacksonException
    {
        try {
            return _buildMethod.getMember().invoke(builder, (Object[]) null);
        } catch (Exception e) {
            return wrapInstantiationProblem(ctxt, e);
        }
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        // Let's delegate just in case we got a JSON Object (could error out, alternatively?)
        if (!p.isExpectedStartArrayToken()) {
            return finishBuild(ctxt, _deserializeFromNonArray(p, ctxt));
        }
        if (!_vanillaProcessing) {
            return finishBuild(ctxt, _deserializeNonVanilla(p, ctxt));
        }
        Object builder = _valueInstantiator.createUsingDefault(ctxt);
        final SettableBeanProperty[] props = _orderedProperties;
        int i = 0;
        final int propCount = props.length;
        while (true) {
            if (p.nextToken() == JsonToken.END_ARRAY) {
                return finishBuild(ctxt, builder);
            }
            if (i == propCount) {
                break;
            }
            SettableBeanProperty prop = props[i];
            if (prop != null) { // normal case
                try {
                    builder = prop.deserializeSetAndReturn(p, ctxt, builder);
                } catch (Exception e) {
                    throw wrapAndThrow(e, builder, prop.getName(), ctxt);
                }
            } else { // just skip?
                p.skipChildren();
            }
            ++i;
        }
        // 09-Nov-2016, tatu: Should call `handleUnknownProperty()` in Context, but it'd give
        //   non-optimal exception message so...
        if (!_ignoreAllUnknown && ctxt.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)) {
            ctxt.reportInputMismatch(handledType(),
                    "Unexpected JSON values; expected at most %d properties (in JSON Array)",
                    propCount);
            // fall through
        }
        // otherwise, skip until end
        while (p.nextToken() != JsonToken.END_ARRAY) {
            p.skipChildren();
        }
        return finishBuild(ctxt, builder);
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt, Object value)
        throws JacksonException
    {
        // 26-Oct-2016, tatu: Will fail, but let the original deserializer provide message
        return _delegate.deserialize(p, ctxt, value);
    }

    @Override
    public Object deserializeFromObject(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        return _deserializeFromNonArray(p, ctxt);
    }

    /*
    /**********************************************************************
    /* Helper methods, non-standard creation
    /**********************************************************************
     */

    /**
     * Alternate deserialization method that has to check many more configuration
     * aspects than the "vanilla" processing.
     * Note: should NOT resolve builder; caller will do that
     *
     * @return Builder object in use.
     */
    protected Object _deserializeNonVanilla(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        if (_nonStandardCreation) {
            return deserializeFromObjectUsingNonDefault(p, ctxt);
        }
        Object builder = _valueInstantiator.createUsingDefault(ctxt);
        if (_injectables != null) {
            injectValues(ctxt, builder);
        }
        Class<?> activeView = _needViewProcesing ? ctxt.getActiveView() : null;
        final SettableBeanProperty[] props = _orderedProperties;
        int i = 0;
        final int propCount = props.length;
        while (true) {
            if (p.nextToken() == JsonToken.END_ARRAY) {
                return builder;
            }
            if (i == propCount) {
                break;
            }
            SettableBeanProperty prop = props[i];
            ++i;
            if (prop != null) { // normal case
                if (activeView == null || prop.visibleInView(activeView)) {
                    try {
                        prop.deserializeSetAndReturn(p, ctxt, builder);
                    } catch (Exception e) {
                        throw wrapAndThrow(e, builder, prop.getName(), ctxt);
                    }
                    continue;
                }
            }
            // otherwise, skip it (view-filtered, no prop etc)
            p.skipChildren();
        }
        // Ok; extra fields? Let's fail, unless ignoring extra props is fine
        if (!_ignoreAllUnknown && ctxt.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)) {
            ctxt.reportWrongTokenException(this, JsonToken.END_ARRAY,
                    "Unexpected JSON value(s); expected at most %d properties (in JSON Array)",
                    propCount);
            // will never reach here as exception has been thrown
        }
        // otherwise, skip until end
        while (p.nextToken() != JsonToken.END_ARRAY) {
            p.skipChildren();
        }
        return builder;
    }

    /**
     * Method called to deserialize bean using "property-based creator":
     * this means that a non-default constructor or factory method is
     * called, and then possibly other setters. The trick is that
     * values for creator method need to be buffered, first; and
     * due to non-guaranteed ordering possibly some other properties
     * as well.
     */
    @Override
    protected final Object _deserializeUsingPropertyBased(final JsonParser p,
            final DeserializationContext ctxt)
        throws JacksonException
    {
        final PropertyBasedCreator creator = _propertyBasedCreator;
        PropertyValueBuffer buffer = creator.startBuilding(p, ctxt, _objectIdReader);

        final SettableBeanProperty[] props = _orderedProperties;
        final int propCount = props.length;
        final Class<?> activeView = _needViewProcesing ? ctxt.getActiveView() : null;
        int i = 0;
        Object builder = null;

        for (; p.nextToken() != JsonToken.END_ARRAY; ++i) {
            SettableBeanProperty prop = (i < propCount) ? props[i] : null;
            if (prop == null) { // we get null if there are extra elements; maybe otherwise too?
                p.skipChildren();
                continue;
            }
            if ((activeView != null) && !prop.visibleInView(activeView)) {
                p.skipChildren();
                continue;
            }
            // if we have already constructed POJO, things are simple:
            if (builder != null) {
                try {
                    builder = prop.deserializeSetAndReturn(p, ctxt, builder);
                } catch (Exception e) {
                    throw wrapAndThrow(e, builder, prop.getName(), ctxt);
                }
                continue;
            }
            final String propName = prop.getName();
            // if not yet, maybe we got a creator property?
            final SettableBeanProperty creatorProp = creator.findCreatorProperty(propName);
            // Object Id property?
            if (buffer.readIdProperty(propName) && creatorProp == null) {
                continue;
            }
            if (creatorProp != null) {
                // Last creator property to set?
                if (buffer.assignParameter(creatorProp, creatorProp.deserialize(p, ctxt))) {
                    try {
                        builder = creator.build(ctxt, buffer);
                    } catch (Exception e) {
                        throw wrapAndThrow(e, _beanType.getRawClass(), propName, ctxt);
                    }
                    //  polymorphic?
                    if (builder.getClass() != _beanType.getRawClass()) {
                        /* 23-Jul-2012, tatu: Not sure if these could ever be properly
                         *   supported (since ordering of elements may not be guaranteed);
                         *   but make explicitly non-supported for now.
                         */
                        return ctxt.reportBadDefinition(_beanType, String.format(
"Cannot support implicit polymorphic deserialization for POJOs-as-Arrays style: nominal type %s, actual type %s",
                                ClassUtil.getTypeDescription(_beanType),
                                builder.getClass().getName()));
                    }
                }
                continue;
            }
            // regular property? needs buffering
            buffer.bufferProperty(prop, prop.deserialize(p, ctxt));
        }

        // In case we didn't quite get all the creator properties, we may have to do this:
        if (builder == null) {
            try {
                builder = creator.build(ctxt, buffer);
            } catch (Exception e) {
                return wrapInstantiationProblem(ctxt, e);
            }
        }
        return builder;
    }

    /*
    /**********************************************************************
    /* Helper methods, error reporting
    /**********************************************************************
     */

    protected Object _deserializeFromNonArray(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        // Let's start with failure
        return ctxt.handleUnexpectedToken(getValueType(ctxt), p.currentToken(), p,
                "Cannot deserialize a POJO (of type %s) from non-Array representation (token: %s): "
                +"type/property designed to be serialized as JSON Array",
                _beanType.getRawClass().getName(),
                p.currentToken());
        // in future, may allow use of "standard" POJO serialization as well; if so, do:
        //return _delegate.deserialize(p, ctxt);
    }
}
