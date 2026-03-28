package tools.jackson.databind.objectid;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.util.TokenBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for handling of "native" Object Ids and Type Ids -- that is, ids
 * provided by format (like YAML anchors for Object Ids, or tagged types)
 * rather than as JSON properties. Tests use {@link TokenBuffer} to simulate
 * a parser that supports native ids.
 *<p>
 * Native ids are written via {@code writeObjectId()} / {@code writeTypeId()}
 * before the token they should be associated with. The TokenBuffer stores
 * the pending id with the next appended token.
 */
public class NativeObjectIdAndTypeIdTest extends DatabindTestUtil
{
    /*
    /**********************************************************
    /* Helper types for Object Id tests
    /**********************************************************
     */

    // Simple bean with Object Identity using IntSequenceGenerator
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
    static class IdBean {
        public int value;
        public IdBean next;

        public IdBean() { }
        public IdBean(int v) { value = v; }
    }

    // Bean using PropertyGenerator for Object Id (native id replaces property)
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    static class PropIdBean {
        public String id;
        public String name;

        public PropIdBean() { }
    }

    // Bean with @JsonCreator and property-based Object Id
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    static class CreatorIdBean {
        public String id;
        public String name;

        @JsonCreator
        public CreatorIdBean(@JsonProperty("id") String id, @JsonProperty("name") String name) {
            this.id = id;
            this.name = name;
        }
    }

    // Wrapper that references an IdBean
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
    static class IdBeanWrapper {
        public IdBean first;
        public IdBean second;
    }

    /*
    /**********************************************************
    /* Helper types for Type Id tests
    /**********************************************************
     */

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = Dog.class, name = "dog"),
        @JsonSubTypes.Type(value = Cat.class, name = "cat")
    })
    static class Animal {
        public String name;
    }

    static class Dog extends Animal {
        public String breed;
    }

    static class Cat extends Animal {
        public int lives;
    }

    // Container for polymorphic value
    static class AnimalWrapper {
        public Animal animal;
    }

    /*
    /**********************************************************
    /* Helper types combining Object Id + Type Id
    /**********************************************************
     */

    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = TypedIdNodeSub.class, name = "sub")
    })
    static class TypedIdNode {
        public String name;
    }

    static class TypedIdNodeSub extends TypedIdNode {
        public int extra;
    }

    /*
    /**********************************************************
    /* Test methods: Native Object Id
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // Basic: native Object Id with default constructor bean
    @Test
    public void testNativeObjectIdBasic() throws Exception
    {
        // Native id is set before first property name, so it's on the FIELD_NAME
        // token where BeanDeserializer checks for it (after consuming START_OBJECT)
        TokenBuffer buf = new TokenBuffer(null, true);
        buf.writeStartObject();
        buf.writeObjectId(1);   // will be associated with next token (FIELD_NAME)
        buf.writeName("value");
        buf.writeNumber(42);
        buf.writeEndObject();

        JsonParser p = buf.asParser(ObjectReadContext.empty());
        IdBean result = MAPPER.readValue(p, IdBean.class);
        p.close();
        buf.close();

        assertNotNull(result);
        assertEquals(42, result.value);
    }

    // Native Object Id with forward reference: second reference uses id
    @Test
    public void testNativeObjectIdWithReference() throws Exception
    {
        TokenBuffer buf = new TokenBuffer(null, true);
        buf.writeStartObject();
        // "first": full object with native id
        buf.writeName("first");
        buf.writeStartObject();
        buf.writeObjectId(1); // associated with the next FIELD_NAME
        buf.writeName("value");
        buf.writeNumber(99);
        buf.writeEndObject();
        // "second": reference by id value
        buf.writeName("second");
        buf.writeNumber(1);
        buf.writeEndObject();

        JsonParser p = buf.asParser(ObjectReadContext.empty());
        IdBeanWrapper result = MAPPER.readValue(p, IdBeanWrapper.class);
        p.close();
        buf.close();

        assertNotNull(result);
        assertNotNull(result.first);
        assertEquals(99, result.first.value);
        // second should resolve to the same instance
        assertSame(result.first, result.second);
    }

    // Native Object Id with PropertyGenerator
    @Test
    public void testNativeObjectIdPropertyGenerator() throws Exception
    {
        TokenBuffer buf = new TokenBuffer(null, true);
        buf.writeStartObject();
        buf.writeObjectId("myId123"); // native id
        buf.writeName("id");
        buf.writeString("myId123");
        buf.writeName("name");
        buf.writeString("test");
        buf.writeEndObject();

        JsonParser p = buf.asParser(ObjectReadContext.empty());
        PropIdBean result = MAPPER.readValue(p, PropIdBean.class);
        p.close();
        buf.close();

        assertNotNull(result);
        assertEquals("myId123", result.id);
        assertEquals("test", result.name);
    }

    // Native Object Id with @JsonCreator (property-based creator path)
    @Test
    public void testNativeObjectIdWithCreator() throws Exception
    {
        TokenBuffer buf = new TokenBuffer(null, true);
        buf.writeStartObject();
        buf.writeObjectId("creatorId"); // native id
        buf.writeName("id");
        buf.writeString("creatorId");
        buf.writeName("name");
        buf.writeString("created");
        buf.writeEndObject();

        JsonParser p = buf.asParser(ObjectReadContext.empty());
        CreatorIdBean result = MAPPER.readValue(p, CreatorIdBean.class);
        p.close();
        buf.close();

        assertNotNull(result);
        assertEquals("creatorId", result.id);
        assertEquals("created", result.name);
    }

    /*
    /**********************************************************
    /* Test methods: Native Type Id
    /**********************************************************
     */

    // Native Type Id with AS_PROPERTY -- type id written before START_OBJECT
    // so the type deserializer sees it on the START_OBJECT token
    @Test
    public void testNativeTypeIdAsProperty() throws Exception
    {
        TokenBuffer buf = new TokenBuffer(null, true);
        buf.writeTypeId("dog");     // pending, will attach to next token
        buf.writeStartObject();     // type id now on START_OBJECT
        buf.writeName("name");
        buf.writeString("Rex");
        buf.writeName("breed");
        buf.writeString("Labrador");
        buf.writeEndObject();

        JsonParser p = buf.asParser(ObjectReadContext.empty());
        Animal result = MAPPER.readValue(p, Animal.class);
        p.close();
        buf.close();

        assertNotNull(result);
        assertInstanceOf(Dog.class, result);
        Dog dog = (Dog) result;
        assertEquals("Rex", dog.name);
        assertEquals("Labrador", dog.breed);
    }

    // Native Type Id resolving to a different subtype
    @Test
    public void testNativeTypeIdCat() throws Exception
    {
        TokenBuffer buf = new TokenBuffer(null, true);
        buf.writeTypeId("cat");
        buf.writeStartObject();
        buf.writeName("name");
        buf.writeString("Whiskers");
        buf.writeName("lives");
        buf.writeNumber(9);
        buf.writeEndObject();

        JsonParser p = buf.asParser(ObjectReadContext.empty());
        Animal result = MAPPER.readValue(p, Animal.class);
        p.close();
        buf.close();

        assertNotNull(result);
        assertInstanceOf(Cat.class, result);
        Cat cat = (Cat) result;
        assertEquals("Whiskers", cat.name);
        assertEquals(9, cat.lives);
    }

    // Native Type Id in a nested property context
    @Test
    public void testNativeTypeIdInProperty() throws Exception
    {
        TokenBuffer buf = new TokenBuffer(null, true);
        buf.writeStartObject();
        buf.writeName("animal");
        buf.writeTypeId("cat");     // type id for the nested object
        buf.writeStartObject();
        buf.writeName("name");
        buf.writeString("Felix");
        buf.writeName("lives");
        buf.writeNumber(7);
        buf.writeEndObject();
        buf.writeEndObject();

        JsonParser p = buf.asParser(ObjectReadContext.empty());
        AnimalWrapper result = MAPPER.readValue(p, AnimalWrapper.class);
        p.close();
        buf.close();

        assertNotNull(result);
        assertNotNull(result.animal);
        assertInstanceOf(Cat.class, result.animal);
        assertEquals("Felix", result.animal.name);
        assertEquals(7, ((Cat) result.animal).lives);
    }

    /*
    /**********************************************************
    /* Test methods: Combined Object Id + Type Id
    /**********************************************************
     */

    // Both native Object Id and native Type Id on same object
    @Test
    public void testNativeObjectIdAndTypeIdCombined() throws Exception
    {
        TokenBuffer buf = new TokenBuffer(null, true);
        // Type id before START_OBJECT so type deserializer sees it
        buf.writeTypeId("sub");
        buf.writeStartObject();
        // Object id before first property so bean deserializer sees it
        buf.writeObjectId(1);
        buf.writeName("name");
        buf.writeString("combined");
        buf.writeName("extra");
        buf.writeNumber(7);
        buf.writeEndObject();

        JsonParser p = buf.asParser(ObjectReadContext.empty());
        TypedIdNode result = MAPPER.readValue(p, TypedIdNode.class);
        p.close();
        buf.close();

        assertNotNull(result);
        assertInstanceOf(TypedIdNodeSub.class, result);
        assertEquals("combined", result.name);
        assertEquals(7, ((TypedIdNodeSub) result).extra);
    }

    /*
    /**********************************************************
    /* Test methods: TokenBuffer parser native id API
    /**********************************************************
     */

    // Verify that TokenBuffer parser correctly reports native id capabilities
    @Test
    public void testTokenBufferParserNativeIdFlags() throws Exception
    {
        // With native ids enabled
        try (TokenBuffer buf = new TokenBuffer(null, true)) {
            buf.writeStartObject();
            buf.writeEndObject();
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertTrue(p.canReadObjectId());
                assertTrue(p.canReadTypeId());
            }
        }

        // Without native ids
        try (TokenBuffer buf = TokenBuffer.forGeneration()) {
            buf.writeStartObject();
            buf.writeEndObject();
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertFalse(p.canReadObjectId());
                assertFalse(p.canReadTypeId());
            }
        }
    }

    // Verify native ids are null when not set on a token
    @Test
    public void testTokenBufferParserNativeIdNullWhenNotSet() throws Exception
    {
        try (TokenBuffer buf = new TokenBuffer(null, true)) {
            buf.writeStartObject();
            buf.writeName("field");
            buf.writeString("value");
            buf.writeEndObject();

            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                p.nextToken(); // START_OBJECT
                assertNull(p.getObjectId());
                assertNull(p.getTypeId());
            }
        }
    }

    // Verify native ids survive round-trip through TokenBuffer serialization
    @Test
    public void testNativeIdRoundTripThroughTokenBuffer() throws Exception
    {
        TokenBuffer source = new TokenBuffer(null, true);
        source.writeTypeId("dog");
        source.writeStartObject();
        source.writeName("name");
        source.writeString("Fido");
        source.writeName("breed");
        source.writeString("Mutt");
        source.writeEndObject();

        // Serialize into another TokenBuffer
        TokenBuffer target = new TokenBuffer(null, true);
        source.serialize(target);

        // Read from target
        JsonParser p = target.asParser(ObjectReadContext.empty());
        Animal result = MAPPER.readValue(p, Animal.class);
        p.close();
        target.close();
        source.close();

        assertNotNull(result);
        assertInstanceOf(Dog.class, result);
        assertEquals("Fido", result.name);
        assertEquals("Mutt", ((Dog) result).breed);
    }
}
