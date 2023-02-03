package com.fasterxml.jackson.databind.exc;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;

/**
 * Specialized sub-class of {@link MismatchedInputException}
 * that is used when the underlying problem appears to be that
 * of bad formatting of a value to deserialize.
 *
 * @since 2.1
 */
public class InvalidFormatException
    extends MismatchedInputException // since 2.9
{
    private static final long serialVersionUID = 1L; // silly Eclipse, warnings

    /**
     * Underlying value that could not be deserialized into
     * target type, if available.
     */
    protected final Object _value;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    /**
     * @deprecated Since 2.7 Use variant that takes {@link JsonParser}
     */
    @Deprecated // since 2.7
    public InvalidFormatException(String msg,
            Object value, Class<?> targetType)
    {
        super(null, msg);
        _value = value;
        _targetType = targetType;
    }

    /**
     * @deprecated Since 2.7 Use variant that takes {@link JsonParser}
     */
    @Deprecated // since 2.7
    public InvalidFormatException(String msg, JsonLocation loc,
            Object value, Class<?> targetType)
    {
        super(null, msg, loc);
        _value = value;
        _targetType = targetType;
    }

    /**
     * @since 2.7
     */
    public InvalidFormatException(JsonParser p,
            String msg, Object value, Class<?> targetType)
    {
        super(p, msg, targetType);
        _value = value;
    }

    public static InvalidFormatException from(JsonParser p, String msg,
            Object value, Class<?> targetType)
    {
        return new InvalidFormatException(p, msg, value, targetType);
    }

    /*
    /**********************************************************
    /* Additional accessors
    /**********************************************************
     */

    /**
     * Accessor for checking source value (String, Number usually) that could not
     * be deserialized into target type ({@link #getTargetType}).
     * Note that value may not be available, depending on who throws the exception
     * and when.
     */
    public Object getValue() {
        return _value;
    }
}
