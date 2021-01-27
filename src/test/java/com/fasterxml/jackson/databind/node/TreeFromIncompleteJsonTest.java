package com.fasterxml.jackson.databind.node;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.UnexpectedEndOfInputException;
import com.fasterxml.jackson.databind.*;

public class TreeFromIncompleteJsonTest extends BaseMapTest
{
    final private ObjectMapper MAPPER = objectMapper(); // shared is fine

    public void testErrorHandling() throws IOException {

      String json = "{\"A\":{\"B\":\n";
      JsonParser parser = MAPPER.createParser(json);
      try {
          parser.readValueAsTree();
      } catch (UnexpectedEndOfInputException e) {
          verifyException(e, "Unexpected end-of-input");
      }
      parser.close();

      try {
          MAPPER.readTree(json);
      } catch (UnexpectedEndOfInputException e) {
          verifyException(e, "Unexpected end-of-input");
      }

      try {
          MAPPER.reader().readTree(json);
      } catch (UnexpectedEndOfInputException e) {
          verifyException(e, "Unexpected end-of-input");
      }
    }
}
