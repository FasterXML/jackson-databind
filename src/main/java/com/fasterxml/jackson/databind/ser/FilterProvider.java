package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;

/**
 * Interface for objects that providers instances of {@link PropertyFilter}
 * that match given ids. A provider is configured to be used during serialization,
 * to find filter to used based on id specified by {@link com.fasterxml.jackson.annotation.JsonFilter}
 * annotation on bean class.
 */
public abstract class FilterProvider
{
    /**
     * Lookup method used to find {@link BeanPropertyFilter} that has specified id.
     * Note that id is typically a {@link java.lang.String}, but is not necessarily
     * limited to that; that is, while standard components use String, custom
     * implementation can choose other kinds of keys.
     *
     * @return Filter registered with specified id, if one defined; null if
     *   none found.
     *
     * @deprecated Since 2.3 deprecated because {@link BeanPropertyFilter} is deprecated;
     */
    @Deprecated
    public abstract BeanPropertyFilter findFilter(Object filterId);

    /**
     * Lookup method used to find {@link PropertyFilter} that has specified id.
     * Note that id is typically a {@link java.lang.String}, but is not necessarily
     * limited to that; that is, while standard components use String, custom
     * implementation can choose other kinds of keys.
     *<p>
     * This method is the replacement for {@link #findFilter} starting with 2.3.
     *<p>
     * Note that the default implementation is designed to support short-term
     * backwards compatibility, and will call the deprecated <code>findFilter</code>
     * method, then wrap filter if one found as {@link PropertyFilter}.
     * It should be overridden by up-to-date implementations
     *
     * @param filterId Id of the filter to fetch
     * @param valueToFilter Object being filtered (usually POJO, but may be a {@link java.util.Map},
     *   or in future a container), <b>if available</b>; not available when generating
     *   schemas.
     *
     * @return Filter to use, if any.
     *
     * @since 2.3
     */
    public PropertyFilter findPropertyFilter(Object filterId, Object valueToFilter)
    {
        @SuppressWarnings("deprecation")
        BeanPropertyFilter old = findFilter(filterId);
        if (old == null) {
            return null;
        }
        return SimpleBeanPropertyFilter.from(old);
    }
}
