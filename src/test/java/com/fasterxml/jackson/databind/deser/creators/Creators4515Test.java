package com.fasterxml.jackson.databind.deser.creators;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

/**
 * Tests to help cover simpler cases wrt [databind#4515]
 */
public class Creators4515Test extends DatabindTestUtil
{
    static class ConstructorBeanPropsExplicit {
        int x;

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        protected ConstructorBeanPropsExplicit(@JsonProperty("x") int x) {
            this.x = x;
        }
    }

    static class ConstructorBeanPropsWithName {
        int x;

        @JsonCreator
        protected ConstructorBeanPropsWithName(@JsonProperty("x") int x) {
            this.x = x;
        }
    }

    /*
    /**********************************************************************
    /* Test methods, simple Properties-based, explicitly annotated
    /**********************************************************************
     */
    
    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testPropsBasedExplicit() throws Exception
    {
        ConstructorBeanPropsExplicit bean = MAPPER.readValue("{ \"x\" : 42 }",
                ConstructorBeanPropsExplicit.class);
        assertEquals(42, bean.x);
    }

    @Test
    public void testPropsBasedViaName() throws Exception
    {
        ConstructorBeanPropsWithName bean = MAPPER.readValue("{ \"x\" : 28 }",
                ConstructorBeanPropsWithName.class);
        assertEquals(28, bean.x);
    }
}
