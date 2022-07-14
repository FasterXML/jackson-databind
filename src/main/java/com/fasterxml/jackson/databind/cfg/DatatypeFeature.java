package com.fasterxml.jackson.databind.cfg;

import tools.jackson.core.util.JacksonFeature;

/**
 * Interface that defines interaction with data type specific configuration
 * features.
 */
public interface DatatypeFeature extends JacksonFeature
{
    /**
     * Internal index used for efficient storage and index; no
     * user serviceable contents inside!
     */
    public int featureIndex();
}
