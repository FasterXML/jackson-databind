package com.fasterxml.jackson.databind.deser.creators;

import java.beans.ConstructorProperties;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.*;

public class CreatorPropertiesTest extends BaseMapTest
{
    static class Issue905Bean {
        // 08-Nov-2015, tatu: Note that in real code we would most likely use same
        //    names for properties; but here we use different name on purpose to
        //    ensure that Jackson has no way of binding JSON properties "x" and "y"
        //    using any other mechanism than via `@ConstructorProperties` annotation
        public int _x, _y;

        @ConstructorProperties({"x", "y"})
        // Same as above; use differing local parameter names so that parameter name
        // introspection cannot be used as the source of property names.
        public Issue905Bean(int a, int b) {
            _x = a;
            _y = b;
        }
    }

    // for [databind#1122]
    static class Ambiguity {
        @JsonProperty("bar")
        private int foo;

        protected Ambiguity() {}

        @ConstructorProperties({ "foo" })
        public Ambiguity(int foo) {
            this.foo = foo;
        }

        public int getFoo() {
            return foo;
        }

        @Override
        public String toString() {
            return "Ambiguity [foo=" + foo + "]";
        }
    }

    // for [databind#1371]
    static class Lombok1371Bean {
        public int x, y;

        protected Lombok1371Bean() { }

        @ConstructorProperties({ "x", "y" })
        public Lombok1371Bean(int _x, int _y) {
            x = _x + 1;
            y = _y + 1;
        }
    }

    // [databind#3252]: ensure full skipping of ignored properties
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Value3252 {
        @JsonProperty("name")
        private final String name;
        @JsonProperty("dumbMap")
        private final Map<String, String> dumbMap;

        @JsonCreator
        public Value3252(@JsonProperty("name") String name,
                @JsonProperty("dumbMap") Map<String, String> dumbMap) {
            this.name = name;
            this.dumbMap = (dumbMap == null) ? Collections.emptyMap() : dumbMap;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#905]
    public void testCreatorPropertiesAnnotation() throws Exception
    {
        Issue905Bean b = MAPPER.readValue(a2q("{'y':3,'x':2}"),
                Issue905Bean.class);
        assertEquals(2, b._x);
        assertEquals(3, b._y);
    }

    // [databind#1122]
    public void testPossibleNamingConflict() throws Exception
    {
        String json = "{\"bar\":3}";
        Ambiguity amb = MAPPER.readValue(json, Ambiguity.class);
        assertNotNull(amb);
        assertEquals(3, amb.getFoo());
    }

    // [databind#1371]: MapperFeature.INFER_CREATOR_FROM_CONSTRUCTOR_PROPERTIES
    public void testConstructorPropertiesInference() throws Exception
    {
        final String JSON = a2q("{'x':3,'y':5}");

        // by default, should detect and use arguments-taking constructor as creator
        assertTrue(MAPPER.isEnabled(MapperFeature.INFER_CREATOR_FROM_CONSTRUCTOR_PROPERTIES));
        Lombok1371Bean result = MAPPER.readValue(JSON, Lombok1371Bean.class);
        assertEquals(4, result.x);
        assertEquals(6, result.y);

        // but change if configuration changed
        ObjectMapper mapper = jsonMapperBuilder()
                .disable(MapperFeature.INFER_CREATOR_FROM_CONSTRUCTOR_PROPERTIES)
                .build();
        // in which case fields are set directly:
        result = mapper.readValue(JSON, Lombok1371Bean.class);
        assertEquals(3, result.x);
        assertEquals(5, result.y);
    }

    // [databind#3252]: ensure full skipping of ignored properties
    public void testSkipNonScalar3252() throws Exception
    {
        List<Value3252> testData = MAPPER.readValue(a2q(
"[\n"+
"      {'name': 'first entry'},\n"+
"      {'name': 'second entry', 'breaker': ['' ]},\n"+
"      {'name': 'third entry'}\n"+
"    ]\n"),
            new TypeReference<List<Value3252>>() {});

//System.err.println("JsON: "+MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(testData));
        assertEquals(3, testData.size());
    }
}
