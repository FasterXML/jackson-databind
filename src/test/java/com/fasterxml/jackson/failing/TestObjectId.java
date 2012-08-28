package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

// related to [JACKSON-847]
public class TestObjectId extends BaseMapTest
{
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
    
    public void testColumnMetadata() throws Exception {
        ColumnMetadata columnMetadata = new ColumnMetadata("Billy", "employee", "comment");
        String serialized = MAPPER.writeValueAsString(columnMetadata);
        System.out.println(serialized);
        ColumnMetadata deserialized = MAPPER.readValue(serialized, ColumnMetadata.class);
        assertNotNull(deserialized);
        assertEquals("Billy", deserialized.getName());
        assertEquals("employee", deserialized.getType());
        assertEquals("comment", deserialized.getComment());
        
    }
}
