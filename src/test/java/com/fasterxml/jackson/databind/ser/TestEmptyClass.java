package com.fasterxml.jackson.databind.ser;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;

public class TestEmptyClass
    extends BaseMapTest
{
    static class Empty { }

    @JsonSerialize
    static class EmptyWithAnno { }

    @JsonSerialize(using=NonZeroSerializer.class)
    static class NonZero {
        public int nr;

        public NonZero(int i) { nr = i; }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    static class NonZeroWrapper {
        public NonZero value;

        public NonZeroWrapper(int i) {
            value = new NonZero(i);
        }
    }

    static class NonZeroSerializer extends JsonSerializer<NonZero>
    {
        @Override
        public void serialize(NonZero value, JsonGenerator jgen, SerializerProvider provider) throws IOException
        {
            jgen.writeNumber(value.nr);
        }

        @Override
        public boolean isEmpty(SerializerProvider provider, NonZero value) {
            if (value == null) return true;
            return (value.nr == 0);
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    protected final ObjectMapper mapper = new ObjectMapper();

    /**
     * Test to check that [JACKSON-201] works if there is a recognized
     * annotation (which indicates type is serializable)
     */
    public void testEmptyWithAnnotations() throws Exception
    {
        // First: without annotations, should complain
        try {
            serializeAsString(mapper, new Empty());
        } catch (InvalidDefinitionException e) {
            verifyException(e, "No serializer found for class");
        }

        // But not if there is a recognized annotation
        assertEquals("{}", serializeAsString(mapper, new EmptyWithAnno()));

        // Including class annotation through mix-ins
        ObjectMapper m2 = new ObjectMapper();
        m2.addMixIn(Empty.class, EmptyWithAnno.class);
        assertEquals("{}", m2.writeValueAsString(new Empty()));
    }

    /**
     * Alternative it is possible to use a feature to allow
     * serializing empty classes, too
     */
    public void testEmptyWithFeature() throws Exception
    {
        // should be enabled by default
        assertTrue(mapper.getSerializationConfig().isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS));
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        assertEquals("{}", serializeAsString(mapper, new Empty()));
    }

    // [JACKSON-695], JsonSerializer.isEmpty()
    public void testCustomNoEmpty() throws Exception
    {
        // first non-empty:
        assertEquals("{\"value\":123}", mapper.writeValueAsString(new NonZeroWrapper(123)));
        // then empty:
        assertEquals("{}", mapper.writeValueAsString(new NonZeroWrapper(0)));
    }
}
