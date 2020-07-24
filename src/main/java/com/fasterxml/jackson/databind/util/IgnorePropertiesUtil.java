package com.fasterxml.jackson.databind.util;

import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;

import java.util.Collection;
import java.util.Set;

public class IgnorePropertiesUtil
{
    /**
     * Decide if we need to ignore a property or not, given a set of field to ignore and a set of field to include.
     *
     * @since 2.12
     */
    public static boolean shouldIgnore(Object value, Collection<String> toIgnore, Collection<String> toInclude) {
        if (toIgnore == null && toInclude ==null) {
            return false;
        }

        if (toInclude == null) {
            return toIgnore.contains(value);
        }

        if (toIgnore == null) {
            return !toInclude.contains(value);
        }

        // NOTE: conflict between both, JsonIncludeProperties will take priority.
        return !toInclude.contains(value) || toIgnore.contains(value);
    }
}
