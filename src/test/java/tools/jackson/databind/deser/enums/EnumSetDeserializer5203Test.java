package tools.jackson.databind.deser.enums;

import java.util.EnumSet;

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
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// For [databind#5203]
public class EnumSetDeserializer5203Test
    extends DatabindTestUtil
{
    enum MyEnum {
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

    @Test
    public void nullsDefaultTest() throws Exception {
        // In 2.x, default is to skip nulls (for backwards-compatibility)
        // But in 3.x, should FAIL
        final ObjectMapper mapper = createMapperWithCustomDeserializer(Nulls.DEFAULT);
        assertThrows(
                InvalidNullException.class,
                () -> mapper.readValue("{\"set\":[\"\"]}", new TypeReference<Dst>(){})
                );
    }

    @Test
    public void nullsFailTest() throws Exception {
        final ObjectMapper mapper = createMapperWithCustomDeserializer(Nulls.FAIL);

        assertThrows(
                InvalidNullException.class,
                () -> mapper.readValue("{\"set\":[\"\"]}", new TypeReference<Dst>(){})
        );
    }
    
    @Test
    public void nullsSkipTest() throws Exception {
        final ObjectMapper mapper = createMapperWithCustomDeserializer(Nulls.SKIP);
        _verifySkipResult(mapper);
    }

    private ObjectMapper createMapperWithCustomDeserializer(Nulls nullHandling) {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(MyEnum.class, new EmptyStringToNullDeserializer());

        return JsonMapper.builder()
                .addModule(module)
                .changeDefaultNullHandling(n -> JsonSetter.Value.forContentNulls(nullHandling))
                .build();
    }

    private void _verifySkipResult(ObjectMapper mapper) throws Exception
    {
        Dst dst = mapper.readValue("{\"set\":[\"FOO\",\"\"]}", new TypeReference<Dst>() {});

        // Null value (from empty string) should be skipped, but FOO should be present
        assertEquals(1, dst.getSet().size());
        assertEquals(MyEnum.FOO, dst.getSet().iterator().next());
    }
}
