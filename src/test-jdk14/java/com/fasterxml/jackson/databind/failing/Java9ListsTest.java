package com.fasterxml.jackson.databind.failing;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DefaultTyping;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.NoCheckSubTypeValidator;

// for [databind#2900]
public class Java9ListsTest extends BaseMapTest
{
    private final ObjectMapper MAPPER = JsonMapper.builder()
            .activateDefaultTypingAsProperty(
                    NoCheckSubTypeValidator.instance,
                    DefaultTyping.NON_FINAL,
                 "@class"
            ).build();

    public void testUnmodifiableList() throws Exception {

         final List<String> list = Collections.unmodifiableList(Collections.singletonList("a"));
         final String actualJson = MAPPER.writeValueAsString(list);
// System.out.println("Test/1: json="+actualJson);
         final List<?> output = MAPPER.readValue(actualJson, List.class);
         assertEquals(1, output.size());
    }

    public void testJava9UmodifiableList() throws Exception {
         final List<String> list = List.of("a");
/*
         {
             Class<?> cls = list.getClass();
             JavaType type = MAPPER.constructType(cls);
System.err.println("LIST type: "+type);
System.err.println(" final? "+type.isFinal());
         }
         */
         final String actualJson = MAPPER.writeValueAsString(list);
//System.out.println("Test/2: json="+actualJson);
         final List<?>  output = MAPPER.readValue(actualJson, List.class);
         assertEquals(1, output.size());
    }

    public void testJava9ListWrapped() throws Exception {
         final List<String> list = Collections.unmodifiableList(List.of("a"));
         final String actualJson = MAPPER.writeValueAsString(list);
// System.out.println("Test/3: json="+actualJson);
         final List<?>  output = MAPPER.readValue(actualJson, List.class);
         assertEquals(1, output.size());
    }
}
