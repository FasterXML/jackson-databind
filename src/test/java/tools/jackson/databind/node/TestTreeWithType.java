package tools.jackson.databind.node;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.testutil.NoCheckSubTypeValidator;

public class TestTreeWithType extends BaseMapTest
{
    public static class Foo {
        public String bar;

        public Foo() { }

        public Foo(String bar) {
            this.bar = bar;
        }
    }

    // [databind#353]
    public class SavedCookie {
        public String name, value;

        public SavedCookie() { }
        public SavedCookie(String n, String v) {
            name = n;
            value = v;
        }
    }

    public class SavedCookieDeserializer extends ValueDeserializer<SavedCookie> {
        @Override
        public SavedCookie deserialize(JsonParser jsonParser, DeserializationContext ctxt)
        {
           ObjectReadContext oc = jsonParser.objectReadContext();
           JsonNode node = oc.readTree(jsonParser);
           return new SavedCookie(node.path("name").textValue(),
                   node.path("value").textValue());
        }

        @Override
        public SavedCookie deserializeWithType(JsonParser jp, DeserializationContext ctxt,
                TypeDeserializer typeDeserializer)
        {
            return (SavedCookie) typeDeserializer.deserializeTypedFromObject(jp, ctxt);
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testValueAsStringWithoutDefaultTyping() throws Exception {

        Foo foo = new Foo("baz");
        String json = MAPPER.writeValueAsString(foo);

        JsonNode jsonNode = MAPPER.readTree(json);
        assertEquals(jsonNode.get("bar").textValue(), foo.bar);
    }

    public void testValueAsStringWithDefaultTyping() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(NoCheckSubTypeValidator.instance,
                        DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
                .build();
        Foo foo = new Foo("baz");
        String json = mapper.writeValueAsString(foo);

        JsonNode jsonNode = mapper.readTree(json);
        assertEquals(jsonNode.get("bar").textValue(), foo.bar);
    }

    public void testReadTreeWithDefaultTyping() throws Exception
    {
        final String CLASS = Foo.class.getName();

        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(NoCheckSubTypeValidator.instance,
                        DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
                .build();
        String json = "{\"@class\":\""+CLASS+"\",\"bar\":\"baz\"}";
        JsonNode jsonNode = mapper.readTree(json);
        assertEquals(jsonNode.get("bar").textValue(), "baz");
    }

    public void testValueToTreeWithoutDefaultTyping() throws Exception {

        Foo foo = new Foo("baz");
        JsonNode jsonNode = MAPPER.valueToTree(foo);
        assertEquals(jsonNode.get("bar").textValue(), foo.bar);
    }

    public void testValueToTreeWithDefaultTyping() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(NoCheckSubTypeValidator.instance,
                        DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
                .build();
        Foo foo = new Foo("baz");
        JsonNode jsonNode = mapper.valueToTree(foo);
        assertEquals(jsonNode.get("bar").textValue(), foo.bar);
    }

    public void testIssue353() throws Exception
    {
        SimpleModule testModule = new SimpleModule("MyModule", new Version(1, 0, 0, null, "TEST", "TEST"));
        testModule.addDeserializer(SavedCookie.class, new SavedCookieDeserializer());
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTypingAsProperty(NoCheckSubTypeValidator.instance,
                        DefaultTyping.NON_FINAL, "@class")
                .addModule(testModule)
                .build();
        SavedCookie savedCookie = new SavedCookie("key", "v");
        String json = mapper.writeValueAsString(savedCookie);
        SavedCookie out = mapper.readerFor(SavedCookie.class).readValue(json);

        assertEquals("key", out.name);
        assertEquals("v", out.value);
    }
}
