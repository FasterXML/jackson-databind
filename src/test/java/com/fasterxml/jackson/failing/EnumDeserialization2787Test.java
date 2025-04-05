package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

public class EnumDeserialization2787Test extends BaseMapTest
{
    // [databind#2787]
    static enum SomeEnum2787 {
        none,
        tax10,
        tax20
    }

    static enum  SomeEnumMixin2787 {
        @JsonProperty("zero")
        none,
        @JsonProperty("TypTyp")
        tax10,
        @JsonProperty("PytPyt")
        tax20
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    protected final ObjectMapper MAPPER = newJsonMapper();

    // [databind#2787]
    public void testMixinOnEnumValues2787() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addMixIn(SomeEnum2787.class, SomeEnumMixin2787.class)
                .build();
        SomeEnum2787 result = mapper.readValue(q("zero"), SomeEnum2787.class);
        assertEquals(SomeEnum2787.none, result);
    }
}
