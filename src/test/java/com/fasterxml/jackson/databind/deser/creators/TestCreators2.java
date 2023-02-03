
package com.fasterxml.jackson.databind.deser.creators;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;

public class TestCreators2 extends BaseMapTest
{
    static class HashTest
    {
        final byte[] bytes;
        final String type;

        @JsonCreator
        public HashTest(@JsonProperty("bytes") @JsonDeserialize(using = BytesDeserializer.class) final byte[] bytes,
                @JsonProperty("type") final String type)
        {
            this.bytes = bytes;
            this.type = type;
        }
    }

    static class BytesDeserializer extends JsonDeserializer<byte[]>
    {
        @Override
        public byte[] deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            String str = jp.getText();
            return str.getBytes("UTF-8");
        }
    }

    static class Primitives
    {
        protected int x = 3;
        protected double d = -0.5;
        protected boolean b = true;

        @JsonCreator
        public Primitives(@JsonProperty("x") int x,
                @JsonProperty("d") double d,
                @JsonProperty("b") boolean b)
        {
            this.x = x;
            this.d = d;
            this.b = b;
        }
    }

    protected static class Test431Container {
        protected final List<Item431> items;

        @JsonCreator
        public Test431Container(@JsonProperty("items") final List<Item431> i) {
            items = i;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    protected static class Item431 {
        protected final String id;

        @JsonCreator
        public Item431(@JsonProperty("id") String id) {
            this.id = id;
        }
    }

    // Test class for verifying that creator-call failures are reported as checked exceptions
    static class BeanFor438 {
        @JsonCreator
        public BeanFor438(@JsonProperty("name") String s) {
            throw new IllegalArgumentException("I don't like that name!");
        }
    }

    // For [JACKSON-470]: should be appropriately detected, reported error about
    static class BrokenCreatorBean
    {
        protected String bar;

        @JsonCreator
        public BrokenCreatorBean(@JsonProperty("bar") String bar1, @JsonProperty("bar") String bar2) {
            bar = ""+bar1+"/"+bar2;
        }
    }

    // For [JACKSON-541]: should not need @JsonCreator if SerializationFeature.AUTO_DETECT_CREATORS is on.
    static class AutoDetectConstructorBean
    {
        protected final String foo;
        protected final String bar;

        public AutoDetectConstructorBean(@JsonProperty("bar") String bar,
                @JsonProperty("foo") String foo){
            this.bar = bar;
            this.foo = foo;
        }
    }

    static class BustedCtor {
        @JsonCreator
        BustedCtor(@JsonProperty("a") String value) {
            throw new IllegalArgumentException("foobar");
        }
    }

    static class IgnoredCtor
    {
        @JsonIgnore
        public IgnoredCtor(String arg) {
            throw new RuntimeException("Should never use this constructor");
        }

        public IgnoredCtor() { }
    }

    abstract static class AbstractBase {
        @JsonCreator
        public static AbstractBase create(Map<String,Object> props)
        {
            return new AbstractBaseImpl(props);
        }
    }

    static class AbstractBaseImpl extends AbstractBase
    {
        protected Map<String,Object> props;

        public AbstractBaseImpl(Map<String,Object> props) {
            this.props = props;
        }
    }

    static interface Issue700Set extends java.util.Set<Object> { }

    static class Issue700Bean
    {
        protected Issue700Set item;

        @JsonCreator
        public Issue700Bean(@JsonProperty("item") String item) { }

        public String getItem() { return null; }
    }

    static final class MultiPropCreator1476 {
        private final int intField;
        private final String stringField;

        public MultiPropCreator1476(@JsonProperty("intField") int intField) {
          this(intField, "empty");
        }

        public MultiPropCreator1476(@JsonProperty("stringField") String stringField) {
          this(-1, stringField);
        }

        @JsonCreator
        public MultiPropCreator1476(@JsonProperty("intField") int intField,
                @JsonProperty("stringField") String stringField) {
          this.intField = intField;
          this.stringField = stringField;
        }

        public int getIntField() {
          return intField;
        }

        public String getStringField() {
          return stringField;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testExceptionFromConstructor() throws Exception
    {
        try {
            MAPPER.readValue("{}", BustedCtor.class);
            fail("Expected exception");
        } catch (ValueInstantiationException e) {
            verifyException(e, ": foobar");
            // also: should have nested exception
            Throwable t = e.getCause();
            if (t == null) {
                fail("Should have assigned cause for: ("+e.getClass().getSimpleName()+") "+e);
            }
            assertNotNull(t);
            assertEquals(IllegalArgumentException.class, t.getClass());
            assertEquals("foobar", t.getMessage());
        } catch (Exception e) {
            fail("Should have caught ValueInstantiationException, got: "+e);
        }
    }

    public void testSimpleConstructor() throws Exception
    {
        HashTest test = MAPPER.readValue("{\"type\":\"custom\",\"bytes\":\"abc\" }", HashTest.class);
        assertEquals("custom", test.type);
        assertEquals("abc", new String(test.bytes, "UTF-8"));
    }

    public void testMissingPrimitives() throws Exception
    {
        Primitives p = MAPPER.readValue("{}", Primitives.class);
        assertFalse(p.b);
        assertEquals(0, p.x);
        assertEquals(0.0, p.d);
    }

    public void testJackson431() throws Exception
    {
        final Test431Container foo = MAPPER.readValue(
                "{\"items\":\n"
                +"[{\"bar\": 0,\n"
                +"\"id\": \"id123\",\n"
                +"\"foo\": 1\n"
                +"}]}",
                Test431Container.class);
        assertNotNull(foo);
    }

    // Catch and re-throw exceptions that Creator methods throw
    public void testJackson438() throws Exception
    {
        Exception e = null;
        try {
            MAPPER.readValue("{ \"name\":\"foobar\" }", BeanFor438.class);
            fail("Should have failed");
        } catch (Exception e0) {
            e = e0;
        }
        if (!(e instanceof ValueInstantiationException)) {
            fail("Should have received ValueInstantiationException, caught "+e.getClass().getName());
        }
        verifyException(e, "don't like that name");
        // Ok: also, let's ensure root cause is directly linked, without other extra wrapping:
        Throwable t = e.getCause();
        if (t == null) {
            fail("Should have assigned cause for: ("+e.getClass().getSimpleName()+") "+e);
        }
        assertEquals(IllegalArgumentException.class, t.getClass());
        verifyException(e, "don't like that name");
    }

    public void testCreatorWithDupNames() throws Exception
    {
        try {
            MAPPER.readValue("{\"bar\":\"x\"}", BrokenCreatorBean.class);
            fail("Should have caught duplicate creator parameters");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "duplicate creator property \"bar\"");
            verifyException(e, "for type `com.fasterxml.jackson.databind.");
            verifyException(e, "$BrokenCreatorBean`");
        }
    }

    public void testCreatorMultipleArgumentWithoutAnnotation() throws Exception {
        AutoDetectConstructorBean value = MAPPER.readValue("{\"bar\":\"bar\",\"foo\":\"foo\"}",
                AutoDetectConstructorBean.class);
        assertEquals("bar", value.bar);
        assertEquals("foo", value.foo);
    }

    public void testIgnoredSingleArgCtor() throws Exception
    {
        try {
            MAPPER.readValue(q("abc"), IgnoredCtor.class);
            fail("Should have caught missing constructor problem");
        } catch (MismatchedInputException e) {
            verifyException(e, "no String-argument constructor/factory method");
        }
    }

    public void testAbstractFactory() throws Exception
    {
        AbstractBase bean = MAPPER.readValue("{\"a\":3}", AbstractBase.class);
        assertNotNull(bean);
        AbstractBaseImpl impl = (AbstractBaseImpl) bean;
        assertEquals(1, impl.props.size());
        assertEquals(Integer.valueOf(3), impl.props.get("a"));
    }

    public void testCreatorProperties() throws Exception
    {
        Issue700Bean value = MAPPER.readValue("{ \"item\" : \"foo\" }", Issue700Bean.class);
        assertNotNull(value);
    }

    // [databind#1476]
    public void testConstructorChoice() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        MultiPropCreator1476 pojo = mapper.readValue("{ \"intField\": 1, \"stringField\": \"foo\" }",
                MultiPropCreator1476.class);
        assertEquals(1, pojo.getIntField());
        assertEquals("foo", pojo.getStringField());
    }
}
