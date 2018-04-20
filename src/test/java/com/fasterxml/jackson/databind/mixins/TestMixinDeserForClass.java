package com.fasterxml.jackson.databind.mixins;

import java.io.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import com.fasterxml.jackson.databind.*;

public class TestMixinDeserForClass
    extends BaseMapTest
{
    static class BaseClass
    {
        /* property that is always found; but has lower priority than
         * setter method if both found
         */
        @JsonProperty
        public String a;

        // setter that may or may not be auto-detected
        public void setA(String v) { a = "XXX"+v; }
    }

    @JsonAutoDetect(setterVisibility=Visibility.ANY, fieldVisibility=Visibility.ANY)
    static class LeafClass
        extends BaseClass { }

    @JsonAutoDetect(setterVisibility=Visibility.NONE, fieldVisibility=Visibility.NONE)
    interface MixIn { }

    // [databind#1990]
    public interface HashCodeMixIn {
        @Override
        @JsonProperty
        public int hashCode();
    }

    public class Bean1990WithoutHashCode {
    }

    public class Bean1990WithHashCode {
        @Override
        public int hashCode() { return 13; }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testClassMixInsTopLevel() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        // First: test default behavior: should use setter
        LeafClass result = m.readValue("{\"a\":\"value\"}", LeafClass.class);
        assertEquals("XXXvalue", result.a);

        // Then with leaf-level mix-in; without (method) auto-detect,
        // should use field
        m = new ObjectMapper();
        m.addMixIn(LeafClass.class, MixIn.class);
        result = m.readValue("{\"a\":\"value\"}", LeafClass.class);
        assertEquals("value", result.a);
    }

    // and then a test for mid-level mixin; should have no effect
    // when deserializing leaf (but will if deserializing base class)
    public void testClassMixInsMidLevel() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        m.addMixIn(BaseClass.class, MixIn.class);
        {
            BaseClass result = m.readValue("{\"a\":\"value\"}", BaseClass.class);
            assertEquals("value", result.a);
        }

        // whereas with leaf class, reverts to default
        {
            LeafClass result = m.readValue("{\"a\":\"value\"}", LeafClass.class);
            assertEquals("XXXvalue", result.a);
        }
    }

    /* Also: when mix-in attached to Object.class, will work, if
     * visible (similar to mid-level, basically)
     */
    public void testClassMixInsForObjectClass() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        m.addMixIn(Object.class, MixIn.class);
        // will be seen for BaseClass
        {
            BaseClass result = m.readValue("{\"a\":\"\"}", BaseClass.class);
            assertEquals("", result.a);
        }

        // but LeafClass still overrides
        {
            LeafClass result = m.readValue("{\"a\":\"\"}", LeafClass.class);
            assertEquals("XXX", result.a);
        }
    }

    // [databind#1990]: can apply mix-in to `Object#hashCode()` to force serialization
    public void testHashCodeViaObject() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper()
                .addMixIn(Object.class, HashCodeMixIn.class);

        // First, with something that overrides hashCode()
        assertEquals( "{\"hashCode\":13}",
                mapper.writeValueAsString(new Bean1990WithHashCode()));

        // and then special case of accessing Object#hashCode()
        String prefix = "{\"hashCode\":";
        String json = mapper.writeValueAsString(new Bean1990WithoutHashCode());
        if (!json.startsWith(prefix)) {
            fail("Should start with ["+prefix+"], does not: ["+json+"]");
        }
    }
}
