package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.impl.ReadableObjectId;

public final class UnresolvedForwardReference extends JsonMappingException {
    private static final long serialVersionUID = -5097969645059502061L;
    private final ReadableObjectId _roid;

    public UnresolvedForwardReference(String msg, JsonLocation loc, ReadableObjectId roid)
    {
        super(msg, loc);
        _roid = roid;
    }

    public ReadableObjectId getRoid()
    {
        return _roid;
    }

    public Object getUnresolvedId()
    {
        return _roid.id;
    }
}
