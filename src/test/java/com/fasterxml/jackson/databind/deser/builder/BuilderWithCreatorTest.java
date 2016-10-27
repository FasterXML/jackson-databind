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

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testWithPropertiesCreator() throws Exception
    {
        final String json = aposToQuotes("{'a':1,'c':3,'b':2}");
        PropertyCreatorValue value = MAPPER.readValue(json, PropertyCreatorValue.class);        
        assertEquals(1, value.a);
        assertEquals(2, value.b);
        assertEquals(3, value.c);
    }

    public void testWithDelegatingStringCreator() throws Exception
    {
        final String EXP = "ALAKAZAM";
        StringCreatorValue value = MAPPER.readValue(quote(EXP), StringCreatorValue.class);        
        assertEquals(EXP, value.str);
    }
}
