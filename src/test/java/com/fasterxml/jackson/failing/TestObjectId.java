package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

// related to [JACKSON-847]
public class TestObjectId extends BaseMapTest
{
    @JsonPropertyOrder({"a", "b"})
    static class Wrapper {
        public ColumnMetadata a, b;
    }
    
    @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="@id")
    static class ColumnMetadata {
      private final String name;
      private final String type;
      private final String comment;

      @JsonCreator
      public ColumnMetadata(
        @JsonProperty("name") String name,
        @JsonProperty("type") String type,
        @JsonProperty("comment") String comment
      ) {
        this.name = name;
        this.type = type;
        this.comment = comment;
      }

      @JsonProperty("name")
      public String getName() {
        return name;
      }

      @JsonProperty("type")
      public String getType() {
        return type;
      }

      @JsonProperty("comment")
      public String getComment() {
        return comment;
      }    
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */
    
    private final ObjectMapper MAPPER = new ObjectMapper();
    
    public void testColumnMetadata() throws Exception
    {
        ColumnMetadata col = new ColumnMetadata("Billy", "employee", "comment");
        Wrapper w = new Wrapper();
        w.a = col;
        w.b = col;
        String json = MAPPER.writeValueAsString(w);
        
        Wrapper deserialized = MAPPER.readValue(json, Wrapper.class);
        assertNotNull(deserialized);
        assertNotNull(deserialized.a);
        assertNotNull(deserialized.b);
        
        assertEquals("Billy", deserialized.a.getName());
        assertEquals("employee", deserialized.a.getType());
        assertEquals("comment", deserialized.a.getComment());

        assertSame(deserialized.a, deserialized.b);
    }
}
