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

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    static abstract class FooClassNoRegSubTypes { }
    static class FooClassNoRegSubTypesImpl extends FooClassNoRegSubTypes { }

    @JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
    @JsonSubTypes({@JsonSubTypes.Type(value = FooMinClassImpl.class)})
    static abstract class FooMinClass { }
    static class FooMinClassImpl extends FooMinClass { }
    static class FooMinClassImpl2 extends FooMinClass { }

    /*
    /************************************************************
    /* Unit tests, valid
    /************************************************************
    */

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .enable(DeserializationFeature.FAIL_ON_SUBTYPE_CLASS_NOT_REGISTERED)
            .build();

    @Test
    public void testDeserializationIdClass() throws Exception
    {
        //trying to test if JsonSubTypes enforced
        final String foo1 = MAPPER.writeValueAsString(new FooClassImpl());
        final String foo2 = MAPPER.writeValueAsString(new FooClassImpl2());
        FooClass res1 = MAPPER.readValue(foo1, FooClass.class);
        assertTrue(res1 instanceof FooClassImpl);
        // next bit should fail because FooClassImpl2 is not listed as a subtype (see mapper config)
        assertThrows(InvalidTypeIdException.class, () -> MAPPER.readValue(foo2, FooClass.class));
    }

    @Test
    public void testDeserializationIdClassNoReg() throws Exception
    {
        //trying to test if JsonSubTypes enforced
        final String foo1 = MAPPER.writeValueAsString(new FooClassNoRegSubTypesImpl());
        // next bit should fail because FooClassImpl2 is not listed as a subtype (see mapper config)
        assertThrows(InvalidTypeIdException.class, () -> MAPPER.readValue(foo1, FooClassNoRegSubTypes.class));
    }

    @Test
    public void testDeserializationIdMinimalClass() throws Exception
    {
        //trying to test if JsonSubTypes enforced
        final String foo1 = MAPPER.writeValueAsString(new FooMinClassImpl());
        final String foo2 = MAPPER.writeValueAsString(new FooMinClassImpl2());
        FooMinClass res1 = MAPPER.readValue(foo1, FooMinClass.class);
        assertTrue(res1 instanceof FooMinClassImpl);
        // next bit should fail because FooMinClassImpl2 is not listed as a subtype (see mapper config)
        assertThrows(InvalidTypeIdException.class, () -> MAPPER.readValue(foo2, FooMinClass.class));
    }
}
