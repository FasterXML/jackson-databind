package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.core.Version;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests to verify [JACKSON-278]
 */
public class TestVersions extends com.fasterxml.jackson.test.BaseTest
{
    /**
     * 18-Nov-2010, tatu: Not a good to do this, but has to do, for now...
     */
    private final static int MAJOR_VERSION = 2;
    private final static int MINOR_VERSION = 0;

    public void testMapperVersions()
    {
        ObjectMapper mapper = new ObjectMapper();
        assertVersion(mapper.version(), MAJOR_VERSION, MINOR_VERSION);
        assertVersion(mapper.writer().version(), MAJOR_VERSION, MINOR_VERSION);
        assertVersion(mapper.reader().version(), MAJOR_VERSION, MINOR_VERSION);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
    
    private void assertVersion(Version v, int major, int minor)
    {
        assertFalse("Should find version information (got "+v+")", v.isUknownVersion());
        assertEquals(major, v.getMajorVersion());
        assertEquals(minor, v.getMinorVersion());
        // 07-Jan-2011, tatus: Check patch level initially, comment out for maint versions

        //assertEquals(0, v.getPatchLevel());
    }
}

