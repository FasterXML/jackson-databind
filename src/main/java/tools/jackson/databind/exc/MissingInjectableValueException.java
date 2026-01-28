package tools.jackson.databind.exc;

import tools.jackson.core.JsonParser;

import tools.jackson.databind.BeanProperty;

@SuppressWarnings("deprecation") // extends deprecated MissingInjectableValueExcepion for backward compatibility
public class MissingInjectableValueException
    extends MissingInjectableValueExcepion
{
    private static final long serialVersionUID = 1L;

    protected MissingInjectableValueException(JsonParser p, String msg,
            Object valueId, BeanProperty forProperty, Object beanInstance)
    {
        super(p, msg, valueId, forProperty, beanInstance);
    }

    public static MissingInjectableValueException from(JsonParser p, String msg,
            Object valueId, BeanProperty forProperty, Object beanInstance)
    {
        return new MissingInjectableValueException(p, msg, valueId, forProperty, beanInstance);
    }
}
