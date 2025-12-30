package tools.jackson.databind.deser.jdk;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Feature;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@SuppressWarnings("serial")
class CollectionDeserializer5522Test
{
    @JsonFormat(with = Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    static class CustomNumberList extends ArrayList<Number> { }

    @JsonFormat(with = Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    static class CustomStringList extends ArrayList<String> { }

    static class CustomClassForNumber {
        private CustomNumberList value;

        public CustomNumberList getValue() {
            return value;
        }
        public void setValue(CustomNumberList value) {
            this.value = value;
        }
    }

    static class CustomClassForString {
        private CustomStringList value;

        public CustomStringList getValue() {
            return value;
        }
        public void setValue(CustomStringList value) {
            this.value = value;
        }
    }

    static class CustomClassForListField {
        @JsonFormat(with = Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        private List<String> value;

        public List<String> getValue() {
            return value;
        }
        public void setValue(List<String> value) {
            this.value = value;
        }
    }

    private final ObjectMapper objectMapper = JsonMapper.builder()
        .disable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        .build();

    @Test
    void testCustomNumberCollectionDeserialize() throws Exception{
        // given
        String jsonValue = """
            {
                "value": 1
            }
            """;

        // when
        CustomClassForNumber result = objectMapper.readValue(jsonValue, CustomClassForNumber.class);

        // then
        assertThat(result.value)
            .hasSize(1)
            .containsExactly(1);
    }

    @Test
    void testCustomStringCollectionDeserialize() throws Exception{
        // given
        String jsonValue = """
            {
                "value": "test"
            }
            """;

        // when
        CustomClassForString result = objectMapper.readValue(jsonValue, CustomClassForString.class);

        // then
        assertThat(result.value)
            .hasSize(1)
            .containsExactly("test");
    }

    @Test
    void testStringCollectionDeserializeInField() throws Exception{
        // given
        String jsonValue = """
            {
                "value": "test"
            }
            """;

        // when
        CustomClassForListField result = objectMapper.readValue(jsonValue, CustomClassForListField.class);

        // then
        assertThat(result.value)
            .hasSize(1)
            .containsExactly("test");
    }

}
