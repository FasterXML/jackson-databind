package com.fasterxml.jackson.databind.deser.creators;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;

/**
 * Tests to ensure one can disable {@link JsonCreator} annotations.
 */
public class DisablingCreatorsTest extends BaseMapTest
{
     static class ConflictingCreators {
          @JsonCreator(mode=JsonCreator.Mode.PROPERTIES)
          public ConflictingCreators(@JsonProperty("foo") String foo) { }
          @JsonCreator(mode=JsonCreator.Mode.PROPERTIES)
          public ConflictingCreators(@JsonProperty("foo") String foo,
                    @JsonProperty("value") int value) { }
     }

     static class NonConflictingCreators {
          public String _value;

          @JsonCreator(mode=JsonCreator.Mode.DELEGATING)
          public NonConflictingCreators(String foo) { _value = foo; }

          @JsonCreator(mode=JsonCreator.Mode.DISABLED)
          public NonConflictingCreators(String foo, int value) { }
     }

     /*
     /**********************************************************
     /* Helper methods
     /**********************************************************
      */

     public void testDisabling() throws Exception
     {
          final ObjectMapper mapper = newJsonMapper();

          // first, non-problematic case
          NonConflictingCreators value = mapper.readValue(q("abc"), NonConflictingCreators.class);
          assertNotNull(value);
          assertEquals("abc", value._value);

          // then something that ought to fail
          try {
               /*ConflictingCreators value =*/ mapper.readValue(q("abc"), ConflictingCreators.class);
               fail("Should have failed with JsonCreator conflict");
          } catch (InvalidDefinitionException e) {
               verifyException(e, "Conflicting property-based creators");
          }
     }
}
