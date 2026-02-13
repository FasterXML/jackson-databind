package tools.jackson.databind.ser;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for checking extended auto-detect configuration,
 * in context of serialization
 */
public class AutoDetectForSerializationTest
    extends DatabindTestUtil
{
    static class FieldBean
    {
        public String p1 = "public";
        protected String p2 = "protected";
        @SuppressWarnings("unused")
        private String p3 = "private";
    }

    @JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.PROTECTED_AND_PUBLIC)
    static class ProtFieldBean extends FieldBean { }

    static class MethodBean
    {
        public String getA() { return "a"; }
        protected String getB() { return "b"; }
        @SuppressWarnings("unused")
        private String getC() { return "c"; }
    }

    @JsonAutoDetect(getterVisibility=JsonAutoDetect.Visibility.PROTECTED_AND_PUBLIC)
    static class ProtMethodBean extends MethodBean { }

    final static class FieldBeanWithStatic
    {
        public int x = 1;

        public static int y = 2;

        // not even @JsonProperty should make statics usable...
        @JsonProperty public static int z = 3;
    }

    final static class GetterBeanWithStatic
    {
        public int getX() { return 3; }

        public static int getA() { return -3; }

        // not even @JsonProperty should make statics usable...
        @JsonProperty public static int getFoo() { return 123; }
    }
    
    /*
    /*********************************************************
    /* Test methods
    /*********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testDefaults() throws Exception
    {
        // by default, only public fields and getters are detected
        assertEquals("{\"p1\":\"public\"}",
                MAPPER.writeValueAsString(new FieldBean()));
        assertEquals("{\"a\":\"a\"}",
                MAPPER.writeValueAsString(new MethodBean()));
    }

    @Test
    public void testProtectedViaAnnotations() throws Exception
    {
        Map<String,Object> result = writeAndMap(MAPPER, new ProtFieldBean());
        assertEquals(2, result.size());
        assertEquals("public", result.get("p1"));
        assertEquals("protected", result.get("p2"));
        assertNull(result.get("p3"));

        result = writeAndMap(MAPPER, new ProtMethodBean());
        assertEquals(2, result.size());
        assertEquals("a", result.get("a"));
        assertEquals("b", result.get("b"));
        assertNull(result.get("c"));
    }

    @Test
    public void testStaticFields() throws Exception
    {
        Map<String,Object> result = writeAndMap(MAPPER, new FieldBeanWithStatic());
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(1), result.get("x"));
    }

    @Test
    public void testStaticMethods() throws Exception
    {
        Map<String,Object> result = writeAndMap(MAPPER, new GetterBeanWithStatic());
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(3), result.get("x"));
    }
    
    @Test
    public void testPrivateUsingGlobals() throws Exception
    {
        ObjectMapper m = jsonMapperBuilder()
                .changeDefaultVisibility(vc ->
                    vc.withFieldVisibility(JsonAutoDetect.Visibility.ANY))
                .build();

        Map<String,Object> result = writeAndMap(m, new FieldBean());
        assertEquals(3, result.size());
        assertEquals("public", result.get("p1"));
        assertEquals("protected", result.get("p2"));
        assertEquals("private", result.get("p3"));

        m = jsonMapperBuilder()
                .changeDefaultVisibility(vc ->
                    vc.withGetterVisibility(JsonAutoDetect.Visibility.ANY)
                    )
                .build();
        result = writeAndMap(m, new MethodBean());
        assertEquals(3, result.size());
        assertEquals("a", result.get("a"));
        assertEquals("b", result.get("b"));
        assertEquals("c", result.get("c"));
    }

    @Test
    public void testBasicSetup() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultVisibility(vc ->
                    vc.with(JsonAutoDetect.Visibility.ANY))
                .build();
        Map<String,Object> result = writeAndMap(mapper, new FieldBean());
        assertEquals(3, result.size());
        assertEquals("public", result.get("p1"));
        assertEquals("protected", result.get("p2"));
        assertEquals("private", result.get("p3"));
    }

    @Test
    public void testMapperShortcutMethods() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultVisibility(vc -> vc
                        .withVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY))
                .build();

        Map<String,Object> result = writeAndMap(mapper, new FieldBean());
        assertEquals(3, result.size());
        assertEquals("public", result.get("p1"));
        assertEquals("protected", result.get("p2"));
        assertEquals("private", result.get("p3"));
    }
}
