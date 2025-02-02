package com.fasterxml.jackson.databind.node;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.JsonEOFException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

public class TreeFromIncompleteJsonTest extends DatabindTestUtil
{
    final private ObjectMapper MAPPER = objectMapper(); // shared is fine

    @Test
    public void testErrorHandling() throws IOException {

      String json = "{\"A\":{\"B\":\n";
      JsonParser parser = MAPPER.createParser(json);
      try {
          parser.readValueAsTree();
      } catch (JsonEOFException e) {
          verifyException(e, "Unexpected end-of-input");
      }
      parser.close();

      try {
          MAPPER.readTree(json);
      } catch (JsonEOFException e) {
          verifyException(e, "Unexpected end-of-input");
      }

      try {
          MAPPER.reader().readTree(json);
      } catch (JsonEOFException e) {
          verifyException(e, "Unexpected end-of-input");
      }
    }
}
