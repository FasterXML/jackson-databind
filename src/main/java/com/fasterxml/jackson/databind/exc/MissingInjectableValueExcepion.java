package com.fasterxml.jackson.databind.exc;

import com.fasterxml.jackson.core.JsonParser;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * @since 2.20
 */
public class MissingInjectableValueExcepion
    extends JsonMappingException
{
    private static final long serialVersionUID = 1L;

    protected final Object _valueId;
    protected final BeanProperty _forProperty;
    protected final Object _beanInstance;

    protected MissingInjectableValueExcepion(JsonParser p, String msg,
            Object valueId, BeanProperty forProperty, Object beanInstance)
    {
        super(p, msg);
        _valueId = valueId;
        _forProperty = forProperty;
        _beanInstance = beanInstance;
    }

    public static MissingInjectableValueExcepion from(JsonParser p, String msg,
            Object valueId, BeanProperty forProperty, Object beanInstance)
    {
        return new MissingInjectableValueExcepion(p, msg, valueId, forProperty, beanInstance);
    }

    public Object getValueId() { return _valueId; }
    public BeanProperty getForProperty() { return _forProperty; }
    public Object getBeanInstance() { return _beanInstance; }
}
