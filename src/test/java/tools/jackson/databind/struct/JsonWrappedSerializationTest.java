package tools.jackson.databind.struct;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

import tools.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonWrapped;
import tools.jackson.databind.ser.FilterProvider;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;
import tools.jackson.databind.ser.std.SimpleFilterProvider;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JsonWrapped} annotation serialization.
 */
public class JsonWrappedSerializationTest extends DatabindTestUtil
{
    /*
    /**********************************************************************
    /* Test beans
    /**********************************************************************
     */

    static class Gene {
        public String symbol;

        @JsonWrapped("chr")
        @JsonProperty("id")
        public String chrId;

        @JsonWrapped("chr")
        @JsonProperty("name")
        public String chrName;

        public Gene() { }

        public Gene(String symbol, String chrId, String chrName) {
            this.symbol = symbol;
            this.chrId = chrId;
            this.chrName = chrName;
        }
    }

    static class MultiWrapper {
        @JsonWrapped("w1")
        public int a;

        @JsonWrapped("w1")
        public int b;

        @JsonWrapped("w2")
        public int c;

        @JsonWrapped("w2")
        public int d;

        public MultiWrapper() { }

        public MultiWrapper(int a, int b, int c, int d) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
        }
    }

    static class DisabledWrapping {
        @JsonWrapped("")   // "" = explicitly disabled
        public int x;

        public DisabledWrapping() { }

        public DisabledWrapping(int x) {
            this.x = x;
        }
    }

    // Non-contiguous: x and z wrapped together, y is not wrapped and sits between them
    static class NonContiguousWrapped {
        @JsonWrapped("g")
        public int x;

        public int y;

        @JsonWrapped("g")
        public int z;

        public NonContiguousWrapped() { }

        public NonContiguousWrapped(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    // Ordering: a is not wrapped; b and c are wrapped — wrapper should appear at b's position
    static class OrderingBean {
        public String a;

        @JsonWrapped("w")
        public String b;

        @JsonWrapped("w")
        public String c;

        public OrderingBean() { }

        public OrderingBean(String a, String b, String c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }

    @JsonPropertyOrder({"symbol", "chr"})
    static class GeneWithPropertyOrder {
        @JsonWrapped("chr")
        @JsonProperty("name")
        public String chrName;

        public String symbol;

        @JsonWrapped("chr")
        @JsonProperty("id")
        public String chrId;

        public GeneWithPropertyOrder() { }

        public GeneWithPropertyOrder(String symbol, String chrId, String chrName) {
            this.symbol = symbol;
            this.chrId = chrId;
            this.chrName = chrName;
        }
    }

    static class City {
        public String name;
        public int population;

        public City() { }
        public City(String name, int population) {
            this.name = name;
            this.population = population;
        }
    }

    static class BeanWithPojoWrapped {
        @JsonWrapped("w")
        public City city;

        public BeanWithPojoWrapped() { }
        public BeanWithPojoWrapped(City city) { this.city = city; }
    }

    static class BeanWithListWrapped {
        @JsonWrapped("w")
        public java.util.List<String> tags;

        public BeanWithListWrapped() { }
        public BeanWithListWrapped(java.util.List<String> tags) { this.tags = tags; }
    }

    static class BeanWithMapWrapped {
        @JsonWrapped("w")
        public java.util.Map<String, Integer> counts;

        public BeanWithMapWrapped() { }
        public BeanWithMapWrapped(java.util.Map<String, Integer> counts) { this.counts = counts; }
    }

    static class BeanWithArrayWrapped {
        @JsonWrapped("w")
        public String[] items;

        public BeanWithArrayWrapped() { }
        public BeanWithArrayWrapped(String[] items) { this.items = items; }
    }

    static class BeanWithMixedWrapper {
        @JsonWrapped("w")
        public String label;

        @JsonWrapped("w")
        public City city;

        public BeanWithMixedWrapper() { }
        public BeanWithMixedWrapper(String label, City city) {
            this.label = label;
            this.city = city;
        }
    }

    static class InnerWithWrapped {
        @JsonWrapped("sub")
        public String x;

        @JsonWrapped("sub")
        public String y;

        public InnerWithWrapped() { }
        public InnerWithWrapped(String x, String y) { this.x = x; this.y = y; }
    }

    static class BeanWithNestedWrapping {
        @JsonWrapped("outer")
        public InnerWithWrapped inner;

        public BeanWithNestedWrapping() { }
        public BeanWithNestedWrapping(InnerWithWrapped inner) { this.inner = inner; }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Nested
    @DisplayName("single wrapper tests")
    class SingleWrapperTests {

        @Test
        @DisplayName("should wrap multiple fields under single wrapper name")
        void singleWrapperMultipleFields() throws Exception
        {
            // setup
            Gene gene = new Gene("TP53", "17", "chr17");

            // when
            String json = MAPPER.writeValueAsString(gene);

            // then
            assertThat(json).isEqualTo("{\"symbol\":\"TP53\",\"chr\":{\"id\":\"17\",\"name\":\"chr17\"}}");
        }

        @Test
        @DisplayName("should use @JsonProperty value as inner property name")
        void innerNameControlledByJsonProperty() throws Exception
        {
            // setup
            Gene gene = new Gene("BRCA1", "13", "chr13");

            // when
            String json = MAPPER.writeValueAsString(gene);

            // then — inner names are "id" and "name" from @JsonProperty, not field names "chrId"/"chrName"
            assertThat(json).contains("\"id\":\"13\"");
            assertThat(json).contains("\"name\":\"chr13\"");
            assertThat(json).doesNotContain("chrId");
            assertThat(json).doesNotContain("chrName");
        }
    }

    @Nested
    @DisplayName("multiple wrapper tests")
    class MultipleWrapperTests {

        @Test
        @DisplayName("should produce separate nested objects for different wrapper names")
        void multipleWrappers() throws Exception
        {
            // setup
            MultiWrapper bean = new MultiWrapper(1, 2, 3, 4);

            // when
            String json = MAPPER.writeValueAsString(bean);

            // then
            assertThat(json).isEqualTo("{\"w1\":{\"a\":1,\"b\":2},\"w2\":{\"c\":3,\"d\":4}}");
        }
    }

    @Nested
    @DisplayName("ordering tests")
    class OrderingTests {

        @Test
        @DisplayName("should place wrapper at position of first wrapped field")
        void wrapperPositionAtFirstField() throws Exception
        {
            // setup
            OrderingBean bean = new OrderingBean("x", "y", "z");

            // when
            String json = MAPPER.writeValueAsString(bean);

            // then — wrapper "w" appears at b's position (after "a")
            assertThat(json).isEqualTo("{\"a\":\"x\",\"w\":{\"b\":\"y\",\"c\":\"z\"}}");
        }

        @Test
        @DisplayName("should collect non-contiguous fields into same wrapper")
        void nonContiguousFields() throws Exception
        {
            // setup — x and z share wrapper "g", y is between them but not wrapped
            NonContiguousWrapped bean = new NonContiguousWrapped(1, 2, 3);

            // when
            String json = MAPPER.writeValueAsString(bean);

            // then — wrapper at x's position (first), y remains flat, z is inside wrapper
            assertThat(json).isEqualTo("{\"g\":{\"x\":1,\"z\":3},\"y\":2}");
        }

        @Test
        @DisplayName("should respect @JsonPropertyOrder when wrapper name is listed")
        void wrapperWithJsonPropertyOrder() throws Exception
        {
            // setup — @JsonPropertyOrder({"symbol","chr"}) on GeneWithPropertyOrder
            GeneWithPropertyOrder gene = new GeneWithPropertyOrder("TP53", "17", "chr17");

            // when
            String json = MAPPER.writeValueAsString(gene);

            // then — "symbol" first (per @JsonPropertyOrder), then "chr" object (inner order by field declaration)
            assertThat(json).isEqualTo("{\"symbol\":\"TP53\",\"chr\":{\"id\":\"17\",\"name\":\"chr17\"}}");
        }
    }

    @Nested
    @DisplayName("disabled wrapping tests")
    class DisabledWrappingTests {

        @Test
        @DisplayName("should serialize flat when @JsonWrapped(\"\") disables wrapping")
        void emptyValueDisablesWrapping() throws Exception
        {
            // setup
            DisabledWrapping bean = new DisabledWrapping(42);

            // when
            String json = MAPPER.writeValueAsString(bean);

            // then
            assertThat(json).isEqualTo("{\"x\":42}");
        }
    }

    /*
    /**********************************************************************
    /* MVP limitation tests: @JsonView / @JsonFilter / @JsonInclude ignored
    /* for inner wrapped fields (wrapper always emits all fields)
    /**********************************************************************
     */

    static class Views {
        static class Public { }
        static class Internal extends Public { }
    }

    static class BeanWithViewOnWrappedField {
        @JsonView(Views.Internal.class)
        @JsonWrapped("w")
        public String secret = "hidden";

        @JsonWrapped("w")
        public String visible = "shown";
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class BeanWithNonNullWrappedField {
        @JsonWrapped("w")
        public String present = "value";

        @JsonWrapped("w")
        public String absent = null;
    }

    @JsonFilter("myFilter")
    static class BeanWithFilterOnWrappedField {
        @JsonWrapped("w")
        public String a = "1";

        @JsonWrapped("w")
        public String b = "2";
    }

    @Nested
    @DisplayName("MVP limitation tests")
    class MvpLimitationTests {

        @Test
        @DisplayName("@JsonView on wrapped inner field is ignored — field always included")
        void jsonViewOnWrappedFieldIgnored() throws Exception
        {
            BeanWithViewOnWrappedField bean = new BeanWithViewOnWrappedField();

            // Active view is Public — "secret" has @JsonView(Internal.class), but MVP ignores it
            String json = MAPPER.writerWithView(Views.Public.class).writeValueAsString(bean);

            assertThat(json).contains("\"secret\":\"hidden\"");
            assertThat(json).contains("\"visible\":\"shown\"");
        }

        @Test
        @DisplayName("@JsonInclude(NON_NULL) on bean class still applies to wrapped inner fields (known MVP limitation)")
        void jsonIncludeNonNullOnBeanClassStillApplies() throws Exception
        {
            BeanWithNonNullWrappedField bean = new BeanWithNonNullWrappedField();

            String json = MAPPER.writeValueAsString(bean);

            // Known MVP limitation: class-level @JsonInclude(NON_NULL) still applies
            // to inner wrapped fields because it propagates via SerializationContext.
            // The null field is suppressed, but the wrapper object is still emitted.
            assertThat(json).contains("\"present\":\"value\"");
            assertThat(json).contains("\"w\":{");
            assertThat(json).doesNotContain("\"absent\"");
        }

        @Test
        @DisplayName("@JsonFilter on bean class can suppress wrapper property itself (known MVP limitation)")
        void jsonFilterOnBeanClassCanSuppressWrapper() throws Exception
        {
            // Filter that includes only "a" — the wrapper "w" is not listed.
            // Since filterId is set on the bean serializer, the wrapper itself
            // is subject to class-level filtering by wrapper name.
            FilterProvider filtersExcludingWrapper = new SimpleFilterProvider()
                    .addFilter("myFilter", SimpleBeanPropertyFilter.filterOutAllExcept("a"));
            BeanWithFilterOnWrappedField bean = new BeanWithFilterOnWrappedField();

            String json = MAPPER.writer(filtersExcludingWrapper).writeValueAsString(bean);

            // Known MVP limitation: class-level @JsonFilter applies to the wrapper
            // property by its name. Inner fields are not individually filtered, but
            // the whole wrapper can be excluded if its name is filtered out.
            assertThat(json).isEqualTo("{}");

            // When the filter includes the wrapper name, all inner fields appear.
            FilterProvider filtersIncludingWrapper = new SimpleFilterProvider()
                    .addFilter("myFilter", SimpleBeanPropertyFilter.filterOutAllExcept("w"));
            String jsonWithWrapper = MAPPER.writer(filtersIncludingWrapper).writeValueAsString(bean);
            assertThat(jsonWithWrapper).contains("\"a\":\"1\"");
            assertThat(jsonWithWrapper).contains("\"b\":\"2\"");
        }
    }

    @Nested
    @DisplayName("non-scalar wrapper tests")
    class NonScalarWrapperTests {

        @Test
        @DisplayName("should wrap POJO field under wrapper name")
        void pojoInsideWrapper() throws Exception {
            BeanWithPojoWrapped bean = new BeanWithPojoWrapped(new City("NYC", 8_000_000));

            String json = MAPPER.writeValueAsString(bean);

            assertThat(json).isEqualTo("{\"w\":{\"city\":{\"name\":\"NYC\",\"population\":8000000}}}");
        }

        @Test
        @DisplayName("should wrap null POJO — null field excluded from wrapper")
        void nullPojoInsideWrapper() throws Exception {
            BeanWithPojoWrapped bean = new BeanWithPojoWrapped(null);

            String json = MAPPER.writeValueAsString(bean);

            assertThat(json).isEqualTo("{\"w\":{}}");
        }

        @Test
        @DisplayName("should wrap List field under wrapper name")
        void listInsideWrapper() throws Exception {
            BeanWithListWrapped bean = new BeanWithListWrapped(
                    java.util.Arrays.asList("java", "jackson"));

            String json = MAPPER.writeValueAsString(bean);

            assertThat(json).isEqualTo("{\"w\":{\"tags\":[\"java\",\"jackson\"]}}");
        }

        @Test
        @DisplayName("should wrap empty List as empty array inside wrapper")
        void emptyListInsideWrapper() throws Exception {
            BeanWithListWrapped bean = new BeanWithListWrapped(java.util.Collections.emptyList());

            String json = MAPPER.writeValueAsString(bean);

            assertThat(json).isEqualTo("{\"w\":{\"tags\":[]}}");
        }

        @Test
        @DisplayName("should wrap Map field under wrapper name")
        void mapInsideWrapper() throws Exception {
            java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
            counts.put("a", 1);
            counts.put("b", 2);
            BeanWithMapWrapped bean = new BeanWithMapWrapped(counts);

            String json = MAPPER.writeValueAsString(bean);

            assertThat(json).isEqualTo("{\"w\":{\"counts\":{\"a\":1,\"b\":2}}}");
        }

        @Test
        @DisplayName("should wrap array field under wrapper name")
        void arrayInsideWrapper() throws Exception {
            BeanWithArrayWrapped bean = new BeanWithArrayWrapped(new String[]{"x", "y"});

            String json = MAPPER.writeValueAsString(bean);

            assertThat(json).isEqualTo("{\"w\":{\"items\":[\"x\",\"y\"]}}");
        }

        @Test
        @DisplayName("should group scalar and POJO under same wrapper name")
        void mixedScalarAndPojoSameWrapper() throws Exception {
            BeanWithMixedWrapper bean = new BeanWithMixedWrapper("home", new City("NYC", 8_000_000));

            String json = MAPPER.writeValueAsString(bean);

            assertThat(json).contains("\"w\":{");
            assertThat(json).contains("\"label\":\"home\"");
            assertThat(json).contains("\"city\":{\"name\":\"NYC\",\"population\":8000000}");
        }

        @Test
        @DisplayName("should compose nested @JsonWrapped — POJO inside wrapper that itself has @JsonWrapped fields")
        void nestedWrapping() throws Exception {
            BeanWithNestedWrapping bean = new BeanWithNestedWrapping(new InnerWithWrapped("a", "b"));

            String json = MAPPER.writeValueAsString(bean);

            assertThat(json).isEqualTo("{\"outer\":{\"inner\":{\"sub\":{\"x\":\"a\",\"y\":\"b\"}}}}");
        }
    }
}
