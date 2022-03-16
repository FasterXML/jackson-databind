package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;

public class DeepNestingUntypedDeserTest extends BaseMapTest
{
  // 28-Mar-2021, tatu: Currently 3000 fails for untyped/Object,
  //     4000 for untyped/Array
  private final static int TOO_DEEP_NESTING = 4000;

  private final ObjectMapper MAPPER = newJsonMapper();

  public void testUntypedWithArray() throws Exception
  {
    final String doc = _nestedDoc(TOO_DEEP_NESTING, "[ ", "] ");
    try {
      Object ob = MAPPER.readValue(doc, Object.class);
      assertTrue(ob instanceof List<?>);
    } catch (JsonMappingException jme) {
      assertEquals("JSON is too deeply nested.", jme.getMessage());
    }
  }

  public void testUntypedWithObject() throws Exception
  {
    final String doc = "{"+_nestedDoc(TOO_DEEP_NESTING, "\"x\":{", "} ") + "}";
    try {
      Object ob = MAPPER.readValue(doc, Object.class);
      assertTrue(ob instanceof Map<?, ?>);
    } catch (JsonMappingException jme) {
      assertEquals("JSON is too deeply nested.", jme.getMessage());
    }
  }

  private String _nestedDoc(int nesting, String open, String close) {
    StringBuilder sb = new StringBuilder(nesting * (open.length() + close.length()));
    for (int i = 0; i < nesting; ++i) {
      sb.append(open);
      if ((i & 31) == 0) {
        sb.append("\n");
      }
    }
    for (int i = 0; i < nesting; ++i) {
      sb.append(close);
      if ((i & 31) == 0) {
        sb.append("\n");
      }
    }
    return sb.toString();
  }
}
