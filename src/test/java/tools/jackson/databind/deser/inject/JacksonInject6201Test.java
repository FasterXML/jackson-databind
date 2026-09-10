package tools.jackson.databind.deser.inject;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.InjectableValues;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#6201]: `useInput = OptBoolean.FALSE` must drop value from input for
// Field- and Setter-backed properties too, not just Creator ones
class JacksonInject6201Test extends DatabindTestUtil
{
    static class FieldBean {
        @JacksonInject(value = "tenant", useInput = OptBoolean.FALSE)
        public String tenant = "unset";

        public String title = "";
    }

    static class SetterBean {
        String tenant = "unset";
        public String title = "";

        @JacksonInject(value = "tenant", useInput = OptBoolean.FALSE)
        public void setTenant(String t) { tenant = t; }
    }

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder({ "title", "tenant" })
    static class AsArrayBean {
        public String title = "";

        @JacksonInject(value = "tenant", useInput = OptBoolean.FALSE)
        public String tenant = "unset";
    }

    @JsonDeserialize(builder = BuiltBean.Builder.class)
    static class BuiltBean {
        public final String tenant, title;

        BuiltBean(String tenant, String title) {
            this.tenant = tenant;
            this.title = title;
        }

        @JsonPOJOBuilder(withPrefix = "set")
        static class Builder {
            @JacksonInject(value = "tenant", useInput = OptBoolean.FALSE)
            public String tenant = "unset";

            String title = "";

            public Builder setTitle(String t) { title = t; return this; }

            public BuiltBean build() { return new BuiltBean(tenant, title); }
        }
    }

    static class UnwrappedBean {
        @JsonUnwrapped
        public FieldBean inner = new FieldBean();
    }

    static class AnySetterBean {
        @JacksonInject(value = "tenant", useInput = OptBoolean.FALSE)
        public String tenant = "unset";

        public Map<String, Object> leftovers = new LinkedHashMap<>();

        @JsonAnySetter
        public void addLeftover(String name, Object value) { leftovers.put(name, value); }
    }

    static class UseInputTrueBean {
        @JacksonInject(value = "tenant", useInput = OptBoolean.TRUE)
        public String tenant = "unset";
    }

    static class UseInputDefaultBean {
        @JacksonInject("tenant")
        public String tenant = "unset";
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .injectableValues(new InjectableValues.Std().addValue("tenant", "injected"))
            .build();

    private final String DOC = """
            {"tenant":"from-input","title":"x"}
            """;

    @Test
    void injectOnlyField() throws Exception {
        assertEquals("injected", MAPPER.readValue(DOC, FieldBean.class).tenant);
    }

    @Test
    void injectOnlySetter() throws Exception {
        assertEquals("injected", MAPPER.readValue(DOC, SetterBean.class).tenant);
    }

    @Test
    void injectOnlyAsArray() throws Exception {
        AsArrayBean bean = MAPPER.readValue("""
                ["x","from-input"]
                """, AsArrayBean.class);
        assertEquals("injected", bean.tenant);
        assertEquals("x", bean.title);
    }

    @Test
    void injectOnlyWithBuilder() throws Exception {
        assertEquals("injected", MAPPER.readValue(DOC, BuiltBean.class).tenant);
    }

    @Test
    void injectOnlyWhenUnwrapped() throws Exception {
        assertEquals("injected", MAPPER.readValue(DOC, UnwrappedBean.class).inner.tenant);
    }

    // Value from input must not reach the any-setter either
    @Test
    void injectOnlyWithAnySetter() throws Exception {
        AnySetterBean bean = MAPPER.readValue("""
                {"tenant":"from-input"}
                """, AnySetterBean.class);
        assertEquals("injected", bean.tenant);
        assertEquals(0, bean.leftovers.size());
    }

    @Test
    void injectOnlyWhenUpdating() throws Exception {
        FieldBean bean = MAPPER.readerForUpdating(new FieldBean()).readValue(DOC);
        assertEquals("injected", bean.tenant);
    }

    // ... while the other two settings keep binding from input
    @Test
    void useInputTrueStillBinds() throws Exception {
        assertEquals("from-input", MAPPER.readValue(DOC, UseInputTrueBean.class).tenant);
    }

    @Test
    void useInputDefaultStillBinds() throws Exception {
        assertEquals("from-input", MAPPER.readValue(DOC, UseInputDefaultBean.class).tenant);
    }
}
