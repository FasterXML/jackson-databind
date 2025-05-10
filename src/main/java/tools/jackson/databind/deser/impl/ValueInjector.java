package tools.jackson.databind.deser.impl;

import com.fasterxml.jackson.annotation.JacksonInject;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.*;
import tools.jackson.databind.introspect.AnnotatedMember;

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
     */
    protected final Boolean _optional;

    public ValueInjector(PropertyName propName, JavaType type,
            AnnotatedMember mutator, Object valueId, Boolean optional)
    {
        super(propName, type, null, mutator, PropertyMetadata.STD_OPTIONAL);
        _valueId = valueId;
        _optional = optional;
    }

    public Object findValue(DeserializationContext context, Object beanInstance)
        throws JacksonException
    {
        return context.findInjectableValue(_valueId, this, beanInstance, _optional);
    }

    public void inject(DeserializationContext context, Object beanInstance)
        throws JacksonException
    {
        final Object value = findValue(context, beanInstance);
        if (!JacksonInject.Value.empty().equals(value)) {
            _member.setValue(beanInstance, value);
        }
    }
}
