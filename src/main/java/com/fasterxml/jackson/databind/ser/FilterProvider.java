package com.fasterxml.jackson.databind.ser;

/**
 * Interface for objects that providers instances of {@link PropertyFilter}
 * that match given ids. A provider is configured to be used during serialization,
 * to find filter to used based on id specified by {@link com.fasterxml.jackson.annotation.JsonFilter}
 * annotation on bean class.
 */
public abstract class FilterProvider
{
    /**
     * Lookup method used to find {@link PropertyFilter} that has specified id.
     * Note that id is typically a {@link java.lang.String}, but is not necessarily
     * limited to that; that is, while standard components use String, custom
     * implementation can choose other kinds of keys.
     *
     * @param filterId Id of the filter to fetch
     * @param valueToFilter Object being filtered (usually POJO, but may be a {@link java.util.Map},
     *   or in future a container), <b>if available</b>; not available when generating
     *   schemas.
     * 
     * @return Filter to use, if any.
     */
    public abstract PropertyFilter findPropertyFilter(Object filterId, Object valueToFilter);
}
