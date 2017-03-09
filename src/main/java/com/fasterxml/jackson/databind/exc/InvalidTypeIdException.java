package com.fasterxml.jackson.databind.exc;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;

/**
 * Exception thrown when resolution of a type id fails.
 *
 * @since 2.8
 */
public class InvalidTypeIdException
    extends MismatchedInputException // since 2.9
{
    private static final long serialVersionUID = 1L; // silly Eclipse, warnings

    /**
     * Basetype for which subtype was to be resolved
     */
    protected final JavaType _baseType;

    /**
     * Type id that failed to be resolved to a subtype; `null` in cases
     * where no type id was located (since 2.9).
     */
    protected final String _typeId;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public InvalidTypeIdException(JsonParser p, String msg,
            JavaType baseType, String typeId)
    {
        super(p, msg);
        _baseType = baseType;
        _typeId = typeId;
    }

    public static InvalidTypeIdException from(JsonParser p, String msg,
            JavaType baseType, String typeId) {
        return new InvalidTypeIdException(p, msg, baseType, typeId);
    }

    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

    public JavaType getBaseType() { return _baseType; }
    public String getTypeId() { return _typeId; }
}
