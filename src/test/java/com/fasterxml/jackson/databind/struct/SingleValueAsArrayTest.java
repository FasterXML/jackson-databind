package com.fasterxml.jackson.databind.struct;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class SingleValueAsArrayTest extends DatabindTestUtil
{
    static class Bean1421A
    {
        List<Messages> bs = Collections.emptyList();

        @JsonCreator
        Bean1421A(final List<Messages> bs)
        {
            this.bs = bs;
        }
    }

    static class Messages
    {
        List<MessageWrapper> cs = Collections.emptyList();

        @JsonCreator
        Messages(final List<MessageWrapper> cs)
        {
            this.cs = cs;
        }
    }

    static class MessageWrapper
    {
        String message;

        @JsonCreator
        MessageWrapper(@JsonProperty("message") String message)
        {
            this.message = message;
        }
    }

    static class Bean1421B<T> {
        T value;

        @JsonCreator
        public Bean1421B(T value) {
            this.value = value;
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();
    {
        MAPPER.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    }

    @Test
    public void testSuccessfulDeserializationOfObjectWithChainedArrayCreators() throws IOException
    {
        Bean1421A result = MAPPER.readValue("[{\"message\":\"messageHere\"}]", Bean1421A.class);
        assertNotNull(result);
        assertNotNull(result.bs);
        assertEquals(1, result.bs.size());
    }

    @Test
    public void testWithSingleString() throws Exception {
        Bean1421B<List<String>> a = MAPPER.readValue(q("test2"),
                new TypeReference<Bean1421B<List<String>>>() {});
        List<String> expected = new ArrayList<>();
        expected.add("test2");
        assertEquals(expected, a.value);
    }

    @Test
    public void testPrimitives() throws Exception {
        int[] i = MAPPER.readValue("16", int[].class);
        assertEquals(1, i.length);
        assertEquals(16, i[0]);

        long[] l = MAPPER.readValue("1234", long[].class);
        assertEquals(1, l.length);
        assertEquals(1234L, l[0]);

        double[] d = MAPPER.readValue("12.5", double[].class);
        assertEquals(1, d.length);
        assertEquals(12.5, d[0]);

        boolean[] b = MAPPER.readValue("true", boolean[].class);
        assertEquals(1, d.length);
        assertEquals(true, b[0]);
    }
}
