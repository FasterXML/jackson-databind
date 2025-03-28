package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PolymorphicIdClassDeserTest extends DatabindTestUtil {

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

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .enable(DeserializationFeature.FAIL_ON_POLYMORPHIC_SUBTYPE_CLASS_NOT_EXPLICITLY_REGISTERED)
            .build();

    @Test
    public void testDeserialization() throws Exception
    {
        //trying to test if JsonSubTypes enforced
        final String foo1 = MAPPER.writeValueAsString(new FooClassImpl());
        final String foo2 = MAPPER.writeValueAsString(new FooClassImpl2());
        FooClass res1 = MAPPER.readValue(foo1, FooClass.class);
        assertTrue(res1 instanceof FooClassImpl);
        // next bit should in theory fail because FooClassImpl2 is not listed as a subtype
        assertThrows(InvalidTypeIdException.class, () -> MAPPER.readValue(foo2, FooClass.class));
    }
}
