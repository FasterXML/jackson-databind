package com.fasterxml.jackson.databind.jsontype;

import java.util.List;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class AbstractTypeMapping1186Test extends BaseMapTest
{
    public interface IContainer<T> {
        @JsonProperty("ts")
        List<T> getTs();
    }

    static class MyContainer<T> implements IContainer<T> {

        final List<T> ts;

        @JsonCreator
        public MyContainer(@JsonProperty("ts") List<T> ts) {
            this.ts = ts;
        }

        @Override
        public List<T> getTs() {
            return ts;
        }
    }

    public static class MyObject {
        public String msg;
    }

    public void testDeserializeMyContainer() throws Exception {
        SimpleModule module = new SimpleModule().addAbstractTypeMapping(IContainer.class, MyContainer.class);
        final ObjectMapper mapper = new ObjectMapper().registerModule(module);
        String json = "{\"ts\": [ { \"msg\": \"hello\"} ] }";
        final Object o = mapper.readValue(json,
                mapper.getTypeFactory().constructParametricType(IContainer.class, MyObject.class));
        assertEquals(MyContainer.class, o.getClass());
        MyContainer<?> myc = (MyContainer<?>) o;
        assertEquals(1, myc.ts.size());
        Object value = myc.ts.get(0);
        assertEquals(MyObject.class, value.getClass());
    }
}
