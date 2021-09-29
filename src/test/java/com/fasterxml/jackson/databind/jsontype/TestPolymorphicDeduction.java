package com.fasterxml.jackson.databind.jsontype;

import java.util.ArrayList;
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
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
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

  private static final String deadCatJson = a2q("{'name':'Felix','causeOfDeath':'entropy'}");
  private static final String liveCatJson = a2q("{'name':'Felix','angry':true}");
  private static final String luckyCatJson = a2q("{'name':'Felix','angry':true,'lives':8}");
  private static final String ambiguousCatJson = a2q("{'name':'Felix','age':2}");
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
    Cat cat = MAPPER.reader()
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

  public void testUnrecognizedProperties() throws Exception {
    try {
      /*Cat cat =*/
      MAPPER.readValue(ambiguousCatJson, Cat.class);
      fail("Unable to map, because there is unknown field 'age'");
    } catch (UnrecognizedPropertyException e) {
      verifyException(e, "Unrecognized field");
    }
  }

  public void testNotFailOnUnknownProperty() throws Exception {
    // Given:
    JsonMapper mapper = JsonMapper.builder()
                                  .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                                  .build();
    // When:
    Cat cat = mapper.readValue(ambiguousCatJson, Cat.class);
    // Then:
    // unknown proparty 'age' is ignored, and json is deserialized to Cat class
    assertTrue(cat instanceof Cat);
    assertSame(Cat.class, cat.getClass());
    assertEquals("Felix", cat.name);
  }

  public void testAmbiguousProperties() throws Exception {
    try {
      /*Feline cat =*/ MAPPER.readValue(ambiguousCatJson, Feline.class);
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
    Feline cat = mapper.readValue(ambiguousCatJson, Feline.class);
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

  @JsonTypeInfo(use = DEDUCTION, defaultImpl = ListOfPlaces.class)
  @JsonSubTypes( {@Type(ListOfPlaces.class), @Type(CompositePlace.class), @Type(Place.class)})
  interface WorthSeeing {}

  public static class Place implements WorthSeeing {
    public String name;
  }

  public static class CompositePlace extends Place implements WorthSeeing {

    public Map<String, WorthSeeing> places;
  }

  static class ListOfPlaces extends ArrayList<WorthSeeing> implements WorthSeeing {
  }

  private static final String colosseumJson = a2q("{'name': 'The Colosseum'}");
  private static final String romanForumJson = a2q("{'name': 'The Roman Forum'}");
  private static final String romeJson = a2q("{'name': 'Rome', 'places': {'colosseum': " + colosseumJson + ","+ "'romanForum': "+ romanForumJson +"}}");

  private static final String rialtoBridgeJson = a2q("{'name': 'Rialto Bridge'}");
  private static final String sighsBridgeJson = a2q("{'name': 'The Bridge Of Sighs'}");
  private static final String bridgesJson = a2q("["+ rialtoBridgeJson +"," + sighsBridgeJson +"]");
  private static final String veniceJson = a2q("{'name': 'Venice', 'places': {'bridges':  " + bridgesJson + "}}");

  private static final String alpsJson = a2q("{'name': 'The Alps'}");
  private static final String citesJson = a2q("[" + romeJson + "," + veniceJson + "]");
  private static final String italy = a2q("{'name': 'Italy', 'places': {'mountains': " + alpsJson + ", 'cities': "+ citesJson +"}}}");

  public void testSupertypeInferenceWhenDefaultDefined() throws Exception {
    //When:
    WorthSeeing worthSeeing = MAPPER.readValue(alpsJson, WorthSeeing.class);
    // Then:
    assertEqualsPlace("The Alps", worthSeeing);
  }

  public void testDefaultImplementation() throws Exception {
    // When:
    WorthSeeing worthSeeing = MAPPER.readValue(citesJson, WorthSeeing.class);
    // Then:
    assertCities(worthSeeing);
  }

  public void  testCompositeInference() throws Exception {
    // When:
    WorthSeeing worthSeeing = MAPPER.readValue(italy, WorthSeeing.class);
    // Then:
    assertSame(CompositePlace.class, worthSeeing.getClass());
    CompositePlace italy = (CompositePlace) worthSeeing;
    assertEquals("Italy", italy.name);
    assertEquals(2, italy.places.size());
    assertEqualsPlace("The Alps", italy.places.get("mountains"));
    assertEquals(2, italy.places.size());
    assertCities(italy.places.get("cities"));
  }

  private void assertCities(WorthSeeing worthSeeing) {
    assertSame(ListOfPlaces.class, worthSeeing.getClass());
    ListOfPlaces cities = (ListOfPlaces) worthSeeing;
    assertEquals(2, cities.size());
    assertRome(cities.get(0));
    assertVenice(cities.get(1));
  }

  private void assertRome(WorthSeeing worthSeeing) {
    assertSame(CompositePlace.class, worthSeeing.getClass());
    CompositePlace rome = (CompositePlace) worthSeeing;
    assertEquals("Rome", rome.name);
    assertEquals(2, rome.places.size());
    assertEqualsPlace("The Colosseum", rome.places.get("colosseum"));
    assertEqualsPlace("The Roman Forum", rome.places.get("romanForum"));
  }

  private void assertVenice(WorthSeeing worthSeeing) {
    assertSame(CompositePlace.class, worthSeeing.getClass());
    CompositePlace venice = (CompositePlace) worthSeeing;
    assertEquals("Venice", venice.name);
    assertEquals(1, venice.places.size());
    assertVeniceBridges(venice.places.get("bridges"));

  }

  private void assertVeniceBridges(WorthSeeing worthSeeing){
    assertSame(ListOfPlaces.class, worthSeeing.getClass());
    ListOfPlaces bridges = (ListOfPlaces) worthSeeing;
    assertEqualsPlace("Rialto Bridge", bridges.get(0));
    assertEqualsPlace("The Bridge Of Sighs", bridges.get(1));
  }

  private void assertEqualsPlace(String expectedName, WorthSeeing worthSeeing){
    assertTrue(worthSeeing instanceof Place);
    assertSame(Place.class, worthSeeing.getClass());
    assertEquals(expectedName,((Place) worthSeeing).name);
  }
}
