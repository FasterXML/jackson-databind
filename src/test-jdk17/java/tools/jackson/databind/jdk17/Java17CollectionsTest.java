package tools.jackson.databind.jdk17;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.NoCheckSubTypeValidator;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Java17CollectionsTest extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = JsonMapper.builder()
            .activateDefaultTypingAsProperty(
                 new NoCheckSubTypeValidator(),
                 DefaultTyping.NON_FINAL,
                 "@class"
            ).build();

    // [databind#3404]
    @Test
    public void testJava9StreamOf() throws Exception
    {
        ObjectWriter w = MAPPER.writerFor(List.class);
        List<String> input = Stream.of("a", "b", "c").collect(Collectors.toList());
        String actualJson = w.writeValueAsString(input);
        List<?> result = MAPPER.readValue(actualJson, List.class);
        assertEquals(input, result);

        input = Stream.of("a", "b", "c").toList();
        actualJson = w.writeValueAsString(input);
        result = MAPPER.readValue(actualJson, List.class);
        assertEquals(input, result);
    }
}
