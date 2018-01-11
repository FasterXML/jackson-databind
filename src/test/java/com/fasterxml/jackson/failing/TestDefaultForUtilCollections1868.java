package com.fasterxml.jackson.failing;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

// Unit tests for [databind#1868], related
public class TestDefaultForUtilCollections1868 extends BaseMapTest
{
   private final ObjectMapper DEFAULT_MAPPER = new ObjectMapper();
   {
       DEFAULT_MAPPER.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
   }

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

   protected void _verifyCollection(Collection<?> exp) throws Exception
   {
       String json = DEFAULT_MAPPER.writeValueAsString(exp);
       Collection<?> act = DEFAULT_MAPPER.readValue(json, Collection.class);
       
       assertEquals(exp, act);
       assertEquals(exp.getClass(), act.getClass());
   }

   protected void _verifyMap(Map<?,?> exp) throws Exception
   {
       String json = DEFAULT_MAPPER.writeValueAsString(exp);
       Map<?,?> act = DEFAULT_MAPPER.readValue(json, Map.class);

       assertEquals(exp, act);
       assertEquals(exp.getClass(), act.getClass());
   }
}
