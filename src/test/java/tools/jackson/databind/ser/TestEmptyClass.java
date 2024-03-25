package tools.jackson.databind.ser;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.core.JsonGenerator;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class TestEmptyClass
    extends DatabindTestUtil
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

    static class NonZeroSerializer extends ValueSerializer<NonZero>
    {
        @Override
        public void serialize(NonZero value, JsonGenerator jgen, SerializerProvider provider)
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

    protected final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testEmptyWithAnnotations() throws Exception
    {
        // First: without annotations, should complain
        try {
            MAPPER.writeValueAsString(new Empty());
            fail("Should fail");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "No serializer found for class");
        }

        // But not if there is a recognized annotation
        assertEquals("{}", MAPPER.writeValueAsString(new EmptyWithAnno()));

        // Including class annotation through mix-ins
        ObjectMapper m2 = jsonMapperBuilder()
                .addMixIn(Empty.class, EmptyWithAnno.class)
                .build();
        assertEquals("{}", m2.writeValueAsString(new Empty()));
    }

    /**
     * Alternative it is possible to use a feature to allow
     * serializing empty classes, too
     */
    @Test
    public void testEmptyWithFeature() throws Exception
    {
        // should be enabled by default
        assertTrue(MAPPER.isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS));
        assertEquals("{}",
                MAPPER.writer()
                    .without(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                    .writeValueAsString(new Empty()));
    }

    @Test
    public void testCustomNoEmpty() throws Exception
    {
        // first non-empty:
        assertEquals("{\"value\":123}", MAPPER.writeValueAsString(new NonZeroWrapper(123)));
        // then empty:
        assertEquals("{}", MAPPER.writeValueAsString(new NonZeroWrapper(0)));
    }
}
