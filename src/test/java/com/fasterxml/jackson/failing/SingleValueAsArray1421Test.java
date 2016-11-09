package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class SingleValueAsArray1421Test
{
    private static final String JSON = "[{\"message\":\"messageHere\"}]";

    static class A
    {
        List<B> bs = Collections.emptyList();

        @JsonCreator
        A(final List<B> bs)
        {
            this.bs = bs;
        }
    }

    static class B
    {
        List<C> cs = Collections.emptyList();

        @JsonCreator
        B(final List<C> cs)
        {
            this.cs = cs;
        }
    }

    public static class C
    {
        String message;

        @JsonCreator
        C(@JsonProperty("message") String message)
        {
            this.message = message;
        }
    }

    @Test
    public void testSuccessfulDeserializationOfObjectWithChainedArrayCreators() throws IOException
    {
        ObjectMapper om = new ObjectMapper();
        om.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        om.readValue(JSON, A.class);
    }
}
