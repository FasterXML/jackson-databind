package tools.jackson.databind.introspect;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import tools.jackson.databind.*;
import tools.jackson.databind.introspect.BeanPropertyDefinition;

/**
 * Unit tests for checking handling of some of {@link MapperFeature}s
 * and {@link SerializationFeature}s for serialization.
 */
public class VisibilityForSerializationTest
    extends BaseMapTest
{
    /**
     * Class with one explicitly defined getter, one name-based
     * auto-detectable getter.
     */
    static class GetterClass
    {
        @JsonProperty("x") public int getX() { return -2; }
        public int getY() { return 1; }
    }

    /**
     * Another test-class that explicitly disables auto-detection
     */
    @JsonAutoDetect(getterVisibility=Visibility.NONE)
    static class DisabledGetterClass
    {
        @JsonProperty("x") public int getX() { return -2; }
        public int getY() { return 1; }
    }

    /**
     * Another test-class that explicitly enables auto-detection
     */
    @JsonAutoDetect(isGetterVisibility=Visibility.NONE)
    static class EnabledGetterClass
    {
        @JsonProperty("x") public int getX() { return -2; }
        public int getY() { return 1; }

        // not auto-detected, since "is getter" auto-detect disabled
        public boolean isOk() { return true; }
    }

    /**
     * One more: only detect "isXxx", not "getXXX"
     */
    @JsonAutoDetect(getterVisibility=Visibility.NONE)
    static class EnabledIsGetterClass
    {
        // Won't be auto-detected any more
        public int getY() { return 1; }

        // but this will be
        public boolean isOk() { return true; }
    }

    static class TCls {
        @JsonProperty("groupname")
        private String groupname;

        public void setName(String str) {
            this.groupname = str;
        }
        public String getName() {
            return groupname;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testGlobalAutoDetection() throws IOException
    {
        // First: auto-detection enabled (default):
        ObjectMapper m = new ObjectMapper();
        Map<String,Object> result = writeAndMap(m, new GetterClass());
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(-2), result.get("x"));
        assertEquals(Integer.valueOf(1), result.get("y"));

        // Then auto-detection disabled. But note: we MUST create a new
        // mapper, since old version of serializer may be cached by now
        m = jsonMapperBuilder()
                .changeDefaultVisibility(vc ->
                    vc.withVisibility(PropertyAccessor.GETTER, Visibility.NONE))
                .build();
        result = writeAndMap(m, new GetterClass());
        assertEquals(1, result.size());
        assertTrue(result.containsKey("x"));
    }

    public void testPerClassAutoDetection() throws IOException
    {
        // First: class-level auto-detection disabling
        ObjectMapper m = new ObjectMapper();
        Map<String,Object> result = writeAndMap(m, new DisabledGetterClass());
        assertEquals(1, result.size());
        assertTrue(result.containsKey("x"));

        // And then class-level auto-detection enabling, should override defaults
        m = jsonMapperBuilder()
                .changeDefaultVisibility(vc ->
                    vc.withVisibility(PropertyAccessor.GETTER, Visibility.PUBLIC_ONLY))
                .build();

        result = writeAndMap(m, new EnabledGetterClass());
        assertEquals(2, result.size());
        assertTrue(result.containsKey("x"));
        assertTrue(result.containsKey("y"));
    }

    public void testPerClassAutoDetectionForIsGetter() throws IOException
    {
        ObjectMapper m = jsonMapperBuilder()
                .changeDefaultVisibility(vc ->
        // class level should override
                vc.withVisibility(PropertyAccessor.GETTER, Visibility.PUBLIC_ONLY)
                    .withVisibility(PropertyAccessor.IS_GETTER, Visibility.NONE))
                .build();

        Map<String,Object> result = writeAndMap(m, new EnabledIsGetterClass());
        assertEquals(0, result.size());
        assertFalse(result.containsKey("ok"));
    }

    public void testVisibilityFeatures() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .disable(MapperFeature.USE_GETTERS_AS_SETTERS, MapperFeature.INFER_PROPERTY_MUTATORS)
                .enable(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS, MapperFeature.USE_ANNOTATIONS)
                .changeDefaultVisibility(vc ->
                    vc.withVisibility(PropertyAccessor.ALL, Visibility.NONE))
                .build();
        // Only use explicitly specified values to be serialized/deserialized (i.e., JSONProperty).
        
        BeanDescription desc = ObjectMapperTestAccess.beanDescriptionForSer(mapper, TCls.class);
        List<BeanPropertyDefinition> props = desc.findProperties();
        if (props.size() != 1) {
            fail("Should find 1 property, not "+props.size()+"; properties = "+props);
        }
    }
}
