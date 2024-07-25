package com.fasterxml.jackson.databind.jsontype;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import com.fasterxml.jackson.databind.testutil.NoCheckSubTypeValidator;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class NoTypeInfoTest extends DatabindTestUtil
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    static class Value1654 {
        public int x;

        protected Value1654() {
        }

        public Value1654(int x) {
            this.x = x;
        }
    }

    static class Value1654TypedContainer {
        public List<Value1654> values;

        protected Value1654TypedContainer() {
        }

        public Value1654TypedContainer(Value1654... v) {
            values = Arrays.asList(v);
        }
    }

    @JsonTypeInfo(use=JsonTypeInfo.Id.NONE)
    @JsonDeserialize(as=NoType.class)
    static interface NoTypeInterface {
    }

    final static class NoType implements NoTypeInterface {
        public int a = 3;
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    @Test
    public void testWithIdNone() throws Exception
    {
        final ObjectMapper mapper = jsonMapperBuilder()
            .activateDefaultTyping(NoCheckSubTypeValidator.instance)
            .build();
        // serialize without type info
        String json = mapper.writeValueAsString(new NoType());
        assertEquals("{\"a\":3}", json);

        // and deserialize successfully
        NoTypeInterface bean = mapper.readValue("{\"a\":6}", NoTypeInterface.class);
        assertNotNull(bean);
        NoType impl = (NoType) bean;
        assertEquals(6, impl.a);
    }

    // [databind#1654]
    @Test
    void noTypeElementOverride() throws Exception {
        // regular typed case
        final ObjectMapper mapper = newJsonMapper();
        String json = mapper.writeValueAsString(new Value1654TypedContainer(
            new Value1654(1),
            new Value1654(2),
            new Value1654(3)
        ));
        Value1654TypedContainer result = mapper.readValue(json, Value1654TypedContainer.class);
        assertEquals(3, result.values.size());
        assertEquals(2, result.values.get(1).x);
    }
}
