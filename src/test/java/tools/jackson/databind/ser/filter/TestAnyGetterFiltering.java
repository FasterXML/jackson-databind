package tools.jackson.databind.ser.filter;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.BeanSerializer;
import tools.jackson.databind.ser.FilterProvider;
import tools.jackson.databind.ser.PropertyFilter;
import tools.jackson.databind.ser.PropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;
import tools.jackson.databind.ser.bean.BeanSerializerBase;
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

    // [databind#6136]: content inclusion has to apply on filtered path too,
    // same as it does without a filter (see `NonEmptyAnyBean`)
    @JsonFilter("anyFilter")
    static class FilteredNonEmptyAnyBean
    {
        public String name = "bob";

        @JsonAnyGetter
        @JsonInclude(content = JsonInclude.Include.NON_EMPTY)
        public Map<String, String> anyProperties() {
            Map<String, String> props = new LinkedHashMap<>();
            props.put("a", "1");
            props.put("blank", "");
            return props;
        }
    }

    // [databind#6136]: same as `FilteredNonEmptyAnyBean` but without `@JsonFilter`
    static class NonEmptyAnyBean
    {
        public String name = "bob";

        @JsonAnyGetter
        @JsonInclude(content = JsonInclude.Include.NON_EMPTY)
        public Map<String, String> anyProperties() {
            Map<String, String> props = new LinkedHashMap<>();
            props.put("a", "1");
            props.put("blank", "");
            return props;
        }
    }

    // [databind#6136]: view-gated any-getter (accessor gets wrapped in a
    // view-filtering writer when a view is active) must still filter per entry
    static class Views {
        static class Public { }
        static class Internal { }
        static class Other { }
    }

    @JsonFilter("anyFilter")
    static class ViewAnyBeanWithSecret
    {
        @JsonView(Views.Public.class)
        public String name = "bob";

        private Map<String, String> properties = new LinkedHashMap<String, String>();
        {
            properties.put("a", "1");
            properties.put("secret", "s3cr3t");
        }

        @JsonView(Views.Public.class)
        @JsonAnyGetter
        public Map<String, String> anyProperties() {
            return properties;
        }
    }

    // [databind#6136]: same but any-getter in multiple views (different wrapper)
    @JsonFilter("anyFilter")
    static class MultiViewAnyBeanWithSecret
    {
        @JsonView({ Views.Public.class, Views.Internal.class, Views.Other.class })
        public String name = "bob";

        private Map<String, String> properties = new LinkedHashMap<String, String>();
        {
            properties.put("a", "1");
            properties.put("secret", "s3cr3t");
        }

        @JsonView({ Views.Public.class, Views.Internal.class })
        @JsonAnyGetter
        public Map<String, String> anyProperties() {
            return properties;
        }
    }

    // [databind#6136]: filtered any-getter must still honor Map entry ordering
    @JsonFilter("anyFilter")
    static class UnsortedAnyBean
    {
        public int a = 1;

        @JsonAnyGetter
        public Map<String, Integer> anyProperties() {
            Map<String, Integer> m = new LinkedHashMap<>();
            m.put("z", 1);
            m.put("b", 2);
            return m;
        }
    }

    // [databind#6136]: any-getter with its own filter, in addition to class filter
    @JsonFilter("anyFilter")
    static class MapFilteredAnyBean
    {
        public int a = 1;

        @JsonFilter("mapFilter")
        @JsonAnyGetter
        public Map<String, Integer> anyProperties() {
            Map<String, Integer> m = new LinkedHashMap<>();
            m.put("x", 1);
            m.put("y", 2);
            m.put("secret", 3);
            return m;
        }
    }

    // [databind#6136]: serializer that (like `XmlBeanSerializerBase`) calls
    // `PropertyFilter` directly with each writer, including `AnyGetterWriter`
    static class DirectFilterCallingSerializer extends BeanSerializer
    {
        DirectFilterCallingSerializer(BeanSerializerBase src) {
            super(src);
        }

        @Override
        protected void _serializePropertiesFiltered(Object bean, JsonGenerator g,
                SerializationContext ctxt, Object filterId)
        {
            PropertyFilter filter = findPropertyFilter(ctxt, filterId, bean);
            try {
                for (BeanPropertyWriter prop : _props) {
                    filter.serializeAsProperty(bean, g, ctxt, prop);
                }
            } catch (Exception e) {
                wrapAndThrow(ctxt, e, bean, "?");
            }
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

    // [databind#6136]: `@JsonInclude` content inclusion of the any-getter has to be
    // honored on the filtered path as well -- filtering decides which entries a filter
    // lets through, not whether inclusion criteria apply
    @Test
    public void anyGetterContentInclusionWithFilter() throws Exception
    {
        final String EXP = """
                {"name":"bob","a":"1"}""";

        // Baseline: no filter at all, empty-valued entry suppressed
        assertEquals(EXP, MAPPER.writeValueAsString(new NonEmptyAnyBean()));

        // and same has to hold for both filter styles
        FilterProvider excluding = new SimpleFilterProvider().addFilter("anyFilter",
                SimpleBeanPropertyFilter.serializeAllExcept("secret"));
        assertEquals(EXP,
                MAPPER.writer(excluding).writeValueAsString(new FilteredNonEmptyAnyBean()));

        FilterProvider including = new SimpleFilterProvider().addFilter("anyFilter",
                SimpleBeanPropertyFilter.filterOutAllExcept("name", "a", "blank"));
        assertEquals(EXP,
                MAPPER.writer(including).writeValueAsString(new FilteredNonEmptyAnyBean()));
    }

    // [databind#6136]: excluded entries must not leak when the any-getter is gated
    // by an active JSON View (accessor gets wrapped in a view-filtering writer)
    @Test
    public void anyGetterFilteringWithActiveView() throws Exception
    {
        FilterProvider prov = new SimpleFilterProvider().addFilter("anyFilter",
                SimpleBeanPropertyFilter.serializeAllExcept("secret"));
        assertEquals("""
                {"name":"bob","a":"1"}""",
                MAPPER.writer(prov).withView(Views.Public.class)
                        .writeValueAsString(new ViewAnyBeanWithSecret()));
        // and with a view that excludes the any-getter, no entries at all
        assertEquals("{}",
                MAPPER.writer(prov).withView(Views.Other.class)
                        .writeValueAsString(new ViewAnyBeanWithSecret()));
    }

    // [databind#6136]: same for any-getter in multiple views
    @Test
    public void anyGetterFilteringWithActiveMultiView() throws Exception
    {
        FilterProvider prov = new SimpleFilterProvider().addFilter("anyFilter",
                SimpleBeanPropertyFilter.serializeAllExcept("secret"));
        assertEquals("""
                {"name":"bob","a":"1"}""",
                MAPPER.writer(prov).withView(Views.Internal.class)
                        .writeValueAsString(new MultiViewAnyBeanWithSecret()));
        // and with a view that excludes the any-getter, no entries at all
        assertEquals("""
                {"name":"bob"}""",
                MAPPER.writer(prov).withView(Views.Other.class)
                        .writeValueAsString(new MultiViewAnyBeanWithSecret()));
    }

    // [databind#6136]: filtering must not lose ORDER_MAP_ENTRIES_BY_KEYS
    @Test
    public void anyGetterFilteringWithSortedEntries() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .build();
        FilterProvider prov = new SimpleFilterProvider().addFilter("anyFilter",
                SimpleBeanPropertyFilter.serializeAllExcept("nosuch"));
        assertEquals("""
                {"a":1,"b":2,"z":1}""",
                mapper.writer(prov).writeValueAsString(new UnsortedAnyBean()));
    }

    // [databind#6136]: both class filter and any-getter's own filter must apply
    @Test
    public void anyGetterFilteringWithMapFilter() throws Exception
    {
        FilterProvider prov = new SimpleFilterProvider()
                .addFilter("anyFilter", SimpleBeanPropertyFilter.serializeAllExcept("secret"))
                .addFilter("mapFilter", SimpleBeanPropertyFilter.filterOutAllExcept("x", "secret"));
        assertEquals("""
                {"a":1,"x":1}""",
                MAPPER.writer(prov).writeValueAsString(new MapFilteredAnyBean()));
    }

    // [databind#6136]: filtered ObjectNode any-getter must write names same as
    // unfiltered one (not via custom `String` key serializer)
    @Test
    public void objectNodeAnyGetterFilteringKeyNames() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule().addKeySerializer(String.class,
                        new ValueSerializer<String>() {
                            @Override
                            public void serialize(String value, JsonGenerator g,
                                    SerializationContext ctxt) {
                                g.writeName(value.toUpperCase());
                            }
                        }))
                .build();
        FilterProvider prov = new SimpleFilterProvider().addFilter("anyFilter",
                SimpleBeanPropertyFilter.serializeAllExcept("secret"));
        assertEquals("""
                {"name":"bob","a":"1"}""",
                mapper.writer(prov).writeValueAsString(new ObjectNodeAnyBeanWithSecret()));
    }

    // [databind#6136]: serializers that call filter directly with `AnyGetterWriter`
    // (instead of `BeanPropertyWriter.serializeFilteredAsProperty()`) must still
    // get per-entry filtering
    @Test
    public void anyGetterFilteringWithDirectFilterCall() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule().setSerializerModifier(new ValueSerializerModifier() {
                    @Override
                    public ValueSerializer<?> modifySerializer(SerializationConfig config,
                            BeanDescription.Supplier beanDesc, ValueSerializer<?> ser) {
                        if (ser instanceof BeanSerializer beanSer) {
                            return new DirectFilterCallingSerializer(beanSer);
                        }
                        return ser;
                    }
                }))
                .build();
        FilterProvider excluding = new SimpleFilterProvider().addFilter("anyFilter",
                SimpleBeanPropertyFilter.serializeAllExcept("secret"));
        assertEquals("""
                {"name":"bob","a":"1"}""",
                mapper.writer(excluding).writeValueAsString(new AnyBeanWithSecret()));
        FilterProvider including = new SimpleFilterProvider().addFilter("anyFilter",
                SimpleBeanPropertyFilter.filterOutAllExcept("name", "a"));
        assertEquals("""
                {"name":"bob","a":"1"}""",
                mapper.writer(including).writeValueAsString(new AnyBeanWithSecret()));
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
