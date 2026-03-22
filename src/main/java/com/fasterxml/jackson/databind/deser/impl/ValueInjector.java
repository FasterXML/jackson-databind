package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;

/**
 * Class that encapsulates details of value injection that occurs before
 * deserialization of a POJO. Details include information needed to find
 * injectable value (logical id) as well as method used for assigning
 * value (setter or field)
 */
public class ValueInjector
    extends BeanProperty.Std
{
    private static final long serialVersionUID = 1L;

    /**
     * Identifier used for looking up value to inject
     */
    protected final Object _valueId;

    /**
     * Flag used for configuring the behavior when the value to inject is not found.
     *
     * @since 2.20
     */
    protected final Boolean _optional;

    /**
     * Flag used for configuring the behavior when the input value should be preferred
     * over the value to inject.
     *
     * @since 2.20
     */
    protected final Boolean _useInput;

    /**
     * @since 2.20
     */
    public ValueInjector(PropertyName propName, JavaType type,
            AnnotatedMember mutator, Object valueId, Boolean optional, Boolean useInput)
    {
        super(propName, type, null, mutator, PropertyMetadata.STD_OPTIONAL);
        _valueId = valueId;
        _optional = optional;
        _useInput = useInput;
    }

    /**
     * @deprecated in 2.20 (remove from 3.0)
     */
    @Deprecated // since 2.20
    public ValueInjector(PropertyName propName, JavaType type,
            AnnotatedMember mutator, Object valueId)
    {
        this(propName, type, mutator, valueId, null, null);
    }

    public Object findValue(DeserializationContext context, Object beanInstance)
        throws JsonMappingException
    {
        return context.findInjectableValue(_valueId, this, beanInstance, _optional, _useInput);
    }

    public void inject(DeserializationContext context, Object beanInstance)
        throws IOException
    {
        final Object value = findValue(context, beanInstance);

        if (value == null) {
            if (Boolean.FALSE.equals(_optional)) {
                throw context.missingInjectableValueException(
                        String.format("No injectable value with id '%s' found (for property '%s')",
                                _valueId, getName()),
                        _valueId, null, beanInstance);
            }
        } else if (!Boolean.TRUE.equals(_useInput)) {
            _member.setValue(beanInstance, value);
        }
    }
}
