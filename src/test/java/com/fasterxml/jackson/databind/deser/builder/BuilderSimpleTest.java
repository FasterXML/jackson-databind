package com.fasterxml.jackson.databind.deser.builder;

import java.util.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.core.Version;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;

public class BuilderSimpleTest extends BaseMapTest
{
    // // Simple 2-property value class, builder with standard naming

    @JsonDeserialize(builder=SimpleBuilderXY.class)
    static class ValueClassXY
    {
        final int _x, _y;

        protected ValueClassXY(int x, int y) {
            _x = x+1;
            _y = y+1;
        }
    }

    static class SimpleBuilderXY
    {
        public int x, y;
    	
        public SimpleBuilderXY withX(int x0) {
    		    this.x = x0;
    		    return this;
        }

        public SimpleBuilderXY withY(int y0) {
    		    this.y = y0;
    		    return this;
        }

        public ValueClassXY build() {
    		    return new ValueClassXY(x, y);
        }
    }

    // // 3-property value, with more varied builder

    @JsonDeserialize(builder=BuildABC.class)
    static class ValueClassABC
    {
        final int a, b, c;

        protected ValueClassABC(int a, int b, int c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }

    @JsonIgnoreProperties({ "d" })
    static class BuildABC
    {
        public int a; // to be used as is
        private int b, c;
    	
        @JsonProperty("b")
        public BuildABC assignB(int b0) {
            this.b = b0;
            return this;
        }

        // Also ok NOT to return 'this'
        @JsonSetter("c")
        public void c(int c0) {
            this.c = c0;
        }

        public ValueClassABC build() {
            return new ValueClassABC(a, b, c);
        }
    }

    // // Then Builder that is itself immutable
    
    @JsonDeserialize(builder=BuildImmutable.class)
    static class ValueImmutable
    {
        final int value;
        protected ValueImmutable(int v) { value = v; }
    }
    
    static class BuildImmutable {
        private final int value;
        
        private BuildImmutable() { this(0); }
        private BuildImmutable(int v) {
            value = v;
        }
        public BuildImmutable withValue(int v) {
            return new BuildImmutable(v);
        }
        public ValueImmutable build() {
            return new ValueImmutable(value);
        }
    }
    // And then with custom naming:

    @JsonDeserialize(builder=BuildFoo.class)
    static class ValueFoo
    {
        final int value;
        protected ValueFoo(int v) { value = v; }
    }

    @JsonPOJOBuilder(withPrefix="foo", buildMethodName="construct")
    static class BuildFoo {
        private int value;
        
        public BuildFoo fooValue(int v) {
            value = v;
            return this;
        }
        public ValueFoo construct() {
            return new ValueFoo(value);
        }
    }

    // for [databind#761]

    @JsonDeserialize(builder=ValueInterfaceBuilder.class)
    interface ValueInterface {
        int getX();
    }

    @JsonDeserialize(builder=ValueInterface2Builder.class)
    interface ValueInterface2 {
        int getX();
    }
    
    static class ValueInterfaceImpl implements ValueInterface
    {
        final int _x;

        protected ValueInterfaceImpl(int x) {
            _x = x+1;
        }

        @Override
        public int getX() {
            return _x;
        }
    }

    static class ValueInterface2Impl implements ValueInterface2
    {
        final int _x;

        protected ValueInterface2Impl(int x) {
            _x = x+1;
        }

        @Override
        public int getX() {
            return _x;
        }
    }
    
    static class ValueInterfaceBuilder
    {
        public int x;

        public ValueInterfaceBuilder withX(int x0) {
            this.x = x0;
            return this;
        }

        public ValueInterface build() {
            return new ValueInterfaceImpl(x);
        }
    }

    static class ValueInterface2Builder
    {
        public int x;

        public ValueInterface2Builder withX(int x0) {
            this.x = x0;
            return this;
        }

        // should also be ok: more specific type
        public ValueInterface2Impl build() {
            return new ValueInterface2Impl(x);
        }
    }

    // [databind#777]
    @JsonDeserialize(builder = SelfBuilder777.class)
    @JsonPOJOBuilder(buildMethodName = "", withPrefix = "with")
    static class SelfBuilder777 {
        public int x;

        public SelfBuilder777 withX(int value) {
            x = value;
            return this;
        }
    }

    // [databind#822]
    @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "with")
    static class ValueBuilder822
    {
        public int x;
        private Map<String,Object> stuff = new HashMap<String,Object>();

        // And tweaked slightly for [databind#2415]
        @JsonCreator
        public ValueBuilder822(@JsonProperty("x") int x0) {
            this.x = x0;
        }

        @JsonAnySetter
        public void addStuff(String key, Object value) {
            stuff.put(key, value);
        }

        public ValueClass822 build() {
            return new ValueClass822(x, stuff);
        }
    }

    @JsonDeserialize(builder = ValueBuilder822.class)
    static class ValueClass822 {
        public int x;
        public Map<String,Object> stuff;

        public ValueClass822(int x, Map<String,Object> stuff) {
            this.x = x;
            this.stuff = stuff;
        }
    }

    protected static class NopModule1557 extends com.fasterxml.jackson.databind.Module
    {
        @Override
        public String getModuleName() {
            return "NopModule";
        }

        @Override
        public Version version() {
            return Version.unknownVersion();
        }

        @Override
        public void setupModule(SetupContext setupContext) {
            // This annotation introspector has no opinion about builders, make sure it doesn't interfere
            setupContext.insertAnnotationIntrospector(new NopAnnotationIntrospector() {
                private static final long serialVersionUID = 1L;
                @Override
                public Version version() {
                    return Version.unknownVersion();
                }
            });
        }
    }

    // (related to) [databind#2354]: should be ok to have private inner class:

    @JsonDeserialize(builder=Value2354.Value2354Builder.class)
    static class Value2354
    {
        private final int value;

        protected Value2354(int v) { value = v; }

        public int value() { return value; }
        
        @SuppressWarnings("unused")
        private static class Value2354Builder {
            private int value;
            
            public Value2354Builder withValue(int v) {
                value = v;
                return this;
            }

            // should be ok for this to be private, too, since name is (pre)configured
            private Value2354 build() {
                return new Value2354(value);
            }
        }
    }

    @JsonDeserialize(builder = ValidatingValue.Builder.class)
    static class ValidatingValue
    {
        final String first;
        final String second;

        ValidatingValue(String first, String second) {
            this.first = first;
            this.second = second;
        }

        static class ValidationException extends RuntimeException
        {
            private static final long serialVersionUID = 1L;

            ValidationException(String message) {
                super(message);
            }
        }

        static class Builder
        {

            private String first;
            private String second;

            @JsonSetter("a")
            Builder first(String value) {
                this.first = value;
                return this;
            }

            @JsonSetter("b")
            Builder second(String value) {
                this.second = value;
                return this;
            }

            ValidatingValue build() {
                if (first == null) {
                    throw new ValidationException("Missing first");
                }
                if (second == null) {
                    throw new ValidationException("Missing second");
                }
                return new ValidatingValue(first, second);
            }
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testSimple() throws Exception
    {
        String json = aposToQuotes("{'x':1,'y':2}");
        Object o = MAPPER.readValue(json, ValueClassXY.class);
        assertNotNull(o);
        assertSame(ValueClassXY.class, o.getClass());
        ValueClassXY value = (ValueClassXY) o;
        // note: ctor adds one to both values
        assertEquals(value._x, 2);
        assertEquals(value._y, 3);
    }

    // related to [databind#1214]
    public void testSimpleWithIgnores() throws Exception
    {
        // 'z' is unknown, and would fail by default:
        final String json = aposToQuotes("{'x':1,'y':2,'z':4}");
        Object o = null;

        try {
            o = MAPPER.readValue(json, ValueClassXY.class);
            fail("Should not pass");
        } catch (UnrecognizedPropertyException e) {
            assertEquals("z", e.getPropertyName());
            verifyException(e, "Unrecognized field \"z\"");
        }

        // but with config overrides should pass
        ObjectMapper ignorantMapper = new ObjectMapper();
        ignorantMapper.configOverride(SimpleBuilderXY.class)
                .setIgnorals(JsonIgnoreProperties.Value.forIgnoreUnknown(true));
        o = ignorantMapper.readValue(json, ValueClassXY.class);
        assertNotNull(o);
        assertSame(ValueClassXY.class, o.getClass());
        ValueClassXY value = (ValueClassXY) o;
        // note: ctor adds one to both values
        assertEquals(value._x, 2);
        assertEquals(value._y, 3);
    }
    
    public void testMultiAccess() throws Exception
    {
        String json = aposToQuotes("{'c':3,'a':2,'b':-9}");
        ValueClassABC value = MAPPER.readValue(json, ValueClassABC.class);
        assertNotNull(value);
        assertEquals(2, value.a);
        assertEquals(-9, value.b);
        assertEquals(3, value.c);

        // also, since we can ignore some properties:
        value = MAPPER.readValue(aposToQuotes("{'c':3,'d':5,'b':-9}"), ValueClassABC.class);
        assertNotNull(value);
        assertEquals(0, value.a);
        assertEquals(-9, value.b);
        assertEquals(3, value.c);
    }

    // test for Immutable builder, to ensure return value is used
    public void testImmutable() throws Exception
    {
        final String json = "{\"value\":13}";
        ValueImmutable value = MAPPER.readValue(json, ValueImmutable.class);        
        assertEquals(13, value.value);
    }

    // test with custom 'with-prefix'
    public void testCustomWith() throws Exception
    {
        final String json = "{\"value\":1}";
        ValueFoo value = MAPPER.readValue(json, ValueFoo.class);        
        assertEquals(1, value.value);
    }

    // for [databind#761]
    
    public void testBuilderMethodReturnMoreGeneral() throws Exception
    {
        final String json = "{\"x\":1}";
        ValueInterface value = MAPPER.readValue(json, ValueInterface.class);
        assertEquals(2, value.getX());
    }

    public void testBuilderMethodReturnMoreSpecific() throws Exception
    {
        final String json = "{\"x\":1}";
        ValueInterface2 value = MAPPER.readValue(json, ValueInterface2.class);
        assertEquals(2, value.getX());
    }

    public void testSelfBuilder777() throws Exception
    {
        SelfBuilder777 result = MAPPER.readValue(aposToQuotes("{'x':3}'"),
                SelfBuilder777.class);
        assertNotNull(result);
        assertEquals(3, result.x);
    }

    public void testWithAnySetter822() throws Exception
    {
        final String json = "{\"extra\":3,\"foobar\":[ ],\"x\":1,\"name\":\"bob\"}";
        ValueClass822 value = MAPPER.readValue(json, ValueClass822.class);
        assertEquals(1, value.x);
        assertNotNull(value.stuff);
        assertEquals(3, value.stuff.size());
        assertEquals(Integer.valueOf(3), value.stuff.get("extra"));
        assertEquals("bob", value.stuff.get("name"));
        Object ob = value.stuff.get("foobar");
        assertNotNull(ob);
        assertTrue(ob instanceof List);
        assertTrue(((List<?>) ob).isEmpty());
    }

    public void testPOJOConfigResolution1557() throws Exception
    {
        final String json = "{\"value\":1}";
        MAPPER.registerModule(new NopModule1557());
        ValueFoo value = MAPPER.readValue(json, ValueFoo.class);
        assertEquals(1, value.value);
    }

    // related to [databind#2354] (ensure private inner builder classes are ok)
    public void testPrivateInnerBuilder() throws Exception
    {
        String json = aposToQuotes("{'value':13}");
        Value2354 result = MAPPER.readValue(json, Value2354.class);
        assertEquals(13, result.value());
    }

    public void testSuccessfulValidatingBuilder() throws Exception
    {
        ValidatingValue result = MAPPER.readValue(aposToQuotes("{'a':'1','b':'2'}"), ValidatingValue.class);
        assertEquals("1", result.first);
        assertEquals("2", result.second);
    }


    public void testFailingValidatingBuilderWithExceptionWrapping() throws Exception
    {
        ObjectMapper withWrapping = MAPPER.isEnabled(DeserializationFeature.WRAP_EXCEPTIONS)
                ? MAPPER : MAPPER.copy().enable(DeserializationFeature.WRAP_EXCEPTIONS);
        try {
            withWrapping
                    .readValue(aposToQuotes("{'a':'1'}"), ValidatingValue.class);
            fail("Expected an exception");
        } catch (JsonMappingException e) {
            assertTrue(e.getMessage().contains("Missing second"));
            assertTrue(e.getCause() instanceof ValidatingValue.ValidationException);
        }
    }

    public void testFailingValidatingBuilderWithExceptionWrappingFromTree() throws Exception
    {
        ObjectMapper withWrapping = MAPPER.isEnabled(DeserializationFeature.WRAP_EXCEPTIONS)
                ? MAPPER : MAPPER.copy().enable(DeserializationFeature.WRAP_EXCEPTIONS);
        try {
            JsonNode tree = withWrapping.readTree(aposToQuotes("{'a':'1'}"));
            withWrapping.treeToValue(tree, ValidatingValue.class);
            fail("Expected an exception");
        } catch (ValueInstantiationException e) {
            assertTrue(e.getMessage().contains("Missing second"));
            assertTrue(e.getCause() instanceof ValidatingValue.ValidationException);
        }
    }

    public void testFailingValidatingBuilderWithoutExceptionWrapping() throws Exception
    {
        ObjectMapper withoutWrapping = MAPPER.isEnabled(DeserializationFeature.WRAP_EXCEPTIONS)
                ? MAPPER.copy().disable(DeserializationFeature.WRAP_EXCEPTIONS) : MAPPER;
        try {
            withoutWrapping
                    .readValue(aposToQuotes("{'a':'1'}"), ValidatingValue.class);
            fail("Expected an exception");
        } catch (ValidatingValue.ValidationException e) {
            assertEquals("Missing second", e.getMessage());
        }
    }

    public void testFailingValidatingBuilderWithoutExceptionWrappingFromTree() throws Exception
    {
        ObjectMapper withoutWrapping = MAPPER.isEnabled(DeserializationFeature.WRAP_EXCEPTIONS)
                ? MAPPER.copy().disable(DeserializationFeature.WRAP_EXCEPTIONS) : MAPPER;
        try {
            JsonNode tree = withoutWrapping.readTree(aposToQuotes("{'a':'1'}"));
            withoutWrapping.treeToValue(tree, ValidatingValue.class);
            fail("Expected an exception");
        } catch (ValidatingValue.ValidationException e) {
            assertEquals("Missing second", e.getMessage());
        }
    }
}
