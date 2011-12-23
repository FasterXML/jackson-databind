package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.databind.annotate.JacksonStdImpl;

/**
 * @deprecated Since 1.9 use {@link com.fasterxml.jackson.databind.ser.std.ToStringSerializer}
 */
@Deprecated
@JacksonStdImpl
public final class ToStringSerializer
    extends com.fasterxml.jackson.databind.ser.std.ToStringSerializer
{
    public final static ToStringSerializer instance = new ToStringSerializer();
}
