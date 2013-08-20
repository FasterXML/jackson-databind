package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

/**
 * Reproduction of [https://github.com/FasterXML/jackson-databind/issues/283],
 * contributed by Eric T.
 *<p>
 * Problem here is that although explicit concrete class is indicated, polymorphic
 * deserializer comes to different conclusion (using default implementation class),
 * resulting in a <code>ClassCastException</code>.
 * Whether this is wrong, and if so, can we fix it, is unknown at this point
 * (2.3.0-SNAPSHOT): quite possibly this can not be changed.
 */
public class TestPolymorphicDeserialization extends BaseMapTest
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = ClassA.class)
    @JsonSubTypes({
            @JsonSubTypes.Type(name = "a", value = ClassA.class),
            @JsonSubTypes.Type(name = "b", value = ClassB.class)
    })
    public static interface SomeInterface
    {
      public String get();
    }

    public static class ClassA implements SomeInterface
    {
      @Override
      public String get()
      {
        return "A";
      }
    }

    public static class ClassB implements SomeInterface
    {
      @Override
      public String get()
      {
        return "B";
      }
    }

    public static class ClassC implements SomeInterface
    {
      @Override
      public String get()
      {
        return "C";
      }
    }

    @Test
    public void testName() throws Exception
    {
      ObjectMapper mapper = objectMapper();

      Assert.assertEquals("A", mapper.readValue("{\"type\": \"a\"}", SomeInterface.class).get());
      Assert.assertEquals("A", mapper.readValue("{}", SomeInterface.class).get());
      Assert.assertEquals("B", mapper.readValue("{\"type\": \"b\"}", SomeInterface.class).get());
      Assert.assertEquals("A", mapper.readValue("{\"type\": \"c\"}", SomeInterface.class).get());

      Assert.assertEquals("A", mapper.readValue("{\"type\": \"a\"}", ClassA.class).get());
      Assert.assertEquals("B", mapper.readValue("{\"type\": \"b\"}", ClassB.class).get());
      Assert.assertEquals("C", mapper.readValue("{\"type\": \"c\"}", ClassC.class).get());
    }
}
