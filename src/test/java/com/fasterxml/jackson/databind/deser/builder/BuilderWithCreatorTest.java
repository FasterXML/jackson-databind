package com.fasterxml.jackson.databind.deser.builder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class BuilderWithCreatorTest extends BaseMapTest
{
    @JsonDeserialize(builder=PropertyCreatorBuilder.class)
    static class PropertyCreatorValue
    {
        final int a, b, c;

        protected PropertyCreatorValue(int a, int b, int c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }

    static class PropertyCreatorBuilder {
        private final int a, b;
        private int c;

        @JsonCreator
        public PropertyCreatorBuilder(@JsonProperty("a") int a,
                @JsonProperty("b") int b)
        {
            this.a = a;
            this.b = b;
        }

        public PropertyCreatorBuilder withC(int v) {
            c = v;
            return this;
        }
        public PropertyCreatorValue build() {
            return new PropertyCreatorValue(a, b, c);
        }
    }

    // With String

    @JsonDeserialize(builder=StringCreatorBuilder.class)
    static class StringCreatorValue
    {
        final String str;

        protected StringCreatorValue(String s) { str = s; }
    }

    static class StringCreatorBuilder {
        private final String v;

        @JsonCreator
        public StringCreatorBuilder(String str) {
            v = str;
        }

        public StringCreatorValue build() {
            return new StringCreatorValue(v);
        }
    }

    // With boolean

    @JsonDeserialize(builder=BooleanCreatorBuilder.class)
    static class BooleanCreatorValue
    {
        final boolean value;

        protected BooleanCreatorValue(boolean v) { value = v; }
    }

    static class BooleanCreatorBuilder {
        private final boolean value;

        @JsonCreator
        public BooleanCreatorBuilder(boolean v) {
            value = v;
        }

        public BooleanCreatorValue build() {
            return new BooleanCreatorValue(value);
        }
    }

    // With Int

    @JsonDeserialize(builder=IntCreatorBuilder.class)
    static class IntCreatorValue
    {
        final int value;

        protected IntCreatorValue(int v) { value = v; }
    }

    static class IntCreatorBuilder {
        private final int value;

        @JsonCreator
        public IntCreatorBuilder(int v) {
            value = v;
        }

        public IntCreatorValue build() {
            return new IntCreatorValue(value);
        }
    }

    // With Double

    @JsonDeserialize(builder=DoubleCreatorBuilder.class)
    static class DoubleCreatorValue
    {
        final double value;

        protected DoubleCreatorValue(double v) { value = v; }
    }

    static class DoubleCreatorBuilder {
        private final double value;

        @JsonCreator
        public DoubleCreatorBuilder(double v) {
            value = v;
        }

        public DoubleCreatorValue build() {
            return new DoubleCreatorValue(value);
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testWithPropertiesCreator() throws Exception
    {
        final String json = a2q("{'a':1,'c':3,'b':2}");
        PropertyCreatorValue value = MAPPER.readValue(json, PropertyCreatorValue.class);
        assertEquals(1, value.a);
        assertEquals(2, value.b);
        assertEquals(3, value.c);
    }

    public void testWithDelegatingStringCreator() throws Exception
    {
        final int EXP = 139;
        IntCreatorValue value = MAPPER.readValue(String.valueOf(EXP),
                IntCreatorValue.class);
        assertEquals(EXP, value.value);
    }

    public void testWithDelegatingIntCreator() throws Exception
    {
        final double EXP = -3.75;
        DoubleCreatorValue value = MAPPER.readValue(String.valueOf(EXP),
                DoubleCreatorValue.class);
        assertEquals(EXP, value.value);
    }

    public void testWithDelegatingBooleanCreator() throws Exception
    {
        final boolean EXP = true;
        BooleanCreatorValue value = MAPPER.readValue(String.valueOf(EXP),
                BooleanCreatorValue.class);
        assertEquals(EXP, value.value);
    }
}

