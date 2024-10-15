package com.fasterxml.jackson.databind.exc;

import java.io.*;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.testutil.BrokenStringWriter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.verifyException;

/**
 * Unit test for verifying that exceptions are properly handled (caught,
 * re-thrown or wrapped, depending)
 * with Object serialization.
 */
public class TestExceptionsDuringWriting
{
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    static class Bean {
        // no methods, we'll use our custom serializer
    }

    static class SerializerWithErrors
        extends JsonSerializer<Bean>
    {
        @Override
        public void serialize(Bean value, JsonGenerator jgen, SerializerProvider provider)
        {
            throw new IllegalArgumentException("test string");
        }
    }

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */

    /**
     * Unit test that verifies that by default all exceptions except for
     * JacksonExceptions are caught and wrapped.
     */
    @Test
    public void testCatchAndRethrow()
        throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test-exceptions", Version.unknownVersion());
        module.addSerializer(Bean.class, new SerializerWithErrors());
        mapper.registerModule(module);
        try {
            StringWriter sw = new StringWriter();
            /* And just to make things more interesting, let's create
             * a nested data struct...
             */
            Bean[] b = { new Bean() };
            List<Bean[]> l = new ArrayList<Bean[]>();
            l.add(b);
            mapper.writeValue(sw, l);
            fail("Should have gotten an exception");
        } catch (IOException e) {
            // should contain original message somewhere
            verifyException(e, "test string");
            Throwable root = e.getCause();
            assertNotNull(root);

            if (!(root instanceof IllegalArgumentException)) {
                fail("Wrapped exception not IAE, but "+root.getClass());
            }
        }
    }

    /**
     * Unit test for verifying that regular IOExceptions are not wrapped
     * but are passed through as is.
     */
    @Test
    public void testExceptionWithSimpleMapper()
        throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        try (BrokenStringWriter sw = new BrokenStringWriter("TEST")) {
            mapper.writeValue(sw, createLongObject());
            fail("Should have gotten an exception");
        } catch (IOException e) {
            verifyException(e, "TEST");
        }
    }

    @Test
    public void testExceptionWithMapperAndGenerator()
        throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory f = new MappingJsonFactory();
        BrokenStringWriter sw = new BrokenStringWriter("TEST");
        try (JsonGenerator jg = f.createGenerator(sw)) {
            mapper.writeValue(jg, createLongObject());
            fail("Should have gotten an exception");
        } catch (IOException e) {
            verifyException(e, "TEST");
        }
    }

    @Test
    public void testExceptionWithGeneratorMapping()
        throws Exception
    {
        JsonFactory f = new MappingJsonFactory();
        try (JsonGenerator jg = f.createGenerator(new BrokenStringWriter("TEST"))) {
            jg.writeObject(createLongObject());
            fail("Should have gotten an exception");
        } catch (IOException e) {
            verifyException(e, "TEST");
        }
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    Object createLongObject()
    {
        List<Object> leaf = new ArrayList<Object>();
        for (int i = 0; i < 256; ++i) {
            leaf.add(Integer.valueOf(i));
        }
        List<Object> root = new ArrayList<Object>(256);
        for (int i = 0; i < 256; ++i) {
            root.add(leaf);
        }
        return root;
    }
}

