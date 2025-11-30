package tools.jackson.databind.records.tofix;

import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

// [databind#5312] Include.NON_DEFAULT regression for objects with @JsonValue
public class JsonIncludeNonDefaultOnRecord5312Test
{
    record StringValue(String value) {
        @Override
        @JsonValue
        public String value() {
            return value;
        }
    }

    record Pojo1(StringValue value) { }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    record Pojo2(StringValue value) { }

    record Pojo3(@JsonInclude(JsonInclude.Include.NON_DEFAULT) StringValue value) { }

    private final ObjectMapper MAPPER = JsonMapper.builder()
            //might be relevant for analysis, but does not affect test outcome
            .changeDefaultPropertyInclusion(incl -> JsonInclude.Value.construct(NON_DEFAULT, NON_DEFAULT))
            .withConfigOverride(String.class,
                    o -> o.setInclude(JsonInclude.Value.construct(NON_NULL, NON_NULL)))

            .build();

    @JacksonTestFailureExpected
    @Test
    void testSerialization1() throws Exception {
        //FAIL on jackson 2.18.2 / 2.20.0
        Assertions.assertEquals("{\"value\":\"\"}",
                MAPPER.writeValueAsString(new Pojo1(new StringValue(""))));
    }

    //PASS
    @Test
    void testSerialization2() throws Exception {
        Assertions.assertEquals("{\"value\":\"\"}",
                MAPPER.writeValueAsString(new Pojo2(new StringValue(""))));
        }

    @JacksonTestFailureExpected
    @Test
    void testSerialization3() throws Exception {
        //FAIL on jackson 2.18.2 / 2.20.0
        Assertions.assertEquals("{\"value\":\"\"}", MAPPER.writeValueAsString(new Pojo3(new StringValue(""))));
    }
}
