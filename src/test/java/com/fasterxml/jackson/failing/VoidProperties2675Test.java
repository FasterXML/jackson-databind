package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.databind.*;

public class VoidProperties2675Test extends BaseMapTest
{
    static class VoidBean {
        public Void getValue() { return null; }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testVoidBean() throws Exception {
        final String EXP = "{\"value\":null}";
        assertEquals(EXP, MAPPER.writeValueAsString(new VoidBean()));
        VoidBean result = MAPPER.readValue(EXP, VoidBean.class);
        assertNotNull(result);
    }
}
