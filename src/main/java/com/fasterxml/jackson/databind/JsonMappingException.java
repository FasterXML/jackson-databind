package com.fasterxml.jackson.databind;

import java.io.Closeable;

import com.fasterxml.jackson.core.*;

/**
 * @deprecated Since 3.0
 */
@Deprecated
public class JsonMappingException
    extends DatabindException
{
    private static final long serialVersionUID = 3L;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public JsonMappingException(Closeable processor, String msg) {
        super(processor, msg);
    }

    public JsonMappingException(Closeable processor, String msg, Throwable problem) {
        super(processor, msg, problem);
    }

    public JsonMappingException(Closeable processor, String msg, JsonLocation loc) {
        super(msg, loc);
        _processor = processor;
    }
}
