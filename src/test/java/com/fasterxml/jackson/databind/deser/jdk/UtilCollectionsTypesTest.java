package com.fasterxml.jackson.databind.deser.jdk;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.NoCheckSubTypeValidator;

// Unit tests for [databind#1868], [databind#1880], [databind#2265]
public class UtilCollectionsTypesTest extends BaseMapTest
{
   private final ObjectMapper DEFAULT_MAPPER = JsonMapper.builder()
           .activateDefaultTyping(NoCheckSubTypeValidator.instance,
                   ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
           .build();

   /*
   /**********************************************************
   /* Unit tests, "empty" types
   /**********************************************************
    */

   public void testEmptyList() throws Exception {
       _verifyCollection(Collections.emptyList());
   }

   public void testEmptySet() throws Exception {
       _verifyCollection(Collections.emptySet());
   }

   public void testEmptyMap() throws Exception {
       _verifyMap(Collections.emptyMap());
   }

   /*
   /**********************************************************
   /* Unit tests, "singleton" types
   /**********************************************************
    */

   public void testSingletonList() throws Exception {
       _verifyCollection(Collections.singletonList(Arrays.asList("TheOne")));
   }

   public void testSingletonSet() throws Exception {
       _verifyCollection(Collections.singleton(Arrays.asList("TheOne")));
   }

   public void testSingletonMap() throws Exception {
       _verifyMap(Collections.singletonMap("foo", "bar"));
   }

   /*
   /**********************************************************
   /* Unit tests, "unmodifiable" types
   /**********************************************************
    */

   public void testUnmodifiableList() throws Exception {
       _verifyCollection(Collections.unmodifiableList(Arrays.asList("first", "second")));
   }

   // [databind#2265]
   public void testUnmodifiableListFromLinkedList() throws Exception {
       final List<String> input = new LinkedList<>();
       input.add("first");
       input.add("second");

       // Can't use simple "_verifyCollection" as type may change; instead use
       // bit more flexible check:
       Collection<?> act = _writeReadCollection(Collections.unmodifiableList(input));
       assertEquals(input, act);

       // and this check may be bit fragile (may need to revisit), but is good enough for now:
       assertEquals(Collections.unmodifiableList(new ArrayList<>(input)).getClass(), act.getClass());
   }

   public void testUnmodifiableSet() throws Exception
   {
       Set<String> input = new LinkedHashSet<>(Arrays.asList("first", "second"));
       _verifyCollection(Collections.unmodifiableSet(input));
   }

   public void testUnmodifiableMap() throws Exception
   {
       Map<String,String> input = new LinkedHashMap<>();
       input.put("a", "b");
       input.put("c", "d");
       _verifyMap(Collections.unmodifiableMap(input));
   }

   /*
   /**********************************************************
   /* Unit tests, other
   /**********************************************************
    */

   public void testArraysAsList() throws Exception
   {
       // Here there are no semantics to preserve, so simply check that
       // contents remain the same
       List<String> input = Arrays.asList("a", "bc", "def");
       String json = DEFAULT_MAPPER.writeValueAsString(input);
       List<?> result = DEFAULT_MAPPER.readValue(json, List.class);
       assertEquals(input, result);
   }

   /*
   /**********************************************************
   /* Helper methods
   /**********************************************************
    */

   protected void _verifyCollection(Collection<?> exp) throws Exception {
       Collection<?> act = _writeReadCollection(exp);
       assertEquals(exp, act);
       assertEquals(exp.getClass(), act.getClass());
   }

   protected Collection<?> _writeReadCollection(Collection<?> input) throws Exception {
       final String json = DEFAULT_MAPPER.writeValueAsString(input);
       return DEFAULT_MAPPER.readValue(json, Collection.class);
   }
   
   protected void _verifyMap(Map<?,?> exp) throws Exception
   {
       String json = DEFAULT_MAPPER.writeValueAsString(exp);
       Map<?,?> act = DEFAULT_MAPPER.readValue(json, Map.class);

       assertEquals(exp, act);
       assertEquals(exp.getClass(), act.getClass());
   }
}
