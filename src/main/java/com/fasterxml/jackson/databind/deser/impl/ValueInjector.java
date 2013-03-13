package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;


import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.util.Annotations;

/**
 * Class that encapsulates details of value injection that occurs before
 * deserialization of a POJO. Details include information needed to find
 * injectable value (logical id) as well as method used for assigning
 * value (setter or field)
 */
public class ValueInjector
    extends BeanProperty.Std
{
    /**
     * Identifier used for looking up value to inject
     */
    protected final Object _valueId;

    public ValueInjector(String propertyName, JavaType type,
            Annotations contextAnnotations, AnnotatedMember mutator,
            Object valueId)
    {
        super(propertyName, type, null, contextAnnotations, mutator, false);
        _valueId = valueId;
    }

    public Object findValue(DeserializationContext context, Object beanInstance)
    {
        return context.findInjectableValue(_valueId, this, beanInstance);
    }
    
    public void inject(DeserializationContext context, Object beanInstance)
        throws IOException
    {
        _member.setValue(beanInstance, findValue(context, beanInstance));
    }
}