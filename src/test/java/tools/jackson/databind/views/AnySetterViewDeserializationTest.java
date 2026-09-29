package tools.jackson.databind.views;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// Verify that `@JsonView` on `@JsonAnySetter` is honored on deserialization,
// same as with regular properties (and `@JsonAnyGetter` on serialization)
public class AnySetterViewDeserializationTest extends DatabindTestUtil
{
    static class ViewA { }
    static class ViewB { }

    static class MethodAnyBean {
        @JsonView(ViewA.class)
        public String a;
        @JsonView(ViewB.class)
        public String b;

        final Map<String, Object> other = new LinkedHashMap<>();

        @JsonAnySetter
        @JsonView(ViewB.class)
        public void set(String key, Object value) {
            other.put(key, value);
        }
    }

    static class FieldAnyBean {
        @JsonView(ViewA.class)
        public String a;

        @JsonAnySetter
        @JsonView(ViewB.class)
        public Map<String, Object> other = new LinkedHashMap<>();
    }

    // Any-setter but without views: behavior unchanged
    static class NoViewAnyBean {
        @JsonView(ViewA.class)
        public String a;

        @JsonAnySetter
        public Map<String, Object> other = new LinkedHashMap<>();
    }

    static class CreatorAnyBean {
        @JsonView(ViewA.class)
        public String a;

        final Map<String, Object> other = new LinkedHashMap<>();

        @JsonCreator
        public CreatorAnyBean(@JsonProperty("a") String a) {
            this.a = a;
        }

        @JsonAnySetter
        @JsonView(ViewB.class)
        public void set(String key, Object value) {
            other.put(key, value);
        }
    }

    record AnyRecord(@JsonView(ViewA.class) String a,
            @JsonAnySetter @JsonView(ViewB.class) Map<String, Object> other) { }

    @JsonDeserialize(builder = BuiltBean.Builder.class)
    static class BuiltBean {
        final String a;
        final Map<String, Object> other;

        BuiltBean(String a, Map<String, Object> other) {
            this.a = a;
            this.other = other;
        }

        @JsonPOJOBuilder(withPrefix = "")
        static class Builder {
            String a;
            final Map<String, Object> other = new LinkedHashMap<>();

            @JsonView(ViewA.class)
            public Builder a(String a) {
                this.a = a;
                return this;
            }

            @JsonAnySetter
            @JsonView(ViewB.class)
            public void set(String key, Object value) {
                other.put(key, value);
            }

            public BuiltBean build() {
                return new BuiltBean(a, other);
            }
        }
    }

    private static final String JSON = """
            {"a":"1","b":"2","x":"3","y":4}
            """;

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .build();

    @Test
    public void methodAnySetterWithView() throws Exception
    {
        MethodAnyBean bean = MAPPER.readerWithView(ViewA.class)
                .forType(MethodAnyBean.class)
                .readValue(JSON);
        assertEquals("1", bean.a);
        assertNull(bean.b);
        assertEquals(Collections.emptyMap(), bean.other);

        bean = MAPPER.readerWithView(ViewB.class)
                .forType(MethodAnyBean.class)
                .readValue(JSON);
        assertNull(bean.a);
        assertEquals("2", bean.b);
        assertEquals(Map.of("x", "3", "y", 4), bean.other);

        // and with no active view, all included
        bean = MAPPER.readValue(JSON, MethodAnyBean.class);
        assertEquals("1", bean.a);
        assertEquals("2", bean.b);
        assertEquals(Map.of("x", "3", "y", 4), bean.other);
    }

    @Test
    public void fieldAnySetterWithView() throws Exception
    {
        FieldAnyBean bean = MAPPER.readerWithView(ViewA.class)
                .forType(FieldAnyBean.class)
                .readValue(JSON);
        assertEquals("1", bean.a);
        assertEquals(Collections.emptyMap(), bean.other);

        bean = MAPPER.readerWithView(ViewB.class)
                .forType(FieldAnyBean.class)
                .readValue(JSON);
        assertNull(bean.a);
        assertEquals(Map.of("b", "2", "x", "3", "y", 4), bean.other);
    }

    @Test
    public void anySetterWithoutViewUnchanged() throws Exception
    {
        NoViewAnyBean bean = MAPPER.readerWithView(ViewB.class)
                .forType(NoViewAnyBean.class)
                .readValue(JSON);
        assertNull(bean.a);
        assertEquals(Map.of("b", "2", "x", "3", "y", 4), bean.other);
    }

    @Test
    public void creatorBeanAnySetterWithView() throws Exception
    {
        CreatorAnyBean bean = MAPPER.readerWithView(ViewA.class)
                .forType(CreatorAnyBean.class)
                .readValue(JSON);
        assertEquals("1", bean.a);
        assertEquals(Collections.emptyMap(), bean.other);

        bean = MAPPER.readerWithView(ViewB.class)
                .forType(CreatorAnyBean.class)
                .readValue(JSON);
        assertEquals(Map.of("b", "2", "x", "3", "y", 4), bean.other);
    }

    @Test
    public void creatorParameterAnySetterWithView() throws Exception
    {
        AnyRecord rec = MAPPER.readerWithView(ViewA.class)
                .forType(AnyRecord.class)
                .readValue(JSON);
        assertEquals("1", rec.a());
        assertEquals(Collections.emptyMap(), rec.other());

        rec = MAPPER.readerWithView(ViewB.class)
                .forType(AnyRecord.class)
                .readValue(JSON);
        assertNull(rec.a());
        assertEquals(Map.of("b", "2", "x", "3", "y", 4), rec.other());
    }

    @Test
    public void builderAnySetterWithView() throws Exception
    {
        BuiltBean bean = MAPPER.readerWithView(ViewA.class)
                .forType(BuiltBean.class)
                .readValue(JSON);
        assertEquals("1", bean.a);
        assertEquals(Collections.emptyMap(), bean.other);

        bean = MAPPER.readerWithView(ViewB.class)
                .forType(BuiltBean.class)
                .readValue(JSON);
        assertNull(bean.a);
        assertEquals(Map.of("b", "2", "x", "3", "y", 4), bean.other);
    }

    @Test
    public void anySetterNotInViewFailsIfEnabled() throws Exception
    {
        ObjectReader r = MAPPER.readerWithView(ViewA.class)
                .forType(FieldAnyBean.class)
                .with(DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES);
        MismatchedInputException e = assertThrows(MismatchedInputException.class,
                () -> r.readValue(JSON));
        verifyException(e, "Property 'b' is not part of current active view");
    }
}
