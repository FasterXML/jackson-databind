package tools.jackson.databind.jsontype;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DatabindContext;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.annotation.JsonTypeIdResolver;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.impl.TypeIdResolverBase;
import tools.jackson.databind.testutil.DatabindTestUtil;

public class CustomTypeIdResolverGenericTest extends DatabindTestUtil {

  private final JsonMapper mapper = JsonMapper.builder()
      .enable(SerializationFeature.INDENT_OUTPUT)
      .build();

  @Test
  public void testRoot() throws Exception {
    // given
    Bar bar = new Bar("test");
    String expected = "{\n" +
        "  \"@type\" : \"BAR\",\n" +
        "  \"any\" : \"test\"\n" +
        "}";

    // when
    String actual = mapper.writeValueAsString(bar);

    // then
    assertEquals(expected, actual);
  }

  @Test
  public void testNestedMap() throws Exception {
    // given
    Map<String, Foo> map = new HashMap<>();
    map.put("bar", new Bar("test"));
    String expected = "{\n" +
        "  \"bar\" : {\n" +
        "    \"@type\" : \"BAR\",\n" +
        "    \"any\" : \"test\"\n" +
        "  }\n" +
        "}";

    // when
    String actual = mapper.writeValueAsString(map);

    // then
    assertEquals(expected, actual);
  }

  @Test
  public void testNestedPojo() throws Exception {
    // given
    Box box = new Box(new Qux(1));
    String expected = "{\n" +
        "  \"value\" : {\n" +
        "    \"@type\" : \"QUX\",\n" +
        "    \"any\" : 1\n" +
        "  }\n" +
        "}";

    // when
    String actual = mapper.writeValueAsString(box);

    // then
    assertEquals(expected, actual);
  }

  @Test
  public void testNestedGenericPojo() throws Exception {
    // given
    GenericBox<Foo> box = new GenericBox<>(new Qux(1));
    String expected = "{\n" +
        "  \"value\" : {\n" +
        "    \"@type\" : \"QUX\",\n" +
        "    \"any\" : 1\n" +
        "  }\n" +
        "}";

    // when
    String actual = mapper.writeValueAsString(box);

    // then
    assertEquals(expected, actual);
  }

  @Test
  public void testNestedGenericPojoExplicitWriter() throws Exception {
    // given
    GenericBox<Foo> box = new GenericBox<>(new Qux(1));
    String expected = "{\n" +
        "  \"value\" : {\n" +
        "    \"@type\" : \"QUX\",\n" +
        "    \"any\" : 1\n" +
        "  }\n" +
        "}";

    // when
    String actual = mapper.writer().forType(new TypeReference<GenericBox<Foo>>() {
    }).writeValueAsString(box);

    // then
    assertEquals(expected, actual);
  }

  // Test classes

  @JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "@type")
  @JsonTypeIdResolver(FooIdResolver.class)
  public interface Foo {
  }

  public static class Bar implements Foo {
    private String any;

    public Bar() {
    }

    public Bar(String any) {
      this.any = any;
    }

    public String getAny() {
      return any;
    }

    public void setAny(String any) {
      this.any = any;
    }
  }

  public static class Qux implements Foo {
    private int any;

    public Qux() {
    }

    public Qux(int any) {
      this.any = any;
    }

    public int getAny() {
      return any;
    }

    public void setAny(int any) {
      this.any = any;
    }
  }

  public static class Box {
    private Foo value;

    public Box() {
    }

    public Box(Foo value) {
      this.value = value;
    }

    public Foo getValue() {
      return value;
    }

    public void setValue(Foo value) {
      this.value = value;
    }
  }

  public static class GenericBox<T> {
    private T value;

    public GenericBox() {
    }

    public GenericBox(T value) {
      this.value = value;
    }

    public T getValue() {
      return value;
    }

    public void setValue(T value) {
      this.value = value;
    }
  }

  public static class FooIdResolver extends TypeIdResolverBase {

    @Override
    public String idFromValue(DatabindContext ctxt, Object value) {
      return idFromValueAndType(ctxt, value, value.getClass());
    }

    @Override
    public String idFromValueAndType(DatabindContext ctxt, Object value, Class<?> suggestedType) {
      if (value instanceof Bar) {
        return "BAR";
      } else if (value instanceof Qux) {
        return "QUX";
      }
      throw new IllegalStateException("Unexpected value: " + value);
    }

    @Override
    public Id getMechanism() {
      return Id.CUSTOM;
    }
  }
}
