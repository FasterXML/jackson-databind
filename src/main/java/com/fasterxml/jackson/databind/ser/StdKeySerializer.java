package com.fasterxml.jackson.databind.ser;

/**
 * @deprecated Since 1.9 use {@link com.fasterxml.jackson.databind.ser.std.StdKeySerializer} instead
 */
@Deprecated
public final class StdKeySerializer
    extends com.fasterxml.jackson.databind.ser.std.StdKeySerializer
{
    final static StdKeySerializer instace = new StdKeySerializer();
}
