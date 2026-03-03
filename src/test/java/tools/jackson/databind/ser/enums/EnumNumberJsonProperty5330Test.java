package tools.jackson.databind.ser.enums;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * [databind#5330] Test that @JsonProperty value is used as numeric index for Enums with `Shape.NUMBER`
 */
public class EnumNumberJsonProperty5330Test extends DatabindTestUtil
{
    // no JsonFormat override: used to verify that global WRITE_ENUMS_USING_INDEX keeps ordinal semantics.
    public enum MyEnumNoFormat {
        @JsonProperty("7")
        FOO,
        @JsonProperty("42")
        BAR
    }

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    public enum MyEnum {
        @JsonProperty("7")
        FOO,
        @JsonProperty("42")
        BAR
    }

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    public enum NonNumericEnum {
        @JsonProperty("NOT_A_NUMBER")
        VALUE
    }

    static class EnumBean {
        public MyEnum value = MyEnum.BAR;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void shouldSerializeUsingNumericJsonPropertyAsIndexWhenNumberShapeIsEnabled() throws Exception {
        assertEquals("7", MAPPER.writeValueAsString(MyEnum.FOO));

        assertEquals(a2q("{'value':42}"), MAPPER.writeValueAsString(new EnumBean()));
        assertEquals("[7,42]", MAPPER.writeValueAsString(Arrays.asList(MyEnum.FOO, MyEnum.BAR)));
        assertEquals("[7]", MAPPER.writeValueAsString(EnumSet.of(MyEnum.FOO)));
    }

    @Test
    public void shouldDeserializeFromNumericAndQuotedNumericIndexWhenNumberShapeIsEnabled() throws Exception {
        assertEquals(MyEnum.FOO, MAPPER.readValue("7", MyEnum.class));
        assertEquals(MyEnum.FOO, MAPPER.readValue(q("7"), MyEnum.class));
    }

    @Test
    public void shouldUseNumericJsonPropertyIndexForEnumMapKeysWhenNumberShapeIsEnabled() throws Exception {
        Map<MyEnum, String> map = new HashMap<>();
        map.put(MyEnum.FOO, "lucky");

        String json = MAPPER.writeValueAsString(map);
        assertEquals(a2q("{'7':'lucky'}"), json);

        JavaType type = MAPPER.getTypeFactory().constructMapType(HashMap.class, MyEnum.class, String.class);
        Map<MyEnum, String> result = MAPPER.readValue(json, type);

        assertEquals(1, result.size());
        assertEquals("lucky", result.get(MyEnum.FOO));
    }

    @Test
    public void shouldOverrideGlobalIndexFeatureDisableWhenNumberShapeIsEnabled() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
            .disable(EnumFeature.WRITE_ENUMS_USING_INDEX)
            .build();

        assertEquals("7", mapper.writeValueAsString(MyEnum.FOO));
    }

    @Test
    public void shouldKeepOrdinalWhenGlobalIndexFeatureIsEnabledWithoutFormatOverride() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
            .enable(EnumFeature.WRITE_ENUMS_USING_INDEX)
            .build();

        // ordinal semantics: FOO=0, BAR=1 (NOT @JsonProperty("7"/"42"))
        assertEquals("0", mapper.writeValueAsString(MyEnumNoFormat.FOO));
        assertEquals("1", mapper.writeValueAsString(MyEnumNoFormat.BAR));

        // also verify container use-cases (same serializer path)
        assertEquals("[0,1]", mapper.writeValueAsString(Arrays.asList(MyEnumNoFormat.FOO, MyEnumNoFormat.BAR)));
        assertEquals("[1]", mapper.writeValueAsString(EnumSet.of(MyEnumNoFormat.BAR)));
    }

    @Test
    public void shouldFallbackToOrdinalWhenJsonPropertyIsNotNumericEvenWithNumberShapeEnabled() throws Exception {
        assertEquals("0", MAPPER.writeValueAsString(NonNumericEnum.VALUE));
        assertEquals(NonNumericEnum.VALUE, MAPPER.readValue("0", NonNumericEnum.class));
    }

    @Test
    public void shouldNotUseNumericJsonPropertyIndexWithoutNumberShapeOnDeserialization() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder().build();

        // numeric token: should NOT treat @JsonProperty("7"/"42") as numeric index
        assertThrows(MismatchedInputException.class, () -> mapper.readValue("7", MyEnumNoFormat.class));
        assertThrows(MismatchedInputException.class, () -> mapper.readValue("42", MyEnumNoFormat.class));

        // string token: should map by NAME (i.e. @JsonProperty), regardless of number-shape
        assertEquals(MyEnumNoFormat.FOO, mapper.readValue(q("7"), MyEnumNoFormat.class));
        assertEquals(MyEnumNoFormat.BAR, mapper.readValue(q("42"), MyEnumNoFormat.class));
    }

    @Test
    public void shouldNotUseNumericJsonPropertyIndexWithoutNumberShapeEvenWhenGlobalIndexFeatureIsEnabled() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
            .enable(EnumFeature.WRITE_ENUMS_USING_INDEX)
            .build();

        // still must not treat numeric @JsonProperty as numeric index
        assertThrows(MismatchedInputException.class, () -> mapper.readValue("7", MyEnumNoFormat.class));
        assertThrows(MismatchedInputException.class, () -> mapper.readValue("42", MyEnumNoFormat.class));

        // but quoted values are names -> should work
        assertEquals(MyEnumNoFormat.FOO, mapper.readValue(q("7"), MyEnumNoFormat.class));
        assertEquals(MyEnumNoFormat.BAR, mapper.readValue(q("42"), MyEnumNoFormat.class));
    }
}
