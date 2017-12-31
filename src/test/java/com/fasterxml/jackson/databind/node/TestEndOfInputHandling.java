package com.fasterxml.jackson.databind.node;

import java.io.IOException;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;

public class TestEndOfInputHandling extends BaseMapTest
{
  public void testErrorHandling() throws IOException {
      ObjectMapper mapper = new ObjectMapper();

      String json = "{\"A\":{\"B\":\n";
      try {
          mapper.readTree(json);
      }
      catch(JsonParseException e) {
          verifyException(e, "Unexpected end-of-input");
      }
  }
}
