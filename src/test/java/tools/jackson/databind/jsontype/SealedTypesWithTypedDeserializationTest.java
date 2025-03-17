package tools.jackson.databind.jsontype;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import tools.jackson.core.JsonParser;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class SealedTypesWithTypedDeserializationTest
    extends DatabindTestUtil
{
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */

    /**
     * Polymorphic base class
     */
    @JsonTypeInfo(use=Id.CLASS, include=As.PROPERTY, property="@classy")
    static sealed abstract class Animal permits Dog, Cat, Fish, NullAnimal {
        public String name;

        protected Animal(String n)  { name = n; }
    }

    @JsonTypeName("doggie")
    static final class Dog extends Animal
    {
        public int boneCount;

        @JsonCreator
        public Dog(@JsonProperty("name") String name) {
            super(name);
        }

        public void setBoneCount(int i) { boneCount = i; }
    }

    @JsonTypeName("kitty")
    static final class Cat extends Animal
    {
        public String furColor;

        @JsonCreator
        public Cat(@JsonProperty("furColor") String c) {
            super(null);
            furColor = c;
        }

        public void setName(String n) { name = n; }
    }

    // Allow "empty" beans
    @JsonTypeName("fishy")
    static final class Fish extends Animal
    {
        @JsonCreator
        public Fish()
        {
            super(null);
        }
    }

    // [databind#2467]: Allow missing "content" for as-array deserialization
    @JsonDeserialize(using = NullAnimalDeserializer.class)
    static final class NullAnimal extends Animal
    {
        public static final NullAnimal NULL_INSTANCE = new NullAnimal();

        public NullAnimal() {
            super(null);
        }
    }

    static class NullAnimalDeserializer extends ValueDeserializer<NullAnimal> {
        @Override
        public NullAnimal getNullValue(final DeserializationContext context) {
            return NullAnimal.NULL_INSTANCE;
        }

        @Override
        public NullAnimal deserialize(final JsonParser parser, final DeserializationContext context) {
            throw new UnsupportedOperationException();
        }
    }

    static class AnimalContainer {
        public Animal animal;
    }

    // base class with no useful info
    @JsonTypeInfo(use=Id.CLASS, include=As.WRAPPER_ARRAY)
    static abstract sealed class DummyBase  permits DummyImpl {
        protected DummyBase(boolean foo) { }
    }

    static final class DummyImpl extends DummyBase {
        public int x;

        public DummyImpl() { super(true); }
    }

    @JsonTypeInfo(use=Id.MINIMAL_CLASS, include=As.WRAPPER_OBJECT)
    interface TypeWithWrapper { }

    @JsonTypeInfo(use=Id.CLASS, include=As.WRAPPER_ARRAY)
    interface TypeWithArray { }

    static class Issue506DateBean {
        @JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type2")
        public Date date;
    }

    static class Issue506NumberBean
    {
        @JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type3")
        @JsonSubTypes({ @Type(Long.class),
            @Type(Integer.class) })
        public Number number;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_ARRAY)
    static sealed interface Issue1751ArrBase permits Issue1751ArrImpl { }

    @JsonTypeName("0")
    static final class Issue1751ArrImpl implements Issue1751ArrBase { }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY,
            property = "type")
    @JsonSubTypes({ @Type(value = Issue1751PropImpl.class, name = "1") })
    static interface Issue1751PropBase { }

    static class Issue1751PropImpl implements Issue1751PropBase { }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    /**
     * First things first, let's ensure we can serialize using
     * class name, written as main-level property name
     */
    @Test
    public void testSimpleClassAsProperty() throws Exception
    {
        Animal a = MAPPER.readValue(asJSONObjectValueString(MAPPER,
                "@classy", Cat.class.getName(),
                "furColor", "tabby", "name", "Garfield"), Animal.class);
        assertNotNull(a);
        assertEquals(Cat.class, a.getClass());
        Cat c = (Cat) a;
        assertEquals("Garfield", c.name);
        assertEquals("tabby", c.furColor);
    }

    // Test inclusion using wrapper style
    @Test
    public void testTypeAsWrapper() throws Exception
    {
        ObjectMapper m = jsonMapperBuilder()
                .addMixIn(Animal.class, TypeWithWrapper.class)
                .build();
        String JSON = "{\".SealedTypesWithTypedDeserializationTest$Dog\" : "
            +asJSONObjectValueString(m, "name", "Scooby", "boneCount", "6")+" }";
        Animal a = m.readValue(JSON, Animal.class);
        assertTrue(a instanceof Animal);
        assertEquals(Dog.class, a.getClass());
        Dog d = (Dog) a;
        assertEquals("Scooby", d.name);
        assertEquals(6, d.boneCount);
    }

    // Test inclusion using 2-element array
    @Test
    public void testTypeAsArray() throws Exception
    {
        ObjectMapper m = jsonMapperBuilder()
                .addMixIn(Animal.class, TypeWithArray.class)
                .build();
        // hmmh. Not good idea to rely on exact output, order may change. But...
        String JSON = "[\""+Dog.class.getName()+"\", "
            +asJSONObjectValueString(m, "name", "Martti", "boneCount", "11")+" ]";
        Animal a = m.readValue(JSON, Animal.class);
        assertEquals(Dog.class, a.getClass());
        Dog d = (Dog) a;
        assertEquals("Martti", d.name);
        assertEquals(11, d.boneCount);
    }

    // Use basic Animal as contents of a regular List
    @Test
    public void testListAsArray() throws Exception
    {
        // This time using PROPERTY style (default) again
        String JSON = "[\n"
                +asJSONObjectValueString(MAPPER,
                        "@classy", Cat.class.getName(), "name", "Hello", "furColor", "white")
                +",\n"
                // let's shuffle doggy's fields a bit for testing
                +asJSONObjectValueString(MAPPER,
                        "boneCount", Integer.valueOf(1),
                        "@classy", Dog.class.getName(),
                        "name", "Bob"
                        )
                +",\n"
                +asJSONObjectValueString(MAPPER,
                        "@classy", Fish.class.getName())
                +", null\n]";

        JavaType expType = defaultTypeFactory().constructCollectionType(ArrayList.class, Animal.class);
        List<Animal> animals = MAPPER.readValue(JSON, expType);
        assertNotNull(animals);
        assertEquals(4, animals.size());
        Cat c = (Cat) animals.get(0);
        assertEquals("Hello", c.name);
        assertEquals("white", c.furColor);
        Dog d = (Dog) animals.get(1);
        assertEquals("Bob", d.name);
        assertEquals(1, d.boneCount);
        Fish f = (Fish) animals.get(2);
        assertNotNull(f);
        assertNull(animals.get(3));
    }

    @Test
    public void testCagedAnimal() throws Exception
    {
        String jsonCat = asJSONObjectValueString(MAPPER,
                "@classy", Cat.class.getName(), "name", "Nilson", "furColor", "black");
        String JSON = "{\"animal\":"+jsonCat+"}";

        AnimalContainer cont = MAPPER.readValue(JSON, AnimalContainer.class);
        assertNotNull(cont);
        Animal a = cont.animal;
        assertNotNull(a);
        Cat c = (Cat) a;
        assertEquals("Nilson", c.name);
        assertEquals("black", c.furColor);
    }

    /**
     * Test that verifies that there are few limitations on polymorphic
     * base class.
     */
    @Test
    public void testAbstractEmptyBaseClass() throws Exception
    {
        DummyBase result = MAPPER.readValue(
                "[\""+DummyImpl.class.getName()+"\",{\"x\":3}]", DummyBase.class);
        assertNotNull(result);
        assertEquals(DummyImpl.class, result.getClass());
        assertEquals(3, ((DummyImpl) result).x);
    }

    // [JACKSON-506], wrt Date
    @Test
    public void testIssue506WithDate() throws Exception
    {
        Issue506DateBean input = new Issue506DateBean();
        input.date = new Date(1234L);

        String json = MAPPER.writeValueAsString(input);

        Issue506DateBean output = MAPPER.readValue(json, Issue506DateBean.class);
        assertEquals(input.date, output.date);
    }

    // [JACKSON-506], wrt Number
    @Test
    public void testIssue506WithNumber() throws Exception
    {
        Issue506NumberBean input = new Issue506NumberBean();
        input.number = Long.valueOf(4567L);

        String json = MAPPER.writeValueAsString(input);

        Issue506NumberBean output = MAPPER.readValue(json, Issue506NumberBean.class);
        assertEquals(input.number, output.number);
    }

    private String asJSONObjectValueString(ObjectMapper mapper, Object... args)
    {
        LinkedHashMap<Object,Object> map = new LinkedHashMap<Object,Object>();
        for (int i = 0, len = args.length; i < len; i += 2) {
            map.put(args[i], args[i+1]);
        }
        return mapper.writeValueAsString(map);
    }

    // [databind#1751]: allow ints as ids too
    @Test
    public void testIntAsTypeId1751Array() throws Exception
    {
        Issue1751ArrBase value;

        // Should allow both String and Int:
        value = MAPPER.readValue("[0, { }]", Issue1751ArrBase.class);
        assertNotNull(value);
        assertEquals(Issue1751ArrImpl.class, value.getClass());

        value = MAPPER.readValue("[\"0\", { }]", Issue1751ArrBase.class);
        assertNotNull(value);
        assertEquals(Issue1751ArrImpl.class, value.getClass());
    }

    // [databind#1751]: allow ints as ids too
    @Test
    public void testIntAsTypeId1751Prop() throws Exception
    {
        Issue1751PropBase value;

        // Should allow both String and Int:
        value = MAPPER.readValue("{\"type\" : \"1\"}", Issue1751PropBase.class);
        assertNotNull(value);
        assertEquals(Issue1751PropImpl.class, value.getClass());

        value = MAPPER.readValue("{\"type\" : 1}", Issue1751PropBase.class);
        assertNotNull(value);
        assertEquals(Issue1751PropImpl.class, value.getClass());
    }

    // [databind#2467]: Allow missing "content" for as-array deserialization
    @Test
    public void testTypeAsArrayWithNullableType() throws Exception
    {
        ObjectMapper m = jsonMapperBuilder()
                .addMixIn(Animal.class, TypeWithArray.class)
                .build();
        Animal a = m.readValue(
                "[\""+Fish.class.getName()+"\"]", Animal.class);
        assertNull(a);
    }

    // [databind#2467]
    @Test
    public void testTypeAsArrayWithCustomDeserializer() throws Exception
    {
        ObjectMapper m = jsonMapperBuilder()
                .addMixIn(Animal.class, TypeWithArray.class)
                .build();
        Animal a = m.readValue(
                "[\""+NullAnimal.class.getName()+"\"]", Animal.class);
        assertNotNull(a);
        assertEquals(NullAnimal.class, a.getClass());
        NullAnimal c = (NullAnimal) a;
        assertNull(c.name);
    }
}
