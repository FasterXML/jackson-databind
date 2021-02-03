package com.fasterxml.jackson.databind.failing;

import java.util.Collections;
import java.util.List;

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

         list = List.of();
         actualJson = MAPPER.writeValueAsString(list);
         output = MAPPER.readValue(actualJson, List.class);
         assertEquals(0, output.size());
    }

    public void testJava9ListWrapped() throws Exception
    {
         final List<String> list = Collections.unmodifiableList(List.of("a"));
         final String actualJson = MAPPER.writeValueAsString(list);
         final List<?>  output = MAPPER.readValue(actualJson, List.class);
         assertEquals(1, output.size());
    }
}
