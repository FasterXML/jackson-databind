package tools.jackson.databind.jsontype.ext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonTypeIdResolver;
import tools.jackson.databind.jsontype.TypeIdResolver;
import tools.jackson.databind.jsontype.impl.TypeIdResolverBase;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.NoCheckSubTypeValidator;

import static org.junit.jupiter.api.Assertions.*;

public class ExternalTypeIdWithCreatorTest extends DatabindTestUtil
{
    // [databind#999]

    public static interface Payload999 { }

    @JsonTypeName("foo")
    public static class FooPayload999 implements Payload999 { }

    @JsonTypeName("bar")
    public static class BarPayload999 implements Payload999 { }

    public static class Message<P extends Payload999>
    {
        final String type;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                visible = true,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
        @JsonSubTypes({
                @JsonSubTypes.Type(FooPayload999.class),
                @JsonSubTypes.Type(BarPayload999.class) })
        final P payload;

        @JsonCreator
        public Message(@JsonProperty("type") String type,
                @JsonProperty("payload") P payload)
        {
            this.type = type;
            this.payload = payload;
        }
    }

    // [databind#1198]

    public enum Attacks { KICK, PUNCH }

    static class Character {
        public String name;
        public Attacks preferredAttack;

        @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, defaultImpl=Kick.class,
                include=JsonTypeInfo.As.EXTERNAL_PROPERTY, property="preferredAttack")
        @JsonSubTypes({
            @JsonSubTypes.Type(value=Kick.class, name="KICK"),
            @JsonSubTypes.Type(value=Punch.class, name="PUNCH")
        })
        public Attack attack;
    }

    public static abstract class Attack {
        public String side;

        protected Attack(String side) {
            this.side = side;
        }
    }

    public static class Kick extends Attack {
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public Kick(String side) {
            super(side);
        }
    }

    public static class Punch extends Attack {
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public Punch(String side) {
            super(side);
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#999]
    @Test
    public void testExternalTypeId() throws Exception
    {
        TypeReference<Message<FooPayload999>> type = new TypeReference<Message<FooPayload999>>() { };

        Message<?> msg = MAPPER.readValue(a2q("{ 'type':'foo', 'payload': {} }"), type);
        assertNotNull(msg);
        assertNotNull(msg.payload);
        assertEquals("foo", msg.type);

        // and then with different order
        msg = MAPPER.readValue(a2q("{'payload': {}, 'type':'foo' }"), type);
        assertNotNull(msg);
        assertNotNull(msg.payload);
        assertEquals("foo", msg.type);
    }

    // [databind#1198]
    @Test
    public void test1198Fails() throws Exception {
        String json = "{ \"name\": \"foo\", \"attack\":\"right\" }";

        Character character = MAPPER.readValue(json, Character.class);

        assertNotNull(character);
        assertNotNull(character.attack);
        assertEquals("foo", character.name);
    }

    // [databind#1198]
    @Test
    public void test1198Works() throws Exception {
        String json = "{ \"name\": \"foo\", \"preferredAttack\": \"KICK\", \"attack\":\"right\" }";

        Character character = MAPPER.readValue(json, Character.class);

        assertNotNull(character);
        assertNotNull(character.attack);
        assertEquals("foo", character.name);
    }

    // [databind#1328]
    public interface Animal1328 { }

    public static class Dog1328 implements Animal1328 {
        public String dogStuff;
    }

    public enum AnimalType1328 {
        Dog;
    }

    public static class AnimalAndType1328 {
        public AnimalType1328 type;

        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
            include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
            property = "type")
        @JsonTypeIdResolver(AnimalResolver1328.class)
        private Animal1328 animal;

        public AnimalAndType1328() { }

        @java.beans.ConstructorProperties({"type", "animal"})
        public AnimalAndType1328(final AnimalType1328 type, final Animal1328 animal) {
            this.type = type;
            this.animal = animal;
        }
    }

    static class AnimalResolver1328 implements TypeIdResolver {
        @Override
        public void init(JavaType bt) { }

        @Override
        public String idFromValue(DatabindContext ctxt, Object value) {
            return null;
        }

        @Override
        public String idFromValueAndType(DatabindContext ctxt, Object value, Class<?> suggestedType) {
            return null;
        }

        @Override
        public String idFromBaseType(DatabindContext ctxt) {
            throw new UnsupportedOperationException("Missing action type information - Cannot construct");
        }

        @Override
        public JavaType typeFromId(DatabindContext context, String id) {
            if (AnimalType1328.Dog.toString().equals(id)) {
                return context.constructType(Dog1328.class);
            }
            throw new IllegalArgumentException("What is a " + id);
        }

        @Override
        public String getDescForKnownTypeIds() {
            return null;
        }

        @Override
        public JsonTypeInfo.Id getMechanism() {
            return JsonTypeInfo.Id.CUSTOM;
        }
    }

    // [databind#3045]
    public static class ChildBaseByParentTypeResolver3045 extends TypeIdResolverBase {
        private static final long serialVersionUID = 1L;

        public ChildBaseByParentTypeResolver3045() { }

        private JavaType superType;

        @Override
        public void init(JavaType baseType) {
             superType = baseType;
        }

        @Override
        public JsonTypeInfo.Id getMechanism() {
             return JsonTypeInfo.Id.NAME;
        }

        @Override
        public JavaType typeFromId(DatabindContext context, String id) {
             switch (id) {
             case "track":
                 return context.constructSpecializedType(superType, MyData3045.class);
             }
             throw new IllegalArgumentException("No type with id '"+id+"'");
        }

        @Override
        public String idFromValue(DatabindContext ctxt, Object value) {
             return null;
        }

        @Override
        public String idFromValueAndType(DatabindContext ctxt, Object value, Class<?> suggestedType) {
             return null;
        }
    }

    static class MyData3045
    {
        @JsonAnySetter
        public HashMap<String,Object> data = new HashMap<>();

        public int size() { return data.size(); }
        public Object find(String key) { return data.get(key); }

        @Override
        public String toString() {
            return String.valueOf(data);
        }
    }

    public static class MyJson3045 {
       public final long time;
       public String type;
       public Object data;

       @JsonCreator
       public MyJson3045(@JsonProperty("time") long t) {
           time = t;
       }

       @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
               property = "type", visible = true)
       @JsonTypeIdResolver(ChildBaseByParentTypeResolver3045.class)
       public void setData(Object data) {
           this.data = data;
       }

       @Override
       public String toString() {
           return "[time="+time+", type="+type+", data="+data+"]";
       }
    }

    // [databind#1328]
    @Test
    public void testExternalTypeIdWithEnum1328() throws Exception {
        String json = MAPPER.writerWithDefaultPrettyPrinter()
                .writeValueAsString(Arrays.asList(new AnimalAndType1328(AnimalType1328.Dog, new Dog1328())));
        List<AnimalAndType1328> list = MAPPER.readerFor(new TypeReference<List<AnimalAndType1328>>() { })
            .readValue(json);
        assertNotNull(list);
    }

    // [databind#3045]
    @Test
    public void testExternalIdWithAnySetter3045() throws Exception
    {
        ObjectMapper mapper3045 = jsonMapperBuilder()
                .polymorphicTypeValidator(NoCheckSubTypeValidator.instance)
                .build();
        // First cases where the last Creator argument comes last:
        _testExternalIdWithAnySetter3045(mapper3045, a2q(
                "{'type':'track','data':{'data-internal':'toto'},'time':345}"));
        _testExternalIdWithAnySetter3045(mapper3045, a2q(
                "{'data':{'data-internal':'toto'},'type':'track', 'time':345}"));

        // then a case where it comes in the middle
        _testExternalIdWithAnySetter3045(mapper3045, a2q(
                "{'data':{'data-internal':'toto'},'time':345, 'type':'track'}"));

        // and then finally one where we'll start with it
        _testExternalIdWithAnySetter3045(mapper3045, a2q(
                "{'time':345, 'type':'track', 'data':{'data-internal':'toto'}}"));
    }

    private void _testExternalIdWithAnySetter3045(ObjectMapper mapper, String input) throws Exception
    {
        MyJson3045 result = mapper.readValue(input, MyJson3045.class);

        assertEquals(345, result.time);
        assertNotNull(result.data, "Expected non-null data; result object = "+result);
        assertEquals("track", result.type);
        assertEquals(MyData3045.class, result.data.getClass());
        MyData3045 data = (MyData3045) result.data;
        assertEquals(1, data.size());
        assertEquals("toto", data.find("data-internal"));
    }
}
