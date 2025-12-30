package tools.jackson.databind.struct;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

// https://github.com/FasterXML/jackson-databind/issues/1709
public class UnwrappedEmptyAsNull1709Test extends DatabindTestUtil
{
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class Container1709 {
        public String name;
        @JsonUnwrapped
        public Unwrapped1709 u;
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class Unwrapped1709 {
        public String s;
        public Integer n;
    }

    private final ObjectMapper MAPPER_ENABLED = jsonMapperBuilder()
            .enable(DeserializationFeature.USE_NULL_FOR_EMPTY_UNWRAPPED)
            .build();

    private final ObjectMapper MAPPER_DISABLED = jsonMapperBuilder()
            .disable(DeserializationFeature.USE_NULL_FOR_EMPTY_UNWRAPPED)
            .build();

    /*
    /**********************************************************************
    /* Tests with USE_NULL_FOR_EMPTY_UNWRAPPED enabled
    /**********************************************************************
    */

    @Test
    public void testEmptyUnwrappedAsNull() throws Exception {
        String json = a2q("{'name':'test'}");
        Container1709 result = MAPPER_ENABLED.readValue(json, Container1709.class);
        assertNotNull(result);
        assertEquals("test", result.name);
        assertNull(result.u);
    }

    @Test
    public void testEmptyJsonEmptyUnwrappedAsNull() throws Exception {
        Container1709 result = MAPPER_ENABLED.readValue("{}", Container1709.class);
        assertNotNull(result);
        assertNull(result.name);
        assertNull(result.u);
    }

    @Test
    public void testNonNullUnwrappedPreserved() throws Exception {
        String json = a2q("{'name':'test','s':'value'}");
        Container1709 result = MAPPER_ENABLED.readValue(json, Container1709.class);
        assertNotNull(result);
        assertEquals("test", result.name);
        assertNotNull(result.u);
        assertEquals("value", result.u.s);
    }

    @Test
    public void testPartialNonNullUnwrappedPreserved() throws Exception {
        String json = a2q("{'s':'value'}");
        Container1709 result = MAPPER_ENABLED.readValue(json, Container1709.class);
        assertNotNull(result);
        assertNotNull(result.u);
        assertEquals("value", result.u.s);
        assertNull(result.u.n);
    }

    /*
    /**********************************************************************
    /* Tests with USE_NULL_FOR_EMPTY_UNWRAPPED disabled
    /**********************************************************************
    */

    @Test
    public void testEmptyUnwrappedAsNullWhenDisabled() throws Exception {
        String json = a2q("{'name':'test'}");
        Container1709 result = MAPPER_DISABLED.readValue(json, Container1709.class);
        assertNotNull(result);
        assertEquals("test", result.name);
        assertNotNull(result.u);
        assertNull(result.u.s);
        assertNull(result.u.n);
    }

    @Test
    public void testEmptyJsonEmptyUnwrappedAsNullWhenDisabled() throws Exception {
        Container1709 result = MAPPER_DISABLED.readValue("{}", Container1709.class);
        assertNotNull(result);
        assertNull(result.name);
        assertNotNull(result.u);
        assertNull(result.u.s);
        assertNull(result.u.n);
    }

    @Test
    public void testNonNullUnwrappedPreservedWhenDisabled() throws Exception {
        String json = a2q("{'name':'test','s':'value'}");
        Container1709 result = MAPPER_DISABLED.readValue(json, Container1709.class);
        assertNotNull(result);
        assertEquals("test", result.name);
        assertNotNull(result.u);
        assertEquals("value", result.u.s);
    }

    @Test
    public void testPartialNonNullUnwrappedPreservedWhenDisabled() throws Exception {
        String json = a2q("{'s':'value'}");
        Container1709 result = MAPPER_DISABLED.readValue(json, Container1709.class);
        assertNull(result.u.n);
    }
}
