package com.fasterxml.jackson.databind.deser.creators;

import com.fasterxml.jackson.annotation.JsonCreator;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FactoryAndConstructor2962Test extends BaseMapTest
{
    // [databind#2962]
    static class ExampleDto2962
    {
        final int version;

        // Was not needed in 2.11.x, for 2.12.0 was; fixed:
//        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        ExampleDto2962(int version) {
            this.version = version;
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        static ExampleDto2962 fromJson(Json2962 json) {
            return new ExampleDto2962(json.version);
        }

        static class Json2962 {
            public int version;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#2962]
    public void testImplicitCtorExplicitFactory() throws Exception
    {
        ExampleDto2962 result = MAPPER.readValue("42", ExampleDto2962.class);
        assertEquals(42, result.version);
    }
}
