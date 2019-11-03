package com.fasterxml.jackson.databind.jsontype.vld;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.exc.InsecureTypeMatchException;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

/**
 * Tests for the main user-configurable {@code PolymorphicTypeValidator},
 * {@link BasicPolymorphicTypeValidator}.
 */
public class BasicPTVTest extends BaseMapTest
{
    // // // Value types

    static abstract class BaseValue {
        public int x = 3;
    }

    static class ValueA extends BaseValue {
        protected ValueA() { }
        public ValueA(int x) {
            super();
            this.x = x;
        }
    }

    static class ValueB extends BaseValue {
        protected ValueB() { }
        public ValueB(int x) {
            super();
            this.x = x;
        }
    }

    static class ValueC extends ValueB implements Serializable {}
    static class ValueD extends Object {}


    // // // Value types

    // make this type `final` to avoid polymorphic handling
    static final class BaseValueWrapper {
        public BaseValue value;

        protected BaseValueWrapper() { }

        public static BaseValueWrapper withA(int x) {
            BaseValueWrapper w = new BaseValueWrapper();
            w.value = new ValueA(x);
            return w;
        }

        public static BaseValueWrapper withB(int x) {
            BaseValueWrapper w = new BaseValueWrapper();
            w.value = new ValueB(x);
            return w;
        }
    }

    static final class ObjectWrapper {
        public Object value;

        protected ObjectWrapper() { }
        public ObjectWrapper(Object v) { value = v; }
    }

    static final class NumberWrapper {
        public Number value;

        protected NumberWrapper() { }
        public NumberWrapper(Number v) { value = v; }
    }
    
    /*
    /**********************************************************************
    /* Test methods: by base type, pass
    /**********************************************************************
     */

    // First: test simple Base-type-as-class allowing
    public void testAllowByBaseClass() throws Exception {
        final PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(BaseValue.class)
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
                .build();        

        // First, test accepted case
        final String json = mapper.writeValueAsString(BaseValueWrapper.withA(42));
        BaseValueWrapper w = mapper.readValue(json, BaseValueWrapper.class);
        assertEquals(42, w.value.x);

        // then non-accepted
        final String json2 = mapper.writeValueAsString(new NumberWrapper(Byte.valueOf((byte) 4)));
        try {
            mapper.readValue(json2, NumberWrapper.class);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "Could not resolve type id 'java.lang.Byte'");
            verifyException(e, "as a subtype of");
        }

        // and then yet again accepted one with different config
        ObjectMapper mapper2 = jsonMapperBuilder()
                .activateDefaultTyping(BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Number.class)
                        .build(), DefaultTyping.NON_FINAL)
                .build();
        NumberWrapper nw = mapper2.readValue(json2, NumberWrapper.class);
        assertNotNull(nw);
        assertEquals(Byte.valueOf((byte) 4), nw.value);
    }

    // Then subtype-prefix
    public void testAllowByBaseClassPrefix() throws Exception {
        final PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType("com.fasterxml.")
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
                .build();        

        // First, test accepted case
        final String json = mapper.writeValueAsString(BaseValueWrapper.withA(42));
        BaseValueWrapper w = mapper.readValue(json, BaseValueWrapper.class);
        assertEquals(42, w.value.x);

        // then non-accepted
        final String json2 = mapper.writeValueAsString(new NumberWrapper(Byte.valueOf((byte) 4)));
        try {
            mapper.readValue(json2, NumberWrapper.class);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "Could not resolve type id 'java.lang.Byte'");
            verifyException(e, "as a subtype of");
        }
    }

    // Then subtype-pattern
    public void testAllowByBaseClassPattern() throws Exception {
        final PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Pattern.compile("\\w+\\.fasterxml\\..+"))
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
                .build();        

        // First, test accepted case
        final String json = mapper.writeValueAsString(BaseValueWrapper.withA(42));
        BaseValueWrapper w = mapper.readValue(json, BaseValueWrapper.class);
        assertEquals(42, w.value.x);

        // then non-accepted
        final String json2 = mapper.writeValueAsString(new NumberWrapper(Byte.valueOf((byte) 4)));
        try {
            mapper.readValue(json2, NumberWrapper.class);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "Could not resolve type id 'java.lang.Byte'");
            verifyException(e, "as a subtype of");
        }
    }

    // And finally, block by specific direct-match base type
    public void testDenyByBaseClass() throws Exception {
        final PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                // indicate that all subtypes `BaseValue` would be fine
                .allowIfBaseType(BaseValue.class)
                // but that nominal base type MUST NOT be `Object.class`
                .denyForExactBaseType(Object.class)
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
                .build();        
        final String json = mapper.writeValueAsString(new ObjectWrapper(new ValueA(15)));
        try {
            mapper.readValue(json, ObjectWrapper.class);
            fail("Should not pass");

        // NOTE: different exception type since denial was for whole property, not just specific values
        } catch (InvalidDefinitionException e) {
            verifyException(e, "denied resolution of all subtypes of base type `java.lang.Object`");
        }
    }

    /*
    /**********************************************************************
    /* Test methods: by sub type
    /**********************************************************************
     */

    public void testAllowBySubClass() throws Exception {
        final PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(ValueB.class)
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
                .build();        

        // First, test accepted case
        final String json = mapper.writeValueAsString(BaseValueWrapper.withB(42));
        BaseValueWrapper w = mapper.readValue(json, BaseValueWrapper.class);
        assertEquals(42, w.value.x);

        // then non-accepted
        try {
            mapper.readValue(mapper.writeValueAsString(BaseValueWrapper.withA(43)),
                    BaseValueWrapper.class);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "Could not resolve type id 'com.fasterxml.jackson.");
            verifyException(e, "as a subtype of");
        }
    }

    public void testAllowBySubClassPrefix() throws Exception {
        final PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(ValueB.class.getName())
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
                .build();        

        // First, test accepted case
        final String json = mapper.writeValueAsString(BaseValueWrapper.withB(42));
        BaseValueWrapper w = mapper.readValue(json, BaseValueWrapper.class);
        assertEquals(42, w.value.x);

        // then non-accepted
        try {
            mapper.readValue(mapper.writeValueAsString(BaseValueWrapper.withA(43)),
                    BaseValueWrapper.class);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "Could not resolve type id 'com.fasterxml.jackson.");
            verifyException(e, "as a subtype of");
        }
    }

    public void testAllowBySubClassPattern() throws Exception {
        final PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Pattern.compile(Pattern.quote(ValueB.class.getName())))
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
                .build();        

        // First, test accepted case
        final String json = mapper.writeValueAsString(BaseValueWrapper.withB(42));
        BaseValueWrapper w = mapper.readValue(json, BaseValueWrapper.class);
        assertEquals(42, w.value.x);

        // then non-accepted
        try {
            mapper.readValue(mapper.writeValueAsString(BaseValueWrapper.withA(43)),
                    BaseValueWrapper.class);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "Could not resolve type id 'com.fasterxml.jackson.");
            verifyException(e, "as a subtype of");
        }
    }


    public void testBlockSerializableClass() throws Exception {
        try {
            BasicPolymorphicTypeValidator.builder()
                    .allowIfSubType(Serializable.class)
                    .build();
            fail("no exception was thrown when allowIfSubType Serializable.class");
        } catch (InsecureTypeMatchException tme) {
            assertEquals("Insecure Base class found : java.io.Serializable",tme.getMessage());
        }
    }

    public void testBlockObjectClass() throws Exception {
        try {
            BasicPolymorphicTypeValidator.builder()
                    .allowIfSubType(Object.class)
                    .build();
            fail("no exception was thrown when allowIfSubType Object.class");
        } catch (InsecureTypeMatchException tme) {
            assertEquals("Insecure Base class found : java.io.Serializable",tme.getMessage());
        }
    }



    public void testBlockBaseTypeMatchSerializable() throws Exception {
        try {
            BasicPolymorphicTypeValidator.builder()
                    .allowIfBaseType(Object.class)
                    .build();
            fail("no exception was thrown when allowIfSubType Object.class");
        } catch (InsecureTypeMatchException tme) {
            assertEquals("Insecure Base class found : java.io.Serializable",tme.getMessage());
        }
    }

    public void testBlockBaseTypeMatchObject() throws Exception {
        try {
            BasicPolymorphicTypeValidator.builder()
                    .allowIfBaseType(Object.class)
                    .build();
            fail("no exception was thrown when allowIfSubType Object.class");
        } catch (InsecureTypeMatchException tme) {
            assertEquals("Insecure Base class found : java.io.Serializable",tme.getMessage());
        }
    }

    public void testBlockSubTypeMatchObject() throws Exception {
        try {
            BasicPolymorphicTypeValidator.builder()
                    .allowIfSubType(Object.class.getName())
                    .build();
            fail("no exception was thrown when allowIfSubType Object.class");
        } catch (InsecureTypeMatchException tme) {
            assertEquals("Insecure Base class found : java.lang.Object",tme.getMessage());
        }
    }



    public void testBlockSubTypeMatchSerializable() throws Exception {
        try {
            BasicPolymorphicTypeValidator.builder()
                    .allowIfSubType(Serializable.class.getName())
                    .build();
            fail("no exception was thrown when allowIfSubType Object.class");
        } catch (InsecureTypeMatchException tme) {
            assertEquals("Insecure Base class found : java.io.Serializable",tme.getMessage());
        }
    }


    public void testNotBlockValidBaseTypesByClassName() throws Exception {
        List<Class<?>> validClasses = Arrays.asList(ValueB.class,ValueC.class,ValueD.class);
        for(Class klass : validClasses) {
            try {
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(klass.getName())
                        .build();
            } catch (InsecureTypeMatchException tme) {
                fail("Class "+klass.getName()+" was blocked");
            }
        }
    }

    public void testNotBlockValidBaseTypesByClass()  {
        List<Class<?>> validClasses = Arrays.asList(ValueB.class,ValueC.class,ValueD.class);
        for(Class klass : validClasses) {
            try {
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(klass)
                        .build();
            } catch (InsecureTypeMatchException tme) {
                fail("Class "+klass.getName()+" was blocked");
            }
        }
    }

    public void testNotBlockValidSubTypesByClass() throws Exception {
        List<Class<?>> validClasses = Arrays.asList(ValueB.class,ValueC.class,ValueD.class);
        for(Class klass : validClasses) {
            try {
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType(klass)
                        .build();
            } catch (InsecureTypeMatchException tme) {
                fail("Class "+klass.getName()+" was blocked");
            }
        }
    }

    public void testNotBlockValidSubTypesByName() throws Exception {
        List<Class<?>> validClasses = Arrays.asList(ValueB.class,ValueC.class,ValueD.class);
        for(Class klass : validClasses) {
            try {
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType(klass.getName())
                        .build();
            } catch (InsecureTypeMatchException tme) {
                fail("Class "+klass.getName()+" was blocked");
            }
        }
    }

    public void testNotBlockValidSubTypesByNameWhenBuildingInsecurely() throws Exception {
        List<Class<?>> validClasses = Arrays.asList(ValueB.class,ValueC.class,ValueD.class);
        for(Class klass : validClasses) {
            try {
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType(klass.getName())
                        .build_insecure();
            } catch (InsecureTypeMatchException tme) {
                fail("Class "+klass.getName()+" was blocked");
            }
        }
    }

    public void testNotBlockValidBaseTypesByClassWhenBuildingInsecurely()  {
        List<Class<?>> validClasses = Arrays.asList(ValueB.class,ValueC.class,ValueD.class);
        for(Class klass : validClasses) {
            try {
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(klass)
                        .build_insecure();
            } catch (InsecureTypeMatchException tme) {
                fail("Class "+klass.getName()+" was blocked");
            }
        }
    }

    public void testNotBlockValidSubTypesByClassWhenBuildingInsecurely() throws Exception {
        List<Class<?>> validClasses = Arrays.asList(ValueB.class,ValueC.class,ValueD.class);
        for(Class klass : validClasses) {
            try {
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType(klass)
                        .build_insecure();
            } catch (InsecureTypeMatchException tme) {
                fail("Class "+klass.getName()+" was blocked");
            }
        }
    }

    public void testNotBlockInvalidSubTypesByNameWhenBuildingInsecurely() throws Exception {
        List<Class<?>> invalidClasses = Arrays.asList(Object.class,Serializable.class);
        for(Class klass : invalidClasses) {
            try {
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType(klass.getName())
                        .build_insecure();
            } catch (InsecureTypeMatchException tme) {
                fail("Class "+klass.getName()+" was blocked");
            }
        }
    }

    public void testNotBlockInvalidBaseTypesByClassWhenBuildingInsecurely()  {
        List<Class<?>> invalidClasses = Arrays.asList(Object.class,Serializable.class);
        for(Class klass : invalidClasses) {
            try {
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(klass)
                        .build_insecure();
            } catch (InsecureTypeMatchException tme) {
                fail("Class "+klass.getName()+" was blocked");
            }
        }
    }

    public void testNotBlockInvalidSubTypesByClassWhenBuildingInsecurely() throws Exception {
        List<Class<?>> invalidClasses = Arrays.asList(Object.class,Serializable.class);
        for(Class klass : invalidClasses) {
            try {
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType(klass)
                        .build_insecure();
            } catch (InsecureTypeMatchException tme) {
                fail("Class "+klass.getName()+" was blocked");
            }
        }
    }



}


