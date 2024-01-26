package com.fasterxml.jackson.databind.deser.enums;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.a2q;
import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

public class EnumDeserialization3638Test
{
    /*
    /**********************************************************
    /* Set up
    /**********************************************************
     */

    static enum Member
    {
        FIRST_MEMBER,
        SECOND_MEMBER;
    }

    static class SensitiveBean
    {
        @JsonFormat(without = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
        public Member enumValue;
    }

    static class InsensitiveBean
    {
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
        public Member enumValue;
    }

    private final ObjectMapper MAPPER = newJsonMapper();
    
    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */

    @Test
    public void testCaseSensitive() throws Exception {
        String json = a2q("{'enumValue':'1'}");

        SensitiveBean sensitiveBean = MAPPER.readValue(json, SensitiveBean.class);

        assertEquals(Member.SECOND_MEMBER, sensitiveBean.enumValue);
    }


    @Test
    public void testCaseInsensitive() throws Exception {
        String json = a2q("{'enumValue':'1'}");

        InsensitiveBean insensitiveBean = MAPPER.readValue(json, InsensitiveBean.class);

        assertEquals(Member.SECOND_MEMBER, insensitiveBean.enumValue);
    }
}
