package tools.jackson.databind.node;

import java.time.LocalDateTime;
import java.util.*;

import tools.jackson.core.JsonGenerator;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.StdSerializer;

public class POJONodeTest extends NodeTestBase
{
    @JsonSerialize(using = CustomSer.class)
    public static class Data {
        public String aStr;
    }

    public static class CustomSer extends StdSerializer<Data> {
        public CustomSer() {
            super(Data.class);
        }

        @Override
        public void serialize(Data value, JsonGenerator gen, SerializerProvider provider)
        {
            String attrStr = (String) provider.getAttribute("myAttr");
            gen.writeStartObject();
            gen.writeStringProperty("aStr", "The value is: " + (attrStr == null ? "NULL" : attrStr));
            gen.writeEndObject();
        }
    }

    final ObjectMapper MAPPER = newJsonMapper();

    public void testPOJONodeCustomSer() throws Exception
    {
      Data data = new Data();
      data.aStr = "Hello";

      Map<String, Object> mapTest = new HashMap<>();
      mapTest.put("data", data);

      ObjectNode treeTest = MAPPER.createObjectNode();
      treeTest.putPOJO("data", data);

      final String EXP = "{\"data\":{\"aStr\":\"The value is: Hello!\"}}";

      String mapOut = MAPPER.writer().withAttribute("myAttr", "Hello!").writeValueAsString(mapTest);
      assertEquals(EXP, mapOut);

      String treeOut = MAPPER.writer().withAttribute("myAttr", "Hello!").writeValueAsString(treeTest);
      assertEquals(EXP, treeOut);
    }

    // [databind#3262]: The issue is that
    // `JsonNode.toString()` will use internal "default" ObjectMapper which
    // does not (and cannot) have modules for external datatypes, such as
    // Java 8 Date/Time types. So we'll catch IOException/RuntimeException for
    // POJONode, produce something like "[ERROR: (type) [msg]" TextNode for that case?
    public void testAddJava8DateAsPojo() throws Exception
    {
        JsonNode node = MAPPER.createObjectNode().putPOJO("test", LocalDateTime.now());
        String json = node.toString();
        assertNotNull(json);

        JsonNode result = MAPPER.readTree(json);
        String msg = result.path("test").asText();
        assertTrue("Wrong fail message: "+msg,
                msg.startsWith("[ERROR:"));
        assertTrue("Wrong fail message: "+msg,
                msg.contains("InvalidDefinitionException"));
    }
}
