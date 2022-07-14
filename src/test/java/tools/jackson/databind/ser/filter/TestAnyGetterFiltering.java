package tools.jackson.databind.ser.filter;

import java.util.*;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BaseMapTest;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializerProvider;
import tools.jackson.databind.ser.FilterProvider;
import tools.jackson.databind.ser.PropertyWriter;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;
import tools.jackson.databind.ser.std.SimpleFilterProvider;

/**
 * Unit tests for ensuring that entries accessible via "any filter"
 * can also be filtered with JSON Filter functionality.
 */
public class TestAnyGetterFiltering extends BaseMapTest
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

    static class CustomFilter extends SimpleBeanPropertyFilter {
         @Override
         public void serializeAsProperty(Object pojo, JsonGenerator gen, SerializerProvider provider,
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

    public void testAnyGetterFiltering() throws Exception
    {
        FilterProvider prov = new SimpleFilterProvider().addFilter("anyFilter",
                SimpleBeanPropertyFilter.filterOutAllExcept("b"));
        assertEquals("{\"b\":\"2\"}", MAPPER.writer(prov).writeValueAsString(new AnyBean()));
    }

    // for [databind#1142]
    public void testAnyGetterIgnore() throws Exception
    {
        assertEquals(a2q("{'a':'1','b':'3'}"),
                MAPPER.writeValueAsString(new AnyBeanWithIgnores()));
    }

    // [databind#1655]
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
