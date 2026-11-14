package tools.jackson.databind.deser.filter;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// With case-insensitive property matching, names to ignore (and include) must
// match case-insensitively too; otherwise a name that would have matched the
// property in some other case is reported as unknown, or goes to the any-setter
class IgnoredNamesCaseInsensitiveTest extends DatabindTestUtil
{
    static class IgnoredIdBean {
        @JsonIgnore
        public String id = "unset";
    }

    static class IgnoredFieldBean {
        @JsonIgnore
        public String secret = "unset";

        public String title = "";
    }

    @JsonIgnoreProperties("secret")
    static class ClassIgnoralBean {
        public String secret = "unset";

        public String title = "";
    }

    static class AnySetterBean {
        @JsonIgnore
        public String secret = "unset";

        public Map<String, Object> leftovers = new LinkedHashMap<>();

        @JsonAnySetter
        public void addLeftover(String name, Object value) { leftovers.put(name, value); }
    }

    @JsonIgnoreProperties("secret")
    static class CreatorBean {
        final String secret, title;

        @JsonCreator
        CreatorBean(@JsonProperty("secret") String secret, @JsonProperty("title") String title) {
            this.secret = secret;
            this.title = title;
        }
    }

    @JsonDeserialize(builder = BuiltBean.Builder.class)
    static class BuiltBean {
        final String secret, title;

        BuiltBean(String secret, String title) {
            this.secret = secret;
            this.title = title;
        }

        @JsonPOJOBuilder(withPrefix = "set")
        @JsonIgnoreProperties("secret")
        static class Builder {
            String secret = "unset", title = "";

            public Builder setSecret(String s) { secret = s; return this; }
            public Builder setTitle(String t) { title = t; return this; }

            public BuiltBean build() { return new BuiltBean(secret, title); }
        }
    }

    @JsonIncludeProperties("title")
    static class IncludeBean {
        public String secret = "unset";

        public String title = "";
    }

    static class Inner {
        public String secret = "unset";

        public String title = "";
    }

    // Case-insensitivity enabled per-property, not globally
    static class PerPropertyContainer {
        @JsonIgnoreProperties("secret")
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
        public Inner inner;
    }

    private final ObjectMapper CI_MAPPER = jsonMapperBuilder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final ObjectMapper STRICT_MAPPER = jsonMapperBuilder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final String DOC = """
            {"SECRET":"from-input","TITLE":"x"}
            """;

    @Test
    void ignoredField() throws Exception {
        IgnoredFieldBean bean = CI_MAPPER.readValue(DOC, IgnoredFieldBean.class);
        assertEquals("unset", bean.secret);
        assertEquals("x", bean.title);
    }

    @Test
    void classLevelIgnoral() throws Exception {
        ClassIgnoralBean bean = CI_MAPPER.readValue(DOC, ClassIgnoralBean.class);
        assertEquals("unset", bean.secret);
        assertEquals("x", bean.title);
    }

    @Test
    void ignoredNotPassedToAnySetter() throws Exception {
        AnySetterBean bean = CI_MAPPER.readValue("""
                {"Secret":"from-input","other":1}
                """, AnySetterBean.class);
        assertEquals("unset", bean.secret);
        assertEquals(Map.of("other", 1), bean.leftovers);
    }

    @Test
    void creatorIgnoral() throws Exception {
        CreatorBean bean = CI_MAPPER.readValue(DOC, CreatorBean.class);
        assertNull(bean.secret);
        assertEquals("x", bean.title);
    }

    @Test
    void builderIgnoral() throws Exception {
        BuiltBean bean = CI_MAPPER.readValue(DOC, BuiltBean.class);
        assertEquals("unset", bean.secret);
        assertEquals("x", bean.title);
    }

    @Test
    void includedNames() throws Exception {
        IncludeBean bean = CI_MAPPER.readValue(DOC, IncludeBean.class);
        assertEquals("unset", bean.secret);
        assertEquals("x", bean.title);
    }

    @Test
    void perPropertyCaseInsensitivity() throws Exception {
        PerPropertyContainer c = STRICT_MAPPER.readValue("""
                {"inner":{"SECRET":"from-input","TITLE":"x"}}
                """, PerPropertyContainer.class);
        assertEquals("unset", c.inner.secret);
        assertEquals("x", c.inner.title);
    }

    // Names to ignore must be matched with the same Locale as property names
    // (one deserializer was built with), not that of a later reader: in Turkish,
    // "ID" lower-cases to "ıd" (dotless i)
    @Test
    void localeOfPropertyMatching() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .defaultLocale(Locale.ENGLISH)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        final String json = """
                {"ID":"from-input"}
                """;
        assertEquals("unset", mapper.readValue(json, IgnoredIdBean.class).id);
        assertEquals("unset", mapper.readerFor(IgnoredIdBean.class)
                .with(Locale.forLanguageTag("tr")).<IgnoredIdBean>readValue(json).id);
    }

    // ... but without case-insensitivity, names only match exactly
    @Test
    void caseSensitiveByDefault() throws Exception {
        assertThrows(UnrecognizedPropertyException.class,
                () -> STRICT_MAPPER.readValue("""
                        {"SECRET":"from-input"}
                        """, IgnoredFieldBean.class));
    }
}
