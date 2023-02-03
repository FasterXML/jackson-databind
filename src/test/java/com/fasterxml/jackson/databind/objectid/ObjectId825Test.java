package com.fasterxml.jackson.databind.objectid;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.NoCheckSubTypeValidator;

public class ObjectId825Test extends BaseMapTest
{
    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="oidString")
    public static class AbstractEntity {
        public String oidString;
    }

    public static class TestA extends AbstractEntity {
        public TestAbst testAbst;
        public TestD d;
    }

    static class TestAbst extends AbstractEntity { }

    static class TestC extends TestAbst {
        public TestD d;
    }

    static class TestD extends AbstractEntity { }

    private final ObjectMapper DEF_TYPING_MAPPER = jsonMapperBuilder()
            .activateDefaultTyping(NoCheckSubTypeValidator.instance,
                    ObjectMapper.DefaultTyping.NON_FINAL)
            .build();

    public void testDeserialize() throws Exception {
        TestA a = new TestA();
        a.oidString = "oidA";

        TestC c = new TestC();
        c.oidString = "oidC";

        a.testAbst = c;

        TestD d = new TestD();
        d.oidString = "oidD";

        c.d = d;
        a.d = d;

        String json = DEF_TYPING_MAPPER.writeValueAsString(a);
//        System.out.println("JSON: " + json);
        TestA testADeserialized = DEF_TYPING_MAPPER.readValue(json, TestA.class);

        assertNotNull(testADeserialized);
        assertNotNull(testADeserialized.d);
        assertEquals("oidD", testADeserialized.d.oidString);
    }
}
