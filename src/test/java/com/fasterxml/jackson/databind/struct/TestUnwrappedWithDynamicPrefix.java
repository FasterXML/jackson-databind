package com.fasterxml.jackson.databind.struct;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestUnwrappedWithDynamicPrefix extends DatabindTestUtil {
    private final ObjectMapper MAPPER = newJsonMapper();
    private final ObjectMapper CUSTOM_MAPPER = newJsonMapper()
            .registerModule(new SimpleModule()
                    .addSerializer(ValueAndMap.class, new ValueAndMapSerializer())
                    .addDeserializer(ValueAndMap.class, new ValueAndMapDeserializer()));

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class TwoUnwrappedPropertiesWithSameClass {
        @JsonUnwrapped
        public ValueAndMap prop1;
        @JsonUnwrapped
        public ValueAndMap prop2;
    }

    static class ValueAndMap {
        public String value;
        public Map<String, String> value_l10n;

        @JsonIgnore
        public boolean isEmpty() {
            return value == null && (value_l10n == null || value_l10n.isEmpty());
        }

    }

    @Test
    public void testUnwrappingWithDifferentProperties() throws JsonProcessingException {
        TwoUnwrappedPropertiesWithSameClass o = new TwoUnwrappedPropertiesWithSameClass();
        o.prop1 = new ValueAndMap();
        o.prop1.value = "foo";
        o.prop1.value_l10n = Map.of("a", "b");
        o.prop2 = new ValueAndMap();
        o.prop2.value = "bar";
        o.prop2.value_l10n = Map.of("c", "d");

        // currently produces JSON object with duplicate keys JSON
        String s = MAPPER.writeValueAsString(o);
        // TODO: this is just for visualization, not sure if we should throw an exception to avoid writing invalid JSON
        assertEquals("""
                        {"value":"foo","value_l10n":{"a":"b"},"value":"bar","value_l10n":{"c":"d"}}"""
                , s);

        System.out.println(s);
    }

    @Test
    public void testUnwrappingWithCustomSerializer() throws JsonProcessingException {
        TwoUnwrappedPropertiesWithSameClass o = new TwoUnwrappedPropertiesWithSameClass();
        o.prop1 = new ValueAndMap();
        o.prop1.value = "foo";
        o.prop1.value_l10n = Map.of("a", "b");
        o.prop2 = new ValueAndMap();
        o.prop2.value = "bar";
        o.prop2.value_l10n = Map.of("c", "d");

        String s = CUSTOM_MAPPER.writeValueAsString(o);
        assertEquals("""
                        {"prop1":"foo","prop1_l10n":{"a":"b"},"prop2":"bar","prop2_l10n":{"c":"d"}}"""
                , s);
    }

    @Test
    public void testUnwrappingWithCustomDeserializer() throws JsonProcessingException {
        String json = """
                {"prop1":"foo","prop1_l10n":{"a":"b"},"prop2":"bar","prop2_l10n":{"c":"d"}}""";

        TwoUnwrappedPropertiesWithSameClass o = CUSTOM_MAPPER.readValue(json, TwoUnwrappedPropertiesWithSameClass.class);
        assertNotNull(o.prop1);
        assertEquals("foo", o.prop1.value);
        assertEquals(Map.of("a", "b"), o.prop1.value_l10n);
        assertNotNull(o.prop2);
        assertEquals("bar", o.prop2.value);
        assertEquals(Map.of("c", "d"), o.prop2.value_l10n);
    }

    static class ValueAndMapDeserializer extends StdDeserializer<ValueAndMap> implements ContextualDeserializer {

        private final String name;

        public ValueAndMapDeserializer() {
            this(null);
        }

        public ValueAndMapDeserializer(String name) {
            super(ValueAndMap.class);
            this.name = name == null || name.trim().isEmpty() ? "name" : name;
        }

        @Override
        public ValueAndMap deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            ValueAndMap result = new ValueAndMap();

            JsonNode node = ctxt.readTree(p);
            result.value = getText(node, name);

            JsonNode l10n = node.get(name + "_l10n");
            if (l10n != null && l10n.isObject()) {
                Map<String, String> m = new HashMap<>();
                ObjectNode n = (ObjectNode) l10n;
                Iterator<String> stringIterator = n.fieldNames();
                if (stringIterator != null) {
                    while (stringIterator.hasNext()) {
                        String fieldName = stringIterator.next();
                        m.put(fieldName, n.get(fieldName).textValue());
                    }
                }
                if (!m.isEmpty()) {
                    result.value_l10n = new HashMap<>(m);
                }
            }

            return result.isEmpty() ? null : result;
        }

        private String getText(JsonNode node, String name) {
            JsonNode field = node.get(name);
            return field == null ? null : field.asText();
        }

        @Override
        public JsonDeserializer<ValueAndMap> createContextual(DeserializationContext ctxt, BeanProperty property) {
            if (property == null) {
                return null;
            }
            return new ValueAndMapDeserializer(property.getName());
        }
    }

    static class ValueAndMapSerializer extends StdSerializer<ValueAndMap> implements ContextualSerializer {

        private final String name;

        public ValueAndMapSerializer() {
            this(null);
        }

        public ValueAndMapSerializer(String name) {
            super(ValueAndMap.class);
            this.name = name == null || name.trim().isEmpty() ? "name" : name;
        }

        @Override
        public boolean isUnwrappingSerializer() {
            return true;
        }

        @Override
        public void serialize(ValueAndMap field, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            String value = field.value;
            if (value != null && !value.trim().isEmpty()) {
                jgen.writeStringField(name, value);
            }
            Map<String, String> l10n = field.value_l10n;
            if (l10n != null && !l10n.isEmpty()) {
                jgen.writeFieldName(name + "_l10n");
                jgen.writeStartObject();
                for (Map.Entry<String, String> e : l10n.entrySet()) {
                    jgen.writeStringField(e.getKey(), e.getValue());
                }
                jgen.writeEndObject();
            }
        }

        @Override
        public JsonSerializer<ValueAndMap> createContextual(SerializerProvider prov, BeanProperty property) {
            if (property == null) {
                return null;
            }
            return new ValueAndMapSerializer(property.getName());
        }
    }
}
