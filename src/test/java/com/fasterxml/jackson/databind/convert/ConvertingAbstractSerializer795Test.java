package com.fasterxml.jackson.databind.convert;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.*;
import com.fasterxml.jackson.databind.util.StdConverter;

import static org.junit.jupiter.api.Assertions.*;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.*;

// for [databind#795]
public class ConvertingAbstractSerializer795Test
{
    public static abstract class AbstractCustomType {
        final String value;
        public AbstractCustomType(String v) {
            this.value = v;
        }
    }

    public static class ConcreteCustomType extends AbstractCustomType {
        public ConcreteCustomType(String v) {
            super(v);
        }
    }

    public static class AbstractCustomTypeDeserializationConverter extends StdConverter<String, AbstractCustomType>{

        @Override
        public AbstractCustomType convert(String arg) {
            return new ConcreteCustomType(arg);
        }
    }

    public static class AbstractCustomTypeUser {
        @JsonProperty
        @JsonDeserialize(converter = AbstractCustomTypeDeserializationConverter.class)
        private final AbstractCustomType customField;

        @JsonCreator
        AbstractCustomTypeUser(@JsonProperty("customField") AbstractCustomType cf) {
            this.customField = cf;
        }
    }

    public static  class NonAbstractCustomType {
        final String value;
        public NonAbstractCustomType(String v) {
            this.value = v;
        }
    }

    public static class NonAbstractCustomTypeDeserializationConverter extends StdConverter<String, NonAbstractCustomType>{

        @Override
        public NonAbstractCustomType convert(String arg) {
            return new NonAbstractCustomType(arg);
        }
    }

    public static class NonAbstractCustomTypeUser {
        @JsonProperty
        @JsonDeserialize(converter = NonAbstractCustomTypeDeserializationConverter.class)
        private final NonAbstractCustomType customField;

        @JsonCreator NonAbstractCustomTypeUser(@JsonProperty("customField") NonAbstractCustomType customField) {
            this.customField = customField;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private static final ObjectMapper JSON_MAPPER = newJsonMapper();

    @Test
    public void testAbstractTypeDeserialization() throws Exception {
        String test="{\"customField\": \"customString\"}";
        AbstractCustomTypeUser cu = JSON_MAPPER.readValue(test, AbstractCustomTypeUser.class);
        assertNotNull(cu);
    }

    @Test
    public void testNonAbstractDeserialization() throws Exception {
        String test="{\"customField\": \"customString\"}";
        NonAbstractCustomTypeUser cu = JSON_MAPPER.readValue(test, NonAbstractCustomTypeUser.class);
        assertNotNull(cu);
    }
}
