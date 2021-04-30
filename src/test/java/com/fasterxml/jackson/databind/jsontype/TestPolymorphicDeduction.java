package com.fasterxml.jackson.databind.jsontype;

import java.util.Iterator;
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
    @JsonSubTypes({@Type(LiveCat.class), @Type(DeadCat.class)})
    public static class Cat {
        public String name;
        public Flea flea;
    }

  static class DeadCat extends Cat {
    public String causeOfDeath;
  }

  static class LiveCat extends Cat {
    public boolean angry;
  }

    @JsonTypeInfo(use = DEDUCTION)
    @JsonSubTypes({@Type(NoFlea.class), @Type(HasFlea.class)})
    interface Flea {
    }

    static class NoFlea implements Flea {
    }

    static class HasFlea implements Flea {
        public int count;
    }

  static class Box {
    public Cat cat;
  }

  /*
  /**********************************************************
  /* Mock data
  /**********************************************************
   */

    private static final String deadCatJson = aposToQuotes("{'name':'Felix','flea':null,'causeOfDeath':'entropy'}");
    private static final String liveCatJson = aposToQuotes("{'name':'Felix','flea':null,'angry':true}");
    private static final String liveNoFleaCatJson = aposToQuotes("{'name':'Felix','flea':{},'angry':true}");
    private static final String liveHasFleaCatJson = aposToQuotes("{'name':'Felix','flea':{'count':42},'angry':true}");
    private static final String luckyCatJson = aposToQuotes("{'name':'Felix','flea':null,'angry':true,'lives':8}");
    private static final String ambiguousCatJson = aposToQuotes("{'name':'Felix','age':2}");
    private static final String box1Json = aposToQuotes("{'cat':" + liveCatJson + "}");
    private static final String box2Json = aposToQuotes("{'cat':" + deadCatJson + "}");
    private static final String arrayOfCatsJson = aposToQuotes("[" + liveCatJson + "," + deadCatJson + "," + liveNoFleaCatJson + "]");
    private static final String mapOfCatsJson = aposToQuotes("{'live':" + liveCatJson + ",'noFlea':" + liveNoFleaCatJson + "}");

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
        assertTrue(((LiveCat) cat).angry);

        cat = sharedMapper().readValue(deadCatJson, Cat.class);
        assertTrue(cat instanceof DeadCat);
        assertSame(cat.getClass(), DeadCat.class);
        assertEquals("Felix", cat.name);
        assertEquals("entropy", ((DeadCat) cat).causeOfDeath);

        cat = sharedMapper().readValue(liveNoFleaCatJson, Cat.class);
        assertTrue(cat instanceof LiveCat);
        assertTrue(cat.flea instanceof NoFlea);
        assertSame(cat.getClass(), LiveCat.class);
        assertSame(cat.flea.getClass(), NoFlea.class);
        assertEquals("Felix", cat.name);
        assertTrue(((LiveCat) cat).angry);

        cat = sharedMapper().readValue(liveHasFleaCatJson, Cat.class);
        assertTrue(cat instanceof LiveCat);
        assertTrue(cat.flea instanceof HasFlea);
        assertSame(cat.getClass(), LiveCat.class);
        assertSame(cat.flea.getClass(), HasFlea.class);
        assertEquals("Felix", cat.name);
        assertEquals(42, ((HasFlea) cat.flea).count);
        assertTrue(((LiveCat) cat).angry);
    }

  public void testCaseInsensitiveInference() throws Exception {
    Cat cat = JsonMapper.builder() // Don't use shared mapper!
      .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
      .build()
      .readValue(deadCatJson.toUpperCase().replace("NULL", "null"), Cat.class);
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
    assertTrue(box.cat instanceof LiveCat);
    assertSame(box.cat.getClass(), LiveCat.class);
    assertEquals("Felix", box.cat.name);
    assertTrue(((LiveCat)box.cat).angry);

    box = sharedMapper().readValue(box2Json, Box.class);
    assertTrue(box.cat instanceof DeadCat);
    assertSame(box.cat.getClass(), DeadCat.class);
    assertEquals("Felix", box.cat.name);
    assertEquals("entropy", ((DeadCat)box.cat).causeOfDeath);
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
        assertEquals(2, map.size());

        Iterator<Map.Entry<String, Cat>> iterator = map.entrySet().iterator();
        assertTrue(iterator.next().getValue() instanceof LiveCat);

        Cat noFlea = iterator.next().getValue();
        assertTrue(noFlea instanceof LiveCat);
        assertTrue(noFlea.flea instanceof NoFlea);
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


    @JsonTypeInfo(use = DEDUCTION)
    @JsonSubTypes({@Type(HasFlea.class)})
    abstract static class FleaMixin {

    }
    public void testNoEmptyClassRegistered() throws Exception {
        JsonMapper mapper = JsonMapper.builder() // Don't use shared mapper!
                .addMixIn(Flea.class, FleaMixin.class)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        try {
            mapper.readValue(liveNoFleaCatJson, Cat.class);
        } catch (InvalidTypeIdException e) {
            verifyException(e, "Cannot deduce unique subtype");
        }
    }

    public void testSimpleSerialization() throws Exception {
        // Given:
        JavaType listOfCats = TypeFactory.defaultInstance().constructParametricType(List.class, Cat.class);
        List<Cat> list = sharedMapper().readValue(arrayOfCatsJson, listOfCats);
        Cat liveCat = list.get(0);
        Cat liveNoFleaCat = list.get(2);
        // When:
        String json = sharedMapper().writeValueAsString(liveCat);
        String noFleaJson = sharedMapper().writeValueAsString(liveNoFleaCat);
        // Then:
        assertEquals(liveCatJson, json);
        assertEquals(liveNoFleaCatJson, noFleaJson);
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
