package com.fasterxml.jackson.databind.mixins;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class MixinsCircularTest extends DatabindTestUtil {

    static class First {
        @JsonProperty("first-mixin")
        public String value;
    }

    static class Second {
        @JsonProperty("second-mixin")
        public String value;

    }

    static class Third {
        @JsonProperty("third-mixin")
        public String value;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    @Test
    public void testPojoMixinDeserialization() throws Exception {
        ObjectMapper mxMapper = jsonMapperBuilder()
            .addMixIn(First.class, Second.class)
            .addMixIn(Second.class, Third.class)
            .addMixIn(Third.class, First.class)
            .build();

        // first deserialized from second
        First first = mxMapper.readValue(a2q("{'second-mixin':'second-mixin'}"), First.class);
        assertEquals("second-mixin", first.value);

        // second deserialized from third
        Second second = mxMapper.readValue(a2q("{'third-mixin':'third-mixin'}"), Second.class);
        assertEquals("third-mixin", second.value);

        // third deserialized from first
        Third third = mxMapper.readValue(a2q("{'first-mixin':'first-mixin'}"), Third.class);
        assertEquals("first-mixin", third.value);
    }

    @Test
    public void testPojoMixinSerialization() throws Exception {
        ObjectMapper mxMapper = jsonMapperBuilder()
            .addMixIn(First.class, Second.class)
            .addMixIn(Second.class, Third.class)
            .addMixIn(Third.class, First.class)
            .build();

        // first serialized as second
        First firstBean = new First();
        firstBean.value = "value";
        assertEquals(
            a2q("{'second-mixin':'value'}"),
            mxMapper.writeValueAsString(firstBean));

        // second serialized as third
        Second secondBean = new Second();
        secondBean.value = "value";
        assertEquals(
            a2q("{'third-mixin':'value'}"),
            mxMapper.writeValueAsString(secondBean));

        // third serialized as first
        Third thirdBean = new Third();
        thirdBean.value = "value";
        assertEquals(
            a2q("{'first-mixin':'value'}"),
            mxMapper.writeValueAsString(thirdBean));
    }
}
