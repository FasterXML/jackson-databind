package com.fasterxml.jackson.databind.deser.filter;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class IgnoreUnknownPropertyUsingPropertyBasedTest extends BaseMapTest {

  private final ObjectMapper MAPPER = newJsonMapper();

  @JsonIgnoreProperties(ignoreUnknown = true)
  static class IgnoreUnknownAnySetter {

    int a, b;

    @JsonCreator
    public IgnoreUnknownAnySetter(@JsonProperty("a") int a, @JsonProperty("b") int b) {
      this.a = a;
      this.b = b;
    }

    Map<String, Object> props = new HashMap<>();

    @JsonAnySetter
    public void addProperty(String key, Object value) {
      props.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getProperties() {
      return props;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  static class IgnoreUnknownUnwrapped {

    int a, b;

    @JsonCreator
    public IgnoreUnknownUnwrapped(@JsonProperty("a") int a, @JsonProperty("b") int b) {
      this.a = a;
      this.b = b;
    }

    @JsonUnwrapped
    UnwrappedChild child;

    static class UnwrappedChild {
      public int x, y;
    }
  }

  public void testAnySetterWithFailOnUnknownDisabled() throws Exception {
    IgnoreUnknownAnySetter value = MAPPER.readValue("{\"a\":1, \"b\":2, \"x\":3, \"y\": 4}", IgnoreUnknownAnySetter.class);
    assertNotNull(value);
    assertEquals(1, value.a);
    assertEquals(2, value.b);
    assertEquals(3, value.props.get("x"));
    assertEquals(4, value.props.get("y"));
    assertEquals(2, value.props.size());
  }

  public void testUnwrappedWithFailOnUnknownDisabled() throws Exception {
    IgnoreUnknownUnwrapped value = MAPPER.readValue("{\"a\":1, \"b\": 2, \"x\":3, \"y\":4}", IgnoreUnknownUnwrapped.class);
    assertNotNull(value);
    assertEquals(1, value.a);
    assertEquals(2, value.b);
    assertEquals(3, value.child.x);
    assertEquals(4, value.child.y);
  }

}
