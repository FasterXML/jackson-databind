package com.fasterxml.jackson.databind.deser.enums;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EnumDeserialization3638Test extends BaseMapTest
{
    /*
    /**********************************************************
    /* Set up
    /**********************************************************
     */

    protected final ObjectMapper MAPPER = new ObjectMapper();

    static enum Member
    {
        FIRST_MEMBER(0),
        SECOND_MEMBER(1);

        private int index;

        private Member(int index) {
            this.index = index;
        }
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

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */

    public void testCaseSensitive() throws JsonProcessingException {
        String json = a2q("{'enumValue':'1'}");

        SensitiveBean sensitiveBean = MAPPER.readValue(json, SensitiveBean.class);

        assertEquals(Member.SECOND_MEMBER, sensitiveBean.enumValue);
    }


    public void testCaseInsensitive() throws JsonProcessingException {
        String json = a2q("{'enumValue':'1'}");

        InsensitiveBean insensitiveBean = MAPPER.readValue(json, InsensitiveBean.class);

        assertEquals(Member.SECOND_MEMBER, insensitiveBean.enumValue);
    }
}
