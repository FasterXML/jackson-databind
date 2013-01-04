package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.Versioned;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.cfg.DatabindVersion;
import com.fasterxml.jackson.databind.cfg.PackageVersion;

/**
 * Tests to ensure that we get proper Version information via
 * things defined as Versioned.
 */
public class TestVersions extends com.fasterxml.jackson.test.BaseTest
{
    private final static String GROUP_ID = "com.fasterxml.jackson.core";
    private final static String ARTIFACT_ID = "jackson-databind";

    public void testMapperVersions()
    {
        ObjectMapper mapper = new ObjectMapper();
        assertVersion(mapper);
        assertVersion(mapper.reader());
        assertVersion(mapper.writer());
        assertVersion(new JacksonAnnotationIntrospector());
    }

    public void testDatabindVersion()
    {
        assertEquals(PackageVersion.VERSION, DatabindVersion.instance.version());
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
    
    private void assertVersion(Versioned vers)
    {
        Version v = vers.version();
        assertFalse("Should find version information (got "+v+")", v.isUknownVersion());
        assertEquals(PackageVersion.VERSION, v);
    }
}

