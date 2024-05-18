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

    static class FactoryBeanPropsExplicit {
        double d;

        private FactoryBeanPropsExplicit(double value, boolean dummy) { d = value; }

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        protected static FactoryBeanPropsExplicit createIt(@JsonProperty("f") double value) {
            return new FactoryBeanPropsExplicit(value, true);
        }
    }

    /*
    /**********************************************************************
    /* Test methods, simple Properties-based (constructor) explicitly annotated
    /**********************************************************************
     */
    
    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testPropsBasedConstructorExplicit() throws Exception
    {
        ConstructorBeanPropsExplicit bean = MAPPER.readValue("{ \"x\" : 42 }",
                ConstructorBeanPropsExplicit.class);
        assertEquals(42, bean.x);
    }

    @Test
    public void testPropsBasedConstructorWithName() throws Exception
    {
        ConstructorBeanPropsWithName bean = MAPPER.readValue("{ \"x\" : 28 }",
                ConstructorBeanPropsWithName.class);
        assertEquals(28, bean.x);
    }

    /*
    /**********************************************************************
    /* Test methods, simple Properties-based (constructor) explicitly annotated
    /**********************************************************************
     */

    @Test
    public void testPropsBasedFactoryExplicit() throws Exception
    {
        FactoryBeanPropsExplicit bean = MAPPER.readValue("{ \"f\" : 0.5 }",
                FactoryBeanPropsExplicit.class);
        assertEquals(0.5, bean.d);
    }

    /*
    /**********************************************************************
    /* Test methods, simple Delegating, explicitly annotated
    /**********************************************************************
     */

}
