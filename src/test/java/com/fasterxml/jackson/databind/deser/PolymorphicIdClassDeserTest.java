package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializeUsingJDKTest;
import org.junit.jupiter.api.Test;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PolymorphicIdClassDeserTest {
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    @JsonSubTypes({@JsonSubTypes.Type(value = FooClassImpl.class)})
    static abstract class FooClass { }
    static class FooClassImpl extends FooClass { }
    static class FooClassImpl2 extends FooClass { }

    /*
    /************************************************************
    /* Unit tests, valid
    /************************************************************
    */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testDeserialization() throws Exception
    {
        //trying to test if JsonSubTypes enforced
        final String foo1 = MAPPER.writeValueAsString(new FooClassImpl());
        final String foo2 = MAPPER.writeValueAsString(new FooClassImpl2());
        FooClass res1 = MAPPER.readValue(foo1, FooClass.class);
        assertTrue(res1 instanceof FooClassImpl);
        // next bit should in theory fail because FooClassImpl2 is not listed as a subtype
        FooClass res2 = MAPPER.readValue(foo2, FooClass.class);
        assertTrue(res2 instanceof FooClassImpl2);
    }
}
