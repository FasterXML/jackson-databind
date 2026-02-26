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
import tools.jackson.databind.annotation.JsonWrapped;
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
        @JsonWrapped(value = "w", enabled = false)
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
    @DisplayName("enabled flag tests")
    class EnabledFlagTests {

        @Test
        @DisplayName("should serialize flat when enabled=false")
        void enabledFalse() throws Exception
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
}
