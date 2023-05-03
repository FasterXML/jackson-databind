package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public class Issue1012Test extends BaseMapTest {

    public void testTextBufferOverflow() throws JsonProcessingException {
        ObjectMapper mapper = objectMapper();
        final int fooLen = 5_000_000;
        final int barLen = 10_000_000;
        final MyData myData = new MyData();
        myData.foo = new ArrayList<>(fooLen);
        myData.bar = new ArrayList<>(barLen);
        for (int i = 0; i < fooLen; i++) {
            Item item = new Item();
            item.id = UUID.randomUUID().toString();
            item.value = UUID.randomUUID().toString();
            myData.foo.add(item);
        }
        for (int i = 0; i < barLen; i++) {
            Item item = new Item();
            item.id = String.format("foo=%s,bar=%s,barbar=%s", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
            item.value = UUID.randomUUID().toString();
            myData.bar.add(item);
        }
        String json = mapper.writeValueAsString(myData);
        System.out.println("len=" + json.length());
    }

    static class Item {
        @JsonProperty("id") String id;
        @JsonProperty("value") String value;
    }

    static class MyData {
        @JsonProperty("foo") Collection<Item> foo;
        @JsonProperty("bar") Collection<Item> bar;
    }
}
