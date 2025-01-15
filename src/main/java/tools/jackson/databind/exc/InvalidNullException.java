package tools.jackson.databind.exc;

import tools.jackson.core.JsonParser;

import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.PropertyName;
import tools.jackson.databind.util.ClassUtil;

/**
 * Exception thrown if a `null` value is being encountered for a property
 * designed as "fail on null" property (see {@link com.fasterxml.jackson.annotation.JsonSetter}).
 */
public class InvalidNullException
    extends MismatchedInputException
{
    private static final long serialVersionUID = 1L; // silly Eclipse, warnings

    /**
     * Name of property, if known, for which null was encountered.
     */
    protected final PropertyName _propertyName;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    /**
     * @since 2.19
     */
    protected InvalidNullException(JsonParser p, String msg, PropertyName pname) {
        super(p, msg);
        _propertyName = pname;
    }

    protected InvalidNullException(DeserializationContext ctxt, String msg,
            PropertyName pname)
    {
        this(ctxt == null ? null : ctxt.getParser(), msg, pname);
    }

    public static InvalidNullException from(DeserializationContext ctxt,
            PropertyName name, JavaType type)
    {
        String msg = String.format("Invalid `null` value encountered for property %s",
                ClassUtil.quotedOr(name, "<UNKNOWN>"));
        InvalidNullException exc = new InvalidNullException(ctxt, msg, name);
        if (type != null) {
            exc.setTargetType(type);
        }
        return exc;
    }

    public PropertyName getPropertyName() {
        return _propertyName;
    }
}
