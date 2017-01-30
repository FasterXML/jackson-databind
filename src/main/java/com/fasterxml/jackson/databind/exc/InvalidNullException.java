package com.fasterxml.jackson.databind.exc;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Exception thrown if a `null` value is being encountered for a property
 * designed as "fail on null" property (see {@link com.fasterxml.jackson.annotation.JsonSetter}).
 *
 * @since 2.9
 */
public class InvalidNullException
    extends MismatchedInputException // since 2.9
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

    protected InvalidNullException(DeserializationContext ctxt, String msg,
            PropertyName pname)
    {
        super(ctxt.getParser(), msg);
        _propertyName = pname;
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
