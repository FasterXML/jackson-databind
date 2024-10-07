package tools.jackson.databind.module;

import org.junit.jupiter.api.Test;

import tools.jackson.core.Version;
import tools.jackson.databind.jsontype.NamedType;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class SimpleModuleArgCheckTest extends DatabindTestUtil
{
    /*
    /**********************************************************
    /* Unit tests for invalid deserializers
    /**********************************************************
     */

    @Test
    public void testInvalidForDeserializers() throws Exception
    {
        SimpleModule mod = new SimpleModule("test", Version.unknownVersion());
        try {
            mod.addDeserializer(String.class, null);
            fail("Should not pass");
        } catch (IllegalArgumentException e) {
            verifyException(e, "Cannot pass `null` as deserializer");
        }

        try {
            mod.addKeyDeserializer(String.class, null);
            fail("Should not pass");
        } catch (IllegalArgumentException e) {
            verifyException(e, "Cannot pass `null` as key deserializer");
        }
    }

    /*
    /**********************************************************
    /* Unit tests for invalid misc other
    /**********************************************************
     */

    @Test
    public void testInvalidAbstractTypeMapping() throws Exception
    {
        SimpleModule mod = new SimpleModule("test", Version.unknownVersion());
        try {
            mod.addAbstractTypeMapping(null, String.class);
            fail("Should not pass");
        } catch (IllegalArgumentException e) {
            verifyException(e, "Cannot pass `null` as abstract type to map");
        }
        try {
            mod.addAbstractTypeMapping(String.class, null);
            fail("Should not pass");
        } catch (IllegalArgumentException e) {
            verifyException(e, "Cannot pass `null` as concrete type to map to");
        }
    }

    @Test
    public void testInvalidSubtypeMappings() throws Exception
    {
        SimpleModule mod = new SimpleModule("test", Version.unknownVersion());
        try {
            mod.registerSubtypes(String.class, null);
            fail("Should not pass");
        } catch (IllegalArgumentException e) {
            verifyException(e, "Cannot pass `null` as subtype to register");
        }

        try {
            mod.registerSubtypes(new NamedType(Integer.class), (NamedType) null);
            fail("Should not pass");
        } catch (IllegalArgumentException e) {
            verifyException(e, "Cannot pass `null` as subtype to register");
        }
    }

    @Test
    public void testInvalidValueInstantiator() throws Exception
    {
        SimpleModule mod = new SimpleModule("test", Version.unknownVersion());

        try {
            mod.addValueInstantiator(null, null);
            fail("Should not pass");
        } catch (IllegalArgumentException e) {
            verifyException(e, "Cannot pass `null` as class to register value instantiator for");
        }
        try {
            mod.addValueInstantiator(CharSequence.class, null);
            fail("Should not pass");
        } catch (IllegalArgumentException e) {
            verifyException(e, "Cannot pass `null` as value instantiator");
        }
    }

    @Test
    public void testInvalidMixIn() throws Exception
    {
        SimpleModule mod = new SimpleModule("test", Version.unknownVersion());

        try {
            mod.setMixInAnnotation(null, String.class);
            fail("Should not pass");
        } catch (IllegalArgumentException e) {
            verifyException(e, "Cannot pass `null` as target type");
        }
        try {
            mod.setMixInAnnotation(String.class, null);
            fail("Should not pass");
        } catch (IllegalArgumentException e) {
            verifyException(e, "Cannot pass `null` as mixin class");
        }
    }
}
