package tools.jackson.databind.ser;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonValue;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#6341]: explicit `@JsonValue` accessor must take precedence over
// module-provided serializers for container types
public class JsonValueOverCustomContainerSer6341Test extends DatabindTestUtil
{
    static class BagWithJsonValue extends ArrayList<String> {
        private static final long serialVersionUID = 1L;

        @JsonValue
        public String content() {
            return "bag:1";
        }
    }

    static class MapWithJsonValue extends LinkedHashMap<String, String> {
        private static final long serialVersionUID = 1L;

        @JsonValue
        public String content() {
            return "map:1";
        }
    }

    static class PlainBag extends ArrayList<String> {
        private static final long serialVersionUID = 1L;
    }

    static class Wrapper {
        public BagWithJsonValue bag;

        public Wrapper() {
            bag = new BagWithJsonValue();
        }
    }

    static class CustomContainerSerializer extends ValueSerializer<Object> {
        @Override
        public void serialize(Object value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeString("custom");
        }
    }

    private final ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new SimpleModule()
                    .addSerializer(BagWithJsonValue.class, new CustomContainerSerializer())
                    .addSerializer(MapWithJsonValue.class, new CustomContainerSerializer())
                    .addSerializer(PlainBag.class, new CustomContainerSerializer()))
            .build();

    @Test
    public void jsonValueWinsForCollection() throws Exception {
        assertEquals(a2q("'bag:1'"), MAPPER.writeValueAsString(new BagWithJsonValue()));
    }

    @Test
    public void jsonValueWinsForMap() throws Exception {
        assertEquals(a2q("'map:1'"), MAPPER.writeValueAsString(new MapWithJsonValue()));
    }

    @Test
    public void jsonValueWinsForProperty() throws Exception {
        assertEquals(a2q("{'bag':'bag:1'}"), MAPPER.writeValueAsString(new Wrapper()));
    }

    @Test
    public void customSerializerUsedWithoutJsonValue() throws Exception {
        assertEquals(a2q("'custom'"), MAPPER.writeValueAsString(new PlainBag()));
    }
}
