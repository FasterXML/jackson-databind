package tools.jackson.databind.format;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

class PolymorphicMapEntryPOJOTest extends DatabindTestUtil
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
    @JsonSubTypes(@JsonSubTypes.Type(value = Key.class, name = "key"))
    static abstract class KeyBase {
        public String label;
    }

    static class Key extends KeyBase { }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
    @JsonSubTypes(@JsonSubTypes.Type(value = Value.class, name = "value"))
    static abstract class ValueBase {
        public int number;
    }

    static class Value extends ValueBase { }

    static class Holder {
        @JsonFormat(shape = JsonFormat.Shape.POJO)
        public Map.Entry<KeyBase, ValueBase> entry;
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .withConfigOverride(Map.Entry.class,
                    o -> o.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.POJO)))
            .build();

    @Test
    void polymorphicKeyAndValue() throws Exception {
        Map.Entry<KeyBase, ValueBase> result = MAPPER.readValue(
                a2q("{'key':{'kind':'key','label':'answer'},"
                        + "'value':{'kind':'value','number':42}}"), new TypeReference<>() { });

        assertInstanceOf(Key.class, result.getKey());
        assertEquals("answer", result.getKey().label);
        assertInstanceOf(Value.class, result.getValue());
        assertEquals(42, result.getValue().number);
    }

    @Test
    void polymorphicKeyOnly() throws Exception {
        Map.Entry<KeyBase, String> result = MAPPER.readValue(
                a2q("{'key':{'kind':'key','label':'answer'},'value':'plain'}"),
                new TypeReference<>() { });

        assertInstanceOf(Key.class, result.getKey());
        assertEquals("answer", result.getKey().label);
        assertEquals("plain", result.getValue());
    }

    @Test
    void polymorphicValueOnly() throws Exception {
        Map.Entry<String, ValueBase> result = MAPPER.readValue(
                a2q("{'key':'plain','value':{'kind':'value','number':42}}"),
                new TypeReference<>() { });

        assertEquals("plain", result.getKey());
        assertInstanceOf(Value.class, result.getValue());
        assertEquals(42, result.getValue().number);
    }

    @Test
    void propertyLevelPOJOShape() throws Exception {
        Holder result = newJsonMapper().readValue(
                a2q("{'entry':{'key':{'kind':'key','label':'answer'},"
                        + "'value':{'kind':'value','number':42}}}"), Holder.class);

        assertInstanceOf(Key.class, result.entry.getKey());
        assertEquals("answer", result.entry.getKey().label);
        assertInstanceOf(Value.class, result.entry.getValue());
        assertEquals(42, result.entry.getValue().number);
    }

    @Test
    void naturalShapeStillSupportsPolymorphicValues() throws Exception {
        Map.Entry<String, ValueBase> result = newJsonMapper().readValue(
                a2q("{'answer':{'kind':'value','number':42}}"), new TypeReference<>() { });

        assertEquals("answer", result.getKey());
        assertInstanceOf(Value.class, result.getValue());
        assertEquals(42, result.getValue().number);
    }
}
