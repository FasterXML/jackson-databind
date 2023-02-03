package com.fasterxml.jackson.databind.util;

import java.util.Collection;
import java.util.Collections;
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
     * Factory method for creating and return a {@link Checker} instance if (and only if)
     * one needed.
     *
     * @param toIgnore Set of property names to ignore (may be null)
     * @param toInclude Set of only property names to include (if null, undefined)
     *
     * @return Checker, if validity checks are needed; {@code null} otherwise
     */
    public static Checker buildCheckerIfNeeded(Set<String> toIgnore, Set<String> toInclude) {
        // First: no-op case
        if ((toInclude == null) && ((toIgnore == null) || toIgnore.isEmpty())) {
            return null;
        }
        return Checker.construct(toIgnore, toInclude);
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

    /**
     * Helper class to encapsulate logic from static {@code shouldIgnore} method
     * of util class.
     */
    public final static class Checker
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

        private final Set<String> _toIgnore;
        private final Set<String> _toInclude;

        private Checker(Set<String> toIgnore, Set<String> toInclude) {
            if (toIgnore == null) {
                toIgnore = Collections.emptySet();
            }
            _toIgnore = toIgnore;
            _toInclude = toInclude;
        }

        public static Checker construct(Set<String> toIgnore, Set<String> toInclude) {
            return new Checker(toIgnore, toInclude);
        }

        // May seem odd but during serialization key is not cast up to String:
        public boolean shouldIgnore(Object propertyName) {
            return ((_toInclude != null) && !_toInclude.contains(propertyName))
                    || _toIgnore.contains(propertyName);
        }
    }
}
