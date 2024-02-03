package com.fasterxml.jackson.databind.jdk17;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.NoCheckSubTypeValidator;

public class Java17CollectionsTest extends BaseMapTest
{
    private final ObjectMapper MAPPER = JsonMapper.builder()
            .activateDefaultTypingAsProperty(
                 new NoCheckSubTypeValidator(),
                 ObjectMapper.DefaultTyping.EVERYTHING,
                 "@class"
            ).build();

    // [databind#3404]
    public void testJava9StreamOf() throws Exception
    {
        List<String> input = Stream.of("a", "b", "c").collect(Collectors.toList());
        String actualJson = MAPPER.writeValueAsString(input);
        List<?> result = MAPPER.readValue(actualJson, List.class);
        assertEquals(input, result);

        input = Stream.of("a", "b", "c").toList();
        actualJson = MAPPER.writeValueAsString(input);
        result = MAPPER.readValue(actualJson, List.class);
        assertEquals(input, result);
    }
}
