package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.core.util.VersionUtil;

/**
 * Helper class used for finding and caching version information
 * for the databind bundle.
 * NOTE: although defined as public, should NOT be accessed directly
 * from outside databind bundle itself.
 */
public class DatabindVersion extends VersionUtil
{
    public final static DatabindVersion instance = new DatabindVersion();
}
