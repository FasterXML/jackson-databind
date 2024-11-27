package com.fasterxml.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests to help cover simpler cases wrt [databind#4515]
 */
public class Creators4515Test extends DatabindTestUtil
{
    static class ConstructorBeanPropsExplicit4545 {
        int x;

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        protected ConstructorBeanPropsExplicit4545(@JsonProperty("x") int x) {
            this.x = x;
        }
    }

    static class ConstructorBeanPropsWithName4545 {
        int x;

        @JsonCreator
        protected ConstructorBeanPropsWithName4545(@JsonProperty("x") int x) {
            this.x = x;
        }
    }

    static class FactoryBeanPropsExplicit4545 {
        double d;

        private FactoryBeanPropsExplicit4545(double value, boolean dummy) { d = value; }

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        protected static FactoryBeanPropsExplicit4545 createIt(@JsonProperty("f") double value) {
            return new FactoryBeanPropsExplicit4545(value, true);
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
        ConstructorBeanPropsExplicit4545 bean = MAPPER.readValue("{ \"x\" : 42 }",
                ConstructorBeanPropsExplicit4545.class);
        assertEquals(42, bean.x);
    }

    @Test
    public void testPropsBasedConstructorWithName() throws Exception
    {
        ConstructorBeanPropsWithName4545 bean = MAPPER.readValue("{ \"x\" : 28 }",
                ConstructorBeanPropsWithName4545.class);
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
        FactoryBeanPropsExplicit4545 bean = MAPPER.readValue("{ \"f\" : 0.5 }",
                FactoryBeanPropsExplicit4545.class);
        assertEquals(0.5, bean.d);
    }
}
