package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.WrappedIOException;

/**
 * Exception used to signal fatal problems with mapping of
 * content, distinct from low-level I/O problems (signaled using
 * simple {@link WrappedIOException}s) or data encoding/decoding
 * problems (signaled with {@link com.fasterxml.jackson.core.exc.StreamReadException},
 * {@link com.fasterxml.jackson.core.exc.StreamWriteException}).
 *<p>
 * One additional feature is the ability to denote relevant path
 * of references (during serialization/deserialization) to help in
 * troubleshooting.
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
