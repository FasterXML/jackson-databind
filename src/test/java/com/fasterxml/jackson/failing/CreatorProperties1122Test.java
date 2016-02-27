package com.fasterxml.jackson.failing;

import java.beans.ConstructorProperties;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.*;

public class CreatorProperties1122Test extends BaseMapTest
{
//    @JsonIgnoreProperties(ignoreUnknown = true)
  public class Ambiguity {

      @JsonProperty("bar")
      private int foo;

      protected Ambiguity() {}

      @ConstructorProperties({ "foo" })
      public Ambiguity(int foo) {
          this.foo = foo;
      }

      public int getFoo() {
          return foo;
      }

      @Override
      public String toString() {
          return "Ambiguity [foo=" + foo + "]";
      }

  }    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testIssue1122() throws Exception
    {
        String json = "{\"bar\":3}";
        Ambiguity amb = MAPPER.readValue(json, Ambiguity.class);
        assertNotNull(amb);
        assertEquals(3, amb.getFoo());
    }
}
