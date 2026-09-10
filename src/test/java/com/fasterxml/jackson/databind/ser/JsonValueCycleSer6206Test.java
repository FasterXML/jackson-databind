package com.fasterxml.jackson.databind.ser;

import java.util.*;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#6206]
/**
 * Serializing through a {@code @JsonValue} accessor writes no structural tokens of its
 * own, so a cycle in which every value is serialized that way never trips the nesting
 * limit of {@code StreamWriteConstraints}. Verify it is reported as a
 * {@link JsonMappingException}, the way cycles through bean properties are, rather than
 * letting a {@link StackOverflowError} escape.
 */
public class JsonValueCycleSer6206Test extends DatabindTestUtil
{
    static class SelfValue {
        @JsonValue
        public SelfValue value() { return this; }
    }

    static class ValueCycle {
        public ValueCycle other;

        @JsonValue
        public ValueCycle value() { return other; }
    }

    static class ValueA {
        public ValueB b;

        @JsonValue
        public ValueB value() { return b; }
    }

    static class ValueB {
        public ValueA a;

        @JsonValue
        public ValueA value() { return a; }
    }

    static class TypedSelf {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_ARRAY)
        @JsonValue
        public TypedSelf value() { return this; }
    }

    // Self-reference is fine if the value is NOT serialized the same way
    static class SelfWithCustomSerializer {
        @JsonValue
        @JsonSerialize(using = FixedSerializer.class)
        public SelfWithCustomSerializer value() { return this; }
    }

    static class FixedSerializer extends JsonSerializer<SelfWithCustomSerializer> {
        @Override
        public void serialize(SelfWithCustomSerializer value, JsonGenerator gen, SerializerProvider ctxt)
            throws IOException
        {
            gen.writeString("fixed");
        }
    }

    static class Typed {
        public Typed other;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_ARRAY)
        @JsonValue
        public Typed value() { return other; }
    }

    // Non-cyclic use of `@JsonValue` must keep working
    static class Plain {
        private final String _value;

        Plain(String value) { _value = value; }

        @JsonValue
        public String value() { return _value; }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // Immediate self-reference: detected before recursing, with a specific message
    @Test
    public void accessorReturningItself() throws Exception {
        _verifySelfReference(new SelfValue());
    }

    @Test
    public void accessorReturningItselfWithTypeInformation() throws Exception {
        _verifySelfReference(new TypedSelf());
    }

    @Test
    public void selfReferenceAllowedIfDisabled() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .disable(SerializationFeature.FAIL_ON_SELF_REFERENCES)
                .build();
        try {
            String json = mapper.writeValueAsString(new SelfValue());
            fail("Should not pass, produced: "+json);
        } catch (JsonMappingException e) {
            verifyException(e, "Infinite recursion (StackOverflowError)");
        }
    }

    @Test
    public void selfReferenceWithCustomSerializer() throws Exception {
        assertEquals(q("fixed"), MAPPER.writeValueAsString(new SelfWithCustomSerializer()));
    }

    @Test
    public void cycleBetweenTwoValues() throws Exception {
        ValueCycle v1 = new ValueCycle();
        ValueCycle v2 = new ValueCycle();
        v1.other = v2;
        v2.other = v1;
        _verifyInfiniteRecursion(v1);
    }

    @Test
    public void cycleAcrossTwoTypes() throws Exception {
        ValueA a = new ValueA();
        ValueB b = new ValueB();
        a.b = b;
        b.a = a;
        _verifyInfiniteRecursion(a);
    }

    @Test
    public void cycleWithTypeInformation() throws Exception {
        Typed t1 = new Typed();
        Typed t2 = new Typed();
        t1.other = t2;
        t2.other = t1;
        _verifyInfiniteRecursion(t1);
    }

    // Ordinary `@JsonValue` handling must be unaffected
    @Test
    public void plainValueStillWorks() throws Exception {
        assertEquals(q("abc"), MAPPER.writeValueAsString(new Plain("abc")));
        assertEquals(a2q("['a','b']"),
                MAPPER.writeValueAsString(Arrays.asList(new Plain("a"), new Plain("b"))));
    }

    private void _verifySelfReference(Object value) throws Exception
    {
        try {
            String json = MAPPER.writeValueAsString(value);
            fail("Should not pass, produced: "+json);
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Direct self-reference leading to cycle");
            verifyException(e, "value()");
        }
    }

    private void _verifyInfiniteRecursion(Object value) throws Exception
    {
        try {
            String json = MAPPER.writeValueAsString(value);
            fail("Should not pass, produced: "+json);
        } catch (JsonMappingException e) {
            verifyException(e, "Infinite recursion (StackOverflowError)");
        }
    }
}
