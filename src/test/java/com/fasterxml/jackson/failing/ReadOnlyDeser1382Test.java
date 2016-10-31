package com.fasterxml.jackson.failing;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.*;

public class ReadOnlyDeser1382Test extends BaseMapTest
{
    static class Foo {
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        private List<Long> list = new ArrayList<>();

        List<Long> getList() {
            return list;
        }

        public Foo setList(List<Long> list) {
            this.list = list;
            return this;
        }
    }

    public void testReadOnly() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        String payload = "{\"list\":[1,2,3,4]}";
        Foo foo = mapper.readValue(payload, Foo.class);
        assertTrue("List should be empty", foo.getList().isEmpty());
    }
}
