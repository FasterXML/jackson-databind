package com.fasterxml.jackson.databind.jdk9;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.NoCheckSubTypeValidator;

// for [databind#2900]
public class Java9ListsTest extends BaseMapTest
{
    private final ObjectMapper MAPPER = JsonMapper.builder()
            .activateDefaultTypingAsProperty(
                 new NoCheckSubTypeValidator(),
                 ObjectMapper.DefaultTyping.EVERYTHING,
                 "@class"
            ).build();

    public void testUnmodifiableList() throws Exception
    {
         final List<String> list = Collections.unmodifiableList(Collections.singletonList("a"));
         final String actualJson = MAPPER.writeValueAsString(list);
         final List<?> output = MAPPER.readValue(actualJson, List.class);
         assertEquals(1, output.size());
    }

    public void testJava9ListOf() throws Exception
    {
         List<String> list = List.of("a");
/*         {
             Class<?> cls = list.getClass();
             com.fasterxml.jackson.databind.JavaType type = MAPPER.constructType(cls);
System.err.println("LIST type: "+type);
System.err.println(" final? "+type.isFinal());
         }
         */
         String actualJson = MAPPER.writeValueAsString(list);
         List<?>  output = MAPPER.readValue(actualJson, List.class);
         assertEquals(1, output.size());

         // and couple of alternatives:
         list = List.of("a", "b");
         actualJson = MAPPER.writeValueAsString(list);
         output = MAPPER.readValue(actualJson, List.class);
         assertEquals(2, output.size());

         list = List.of("a", "b", "c");
         actualJson = MAPPER.writeValueAsString(list);
         output = MAPPER.readValue(actualJson, List.class);
         assertEquals(3, output.size());

         list = List.of();
         actualJson = MAPPER.writeValueAsString(list);
         output = MAPPER.readValue(actualJson, List.class);
         assertEquals(0, output.size());
    }

    public void testJava9MapOf() throws Exception
    {
        Map<String,String> map = Map.of("key", "value");
        String actualJson = MAPPER.writeValueAsString(map);
        Map<?,?>  output = MAPPER.readValue(actualJson, Map.class);
        assertEquals(1, output.size());

        // and alternatives
        map = Map.of("key", "value", "foo", "bar");
        actualJson = MAPPER.writeValueAsString(map);
        output = MAPPER.readValue(actualJson, Map.class);
        assertEquals(2, output.size());

        map = Map.of("key", "value", "foo", "bar", "last", "one");
        actualJson = MAPPER.writeValueAsString(map);
        output = MAPPER.readValue(actualJson, Map.class);
        assertEquals(3, output.size());

        map = Map.of();
        actualJson = MAPPER.writeValueAsString(map);
        output = MAPPER.readValue(actualJson, Map.class);
        assertEquals(0, output.size());
    }

    // [databind#3344]
    public void testJava9SetOf() throws Exception
    {
        Set<?> set = Set.of("a", "b", "c");
        String actualJson = MAPPER.writeValueAsString(set);
        Set<?> output = MAPPER.readValue(actualJson, Set.class);
        assertTrue(output instanceof Set<?>);
        assertEquals(set, output);
    }

    public void testJava9ListWrapped() throws Exception
    {
         final List<String> list = Collections.unmodifiableList(List.of("a"));
         final String actualJson = MAPPER.writeValueAsString(list);
         final List<?>  output = MAPPER.readValue(actualJson, List.class);
         assertEquals(1, output.size());
    }
}
