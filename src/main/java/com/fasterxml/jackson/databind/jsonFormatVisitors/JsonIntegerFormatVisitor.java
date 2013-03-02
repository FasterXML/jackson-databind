package com.fasterxml.jackson.databind.jsonFormatVisitors;

import com.fasterxml.jackson.core.JsonParser;

public interface JsonIntegerFormatVisitor extends JsonValueFormatVisitor
{
    /**
     * Method called to provide more exact type of number being serialized
     * (regardless of logical type, which may be {@link java.util.Date} or
     * {@link java.lang.Enum}, in addition to actual numeric types like
     * {@link java.lang.Integer}).
     */
    public void numberType(JsonParser.NumberType type);

    /**
     * Default "empty" implementation, useful as the base to start on;
     * especially as it is guaranteed to implement all the method
     * of the interface, even if new methods are getting added.
     */
    public static class Base extends JsonValueFormatVisitor.Base
        implements JsonIntegerFormatVisitor {
        @Override
        public void numberType(JsonParser.NumberType type) { }
    }
}
