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
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import static com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.DEDUCTION;

// for [databind#43], deduction-based polymorphism
public class TestPolymorphicDeduction extends BaseMapTest {

  @JsonTypeInfo(use = DEDUCTION)
  @JsonSubTypes( {@Type(LiveCat.class), @Type(DeadCat.class), @Type(Fleabag.class)})
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

  private static final String deadCatJson = aposToQuotes("{'name':'Felix','causeOfDeath':'entropy'}");
  private static final String liveCatJson = aposToQuotes("{'name':'Felix','angry':true}");
  private static final String luckyCatJson = aposToQuotes("{'name':'Felix','angry':true,'lives':8}");
  private static final String ambiguousCatJson = aposToQuotes("{'name':'Felix','age':2}");
  private static final String fleabagJson = aposToQuotes("{}");
  private static final String box1Json = aposToQuotes("{'feline':" + liveCatJson + "}");
  private static final String box2Json = aposToQuotes("{'feline':" + deadCatJson + "}");
  private static final String box3Json = aposToQuotes("{'feline':" + fleabagJson + "}");
  private static final String box4Json = aposToQuotes("{'feline':null}");
  private static final String box5Json = aposToQuotes("{}");
  private static final String arrayOfCatsJson = aposToQuotes("[" + liveCatJson + "," + deadCatJson + "]");
  private static final String mapOfCatsJson = aposToQuotes("{'live':" + liveCatJson + "}");

  /*
  /**********************************************************
  /* Test methods
  /**********************************************************
   */

  public void testSimpleInference() throws Exception {
    Cat cat = sharedMapper().readValue(liveCatJson, Cat.class);
    assertTrue(cat instanceof LiveCat);
    assertSame(cat.getClass(), LiveCat.class);
    assertEquals("Felix", cat.name);
    assertTrue(((LiveCat)cat).angry);

    cat = sharedMapper().readValue(deadCatJson, Cat.class);
    assertTrue(cat instanceof DeadCat);
    assertSame(cat.getClass(), DeadCat.class);
    assertEquals("Felix", cat.name);
    assertEquals("entropy", ((DeadCat)cat).causeOfDeath);
  }

  public void testSimpleInferenceOfEmptySubtype() throws Exception {
    // Given:
    ObjectMapper mapper = sharedMapper();
    // When:
    Feline feline = mapper.readValue(fleabagJson, Feline.class);
    // Then:
    assertTrue(feline instanceof Fleabag);
  }

  public void testSimpleInferenceOfEmptySubtypeDoesntMatchNull() throws Exception {
    // Given:
    ObjectMapper mapper = sharedMapper();
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
    Box box = sharedMapper().readValue(box1Json, Box.class);
    assertTrue(box.feline instanceof LiveCat);
    assertSame(box.feline.getClass(), LiveCat.class);
    assertEquals("Felix", ((LiveCat)box.feline).name);
    assertTrue(((LiveCat)box.feline).angry);

    box = sharedMapper().readValue(box2Json, Box.class);
    assertTrue(box.feline instanceof DeadCat);
    assertSame(box.feline.getClass(), DeadCat.class);
    assertEquals("Felix", ((DeadCat)box.feline).name);
    assertEquals("entropy", ((DeadCat)box.feline).causeOfDeath);
  }

  public void testContainedInferenceOfEmptySubtype() throws Exception {
    Box box = sharedMapper().readValue(box3Json, Box.class);
    assertTrue(box.feline instanceof Fleabag);

    box = sharedMapper().readValue(box4Json, Box.class);
    assertNull("null != {}", box.feline);

    box = sharedMapper().readValue(box5Json, Box.class);
    assertNull("<absent> != {}", box.feline);
  }

  public void testListInference() throws Exception {
    JavaType listOfCats = TypeFactory.defaultInstance().constructParametricType(List.class, Cat.class);
    List<Cat> boxes = sharedMapper().readValue(arrayOfCatsJson, listOfCats);
    assertTrue(boxes.get(0) instanceof LiveCat);
    assertTrue(boxes.get(1) instanceof DeadCat);
  }

  public void testMapInference() throws Exception {
    JavaType mapOfCats = TypeFactory.defaultInstance().constructParametricType(Map.class, String.class, Cat.class);
    Map<String, Cat> map = sharedMapper().readValue(mapOfCatsJson, mapOfCats);
    assertEquals(1, map.size());
    assertTrue(map.entrySet().iterator().next().getValue() instanceof LiveCat);
  }

  public void testArrayInference() throws Exception {
    Cat[] boxes = sharedMapper().readValue(arrayOfCatsJson, Cat[].class);
    assertTrue(boxes[0] instanceof LiveCat);
    assertTrue(boxes[1] instanceof DeadCat);
  }

  public void testIgnoreProperties() throws Exception {
    Cat cat = sharedMapper().reader()
      .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .readValue(luckyCatJson, Cat.class);
    assertTrue(cat instanceof LiveCat);
    assertSame(cat.getClass(), LiveCat.class);
    assertEquals("Felix", cat.name);
    assertTrue(((LiveCat)cat).angry);
  }

  static class AnotherLiveCat extends Cat {
    public boolean angry;
  }

  public void testAmbiguousClasses() throws Exception {
    try {
      ObjectMapper mapper = JsonMapper.builder() // Don't use shared mapper!
              .registerSubtypes(AnotherLiveCat.class)
              .build();
      /*Cat cat =*/ mapper.readValue(liveCatJson, Cat.class);
      fail("Should not get here");
    } catch (InvalidDefinitionException e) {
        verifyException(e, "Subtypes ");
        verifyException(e, "have the same signature");
        verifyException(e, "cannot be uniquely deduced");
    }
  }

  public void testAmbiguousProperties() throws Exception {
    try {
      /*Cat cat =*/ sharedMapper().readValue(ambiguousCatJson, Cat.class);
      fail("Should not get here");
    } catch (InvalidTypeIdException e) {
        verifyException(e, "Cannot deduce unique subtype");
    }
  }

  public void testFailOnInvalidSubtype() throws Exception {
    // Given:
    JsonMapper mapper = JsonMapper.builder() // Don't use shared mapper!
      .disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE)
      .build();
    // When:
    Cat cat = mapper.readValue(ambiguousCatJson, Cat.class);
    // Then:
    assertNull(cat);
  }

  @JsonTypeInfo(use = DEDUCTION, defaultImpl = Cat.class)
  abstract static class CatMixin {
  }

  public void testDefaultImpl() throws Exception {
    // Given:
    JsonMapper mapper = JsonMapper.builder() // Don't use shared mapper!
      .addMixIn(Cat.class, CatMixin.class)
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .build();
    // When:
    Cat cat = mapper.readValue(ambiguousCatJson, Cat.class);
    // Then:
    // Even though "age":2 implies this was a failed subtype, we are instructed to fallback to Cat regardless.
    assertTrue(cat instanceof Cat);
    assertSame(Cat.class, cat.getClass());
    assertEquals("Felix", cat.name);
  }

  public void testSimpleSerialization() throws Exception {
    // Given:
    JavaType listOfCats = TypeFactory.defaultInstance().constructParametricType(List.class, Cat.class);
    List<Cat> list = sharedMapper().readValue(arrayOfCatsJson, listOfCats);
    Cat cat = list.get(0);
    // When:
    String json = sharedMapper().writeValueAsString(cat);
    // Then:
    assertEquals(liveCatJson, json);
  }

  public void testListSerialization() throws Exception {
    // Given:
    JavaType listOfCats = TypeFactory.defaultInstance().constructParametricType(List.class, Cat.class);
    List<Cat> list = sharedMapper().readValue(arrayOfCatsJson, listOfCats);
    // When:
    String json = sharedMapper().writeValueAsString(list);
    // Then:
    assertEquals(arrayOfCatsJson, json);
  }

}
