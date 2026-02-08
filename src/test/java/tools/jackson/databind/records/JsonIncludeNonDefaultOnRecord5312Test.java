package tools.jackson.databind.records;

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

    // Record with user-added 0-arg "default" constructor
    record Pojo4(StringValue value) {
        Pojo4() { this(null); }
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    record Pojo5(StringValue value) {
        Pojo5() { this(null); }
    }

    private final ObjectMapper MAPPER = JsonMapper.builder()
            //might be relevant for analysis, but does not affect test outcome
            .changeDefaultPropertyInclusion(incl -> JsonInclude.Value.construct(NON_DEFAULT, NON_DEFAULT))
            .withConfigOverride(String.class,
                    o -> o.setInclude(JsonInclude.Value.construct(NON_NULL, NON_NULL)))

            .build();

    @Test
    void testSerialization1() throws Exception {
        Assertions.assertEquals("{\"value\":\"\"}",
                MAPPER.writeValueAsString(new Pojo1(new StringValue(""))));
    }

    @Test
    void testSerialization2() throws Exception {
        Assertions.assertEquals("{\"value\":\"\"}",
                MAPPER.writeValueAsString(new Pojo2(new StringValue(""))));
    }

    @Test
    void testSerialization3() throws Exception {
        Assertions.assertEquals("{\"value\":\"\"}",
                MAPPER.writeValueAsString(new Pojo3(new StringValue(""))));
    }

    // Record with user-added 0-arg constructor, global NON_DEFAULT
    @Test
    void testSerializationWithDefaultCtor() throws Exception {
        // Non-null value should be included
        Assertions.assertEquals("{\"value\":\"\"}",
                MAPPER.writeValueAsString(new Pojo4(new StringValue(""))));
        // Null value should be suppressed (matches default from 0-arg ctor)
        Assertions.assertEquals("{}",
                MAPPER.writeValueAsString(new Pojo4(null)));
    }

    // Record with user-added 0-arg constructor, class-level NON_DEFAULT
    @Test
    void testSerializationWithDefaultCtorClassLevel() throws Exception {
        // Non-null value should be included
        Assertions.assertEquals("{\"value\":\"\"}",
                MAPPER.writeValueAsString(new Pojo5(new StringValue(""))));
        // Null value should be suppressed (matches default from 0-arg ctor)
        Assertions.assertEquals("{}",
                MAPPER.writeValueAsString(new Pojo5(null)));
    }
}
