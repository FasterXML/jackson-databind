package tools.jackson.databind.jsontype;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
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
  void root() throws Exception {
    //given
    var bar = new Bar("test");
    var expected = """
        {
          "@type" : "BAR",
          "any" : "test"
        }""";

    //when
    var actual = mapper.writeValueAsString(bar);

    //then
    assertEquals(expected, actual);
  }

  @Test
  void nestedMap() throws Exception {
    //given
    var map = Map.of("bar", new Bar("test"));
    var expected = """
        {
          "bar" : {
            "@type" : "BAR",
            "any" : "test"
          }
        }""";

    //when
    var actual = mapper.writeValueAsString(map);

    //then
    assertEquals(expected, actual);
  }

  @Test
  void nestedRecord() throws Exception {
    //given
    record Box(Foo value) {

    }
    var box = new Box(new Qux(1));
    var expected = """
        {
          "value" : {
            "@type" : "QUX",
            "any" : 1
          }
        }""";

    //when
    var actual = mapper.writeValueAsString(box);

    //then
    assertEquals(expected, actual);
  }

  @Test
  void nestedGenericRecord() throws Exception {
    //given
    record Box<T>(T value) {

    }
    var box = new Box<>(new Qux(1));
    var expected = """
        {
          "value" : {
            "@type" : "QUX",
            "any" : 1
          }
        }""";

    //when
    var actual = mapper.writeValueAsString(box);

    //then
    assertEquals(expected, actual);
  }

  @Test
  void nestedGenericRecordExplicitWriter() throws Exception {
    //given
    record Box<T>(T value) {

    }
    var box = new Box<>(new Qux(1));
    var expected = """
        {
          "value" : {
            "@type" : "QUX",
            "any" : 1
          }
        }""";

    var writer = mapper.writer().forType(new TypeReference<Box<Foo>>() {
    });

    //when
    var actual = writer.writeValueAsString(box);

    //then
    assertEquals(expected, actual);
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "@type")
  @JsonTypeIdResolver(FooIdResolver.class)
  public sealed interface Foo permits Bar, Qux {

  }

  record Bar(String any) implements Foo {

  }

  record Qux(int any) implements Foo {

  }

  public static class FooIdResolver extends TypeIdResolverBase {

    @Override
    public String idFromValue(DatabindContext ctxt, Object value) {
      return idFromValueAndType(ctxt, value, value.getClass());
    }

    @Override
    public String idFromValueAndType(DatabindContext ctxt, Object value, Class<?> suggestedType) {
      return switch (value) {
        case Bar _ -> "BAR";
        case Qux _ -> "QUX";
        default -> throw new IllegalStateException("Unexpected value: " + value);
      };
    }

    @Override
    public Id getMechanism() {
      return Id.CUSTOM;
    }
  }
}
