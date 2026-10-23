package tools.jackson.databind.jsontype;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JsonTypeInfo#writeTypeIdForDefaultImpl()} feature,
 * which allows suppressing type id serialization when the runtime type
 * matches {@code defaultImpl}.
 *
 * @since 3.2
 */
public class SkipWriteTypeIdForDefaultImplTest extends DatabindTestUtil
{
    // -- Shared type hierarchy for PROPERTY tests

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY,
            property = "@type",
            defaultImpl = DefaultDog.class,
            writeTypeIdForDefaultImpl = OptBoolean.FALSE)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = DefaultDog.class, name = "dog"),
        @JsonSubTypes.Type(value = Cat.class, name = "cat"),
        @JsonSubTypes.Type(value = Puppy.class, name = "puppy")
    })
    static class Animal {
        public String name;
    }

    static class DefaultDog extends Animal {
        public String breed;
    }

    static class Cat extends Animal {
        public int lives;
    }

    // Subclass of defaultImpl -- should still get type id
    static class Puppy extends DefaultDog {
        public boolean isSmall;
    }

    // -- WRAPPER_ARRAY variant

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_ARRAY,
            defaultImpl = DefaultDog2.class,
            writeTypeIdForDefaultImpl = OptBoolean.FALSE)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = DefaultDog2.class, name = "dog"),
        @JsonSubTypes.Type(value = Cat2.class, name = "cat")
    })
    static class Animal2 {
        public String name;
    }

    static class DefaultDog2 extends Animal2 {
        public String breed;
    }

    static class Cat2 extends Animal2 {
        public int lives;
    }

    // -- WRAPPER_OBJECT variant

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT,
            defaultImpl = DefaultDog3.class,
            writeTypeIdForDefaultImpl = OptBoolean.FALSE)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = DefaultDog3.class, name = "dog"),
        @JsonSubTypes.Type(value = Cat3.class, name = "cat")
    })
    static class Animal3 {
        public String name;
    }

    static class DefaultDog3 extends Animal3 {
        public String breed;
    }

    static class Cat3 extends Animal3 {
        public int lives;
    }

    // -- EXTERNAL_PROPERTY variant (needs wrapper)

    static class AnimalWrapper {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = "@type",
                defaultImpl = DefaultDog4.class,
                writeTypeIdForDefaultImpl = OptBoolean.FALSE)
        @JsonSubTypes({
            @JsonSubTypes.Type(value = DefaultDog4.class, name = "dog"),
            @JsonSubTypes.Type(value = Cat4.class, name = "cat")
        })
        public Animal4 animal;
    }

    static class Animal4 {
        public String name;
    }

    static class DefaultDog4 extends Animal4 {
        public String breed;
    }

    static class Cat4 extends Animal4 {
        public int lives;
    }

    // -- EXISTING_PROPERTY variant

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY,
            property = "type",
            defaultImpl = DefaultDog5.class,
            writeTypeIdForDefaultImpl = OptBoolean.FALSE)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = DefaultDog5.class, name = "dog"),
        @JsonSubTypes.Type(value = Cat5.class, name = "cat")
    })
    static class Animal5 {
        public String name;
        public String type;
    }

    static class DefaultDog5 extends Animal5 {
        public String breed;
    }

    static class Cat5 extends Animal5 {
        public int lives;
    }

    // -- defaultImpl is the base type itself

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY,
            property = "@type",
            defaultImpl = AnimalBase.class,
            writeTypeIdForDefaultImpl = OptBoolean.FALSE)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = AnimalBase.class, name = "base"),
        @JsonSubTypes.Type(value = DogBase.class, name = "dog"),
        @JsonSubTypes.Type(value = CatBase.class, name = "cat")
    })
    static class AnimalBase {
        public String name;
    }

    static class DogBase extends AnimalBase {
        public String breed;
    }

    static class CatBase extends AnimalBase {
        public int lives;
    }

    // -- Feature OFF (default behavior)

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY,
            property = "@type",
            defaultImpl = DefaultDogOff.class)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = DefaultDogOff.class, name = "dog"),
        @JsonSubTypes.Type(value = CatOff.class, name = "cat")
    })
    static class AnimalOff {
        public String name;
    }

    static class DefaultDogOff extends AnimalOff {
        public String breed;
    }

    static class CatOff extends AnimalOff {
        public int lives;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************************
    /* Test methods: PROPERTY inclusion
    /**********************************************************************
     */

    @Test
    public void testPropertyDefaultImplSkipped() throws Exception
    {
        DefaultDog dog = new DefaultDog();
        dog.name = "Rex";
        dog.breed = "Lab";
        String json = MAPPER.writeValueAsString(dog);
        // Should NOT contain type id since runtime type matches defaultImpl
        assertFalse(json.contains("@type"), "Type id should be skipped for defaultImpl; got: " + json);
        assertTrue(json.contains("\"name\":\"Rex\""));
        assertTrue(json.contains("\"breed\":\"Lab\""));
    }

    @Test
    public void testPropertyNonDefaultHasTypeId() throws Exception
    {
        Cat cat = new Cat();
        cat.name = "Whiskers";
        cat.lives = 9;
        String json = MAPPER.writeValueAsString(cat);
        // Should contain type id since Cat is not the defaultImpl
        assertTrue(json.contains("\"@type\":\"cat\""), "Type id should be present for non-default type; got: " + json);
    }

    @Test
    public void testPropertySubclassOfDefaultImplHasTypeId() throws Exception
    {
        // Puppy extends DefaultDog (the defaultImpl) but is not itself the defaultImpl.
        // Since skip uses exact class match (==), Puppy should still get a type id.
        Puppy puppy = new Puppy();
        puppy.name = "Tiny";
        puppy.breed = "Poodle";
        puppy.isSmall = true;
        String json = MAPPER.writerFor(Animal.class).writeValueAsString(puppy);
        assertTrue(json.contains("\"@type\":\"puppy\""),
                "Type id should be present for subclass of defaultImpl; got: " + json);
    }

    @Test
    public void testPropertyRoundTrip() throws Exception
    {
        // Round-trip defaultImpl type (no type id in JSON)
        DefaultDog dog = new DefaultDog();
        dog.name = "Rex";
        dog.breed = "Lab";
        String json = MAPPER.writerFor(Animal.class).writeValueAsString(dog);
        Animal result = MAPPER.readValue(json, Animal.class);
        assertTrue(result instanceof DefaultDog);
        assertEquals("Rex", result.name);
        assertEquals("Lab", ((DefaultDog) result).breed);

        // Round-trip non-default type (type id in JSON)
        Cat cat = new Cat();
        cat.name = "Whiskers";
        cat.lives = 9;
        json = MAPPER.writerFor(Animal.class).writeValueAsString(cat);
        result = MAPPER.readValue(json, Animal.class);
        assertTrue(result instanceof Cat);
        assertEquals("Whiskers", result.name);
        assertEquals(9, ((Cat) result).lives);
    }

    /*
    /**********************************************************************
    /* Test methods: WRAPPER_ARRAY inclusion
    /**********************************************************************
     */

    @Test
    public void testWrapperArrayDefaultImplSkipped() throws Exception
    {
        DefaultDog2 dog = new DefaultDog2();
        dog.name = "Rex";
        dog.breed = "Lab";
        String json = MAPPER.writerFor(Animal2.class).writeValueAsString(dog);
        // Should NOT be wrapped in array
        assertFalse(json.startsWith("["), "Should not have wrapper array for defaultImpl; got: " + json);
        assertTrue(json.startsWith("{"), "Should be plain object; got: " + json);
    }

    @Test
    public void testWrapperArrayNonDefaultHasTypeId() throws Exception
    {
        Cat2 cat = new Cat2();
        cat.name = "Whiskers";
        cat.lives = 9;
        String json = MAPPER.writerFor(Animal2.class).writeValueAsString(cat);
        assertTrue(json.startsWith("["), "Should have wrapper array for non-default type; got: " + json);
        assertTrue(json.contains("\"cat\""), "Should contain type id; got: " + json);
    }

    @Test
    public void testWrapperArrayRoundTrip() throws Exception
    {
        // For WRAPPER_ARRAY, when type id is skipped the output is a plain object
        // (no wrapping array). The deserializer uses defaultImpl for plain objects.
        DefaultDog2 dog = new DefaultDog2();
        dog.name = "Rex";
        dog.breed = "Lab";
        String json = MAPPER.writerFor(Animal2.class).writeValueAsString(dog);
        // The JSON is a plain object, and AsArrayTypeDeserializer should fall back to defaultImpl
        Animal2 result = MAPPER.readValue(json, Animal2.class);
        assertNotNull(result);
        assertEquals("Rex", result.name);
    }

    /*
    /**********************************************************************
    /* Test methods: WRAPPER_OBJECT inclusion
    /**********************************************************************
     */

    @Test
    public void testWrapperObjectDefaultImplSkipped() throws Exception
    {
        DefaultDog3 dog = new DefaultDog3();
        dog.name = "Rex";
        dog.breed = "Lab";
        String json = MAPPER.writerFor(Animal3.class).writeValueAsString(dog);
        // Should NOT have wrapper object with "dog" key
        assertFalse(json.contains("\"dog\""), "Should not have wrapper object key for defaultImpl; got: " + json);
        assertTrue(json.contains("\"name\":\"Rex\""));
    }

    @Test
    public void testWrapperObjectNonDefaultHasTypeId() throws Exception
    {
        Cat3 cat = new Cat3();
        cat.name = "Whiskers";
        cat.lives = 9;
        String json = MAPPER.writerFor(Animal3.class).writeValueAsString(cat);
        assertTrue(json.contains("\"cat\""), "Should contain type name wrapper for non-default type; got: " + json);
    }

    @Test
    public void testWrapperObjectNonDefaultRoundTrip() throws Exception
    {
        // Round-trip with non-default type (wrapper object present)
        Cat3 cat = new Cat3();
        cat.name = "Whiskers";
        cat.lives = 9;
        String json = MAPPER.writerFor(Animal3.class).writeValueAsString(cat);
        Animal3 result = MAPPER.readValue(json, Animal3.class);
        assertTrue(result instanceof Cat3);
        assertEquals("Whiskers", result.name);
        assertEquals(9, ((Cat3) result).lives);
    }

    /*
    /**********************************************************************
    /* Test methods: EXTERNAL_PROPERTY inclusion
    /**********************************************************************
     */

    @Test
    public void testExternalPropertyDefaultImplSkipped() throws Exception
    {
        AnimalWrapper wrapper = new AnimalWrapper();
        DefaultDog4 dog = new DefaultDog4();
        dog.name = "Rex";
        dog.breed = "Lab";
        wrapper.animal = dog;
        String json = MAPPER.writeValueAsString(wrapper);
        assertFalse(json.contains("@type"), "Type id should be skipped for defaultImpl; got: " + json);
    }

    @Test
    public void testExternalPropertyDefaultImplRoundTrip() throws Exception
    {
        AnimalWrapper wrapper = new AnimalWrapper();
        DefaultDog4 dog = new DefaultDog4();
        dog.name = "Rex";
        dog.breed = "Lab";
        wrapper.animal = dog;
        String json = MAPPER.writeValueAsString(wrapper);
        AnimalWrapper result = MAPPER.readValue(json, AnimalWrapper.class);
        assertTrue(result.animal instanceof DefaultDog4);
        assertEquals("Rex", result.animal.name);
        assertEquals("Lab", ((DefaultDog4) result.animal).breed);
    }

    @Test
    public void testExternalPropertyNonDefaultHasTypeId() throws Exception
    {
        AnimalWrapper wrapper = new AnimalWrapper();
        Cat4 cat = new Cat4();
        cat.name = "Whiskers";
        cat.lives = 9;
        wrapper.animal = cat;
        String json = MAPPER.writeValueAsString(wrapper);
        assertTrue(json.contains("\"@type\":\"cat\""), "Type id should be present for non-default type; got: " + json);
    }

    /*
    /**********************************************************************
    /* Test methods: EXISTING_PROPERTY inclusion
    /**********************************************************************
     */

    @Test
    public void testExistingPropertyDefaultImplTypeFieldStillWritten() throws Exception
    {
        DefaultDog5 dog = new DefaultDog5();
        dog.name = "Rex";
        dog.breed = "Lab";
        dog.type = "dog";
        String json = MAPPER.writerFor(Animal5.class).writeValueAsString(dog);
        // With EXISTING_PROPERTY, the "type" field is a real bean property so it
        // is always written by the bean serializer even though the TypeSerializer
        // suppresses the type id. This is expected.
        assertTrue(json.contains("\"type\":\"dog\""),
                "Existing property should still be written as bean property; got: " + json);
        assertTrue(json.contains("\"name\":\"Rex\""));
    }

    @Test
    public void testExistingPropertyNonDefaultHasTypeId() throws Exception
    {
        Cat5 cat = new Cat5();
        cat.name = "Whiskers";
        cat.lives = 9;
        cat.type = "cat";
        String json = MAPPER.writerFor(Animal5.class).writeValueAsString(cat);
        assertTrue(json.contains("\"type\":\"cat\""),
                "Type property should be present for non-default type; got: " + json);
    }

    /*
    /**********************************************************************
    /* Test methods: defaultImpl is the base type
    /**********************************************************************
     */

    @Test
    public void testBaseTypeAsDefaultImplSkipped() throws Exception
    {
        // When defaultImpl is the base type itself, exact match skips type id
        AnimalBase base = new AnimalBase();
        base.name = "Generic";
        String json = MAPPER.writerFor(AnimalBase.class).writeValueAsString(base);
        assertFalse(json.contains("@type"), "Type id should be skipped for base-type defaultImpl; got: " + json);
    }

    @Test
    public void testBaseTypeAsDefaultImplSubclassHasTypeId() throws Exception
    {
        // Subclass of the base-type defaultImpl should still get type id
        DogBase dog = new DogBase();
        dog.name = "Rex";
        dog.breed = "Lab";
        String json = MAPPER.writerFor(AnimalBase.class).writeValueAsString(dog);
        assertTrue(json.contains("\"@type\":\"dog\""),
                "Type id should be present for subclass when defaultImpl is base type; got: " + json);
    }

    /*
    /**********************************************************************
    /* Test methods: Feature OFF (backwards compatibility)
    /**********************************************************************
     */

    @Test
    public void testFeatureOffAlwaysWritesTypeId() throws Exception
    {
        DefaultDogOff dog = new DefaultDogOff();
        dog.name = "Rex";
        dog.breed = "Lab";
        String json = MAPPER.writerFor(AnimalOff.class).writeValueAsString(dog);
        // With feature OFF, type id should always be present
        assertTrue(json.contains("\"@type\":\"dog\""), "Type id should be present when feature is off; got: " + json);
    }
}
