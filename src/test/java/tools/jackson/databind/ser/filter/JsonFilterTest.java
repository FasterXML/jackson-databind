package tools.jackson.databind.ser.filter;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.TokenStreamContext;
import tools.jackson.databind.*;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.ser.FilterProvider;
import tools.jackson.databind.ser.PropertyWriter;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;
import tools.jackson.databind.ser.std.SimpleFilterProvider;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for verifying that bean property filtering using {@link JsonFilter}
 * works as expected, including {@link SimpleFilterProvider} registration.
 */
public class JsonFilterTest extends DatabindTestUtil
{
    @JsonFilter("RootFilter")
    @JsonPropertyOrder({ "a", "b" })
    static class Bean {
        public String a = "a";
        public String b = "b";
    }

    @JsonFilter("checkSiblingContextFilter")
    static class CheckSiblingContextBean {
        public A a = new A();
        public B b = new B();
        @JsonFilter("checkSiblingContextFilter")
        static class A { }
        @JsonFilter("checkSiblingContextFilter")
        static class B {
            public C c = new C();
            @JsonFilter("checkSiblingContextFilter")
            static class C { }
        }
    }

    @JsonFilter("filterB")
    @JsonPropertyOrder({ "a", "b", "c"})
    static class BeanB {
        public String a;
        public String b;
        public String c;

        public BeanB(String a, String b, String c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }

    static class CheckSiblingContextFilter extends SimpleBeanPropertyFilter {
        @Override
        public void serializeAsProperty(Object bean, JsonGenerator jgen, SerializationContext prov,
                PropertyWriter writer) throws Exception {
            TokenStreamContext sc = jgen.streamWriteContext();

            if (writer.getName() != null && writer.getName().equals("c")) {
                assertEquals("b", sc.getParent().currentName());
            }
            writer.serializeAsProperty(bean, jgen, prov);
        }
    }

    // [databind#89]
    static class Pod
    {
        protected String username;
        protected String userPassword;

        public String getUsername() { return username; }
        public void setUsername(String value) { this.username = value; }

        @JsonIgnore
        @JsonProperty(value = "user_password")
        public String getUserPassword() { return userPassword; }

        @JsonProperty(value = "user_password")
        public void setUserPassword(String value) { this.userPassword = value; }
    }

    // [databind#306]: @JsonFilter for properties too
    @JsonPropertyOrder(alphabetic=true)
    static class FilteredProps
    {
        // will default to using "RootFilter", only including 'a'
        public Bean first = new Bean();

        // but minimal includes 'b'
        @JsonFilter("b")
        public Bean second = new Bean();
    }

    // For SimpleFilterProvider tests
    @JsonFilter("filterB")
    public static class AnyBeanB
    {
        public String a;
        public String b;

        public AnyBeanB(String a, String b) {
            this.a = a;
            this.b = b;
        }
    }

    @JsonFilter(value = "")
    public static class AnyBeanC
    {
        public String c;
        public String d;

        public AnyBeanC(String c, String d) {
            this.c = c;
            this.d = d;
        }
    }

    /*
    /**********************************************************************
    /* Test methods, @JsonFilter annotation
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testSimpleInclusionFilter() throws Exception
    {
        FilterProvider prov = new SimpleFilterProvider().addFilter("RootFilter",
                SimpleBeanPropertyFilter.filterOutAllExcept("a"));
        assertEquals("{\"a\":\"a\"}", MAPPER.writer(prov).writeValueAsString(new Bean()));

        ObjectMapper mapper = jsonMapperBuilder()
                .filterProvider(prov)
                .build();
        assertEquals("{\"a\":\"a\"}", mapper.writeValueAsString(new Bean()));
    }

    @Test
    public void testIncludeAllFilter() throws Exception
    {
        FilterProvider prov = new SimpleFilterProvider().addFilter("RootFilter",
                SimpleBeanPropertyFilter.serializeAll());
        assertEquals("{\"a\":\"a\",\"b\":\"b\"}", MAPPER.writer(prov).writeValueAsString(new Bean()));
    }

    @Test
    public void testExcludeAllFilter() throws Exception
    {
        FilterProvider prov = new SimpleFilterProvider().addFilter("RootFilter",
            SimpleBeanPropertyFilter.filterOutAll());
        assertEquals("{}", MAPPER.writer(prov).writeValueAsString(new Bean()));
    }

    @Test
    public void testSimpleExclusionFilter() throws Exception
    {
        FilterProvider prov = new SimpleFilterProvider().addFilter("RootFilter",
                SimpleBeanPropertyFilter.serializeAllExcept("a"));
        assertEquals("{\"b\":\"b\"}", MAPPER.writer(prov).writeValueAsString(new Bean()));
    }

    @Test
    public void testMissingFilter() throws Exception
    {
        // First: default behavior should be to throw an exception
        try {
            MAPPER.writeValueAsString(new Bean());
            fail("Should have failed without configured filter");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Cannot resolve PropertyFilter with id 'RootFilter'");
        }

        // but when changing behavior, should work differently
        SimpleFilterProvider fp = new SimpleFilterProvider().setFailOnUnknownId(false);
        ObjectMapper mapper = jsonMapperBuilder()
                .filterProvider(fp)
                .build();
        assertEquals("{\"a\":\"a\",\"b\":\"b\"}", mapper.writeValueAsString(new Bean()));
    }

    @Test
    public void testDefaultFilter() throws Exception
    {
        FilterProvider prov = new SimpleFilterProvider().setDefaultFilter(SimpleBeanPropertyFilter.filterOutAllExcept("b"));
        assertEquals("{\"b\":\"b\"}", MAPPER.writer(prov).writeValueAsString(new Bean()));
    }

    // [databind#89] combining @JsonIgnore, @JsonProperty
    @Test
    public void testIssue89() throws Exception
    {
        Pod pod = new Pod();
        pod.username = "Bob";
        pod.userPassword = "s3cr3t!";

        assertEquals("{\"username\":\"Bob\"}", MAPPER.writeValueAsString(pod));

        Pod pod2 = MAPPER.readValue("{\"username\":\"Bill\",\"user_password\":\"foo!\"}", Pod.class);
        assertEquals("Bill", pod2.username);
        assertEquals("foo!", pod2.userPassword);
    }

    // [databind#306]
    @Test
    public void testFilterOnProperty() throws Exception
    {
        FilterProvider prov = new SimpleFilterProvider()
            .addFilter("RootFilter", SimpleBeanPropertyFilter.filterOutAllExcept("a"))
            .addFilter("b", SimpleBeanPropertyFilter.filterOutAllExcept("b"));

        assertEquals("{\"first\":{\"a\":\"a\"},\"second\":{\"b\":\"b\"}}",
                MAPPER.writer(prov).writeValueAsString(new FilteredProps()));
    }

    @Test
    public void testAllFiltersWithSameOutput() throws Exception
    {
        SimpleBeanPropertyFilter[] allPossibleFilters = new SimpleBeanPropertyFilter[]{
            SimpleBeanPropertyFilter.filterOutAllExcept("a", "b"),
            SimpleBeanPropertyFilter.filterOutAllExcept(setOf("a", "b")),
            SimpleBeanPropertyFilter.serializeAllExcept("c"),
            SimpleBeanPropertyFilter.serializeAllExcept(setOf("c")),
            new SimpleBeanPropertyFilter.SerializeExceptFilter(setOf("c")),
            SimpleBeanPropertyFilter.SerializeExceptFilter.serializeAllExcept("c"),
            SimpleBeanPropertyFilter.SerializeExceptFilter.serializeAllExcept(setOf("c")),
            SimpleBeanPropertyFilter.SerializeExceptFilter.filterOutAllExcept("a", "b"),
            SimpleBeanPropertyFilter.SerializeExceptFilter.filterOutAllExcept(setOf("a", "b")),
            new SimpleBeanPropertyFilter.FilterExceptFilter(setOf("a", "b")),
            SimpleBeanPropertyFilter.FilterExceptFilter.serializeAllExcept("c"),
            SimpleBeanPropertyFilter.FilterExceptFilter.serializeAllExcept(setOf("c")),
            SimpleBeanPropertyFilter.FilterExceptFilter.filterOutAllExcept(setOf("a", "b")),
            SimpleBeanPropertyFilter.FilterExceptFilter.filterOutAllExcept("a", "b")
        };

        for (SimpleBeanPropertyFilter filter : allPossibleFilters) {
            BeanB beanB = new BeanB("aa", "bb", "cc");
            SimpleFilterProvider prov = new SimpleFilterProvider().addFilter("filterB", filter);
            assertEquals(a2q("{'a':'aa','b':'bb'}"), MAPPER.writer(prov).writeValueAsString(beanB));
        }
    }

    @Test
    public void testCheckSiblingContextFilter() {
        FilterProvider prov = new SimpleFilterProvider().addFilter("checkSiblingContextFilter",
                new CheckSiblingContextFilter());

        ObjectMapper mapper = jsonMapperBuilder()
                .filterProvider(prov)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .build();
        mapper.valueToTree(new CheckSiblingContextBean());
    }

    /*
    /**********************************************************************
    /* Test methods, SimpleFilterProvider registration
    /**********************************************************************
     */

    @Test
    public void testAddFilterLastOneRemains() throws Exception {
        FilterProvider prov = new SimpleFilterProvider()
                .addFilter("filterB", SimpleBeanPropertyFilter.serializeAll())
                .addFilter("filterB", SimpleBeanPropertyFilter.filterOutAllExcept());
        AnyBeanB beanB = new AnyBeanB("1a", "2b");

        assertEquals("{}", MAPPER.writer(prov).writeValueAsString(beanB));
    }

    @Test
    public void testAddFilterLastOneRemainsFlip() throws Exception {
        FilterProvider prov = new SimpleFilterProvider()
                .addFilter("filterB", SimpleBeanPropertyFilter.filterOutAllExcept("a"))
                .addFilter("filterB", SimpleBeanPropertyFilter.serializeAll());
        AnyBeanB beanB = new AnyBeanB("1a", "2b");

        String jsonString = MAPPER.writer(prov).writeValueAsString(beanB);
        Map<?,?> actualMap = MAPPER.readValue(jsonString, Map.class);
        Map<String, Object> expectedMap = new LinkedHashMap<>();
        expectedMap.put("a", "1a");
        expectedMap.put("b", "2b");

        assertEquals(expectedMap, actualMap);
    }

    @Test
    public void testAddFilterWithEmptyStringId() throws Exception {
        FilterProvider prov = new SimpleFilterProvider()
                .addFilter("", SimpleBeanPropertyFilter.filterOutAllExcept("d"));
        AnyBeanC bean = new AnyBeanC(null, "D is filtered");

        String jsonString = MAPPER.writer(prov).writeValueAsString(bean);
        Map<?,?> actualMap = MAPPER.readValue(jsonString, Map.class);
        Map<String, Object> expectedMap = new LinkedHashMap<>();
        expectedMap.put("c", null);
        expectedMap.put("d", "D is filtered");

        assertEquals(expectedMap, actualMap);
    }

    @Test
    public void testAddingNullFilter2ThrowsException() throws Exception {
        FilterProvider prov = new SimpleFilterProvider()
                .addFilter("filterB", null);
        ObjectWriter writer = MAPPER.writer(prov);
        AnyBeanB beanD = new AnyBeanB("1a", "2b");

        try {
            writer.writeValueAsString(beanD);
            fail("Should not have passed");
        } catch (DatabindException e) {
            verifyException(e, "No filter configured with id 'filterB'");
        }
    }

    @Test
    public void testAddingNullFilterIdThrowsException() throws Exception {
        FilterProvider prov = new SimpleFilterProvider()
                .addFilter(null, SimpleBeanPropertyFilter.serializeAll());
        ObjectWriter writer = MAPPER.writer(prov);
        AnyBeanB beanD = new AnyBeanB("1a", "2b");

        try {
            writer.writeValueAsString(beanD);
            fail("Should not have passed");
        } catch (DatabindException e) {
            verifyException(e, "No filter configured with id 'filterB'");
        }
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private Set<String> setOf(String... properties) {
        Set<String> set = new HashSet<>(properties.length);
        set.addAll(Arrays.asList(properties));
        return set;
    }
}
