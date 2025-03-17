package tools.jackson.databind.jsontype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidTypeIdException;
import tools.jackson.databind.jsontype.impl.SimpleNameIdResolver;
import tools.jackson.databind.testutil.DatabindTestUtil;

/**
 * Test for <a href="https://github.com/FasterXML/jackson-databind/issues/4061"> [databind#4061] Add
 * JsonTypeInfo.Id.SIMPLE_NAME
 */
public class SealedTypesWithJsonTypeInfoSimpleClassName4061Test extends DatabindTestUtil {
  @JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME)
  static sealed class InnerSuper4061 permits InnerSub4061A, InnerSub4061B {
  }

  static final class InnerSub4061A extends InnerSuper4061 {
  }

  static final class InnerSub4061B extends InnerSuper4061 {
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
  static sealed class MinimalInnerSuper4061 permits MinimalInnerSub4061A, MinimalInnerSub4061B {
  }

  static final class MinimalInnerSub4061A extends MinimalInnerSuper4061 {
  }

  static final class MinimalInnerSub4061B extends MinimalInnerSuper4061 {
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME)
  static sealed class MixedSuper4061
      permits MixedSub4061AForSealedClasses, MixedSub4061BForSealedClasses {
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
  static sealed class MixedMinimalSuper4061
      permits MixedMinimalSub4061AForSealedClasses, MixedMinimalSub4061BForSealedClasses {
  }

  static class Root {
    @JsonMerge
    public MergeChild child;
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME)
  static abstract sealed class MergeChild permits MergeChildA, MergeChildB {
  }

  static final class MergeChildA extends MergeChild {
    public String name;
  }

  static final class MergeChildB extends MergeChild {
    public String code;
  }

  static class PolyWrapperForAlias {
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_ARRAY)
    @JsonSubTypes({@JsonSubTypes.Type(value = AliasBean.class, name = "ab")})
    public Object value;

    protected PolyWrapperForAlias() {}

    public PolyWrapperForAlias(Object v) {
      value = v;
    }
  }

  static class AliasBean {
    @JsonAlias({"nm", "Name"})
    public String name;
    int _xyz;
    int _a;

    @JsonCreator
    public AliasBean(@JsonProperty("a") @JsonAlias("A") int a) {
      _a = a;
    }

    @JsonAlias({"Xyz"})
    public void setXyz(int x) {
      _xyz = x;
    }
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME)
  static sealed class DuplicateSuperClass permits DuplicateSubClassForSealedClasses,
      tools.jackson.databind.jsontype.DuplicateSubClassForSealedClasses {
  }

  static final class DuplicateSubClassForSealedClasses extends DuplicateSuperClass {
  }

  /*
   * /********************************************************** /* Unit tests
   * /**********************************************************
   */

  private final ObjectMapper MAPPER = newJsonMapper();

  // inner class that has contains dollar sign
  @Test
  public void testInnerClass() throws Exception {
    String jsonStr = a2q("{'@type':'InnerSub4061A'}");

    // ser
    assertEquals(jsonStr, MAPPER.writeValueAsString(new InnerSub4061A()));

    // deser <- breaks!
    InnerSuper4061 bean = MAPPER.readValue(jsonStr, InnerSuper4061.class);
    assertInstanceOf(InnerSuper4061.class, bean);
  }

  // inner class that has contains dollar sign
  @Test
  public void testMinimalInnerClass() throws Exception {
    String jsonStr =
        a2q("{'@c':'.SealedTypesWithJsonTypeInfoSimpleClassName4061Test$MinimalInnerSub4061A'}");

    // ser
    assertEquals(jsonStr, MAPPER.writeValueAsString(new MinimalInnerSub4061A()));

    // deser <- breaks!
    MinimalInnerSuper4061 bean = MAPPER.readValue(jsonStr, MinimalInnerSuper4061.class);
    assertInstanceOf(MinimalInnerSuper4061.class, bean);
    assertNotNull(bean);
  }

  // Basic : non-inner class, without dollar sign
  @Test
  public void testBasicClass() throws Exception {
    String jsonStr = a2q("{'@type':'BasicSub4061A'}");

    // ser
    assertEquals(jsonStr, MAPPER.writeValueAsString(new BasicSub4061A()));

    // deser
    BasicSuper4061 bean = MAPPER.readValue(jsonStr, BasicSuper4061.class);
    assertInstanceOf(BasicSuper4061.class, bean);
    assertInstanceOf(BasicSub4061A.class, bean);

  }

  // Mixed SimpleClassName : parent as inner, subtype as basic
  @Test
  public void testMixedClass() throws Exception {
    String jsonStr = a2q("{'@type':'MixedSub4061AForSealedClasses'}");

    // ser
    assertEquals(jsonStr, MAPPER.writeValueAsString(new MixedSub4061AForSealedClasses()));

    // deser
    MixedSuper4061 bean = MAPPER.readValue(jsonStr, MixedSuper4061.class);
    assertInstanceOf(MixedSuper4061.class, bean);
    assertInstanceOf(MixedSub4061AForSealedClasses.class, bean);
  }

  // Mixed MinimalClass : parent as inner, subtype as basic
  @Test
  public void testMixedMinimalClass() throws Exception {
    String jsonStr = a2q("{'@c':'.MixedMinimalSub4061AForSealedClasses'}");

    // ser
    assertEquals(jsonStr, MAPPER.writeValueAsString(new MixedMinimalSub4061AForSealedClasses()));

    // deser
    MixedMinimalSuper4061 bean = MAPPER.readValue(jsonStr, MixedMinimalSuper4061.class);
    assertInstanceOf(MixedMinimalSuper4061.class, bean);
    assertInstanceOf(MixedMinimalSub4061AForSealedClasses.class, bean);
  }

  @Test
  public void testPolymorphicNewObject() throws Exception {
    String jsonStr = "{\"child\": { \"@type\": \"MergeChildA\", \"name\": \"I'm child A\" }}";

    Root root = MAPPER.readValue(jsonStr, Root.class);

    assertTrue(root.child instanceof MergeChildA);
    assertEquals("I'm child A", ((MergeChildA) root.child).name);
  }

  // case insenstive type name
  @Test
  public void testPolymorphicNewObjectCaseInsensitive() throws Exception {
    String jsonStr = "{\"child\": { \"@type\": \"mergechilda\", \"name\": \"I'm child A\" }}";
    ObjectMapper mapper =
        jsonMapperBuilder().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES).build();

    Root root = mapper.readValue(jsonStr, Root.class);

    assertTrue(root.child instanceof MergeChildA);
    assertEquals("I'm child A", ((MergeChildA) root.child).name);
  }

  @Test
  public void testPolymorphicNewObjectUnknownTypeId() throws Exception {
    try {
      MAPPER.readValue("{\"child\": { \"@type\": \"UnknownChildA\", \"name\": \"I'm child A\" }}",
          Root.class);
    } catch (InvalidTypeIdException e) {
      verifyException(e, "Could not resolve type id 'UnknownChildA' as a subtype of");
    }
  }

  @Test
  public void testAliasWithPolymorphic() throws Exception {
    String jsonStr = a2q("{'value': ['ab', {'nm' : 'Bob', 'A' : 17} ] }");

    PolyWrapperForAlias value = MAPPER.readValue(jsonStr, PolyWrapperForAlias.class);

    assertNotNull(value.value);
    AliasBean bean = (AliasBean) value.value;
    assertEquals("Bob", bean.name);
    assertEquals(17, bean._a);
  }

  @Test
  public void testGetMechanism() {
    final DeserializationConfig config = MAPPER.deserializationConfig();
    JavaType javaType = config.constructType(InnerSub4061B.class);
    List<NamedType> namedTypes = new ArrayList<>();
    namedTypes.add(new NamedType(InnerSub4061A.class));
    namedTypes.add(new NamedType(InnerSub4061B.class));

    SimpleNameIdResolver idResolver =
        SimpleNameIdResolver.construct(config, javaType, namedTypes, false, true);

    assertEquals(JsonTypeInfo.Id.SIMPLE_NAME, idResolver.getMechanism());
  }

  @Test
  public void testDuplicateNameLastOneWins() throws Exception {
    String jsonStr = a2q("{'@type':'DuplicateSubClassForSealedClasses'}");

    // deser
    DuplicateSuperClass bean = MAPPER.readValue(jsonStr, DuplicateSuperClass.class);
    assertInstanceOf(tools.jackson.databind.jsontype.DuplicateSubClassForSealedClasses.class, bean);
  }
}


@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME)
sealed class BasicSuper4061ForSealedTypes
    permits BasicSub4061AForSealedTypes, BasicSub4061BForSealedTypes {
}


final class BasicSub4061AForSealedTypes extends BasicSuper4061ForSealedTypes {
}


final class BasicSub4061BForSealedTypes extends BasicSuper4061ForSealedTypes {
}


final class MixedSub4061AForSealedClasses
    extends SealedTypesWithJsonTypeInfoSimpleClassName4061Test.MixedSuper4061 {
}


final class MixedSub4061BForSealedClasses
    extends SealedTypesWithJsonTypeInfoSimpleClassName4061Test.MixedSuper4061 {
}


final class MixedMinimalSub4061AForSealedClasses
    extends SealedTypesWithJsonTypeInfoSimpleClassName4061Test.MixedMinimalSuper4061 {
}


final class MixedMinimalSub4061BForSealedClasses
    extends SealedTypesWithJsonTypeInfoSimpleClassName4061Test.MixedMinimalSuper4061 {
}


final class DuplicateSubClassForSealedClasses
    extends SealedTypesWithJsonTypeInfoSimpleClassName4061Test.DuplicateSuperClass {
}
