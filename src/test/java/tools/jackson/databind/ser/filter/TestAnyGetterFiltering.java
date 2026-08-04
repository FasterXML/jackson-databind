package tools.jackson.databind.ser.filter;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import tools.jackson.databind.ser.FilterProvider;
import tools.jackson.databind.ser.PropertyFilter;
import tools.jackson.databind.ser.PropertyWriter;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;
import tools.jackson.databind.ser.std.SimpleFilterProvider;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ensuring that entries accessible via "any filter"
 * can also be filtered with JSON Filter functionality.
 */
public class TestAnyGetterFiltering extends DatabindTestUtil
{
    @JsonFilter("anyFilter")
    public static class AnyBean
    {
        private Map<String, String> properties = new HashMap<String, String>();
        {
            properties.put("a", "1");
            properties.put("b", "2");
        }

        @JsonAnyGetter
        public Map<String, String> anyProperties()
        {
            return properties;
        }
    }

    public static class AnyBeanWithIgnores
    {
        private Map<String, String> properties = new LinkedHashMap<String, String>();
        {
            properties.put("a", "1");
            properties.put("bogus", "2");
            properties.put("b", "3");
        }

        @JsonAnyGetter
        @JsonIgnoreProperties({ "bogus" })
        public Map<String, String> anyProperties()
        {
            return properties;
        }
    }

    // [databind#1281]
    public static class AnyBeanWithMultipleIgnores
    {
        public String name = "bob";

        private Map<String, String> properties = new LinkedHashMap<String, String>();
        {
            properties.put("a", "1");
            properties.put("secret", "s");
            properties.put("b", "2");
            properties.put("internal", "i");
        }

        @JsonAnyGetter
        @JsonIgnoreProperties({ "secret", "internal" })
        public Map<String, String> anyProperties()
        {
            return properties;
        }
    }

    // [databind#6136]
    @JsonFilter("anyFilter")
    static class AnyBeanWithSecret
    {
        public String name = "bob";

        private Map<String, String> properties = new LinkedHashMap<String, String>();
        {
            properties.put("a", "1");
            properties.put("secret", "s3cr3t");
        }

        @JsonAnyGetter
        public Map<String, String> anyProperties() {
            return properties;
        }
    }

    // [databind#6136]
    @JsonFilter("anyFilter")
    static class ObjectNodeAnyBeanWithSecret
    {
        public String name = "bob";

        @JsonAnyGetter
        public ObjectNode anyProperties() {
            return JsonNodeFactory.instance.objectNode()
                    .put("a", "1")
                    .put("secret", "s3cr3t");
        }
    }

    // [databind#1655]
    @JsonFilter("CustomFilter")
    static class OuterObject {
         public int getExplicitProperty() {
              return 42;
         }

         @JsonAnyGetter
         public Map<String, Object> getAny() {
              Map<String, Object> extra = new HashMap<>();
              extra.put("dynamicProperty", "I will not serialize");
              return extra;
         }
    }

    // [databind#6136]: filter that implements `PropertyFilter` directly instead of
    // extending `SimpleBeanPropertyFilter` -- must also get per-entry decisions
    static class DirectExcludingFilter implements PropertyFilter
    {
        private final Set<String> _excluded;

        public DirectExcludingFilter(String... names) {
            _excluded = new HashSet<>(Arrays.asList(names));
        }

        @Override
        public PropertyFilter snapshot() { return this; }

        @Override
        public void serializeAsProperty(Object pojo, JsonGenerator g, SerializationContext ctxt,
                PropertyWriter writer)
            throws Exception
        {
            if (!_excluded.contains(writer.getName())) {
                writer.serializeAsProperty(pojo, g, ctxt);
            }
        }

        @Override
        public void serializeAsElement(Object elementValue, JsonGenerator g, SerializationContext ctxt,
                PropertyWriter writer)
            throws Exception
        {
            writer.serializeAsElement(elementValue, g, ctxt);
        }

        @Override
        public void depositSchemaProperty(PropertyWriter writer, JsonObjectFormatVisitor v,
                SerializationContext ctxt) {
            writer.depositSchemaProperty(v, ctxt);
        }
    }

    static class CustomFilter extends SimpleBeanPropertyFilter {
         @Override
         public void serializeAsProperty(Object pojo, JsonGenerator gen, SerializationContext provider,
                 PropertyWriter writer) throws Exception
         {
             if (pojo instanceof OuterObject) {
                 writer.serializeAsProperty(pojo, gen, provider);
              }
         }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void testAnyGetterFiltering() throws Exception
    {
        FilterProvider prov = new SimpleFilterProvider().addFilter("anyFilter",
                SimpleBeanPropertyFilter.filterOutAllExcept("b"));
        assertEquals("{\"b\":\"2\"}", MAPPER.writer(prov).writeValueAsString(new AnyBean()));
    }

    // [databind#6136]
    @Test
    public void anyGetterSerializeAllExcept() throws Exception
    {
        FilterProvider prov = new SimpleFilterProvider().addFilter("anyFilter",
                SimpleBeanPropertyFilter.serializeAllExcept("secret"));
        assertEquals("""
                {"name":"bob","a":"1"}""",
                MAPPER.writer(prov).writeValueAsString(new AnyBeanWithSecret()));
    }

    // [databind#6136]
    @Test
    public void objectNodeAnyGetterFiltering() throws Exception
    {
        FilterProvider excluding = new SimpleFilterProvider().addFilter("anyFilter",
                SimpleBeanPropertyFilter.serializeAllExcept("secret"));
        assertEquals("""
                {"name":"bob","a":"1"}""",
                MAPPER.writer(excluding).writeValueAsString(new ObjectNodeAnyBeanWithSecret()));

        FilterProvider including = new SimpleFilterProvider().addFilter("anyFilter",
                SimpleBeanPropertyFilter.filterOutAllExcept("name", "a"));
        assertEquals("""
                {"name":"bob","a":"1"}""",
                MAPPER.writer(including).writeValueAsString(new ObjectNodeAnyBeanWithSecret()));
    }

    // [databind#6136]: also has to work for filters that do not extend
    // `SimpleBeanPropertyFilter`
    @Test
    public void anyGetterFilteringWithDirectFilterImpl() throws Exception
    {
        FilterProvider prov = new SimpleFilterProvider().addFilter("anyFilter",
                new DirectExcludingFilter("secret"));
        assertEquals("""
                {"name":"bob","a":"1"}""",
                MAPPER.writer(prov).writeValueAsString(new AnyBeanWithSecret()));
        assertEquals("""
                {"name":"bob","a":"1"}""",
                MAPPER.writer(prov).writeValueAsString(new ObjectNodeAnyBeanWithSecret()));
    }

    // for [databind#1142]
    @Test
    public void testAnyGetterIgnore() throws Exception
    {
        assertEquals(a2q("{'a':'1','b':'3'}"),
                MAPPER.writeValueAsString(new AnyBeanWithIgnores()));
    }

    // [databind#1281]: @JsonIgnoreProperties on @JsonAnyGetter method should
    //   filter multiple map entries, coexist with regular properties
    @Test
    public void testAnyGetterIgnoreProperties1281() throws Exception
    {
        assertEquals(a2q("{'name':'bob','a':'1','b':'2'}"),
                MAPPER.writeValueAsString(new AnyBeanWithMultipleIgnores()));
    }

    // [databind#1655]
    @Test
    public void testAnyGetterPojo1655() throws Exception
    {
        FilterProvider filters = new SimpleFilterProvider().addFilter("CustomFilter", new CustomFilter());
        String json = MAPPER.writer(filters).writeValueAsString(new OuterObject());
        Map<?,?> stuff = MAPPER.readValue(json, Map.class);
        if (stuff.size() != 2) {
            fail("Should have 2 properties, got: "+stuff);
        }
   }
}
