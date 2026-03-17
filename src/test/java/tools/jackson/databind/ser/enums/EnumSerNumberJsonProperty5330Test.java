package tools.jackson.databind.ser.enums;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * [databind#5330] Serialization: {@code @JsonProperty} value used as numeric index
 * for Enums with {@code Shape.NUMBER}
 */
public class EnumSerNumberJsonProperty5330Test extends DatabindTestUtil
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
    public void shouldSerializeUsingNumericJsonPropertyAsIndex() throws Exception {
        assertEquals("7", MAPPER.writeValueAsString(MyEnum.FOO));

        assertEquals(a2q("{'value':42}"), MAPPER.writeValueAsString(new EnumBean()));
        assertEquals("[7,42]", MAPPER.writeValueAsString(Arrays.asList(MyEnum.FOO, MyEnum.BAR)));
        assertEquals("[7]", MAPPER.writeValueAsString(EnumSet.of(MyEnum.FOO)));
    }

    @Test
    public void shouldSerializeEnumMapKeysUsingNumericJsonPropertyIndex() throws Exception {
        Map<MyEnum, String> map = new HashMap<>();
        map.put(MyEnum.FOO, "lucky");

        assertEquals(a2q("{'7':'lucky'}"), MAPPER.writeValueAsString(map));
    }

    @Test
    public void shouldOverrideGlobalIndexFeatureDisable() throws Exception {
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
    public void shouldUseJsonPropertyStringWhenNotNumericWithNumberShape() throws Exception {
        // Non-numeric @JsonProperty with Shape.NUMBER: use @JsonProperty value as-is (String)
        assertEquals(q("NOT_A_NUMBER"), MAPPER.writeValueAsString(NonNumericEnum.VALUE));
    }

    @Test
    public void shouldUseOrdinalForNonNumericJsonPropertyWithGlobalIndexFeature() throws Exception {
        // Enum WITHOUT @JsonFormat — global feature uses ordinal, ignores @JsonProperty
        ObjectMapper mapper = jsonMapperBuilder()
            .enable(EnumFeature.WRITE_ENUMS_USING_INDEX)
            .build();

        assertEquals("0", mapper.writeValueAsString(MyEnumNoFormat.FOO));
        assertEquals("1", mapper.writeValueAsString(MyEnumNoFormat.BAR));
    }
}
