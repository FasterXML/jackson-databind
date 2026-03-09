package tools.jackson.databind.jsontype.deftyping;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#3194]: Discrepancy between Type Id inclusion on serialization vs
// expectation during deserialization causes mismatch and fails deserialization.
class PolymorphicArrays3194Test extends DatabindTestUtil
{
    static final class ArrayBean3194 {
        public Object[][] value;
    }

    static class UntypedWrapper3195 {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
                include = JsonTypeInfo.As.WRAPPER_ARRAY)
        public Object value;
    }

    static final PolymorphicTypeValidator OBJECT_ALLOWING_VALIDATOR =
            BasicPolymorphicTypeValidator.builder()
                .allowIfSubTypeIsArray()
                .allowIfSubType(Object.class)
                .build();

    @Test
    void twoDimensionalArrayViaUntyped() throws Exception
    {
        ObjectMapper mapper = JsonMapper
                .builder()
                .polymorphicTypeValidator(OBJECT_ALLOWING_VALIDATOR)
                .build();

        String[][] strs = new String[1][];
        strs[0] = new String[] { "abc", "def" };
        UntypedWrapper3195 input = new UntypedWrapper3195();
        input.value = strs;

        final String json = mapper.writeValueAsString(input);
        UntypedWrapper3195 result = mapper.readerFor(UntypedWrapper3195.class)
                .readValue(json);
        assertThat(result.value).isInstanceOf(String[][].class);
        String[][] resultStrs = (String[][]) result.value;

        assertEquals(1, resultStrs.length);
        assertEquals(2, resultStrs[0].length);
        assertEquals(strs[0][0], resultStrs[0][0]);
        assertEquals(strs[0][1], resultStrs[0][1]);
    }

    // [databind#3194]
    @Test
    void twoDimensionalArrayViaDefaultTyping() throws Exception
    {
        ObjectMapper mapper = JsonMapper
                .builder()
                .activateDefaultTyping(OBJECT_ALLOWING_VALIDATOR, DefaultTyping.NON_FINAL)
                .build();

        ArrayBean3194 instance = new ArrayBean3194();
        instance.value = new String[][]{{"1.1", "1.2"}, {"2.1", "2.2"}};
        String json = mapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(instance);

        ArrayBean3194 result = mapper.readValue(json, ArrayBean3194.class);
        assertEquals(String[][].class, result.value.getClass());
        assertEquals(String[].class, result.value[0].getClass());
    }
}
