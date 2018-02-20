package com.fasterxml.jackson.databind.mixins;

import java.io.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

public class TestMixinDeserForCreators
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper bean classes
    /**********************************************************
     */

    static class BaseClass
    {
        protected String _a;

        public BaseClass(String a) {
            _a = a+"...";
        }

        private BaseClass(String value, boolean dummy) {
            _a = value;
        }

        public static BaseClass myFactory(String a) {
            return new BaseClass(a+"X", true);
        }
    }

    static class BaseClassWithPrivateCtor
    {
        protected String _a;
        private BaseClassWithPrivateCtor(String a) {
            _a = a+"...";
        }

    }

    /**
     * Mix-in class that will effectively suppresses String constructor,
     * and marks a non-auto-detectable static method as factory method
     * as a creator.
     *<p>
     * Note that method implementations are not used for anything; but
     * we have to a class: interface won't do, as they can't have
     * constructors or static methods.
     */
    static class MixIn
    {
        @JsonIgnore protected MixIn(String s) { }

        @JsonCreator
        static BaseClass myFactory(String a) { return null; }
    }

    static class MixInForPrivate
    {
        @JsonCreator MixInForPrivate(String s) { }
    }

    static class StringWrapper {
        String _value;
        private StringWrapper(String s, boolean foo) { _value = s; }

        @SuppressWarnings("unused")
		private static StringWrapper create(String str) {
            return new StringWrapper(str, false);
        }
    }

    abstract static class StringWrapperMixIn {
        @JsonCreator static StringWrapper create(String str) { return null; }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testForConstructor() throws IOException
    {
        ObjectMapper mapper = ObjectMapper.builder()
                .addMixIn(BaseClassWithPrivateCtor.class, MixInForPrivate.class)
                .build();
        BaseClassWithPrivateCtor result = mapper.readValue("\"?\"", BaseClassWithPrivateCtor.class);
        assertEquals("?...", result._a);
    }

    public void testForFactoryAndCtor() throws IOException
    {
        BaseClass result;

        // First: test default behavior: should use constructor
        result = new ObjectMapper().readValue("\"string\"", BaseClass.class);
        assertEquals("string...", result._a);

        // Then with simple mix-in: should change to use the factory method
        ObjectMapper mapper = ObjectMapper.builder()
                .addMixIn(BaseClass.class, MixIn.class)
                .build();
        result = mapper.readValue("\"string\"", BaseClass.class);
        assertEquals("stringX", result._a);
    }

    public void testFactoryMixIn() throws IOException
    {
        ObjectMapper mapper = ObjectMapper.builder()
                .addMixIn(StringWrapper.class, StringWrapperMixIn.class)
                .build();
        StringWrapper result = mapper.readValue("\"a\"", StringWrapper.class);
        assertEquals("a", result._value);
    }
}
