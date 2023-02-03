package com.fasterxml.jackson.databind.jsontype;

import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.testutil.NoCheckSubTypeValidator;

public class PolymorphicViaRefTypeTest extends BaseMapTest
{

    @JsonSubTypes({
        @JsonSubTypes.Type(name = "impl5", value = ImplForAtomic.class)
    })
    static class BaseForAtomic {
    }

    static class ImplForAtomic extends BaseForAtomic {
        public int x;

        protected ImplForAtomic() { }
        public ImplForAtomic(int x) { this.x = x; }
    }

    static class TypeInfoAtomic {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "$type")
        public AtomicReference<BaseForAtomic> value;
    }

    static class AtomicStringWrapper {
        public AtomicReference<StringWrapper> wrapper;

        protected AtomicStringWrapper() { }
        public AtomicStringWrapper(String str) {
            wrapper = new AtomicReference<StringWrapper>(new StringWrapper(str));
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = objectMapper();

    public void testPolymorphicAtomicRefProperty() throws Exception
    {
        TypeInfoAtomic data = new TypeInfoAtomic();
        data.value = new AtomicReference<BaseForAtomic>(new ImplForAtomic(42));
        String json = MAPPER.writeValueAsString(data);
        TypeInfoAtomic result = MAPPER.readValue(json, TypeInfoAtomic.class);
        assertNotNull(result);
        BaseForAtomic value = result.value.get();
        assertNotNull(value);
        assertEquals(ImplForAtomic.class, value.getClass());
        assertEquals(42, ((ImplForAtomic) value).x);
    }

    public void testAtomicRefViaDefaultTyping() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(NoCheckSubTypeValidator.instance,
                        DefaultTyping.NON_FINAL)
                .build();
        AtomicStringWrapper data = new AtomicStringWrapper("foo");
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        AtomicStringWrapper result = mapper.readValue(json, AtomicStringWrapper.class);
        assertNotNull(result);
        assertNotNull(result.wrapper);
        assertEquals(AtomicReference.class, result.wrapper.getClass());
        StringWrapper w = result.wrapper.get();
        assertEquals("foo", w.str);
    }
}
