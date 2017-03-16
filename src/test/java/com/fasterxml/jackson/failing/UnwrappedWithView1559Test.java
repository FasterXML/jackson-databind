package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.*;

public class UnwrappedWithView1559Test extends BaseMapTest
{
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    static final class Health {
        @JsonUnwrapped
        public Status status;
    }

    // NOTE: `final` is required to trigger [databind#1559]
    static final class Status {
        public String code;
    }

    /*
    /**********************************************************
    /* Tests methods
    /**********************************************************
     */

    // for [databind#1559]
    public void testCanSerializeSimpleWithDefaultView() throws Exception {
        String json = new ObjectMapper().configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false)
                .writeValueAsString(new Health());
        assertEquals(aposToQuotes("{'status':null}"), json);
    }
}
