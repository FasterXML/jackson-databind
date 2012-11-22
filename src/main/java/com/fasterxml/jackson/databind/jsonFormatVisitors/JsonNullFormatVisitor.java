package com.fasterxml.jackson.databind.jsonFormatVisitors;

public interface JsonNullFormatVisitor {
    /**
     * Default "empty" implementation, useful as the base to start on;
     * especially as it is guaranteed to implement all the method
     * of the interface, even if new methods are getting added.
     */
    public static class Base implements JsonNullFormatVisitor { }
}
