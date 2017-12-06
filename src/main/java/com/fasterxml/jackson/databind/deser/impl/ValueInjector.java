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

    public ValueInjector(PropertyName propName, JavaType type,
            AnnotatedMember mutator, Object valueId)
    {
        super(propName, type, null, mutator, PropertyMetadata.STD_OPTIONAL);
        _valueId = valueId;
    }

    public Object findValue(DeserializationContext context, Object beanInstance)
        throws JsonMappingException
    {
        return context.findInjectableValue(_valueId, this, beanInstance);
    }
    
    public void inject(DeserializationContext context, Object beanInstance)
        throws IOException
    {
        _member.setValue(beanInstance, findValue(context, beanInstance));
    }
}