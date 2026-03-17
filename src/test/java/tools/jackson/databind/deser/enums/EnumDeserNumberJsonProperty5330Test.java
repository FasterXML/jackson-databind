package tools.jackson.databind.deser.enums;

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
 * [databind#5330] Deserialization: {@code @JsonProperty} value used as numeric index
 * for Enums with {@code Shape.NUMBER}
 */
public class EnumDeserNumberJsonProperty5330Test extends DatabindTestUtil
{
    // no JsonFormat override: used to verify that numeric @JsonProperty is NOT used as index.
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

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void shouldDeserializeFromNumericAndQuotedNumericIndex() throws Exception {
        assertEquals(MyEnum.FOO, MAPPER.readValue("7", MyEnum.class));
        assertEquals(MyEnum.FOO, MAPPER.readValue(q("7"), MyEnum.class));
    }

    @Test
    public void shouldDeserializeEnumMapKeysUsingNumericJsonPropertyIndex() throws Exception {
        JavaType type = MAPPER.getTypeFactory().constructMapType(HashMap.class, MyEnum.class, String.class);
        Map<MyEnum, String> result = MAPPER.readValue(a2q("{'7':'lucky'}"), type);

        assertEquals(1, result.size());
        assertEquals("lucky", result.get(MyEnum.FOO));
    }

    @Test
    public void shouldFallbackToOrdinalWhenJsonPropertyIsNotNumeric() throws Exception {
        assertEquals(NonNumericEnum.VALUE, MAPPER.readValue("0", NonNumericEnum.class));
    }

    @Test
    public void shouldNotUseNumericJsonPropertyIndexWithoutNumberShape() throws Exception {
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
