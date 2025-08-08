package tools.jackson.databind.tofix;

import java.util.EnumSet;

import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import tools.jackson.core.JsonParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.InvalidNullException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// For [databind#5165]
public class EnumSetDeserializer5165Test
{
    public enum MyEnum {
        FOO
    }

    static class Dst {
        private EnumSet<MyEnum> set;

        public EnumSet<MyEnum> getSet() {
            return set;
        }

        public void setSet(EnumSet<MyEnum> set) {
            this.set = set;
        }
    }

    // Custom deserializer that converts empty strings to null
    static class EmptyStringToNullDeserializer extends StdDeserializer<MyEnum> {
        public EmptyStringToNullDeserializer() {
            super(MyEnum.class);
        }

        @Override
        public MyEnum deserialize(JsonParser p, DeserializationContext ctxt) {
            String value = p.getValueAsString();
            if (value != null && value.isEmpty()) {
                return null;
            }
            return MyEnum.valueOf(value);
        }
    }

    private ObjectMapper createMapperWithCustomDeserializer() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(MyEnum.class, new EmptyStringToNullDeserializer());

        return JsonMapper.builder()
                .addModule(module)
                .changeDefaultNullHandling(n -> JsonSetter.Value.forContentNulls(Nulls.FAIL))
                .build();
    }

    @JacksonTestFailureExpected
    @Test
    public void nullsFailTest() {
        ObjectMapper mapper = createMapperWithCustomDeserializer();

        assertThrows(
                InvalidNullException.class,
                () -> mapper.readValue("{\"set\":[\"\"]}", new TypeReference<Dst>(){})
        );
    }

    @JacksonTestFailureExpected
    @Test
    public void nullsSkipTest() throws Exception {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(MyEnum.class, new EmptyStringToNullDeserializer());

        ObjectMapper mapper = JsonMapper.builder()
                .addModule(module)
                .changeDefaultNullHandling(n -> JsonSetter.Value.forContentNulls(Nulls.SKIP))
                .build();

        Dst dst = mapper.readValue("{\"set\":[\"FOO\",\"\"]}", new TypeReference<Dst>() {});

        assertTrue(dst.getSet().isEmpty(), "Null values should be skipped");
    }
}
