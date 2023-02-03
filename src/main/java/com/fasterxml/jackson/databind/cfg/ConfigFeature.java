package com.fasterxml.jackson.databind.cfg;

/**
 * Interface that actual SerializationFeature enumerations used by
 * {@link MapperConfig} implementations must implement.
 * Necessary since enums cannot be extended using normal
 * inheritance, but can implement interfaces
 */
public interface ConfigFeature
{
    /**
     * Accessor for checking whether this feature is enabled by default.
     */
    public boolean enabledByDefault();

    /**
     * Returns bit mask for this feature instance
     */
    public int getMask();

    /**
     * Convenience method for checking whether feature is enabled in given bitmask
     *
     * @since 2.6
     */
    public boolean enabledIn(int flags);
}
