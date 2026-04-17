package tools.jackson.databind.records;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import tools.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class RecordJsonCreatorAndJsonValue5923Test {

    public record Inner(@JsonProperty(required = true, value = "innerValue") boolean innerValue) {}

    public record Outer(Inner bools) {
        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public static Outer fromJson(@JsonProperty(required = true, value = "renamed") boolean booleanValue) {
            return new Outer(new Inner(booleanValue));
        }

        @JsonValue
        public Map<String, Boolean> toJson() {
            return Map.of("renamed", bools.innerValue());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    public void testDeserializationBug(boolean testVal) throws Exception {
        var om = new ObjectMapper();
        var bw = om.readValue(String.format("{ \"renamed\": %s }", testVal), Outer.class);
        Assertions.assertEquals(testVal, bw.bools.innerValue, String.valueOf(bw));
    }
}
