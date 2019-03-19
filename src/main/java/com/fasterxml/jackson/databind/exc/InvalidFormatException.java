package com.fasterxml.jackson.databind.exc;

import com.fasterxml.jackson.core.JsonParser;

/**
 * Specialized sub-class of {@link MismatchedInputException}
 * that is used when the underlying problem appears to be that
 * of bad formatting of a value to deserialize.
 */
public class InvalidFormatException
    extends MismatchedInputException
{
    private static final long serialVersionUID = 1L;

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
