package com.fasterxml.jackson.databind.jsontype;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import static com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.DEDUCTION;

// for [databind#43], deduction-based polymorphism
public class TestPolymorphicDeduction extends BaseMapTest {

  @JsonTypeInfo(use = DEDUCTION)
  @JsonSubTypes( {@Type(LiveCat.class), @Type(DeadCat.class), @Type(Fleabag.class)}) // NOT including Cat
  // A general supertype with no properties - used for tests involving {}
  interface Feline {}

  @JsonTypeInfo(use = DEDUCTION)
  @JsonSubTypes( {@Type(LiveCat.class), @Type(DeadCat.class)})
  // A supertype containing common properties
  public static class Cat implements Feline {
    public String name;
  }

  // Distinguished by its parent and a unique property
  static class DeadCat extends Cat {
    public String causeOfDeath;
  }

  // Distinguished by its parent and a unique property
  static class LiveCat extends Cat {
    public boolean angry;
  }

  // No distinguishing properties whatsoever
  static class Fleabag implements Feline {
    // NO OP
  }

  // Something to put felines in
  static class Box {
    public Feline feline;
  }

  /*
  /**********************************************************
  /* Mock data
  /**********************************************************
   */

  private static final String catJson = a2q("{'name':'Felix'}");
  private static final String deadCatJson = a2q("{'name':'Felix','causeOfDeath':'entropy'}");
  private static final String liveCatJson = a2q("{'name':'Felix','angry':true}");
  private static final String luckyCatJson = a2q("{'name':'Felix','angry':true,'lives':8}");
  private static final String unknownCatJson = a2q("{'name':'Felix','age':2}");
  private static final String fleabagJson = a2q("{}");
  private static final String box1Json = a2q("{'feline':" + liveCatJson + "}");
  private static final String box2Json = a2q("{'feline':" + deadCatJson + "}");
  private static final String box3Json = a2q("{'feline':" + fleabagJson + "}");
  private static final String box4Json = a2q("{'feline':null}");
  private static final String box5Json = a2q("{}");
  private static final String arrayOfCatsJson = a2q("[" + liveCatJson + "," + deadCatJson + "]");
  private static final String mapOfCatsJson = a2q("{'live':" + liveCatJson + "}");

  /*
  /**********************************************************
  /* Test methods
  /**********************************************************
   */

  private final ObjectMapper MAPPER = newJsonMapper();

  public void testSimpleInference() throws Exception {
    Cat cat = MAPPER.readValue(liveCatJson, Cat.class);
    assertTrue(cat instanceof LiveCat);
    assertSame(cat.getClass(), LiveCat.class);
    assertEquals("Felix", cat.name);
    assertTrue(((LiveCat)cat).angry);

    cat = MAPPER.readValue(deadCatJson, Cat.class);
    assertTrue(cat instanceof DeadCat);
    assertSame(cat.getClass(), DeadCat.class);
    assertEquals("Felix", cat.name);
    assertEquals("entropy", ((DeadCat)cat).causeOfDeath);
  }

  public void testSimpleInferenceOfEmptySubtype() throws Exception {
    // Given:
    ObjectMapper mapper = MAPPER;
    // When:
    Feline feline = mapper.readValue(fleabagJson, Feline.class);
    // Then:
    assertTrue(feline instanceof Fleabag);
  }

  public void testSimpleInferenceOfEmptySubtypeDoesntMatchNull() throws Exception {
    // Given:
    ObjectMapper mapper = MAPPER;
    // When:
    Feline feline = mapper.readValue("null", Feline.class);
    // Then:
    assertNull(feline);
  }

  public void testCaseInsensitiveInference() throws Exception {
    Cat cat = JsonMapper.builder() // Don't use shared mapper!
      .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
      .build()
      .readValue(deadCatJson.toUpperCase(), Cat.class);
    assertTrue(cat instanceof DeadCat);
    assertSame(cat.getClass(), DeadCat.class);
    assertEquals("FELIX", cat.name);
    assertEquals("ENTROPY", ((DeadCat)cat).causeOfDeath);
  }

  // TODO not currently supported
//  public void testCaseInsensitivePerFieldInference() throws Exception {
//    ObjectMapper mapper = JsonMapper.builder() // Don't use shared mapper!
//       .configOverride(DeadCat.class)
//      .setFormat(JsonFormat.Value.empty()
//      .withFeature(JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES));
//    Cat cat = mapper.readValue(deadCatJson.replace("causeOfDeath", "CAUSEOFDEATH"), Cat.class);
//    assertTrue(cat instanceof DeadCat);
//    assertSame(cat.getClass(), DeadCat.class);
//    assertEquals("Felix", cat.name);
//    assertEquals("Entropy", ((DeadCat)cat).causeOfDeath);
//  }

  public void testContainedInference() throws Exception {
    Box box = MAPPER.readValue(box1Json, Box.class);
    assertTrue(box.feline instanceof LiveCat);
    assertSame(box.feline.getClass(), LiveCat.class);
    assertEquals("Felix", ((LiveCat)box.feline).name);
    assertTrue(((LiveCat)box.feline).angry);

    box = MAPPER.readValue(box2Json, Box.class);
    assertTrue(box.feline instanceof DeadCat);
    assertSame(box.feline.getClass(), DeadCat.class);
    assertEquals("Felix", ((DeadCat)box.feline).name);
    assertEquals("entropy", ((DeadCat)box.feline).causeOfDeath);
  }

  public void testContainedInferenceOfEmptySubtype() throws Exception {
    Box box = MAPPER.readValue(box3Json, Box.class);
    assertTrue(box.feline instanceof Fleabag);

    box = MAPPER.readValue(box4Json, Box.class);
    assertNull("null != {}", box.feline);

    box = MAPPER.readValue(box5Json, Box.class);
    assertNull("<absent> != {}", box.feline);
  }

  public void testListInference() throws Exception {
    JavaType listOfCats = TypeFactory.defaultInstance().constructParametricType(List.class, Cat.class);
    List<Cat> boxes = MAPPER.readValue(arrayOfCatsJson, listOfCats);
    assertTrue(boxes.get(0) instanceof LiveCat);
    assertTrue(boxes.get(1) instanceof DeadCat);
  }

  public void testMapInference() throws Exception {
    JavaType mapOfCats = TypeFactory.defaultInstance().constructParametricType(Map.class, String.class, Cat.class);
    Map<String, Cat> map = MAPPER.readValue(mapOfCatsJson, mapOfCats);
    assertEquals(1, map.size());
    assertTrue(map.entrySet().iterator().next().getValue() instanceof LiveCat);
  }

  public void testArrayInference() throws Exception {
    Cat[] boxes = MAPPER.readValue(arrayOfCatsJson, Cat[].class);
    assertTrue(boxes[0] instanceof LiveCat);
    assertTrue(boxes[1] instanceof DeadCat);
  }

  public void testIgnoreProperties() throws Exception {
    // Given:
    ObjectReader reader = MAPPER.reader()
      .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    // When:
    Cat cat = reader.readValue(luckyCatJson, Cat.class);
    // Then:
    assertTrue("Should ignore 'lives' property", cat instanceof LiveCat);
    assertSame(cat.getClass(), LiveCat.class);
    assertEquals("Felix", cat.name);
    assertTrue(((LiveCat)cat).angry);
  }

  public void testAmbiguousPropertiesMatchSupertype() throws Exception {
    // Given:
    JsonMapper mapper = JsonMapper.builder() // Don't use shared mapper!
      .build();
    // When:
    Feline feline = mapper.readValue(catJson, Cat.class);
    // Then:
    assertTrue("Cat is included in subtypes and is an exact match", feline instanceof Cat);
    assertSame(feline.getClass(), Cat.class);
    assertEquals("Felix", ((Cat)feline).name);
  }

  public void testIgnorePropertiesCanMatchSupertype() throws Exception {
    // Given:
    ObjectReader reader = MAPPER.reader()
      .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    // When:
    Cat cat = reader.readValue(unknownCatJson, Cat.class);
    // Then:
    assertTrue("Should ignore 'age' property, leaving 'name' to match Cat", cat instanceof Cat);
    assertSame(cat.getClass(), Cat.class);
    assertEquals("Felix", cat.name);
  }

  static class AnotherLiveCat extends Cat {
    public boolean angry;
  }

  public void testAmbiguousClasses() throws Exception {
    try {
      // Given:
      ObjectMapper mapper = JsonMapper.builder() // Don't use shared mapper!
        .registerSubtypes(AnotherLiveCat.class)
        .build();
      // When:
      mapper.readValue(liveCatJson, Cat.class);

      fail("AnotherLiveCat and LiveCat have same signature and should fail to register");
    } catch (InvalidDefinitionException e) {
      // Then:
      verifyException(e, "Subtypes ");
      verifyException(e, "have the same signature");
      verifyException(e, "cannot be uniquely deduced");
    }
  }

  public void testAmbiguousProperties() throws Exception {
    try {
      // When:
      MAPPER.readValue(catJson, Feline.class);

      fail("Feline doesn't include the Cat subtype");
    } catch (InvalidTypeIdException e) {
      // Then:
      verifyException(e, "Cannot deduce unique subtype");
    }
  }

  public void testFailOnInvalidSubtype() throws Exception {
    // When:
    ObjectReader reader = MAPPER.reader()
      .without(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
    // Given:
    Feline cat = reader.readValue(unknownCatJson, Feline.class);
    // Then:
    assertNull("Should return null not an Exception", cat);
  }

  @JsonTypeInfo(use = DEDUCTION, defaultImpl = AnotherLiveCat.class)
  abstract static class FelineMixin {
  }

  public void testDefaultImpl() throws Exception {
    // Given:
    JsonMapper mapper = JsonMapper.builder() // Don't use shared mapper!
      .addMixIn(Feline.class, FelineMixin.class)
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .build();
    // When:
    Feline cat = mapper.readValue(unknownCatJson, Feline.class);
    // Then:
    assertTrue("Should fallback when unknown property blocks deduction", cat instanceof AnotherLiveCat);
    assertSame(AnotherLiveCat.class, cat.getClass());
  }

  public void testDefaultImplSupercededByUnknownProperty() throws Exception {
    try {
      // Given:
      JsonMapper mapper = JsonMapper.builder() // Don't use shared mapper!
        .addMixIn(Cat.class, FelineMixin.class)
        //.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build();
      // When:
      mapper.readValue(unknownCatJson, Cat.class);

      fail("Unknown property supercedes defaultImpl");
    } catch (UnrecognizedPropertyException e) {
      // Then:
    }
  }

  public void testSimpleSerialization() throws Exception {
    // Given:
    JavaType listOfCats = TypeFactory.defaultInstance().constructParametricType(List.class, Cat.class);
    List<Cat> list = MAPPER.readValue(arrayOfCatsJson, listOfCats);
    Cat cat = list.get(0);
    // When:
    String json = MAPPER.writeValueAsString(cat);
    // Then:
    assertEquals(liveCatJson, json);
  }

  public void testListSerialization() throws Exception {
    // Given:
    JavaType listOfCats = TypeFactory.defaultInstance().constructParametricType(List.class, Cat.class);
    List<Cat> list = MAPPER.readValue(arrayOfCatsJson, listOfCats);
    // When:
    String json = MAPPER.writeValueAsString(list);
    // Then:
    assertEquals(arrayOfCatsJson, json);
  }
}
