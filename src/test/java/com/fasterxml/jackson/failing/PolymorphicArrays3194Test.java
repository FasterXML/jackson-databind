package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

// [databund#3194]: Discrepancy between Type Id inclusion on serialization vs
// expectation during deserialization causes mismatch and fails deserialization.
public class PolymorphicArrays3194Test extends BaseMapTest
{
    static final class SomeBean {
        public Object[][] value;
    }

    public void testTwoDimensionalArrayMapping() throws Exception
    {
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubTypeIsArray()
                .allowIfSubType(Object.class)
                .build();

        ObjectMapper mapper = JsonMapper
                .builder()
                .activateDefaultTyping(typeValidator, DefaultTyping.NON_FINAL)
                .build();

        SomeBean instance = new SomeBean();
        instance.value = new String[][]{{"1.1", "1.2"}, {"2.1", "2.2"}};
        String json = mapper
//                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(instance);

        // Note: we'll see something like:
        //
//  {
//    "value" : [ "[[Ljava.lang.String;", [ [ "[Ljava.lang.String;", [ "1.1", "1.2" ] ], [ "[Ljava.lang.String;", [ "2.1", "2.2" ] ] ] ]
//  }

     // that is, type ids for both array levels.

// System.err.println("JSON:\n"+json);
        SomeBean result = mapper.readValue(json, SomeBean.class); // fails
        assertEquals(String[][].class, result.value.getClass());
        assertEquals(String[].class, result.value[0].getClass());
    }
}
