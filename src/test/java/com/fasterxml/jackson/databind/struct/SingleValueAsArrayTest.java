package com.fasterxml.jackson.databind.struct;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.*;

public class SingleValueAsArrayTest extends BaseMapTest
{
    private static final String JSON = "[{\"message\":\"messageHere\"}]";

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
    
    private final ObjectMapper MAPPER = new ObjectMapper();
    {
        MAPPER.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    }

    public void testSuccessfulDeserializationOfObjectWithChainedArrayCreators() throws IOException
    {
        MAPPER.readValue(JSON, Bean1421A.class);
    }

    public void testWithSingleString() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        Bean1421B<List<String>> a = objectMapper.readValue(quote("test2"),
                new TypeReference<Bean1421B<List<String>>>() {});
        List<String> expected = new ArrayList<>();
        expected.add("test2");
        assertEquals(expected, a.value);
    }
}
