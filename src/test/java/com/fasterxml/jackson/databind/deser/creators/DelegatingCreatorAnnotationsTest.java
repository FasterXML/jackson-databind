package com.fasterxml.jackson.databind.deser.creators;

import java.io.IOException;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;

import com.fasterxml.jackson.core.JsonParser;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

// Tests for problems uncovered with [databind#2016]; related to
// `@JsonDeserialize` modifications to type, deserializer(s)
@SuppressWarnings("serial")
public class DelegatingCreatorAnnotationsTest
{
    // [databind#2016]
    static class Wrapper2016As {
        Object value;

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public Wrapper2016As(@JsonDeserialize(as = java.util.Date.class) Object v) {
            value = v;
        }
    }

    static class Wrapper2016ContentAs {
        List<Object> value;

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public Wrapper2016ContentAs(@JsonDeserialize(contentAs = java.util.Date.class) List<Object> v) {
            value = v;
        }
    }

    // [databind#2021]
    static class DelegatingWithCustomDeser2021 {
        public final static Double DEFAULT = 0.25;

        Number value;

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public DelegatingWithCustomDeser2021(@JsonDeserialize(using = ValueDeser2021.class) Number v) {
            value = v;
        }
    }

    static class ValueDeser2021 extends StdDeserializer<Number> {
        public ValueDeser2021() { super(Number.class); }

        @Override
        public Number deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException
        {
            p.skipChildren();
            return DelegatingWithCustomDeser2021.DEFAULT;
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#2016]
    @Test
    public void testDelegatingWithAs() throws Exception
    {
        Wrapper2016As actual = MAPPER.readValue("123", Wrapper2016As.class);
        assertEquals(Date.class, actual.value.getClass());
    }

    // [databind#2016]
    @Test
    public void testDelegatingWithContentAs() throws Exception
    {
        Wrapper2016ContentAs actual = MAPPER.readValue("[123]", Wrapper2016ContentAs.class);
        List<Object> l = actual.value;
        assertEquals(1, l.size());
        assertEquals(Date.class, l.get(0).getClass());
    }

    // [databind#2021]
    @Test
    public void testCustomDeserForDelegating() throws Exception
    {
        DelegatingWithCustomDeser2021 actual = MAPPER.readValue(" true ", DelegatingWithCustomDeser2021.class);
        assertEquals(DelegatingWithCustomDeser2021.DEFAULT, actual.value);
    }
}
