package com.fasterxml.jackson.databind.deser.impl;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.NullValueProvider;
import com.fasterxml.jackson.databind.util.AccessPattern;

/**
 * Simple {@link NullValueProvider} that will simply return given
 * constant value when a null is encountered; or, with a specially
 * constructed instance (see {@link #skipper}, indicate the need
 * for special behavior of skipping property altogether (not setting
 * as anything OR throwing exception).
 */
public class NullsConstantProvider
    implements NullValueProvider, java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    private final static NullsConstantProvider SKIPPER = new NullsConstantProvider(null);

    private final static NullsConstantProvider NULLER = new NullsConstantProvider(null);

    protected final Object _nullValue;

    protected final AccessPattern _access;

    protected NullsConstantProvider(Object nvl) {
        _nullValue = nvl;
        _access = (_nullValue == null) ? AccessPattern.ALWAYS_NULL
                : AccessPattern.CONSTANT;
    }

    /**
     * Static accessor for a stateless instance used as marker, to indicate
     * that all input `null` values should be skipped (ignored), so that
     * no corresponding property value is set (with POJOs), and no content
     * values (array/Collection elements, Map entries) are added.
     */
    public static NullsConstantProvider skipper() {
        return SKIPPER;
    }

    public static NullsConstantProvider nuller() {
        return NULLER;
    }

    public static NullsConstantProvider forValue(Object nvl) {
        if (nvl == null) {
            return NULLER;
        }
        return new NullsConstantProvider(nvl);
    }

    /**
     * Utility method that can be used to check if given null value provider
     * is "skipper", marker provider that means that all input `null`s should
     * be skipped (ignored), instead of converted
     */
    public static boolean isSkipper(NullValueProvider p) {
        return (p == SKIPPER);
    }

    /**
     * Utility method that can be used to check if given null value provider
     * is "nuller", no-operation provider that will always simply return
     * Java `null` for any and all input `null`s.
     */
    public static boolean isNuller(NullValueProvider p) {
        return (p == NULLER);
    }

    @Override
    public AccessPattern getNullAccessPattern() {
        return _access;
    }

    @Override
    public Object getNullValue(DeserializationContext ctxt) {
        return _nullValue;
    }
}
