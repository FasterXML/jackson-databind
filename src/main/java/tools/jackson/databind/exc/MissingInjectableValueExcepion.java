package tools.jackson.databind.exc;

import tools.jackson.core.JsonParser;

import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DatabindException;

/**
 * @deprecated Since 3.1 use {@link MissingInjectableValueException} instead.
 */
@Deprecated // @since 3.1
public class MissingInjectableValueExcepion
    extends DatabindException
{
    private static final long serialVersionUID = 2L;

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

    /**
     * @deprecated Use {@link MissingInjectableValueException#from} instead.
     */
    @Deprecated
    public static MissingInjectableValueExcepion from(JsonParser p, String msg,
            Object valueId, BeanProperty forProperty, Object beanInstance)
    {
        return MissingInjectableValueException.from(p, msg, valueId, forProperty, beanInstance);
    }

    public Object getValueId() { return _valueId; }
    public BeanProperty getForProperty() { return _forProperty; }
    public Object getBeanInstance() { return _beanInstance; }
}
