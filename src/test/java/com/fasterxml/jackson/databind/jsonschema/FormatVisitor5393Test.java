package com.fasterxml.jackson.databind.jsonschema;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#5393] @JsonAnyGetter property gets included in generated schema since 2.19.0
public class FormatVisitor5393Test
    extends DatabindTestUtil
{
    static class TestJsonIgnoredProperties
    {
        @JsonIgnore
        public String ignoredProp;

        public String normalProperty;

        @JsonProperty("renamedProperty")
        public String someProperty;

        // [databind#5393]
        @JsonAnyGetter
        public Map<String, Object> anyProperties() {
            return new TreeMap<>();
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#5393]: regression wrt `@JsonAnyGetter`
    @Test
    public void ignoreExplicitlyIgnoredAndAnyGetter() throws Exception {
        final TreeSet<String> expected = new TreeSet<>();
        expected.add("normalProperty");
        expected.add("renamedProperty");

        final Set<String> actual = new TreeSet<>();
        MAPPER.acceptJsonFormatVisitor(TestJsonIgnoredProperties.class,
                new JsonFormatVisitorWrapper.Base() {
                    @Override
                    public JsonObjectFormatVisitor expectObjectFormat(JavaType type) {
                        return new JsonObjectFormatVisitor.Base() {
                            @Override
                            public void property(BeanProperty prop) {
                                actual.add(prop.getName());
                            }

                            @Override
                            public void property(String name, JsonFormatVisitable handler, JavaType propertyTypeHint) {
                                actual.add(name);
                            }

                            @Override
                            public void optionalProperty(BeanProperty prop) {
                                actual.add(prop.getName());
                            }

                            @Override
                            public void optionalProperty(String name, JsonFormatVisitable handler, JavaType propertyTypeHint) {
                                actual.add(name);
                            }
                        };
                    }
                });

        assertEquals(expected, actual);
    }
}
