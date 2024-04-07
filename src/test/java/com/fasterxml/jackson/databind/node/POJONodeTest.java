package com.fasterxml.jackson.databind.node;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class POJONodeTest extends NodeTestBase
{
    @JsonSerialize(using = CustomSer.class)
    public static class Data {
      public String aStr;
    }

    @SuppressWarnings("serial")
    public static class CustomSer extends StdSerializer<Data> {
      public CustomSer() {
          super(Data.class);
      }

      @Override
      public void serialize(Data value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        String attrStr = (String) provider.getAttribute("myAttr");
        gen.writeStartObject();
        gen.writeStringField("aStr", "The value is: " + (attrStr == null ? "NULL" : attrStr));
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
}
