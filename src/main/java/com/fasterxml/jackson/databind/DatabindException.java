package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Intermediate base class for all databind level processing problems, as
 * distinct from stream-level problems or I/O issues below.
 *<p>
 * Added in 2.13 to eventually replace {@link com.fasterxml.jackson.databind.JsonMappingException};
 * for 2.x will allow limited use as target (as catching it will also catch mapping exception)
 * but will not be constructed or thrown directly.
 *
 * @since 2.13
 */
public abstract class DatabindException
    extends JsonProcessingException
{
    private static final long serialVersionUID = 3L;

    protected DatabindException(String msg, JsonLocation loc, Throwable rootCause) {
        super(msg, loc, rootCause);
    }

    protected DatabindException(String msg) {
        super(msg);
    }

    protected DatabindException(String msg, JsonLocation loc) {
        this(msg, loc, null);
    }

    protected DatabindException(String msg, Throwable rootCause) {
        this(msg, null, rootCause);
    }

    /**
     * Method called to prepend a reference information in front of
     * current path
     */
    public abstract void prependPath(Object referrer, String fieldName);

    /**
     * Method called to prepend a reference information in front of
     * current path
     */
    public abstract void prependPath(Object referrer, int index);
}
