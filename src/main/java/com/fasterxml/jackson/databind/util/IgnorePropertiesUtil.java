package com.fasterxml.jackson.databind.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @since 2.12
 */
public class IgnorePropertiesUtil
{
    /**
     * Decide if we need to ignore a property or not, given a set of field to ignore and a set of field to include.
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

    /**
     * Helper that encapsulates logic for combining two sets of "included names":
     * default logic is to do intersection (name must be in both to be included
     * in result)
     * 
     * @param prevToInclude Existing set of names to include, if defined; null means "not defined"
     * @param newToInclude New set of names to included, if defined; null means "not defined"
     *
     * @return Resulting set of names, using intersection if neither {@code null}; or the
     *    non-null one (if only one is {@code null}); or {@code null} if both arguments {@code null}.
     */
    public static Set<String> combineNamesToInclude(Set<String> prevToInclude,
            Set<String> newToInclude) {
        if (prevToInclude == null) {
            return newToInclude;
        }
        if (newToInclude == null) {
            return prevToInclude;
        }
        final Set<String> result = new HashSet<>();

        // Make the intersection with the previously included properties
        for (String prop : newToInclude) {
            if (prevToInclude.contains(prop)) {
                result.add(prop);
            }
        }
        return result;
    }
}
