import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestEndOfInputObjectMapper1260 {
  @Test
  public void testErrorHandling() throws IOException {
    JsonParser parser = new JsonFactory().createParser("{\"A\":{\"B\":\n\n.");
    parser.setCodec(new ObjectMapper());
    try {
      parser.readValueAsTree();
    }
    catch(JsonParseException e) {
      // Intentional: Unexpected character ('.' (code 46)):
    }
    try {
      parser.readValueAsTree();
    }
    catch(IOException e) {
      // An IO error would be expected here, but 2.7.4 had a NPE here.
    }
  }
}
