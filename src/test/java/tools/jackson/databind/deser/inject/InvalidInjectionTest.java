package tools.jackson.databind.deser.inject;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JacksonInject;

import tools.jackson.databind.InjectableValues;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tools.jackson.databind.testutil.DatabindTestUtil.*;

// [databind#5217]: Allow multiple injections of same value
public class InvalidInjectionTest
{
    static class Bean1 {
        @JacksonInject protected String prop1;
        @JacksonInject protected String prop2;
    }

    static class Bean2 {
        @JacksonInject("x") protected String prop1;
        @JacksonInject("x") protected String prop2;
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#5217]: multiple injections of same value should work
    @Test
    public void testDuplicateInjectableFieldsWork() throws Exception
    {
        InjectableValues injectables = new InjectableValues.Std()
                .addValue(String.class, "injectedValue");
        ObjectReader reader = MAPPER.readerFor(Bean1.class).with(injectables);

        Bean1 bean = reader.readValue("{}");
        assertEquals("injectedValue", bean.prop1);
        assertEquals("injectedValue", bean.prop2);
    }

    // [databind#5217]: multiple injections with explicit id should work
    @Test
    public void testDuplicateInjectableFieldsWithIdWork() throws Exception
    {
        InjectableValues injectables = new InjectableValues.Std()
                .addValue("x", "injectedX");
        ObjectReader reader = MAPPER.readerFor(Bean2.class).with(injectables);

        Bean2 bean = reader.readValue("{}");
        assertEquals("injectedX", bean.prop1);
        assertEquals("injectedX", bean.prop2);
    }
}
