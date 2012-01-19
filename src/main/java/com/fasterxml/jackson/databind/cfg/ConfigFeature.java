package com.fasterxml.jackson.databind.cfg;

/**
 * Interface that actual Feature enumerations used by
 * {@link MapperConfig} implementations must implement.
 * Necessary since enums can not be extended using normal
 * inheritance, but can implement interfaces
 */
public interface ConfigFeature
{
    /**
     * Accessor for checking whether this feature is enabled by default.
     */
    public boolean enabledByDefault();

    /**
     * Accessor for checking whether feature can be used on per-call basis
     * (true), or not (false): in latter case it can only be configured once
     * before any serialization or deserialization.
     */
    public boolean canUseForInstance();
    
    /**
     * Returns bit mask for this feature instance
     */
    public int getMask();
}
