package tools.jackson.databind.ser;

import java.io.StringWriter;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for verifying functioning of [JACKSON-195], ability to
 * force specific root type for serialization (super type of value)
 */
public class TestRootType
    extends DatabindTestUtil
{
    /*
    /**********************************************************************
    /* Annotated helper classes
    /**********************************************************************
     */

    interface BaseInterface {
        int getB();
    }

    static class BaseType
        implements BaseInterface
    {
        public String a = "a";

        @Override
        public int getB() { return 3; }
    }

    static class SubType extends BaseType {
        public String a2 = "x";

        public boolean getB2() { return true; }
    }

    @JsonTypeInfo(use=Id.NAME, include=As.PROPERTY, property="beanClass")
    public abstract static class BaseClass398 { }

    public static class TestClass398 extends BaseClass398 {
       public String property = "aa";
    }

    @JsonRootName("root")
    static class WithRootName {
        public int a = 3;
    }

    // [databind#412]
    @JsonPropertyOrder({ "uuid", "type" })
    static class TestCommandParent {
        public String uuid;
        public int type;
    }

    static interface Issue822Interface {
        public int getA();
    }

    // If this annotation is added, things will work:
    //@tools.jackson.databind.annotation.JsonSerialize(as=Issue822Interface.class)
    // but it should not be necessary when root type is passed
    static class Issue822Impl implements Issue822Interface {
        @Override
        public int getA() { return 3; }
        public int getB() { return 9; }
    }
    
    static class TestCommandChild extends TestCommandParent { }

    /*
    /**********************************************************************
    /* Main test methods
    /**********************************************************************
     */

    private final ObjectMapper VANILLA_MAPPER = sharedMapper();

    private final ObjectMapper WRAP_ROOT_MAPPER = jsonMapperBuilder()
            .enable(SerializationFeature.WRAP_ROOT_VALUE)
            .build();

    @SuppressWarnings("unchecked")
    @Test
    public void testSuperClass() throws Exception
    {
        SubType bean = new SubType();

        // first, test with dynamically detected type
        Map<String,Object> result = writeAndMap(VANILLA_MAPPER, bean);
        assertEquals(4, result.size());
        assertEquals("a", result.get("a"));
        assertEquals(Integer.valueOf(3), result.get("b"));
        assertEquals("x", result.get("a2"));
        assertEquals(Boolean.TRUE, result.get("b2"));

        // and then using specified typed writer
        ObjectWriter w = VANILLA_MAPPER.writerFor(BaseType.class);
        String json = w.writeValueAsString(bean);
        result = (Map<String,Object>)VANILLA_MAPPER.readValue(json, Map.class);
        assertEquals(2, result.size());
        assertEquals("a", result.get("a"));
        assertEquals(Integer.valueOf(3), result.get("b"));
    }

    @Test
    public void testSuperInterface() throws Exception
    {
        SubType bean = new SubType();

        // let's constrain by interface:
        ObjectWriter w = VANILLA_MAPPER.writerFor(BaseInterface.class);
        String json = w.writeValueAsString(bean);
        @SuppressWarnings("unchecked")
        Map<String,Object> result = VANILLA_MAPPER.readValue(json, Map.class);
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(3), result.get("b"));
    }

    @Test
    public void testInArray() throws Exception
    {
        // must force static typing, otherwise won't matter a lot
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.USE_STATIC_TYPING)
                .build();
        SubType[] ob = new SubType[] { new SubType() };
        String json = mapper.writerFor(BaseInterface[].class).writeValueAsString(ob);
        // should propagate interface type through due to root declaration; static typing
        assertEquals("[{\"b\":3}]", json);
    }

    /**
     * Unit test to ensure that proper exception is thrown if declared
     * root type is not compatible with given value instance.
     */
    @Test
    public void testIncompatibleRootType() throws Exception
    {
        SubType bean = new SubType();

        // and then let's try using incompatible type
        ObjectWriter w = VANILLA_MAPPER.writerFor(HashMap.class);
        try {
            w.writeValueAsString(bean);
            fail("Should have failed due to incompatible type");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Incompatible types");
        }

        // and also with alternate output method
        try {
            w.writeValueAsBytes(bean);
            fail("Should have failed due to incompatible type");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Incompatible types");
        }
    }

    @Test
    public void testJackson398() throws Exception
    {
        JavaType collectionType = defaultTypeFactory().constructCollectionType(ArrayList.class, BaseClass398.class);
        List<TestClass398> typedList = new ArrayList<TestClass398>();
        typedList.add(new TestClass398());

        final String EXP = "[{\"beanClass\":\"TestRootType$TestClass398\",\"property\":\"aa\"}]";

        // First simplest way:
        String json = VANILLA_MAPPER.writerFor(collectionType).writeValueAsString(typedList);
        assertEquals(EXP, json);

        StringWriter out = new StringWriter();
        VANILLA_MAPPER.writerFor(collectionType)
            .writeValue(VANILLA_MAPPER.createGenerator(out), typedList);

        assertEquals(EXP, out.toString());
    }

    // [JACKSON-163]
    @Test
    public void testRootWrapping() throws Exception
    {
        String json = WRAP_ROOT_MAPPER.writeValueAsString(new StringWrapper("abc"));
        assertEquals("{\"StringWrapper\":{\"str\":\"abc\"}}", json);
    }

    /**
     * Test to verify that there is support for specifying root type as primitive,
     * even if wrapper value is passed (there is no way to pass primitive values as
     * Objects); this to support frameworks that may pass unprocessed
     * {@link java.lang.reflect.Type} from field or method.
     */
    @Test
    public void testIssue456WrapperPart() throws Exception
    {
        assertEquals("123", VANILLA_MAPPER.writerFor(Integer.TYPE).writeValueAsString(Integer.valueOf(123)));
        assertEquals("456", VANILLA_MAPPER.writerFor(Long.TYPE).writeValueAsString(Long.valueOf(456L)));
    }

    @Test
    public void testRootNameAnnotation() throws Exception
    {
        String json = WRAP_ROOT_MAPPER.writeValueAsString(new WithRootName());
        assertEquals("{\"root\":{\"a\":3}}", json);
    }

    // [databind#412]
    @Test
    public void testRootNameWithExplicitType() throws Exception
    {
        TestCommandChild cmd = new TestCommandChild();
        cmd.uuid = "1234";
        cmd.type = 1;

        ObjectWriter writer = WRAP_ROOT_MAPPER.writerFor(TestCommandParent.class);
        String json =  writer.writeValueAsString(cmd);

        assertEquals("{\"TestCommandParent\":{\"uuid\":\"1234\",\"type\":1}}", json);
    }

    // First ensure that basic interface-override works:
    @Test
    public void testTypedSerialization() throws Exception
    {
        String singleJson = VANILLA_MAPPER.writerFor(Issue822Interface.class)
                .writeValueAsString(new Issue822Impl());
        // start with specific value case:
        assertEquals("{\"a\":3}", singleJson);
    }

    @Test
    public void testTypedRootArrays() throws Exception
    {
// Work-around when real solution not yet implemented:
//        mapper.enable(MapperFeature.USE_STATIC_TYPING);
        assertEquals("[{\"a\":3}]", VANILLA_MAPPER.writerFor(Issue822Interface[].class).writeValueAsString(
                new Issue822Interface[] { new Issue822Impl() }));
    }

    @Test
    public void testTypedRootLists() throws Exception
    {
     // Work-around when real solution not yet implemented:
//        mapper.enable(MapperFeature.USE_STATIC_TYPING);

        List<Issue822Interface> list = new ArrayList<Issue822Interface>();
        list.add(new Issue822Impl());
        String listJson = VANILLA_MAPPER.writerFor(new TypeReference<List<Issue822Interface>>(){})
                .writeValueAsString(list);
        assertEquals("[{\"a\":3}]", listJson);
    }

    @Test
    public void testTypedRootMaps() throws Exception
    {
        Map<String,Issue822Interface> map = new HashMap<String,Issue822Interface>();
        map.put("a", new Issue822Impl());
        String listJson = VANILLA_MAPPER
                .writerFor(new TypeReference<Map<String,Issue822Interface>>(){})
                .writeValueAsString(map);
        assertEquals("{\"a\":{\"a\":3}}", listJson);
    }
}
