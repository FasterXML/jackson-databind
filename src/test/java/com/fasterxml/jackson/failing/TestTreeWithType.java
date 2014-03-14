package com.fasterxml.jackson.failing;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class TestTreeWithType extends BaseMapTest
{

    // [Issue#353]
    public class SavedCookie {
        public String name, value;

        public SavedCookie() { }
        public SavedCookie(String n, String v) {
            name = n;
            value = v;
        }
    }

    public class SavedCookieDeserializer extends JsonDeserializer<SavedCookie> {
        @Override
        public SavedCookie deserialize(JsonParser jsonParser, DeserializationContext ctxt)
                throws IOException {
           ObjectCodec oc = jsonParser.getCodec();
           JsonNode node = oc.readTree(jsonParser);
           return new SavedCookie(node.path("name").textValue(),
                   node.path("value").textValue());
        }
     }    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testIssue353() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();

        mapper.enableDefaultTypingAsProperty(ObjectMapper.DefaultTyping.NON_FINAL, "@class");

         SimpleModule testModule = new SimpleModule("MyModule", new Version(1, 0, 0, null, "TEST", "TEST"));
         testModule.addDeserializer(SavedCookie.class, new SavedCookieDeserializer());
         mapper.registerModule(testModule);

         SavedCookie savedCookie = new SavedCookie("key", "v");
         String json = mapper.writeValueAsString(savedCookie);
         
         SavedCookie out = mapper.reader(SavedCookie.class).readValue(json);

         assertEquals("key", out.name);
         assertEquals("v", out.value);
    }
}
