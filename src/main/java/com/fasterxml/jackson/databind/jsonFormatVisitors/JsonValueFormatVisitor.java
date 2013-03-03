package com.fasterxml.jackson.databind.jsonFormatVisitors;

import java.util.Set;

public interface JsonValueFormatVisitor {
    /**
     * Method called to indicate configured format for value type being visited.
     */
    void format(JsonValueFormat format);

    /**
     * Method called to indicate enumerated (String) values type being visited
     * can take as values.
     */
    void enumTypes(Set<String> enums);

    /**
     * Default "empty" implementation, useful as the base to start on;
     * especially as it is guaranteed to implement all the method
     * of the interface, even if new methods are getting added.
     */
    public static class Base implements JsonValueFormatVisitor {
        @Override
        public void format(JsonValueFormat format) { }
        @Override
        public void enumTypes(Set<String> enums) { }
    }
}
