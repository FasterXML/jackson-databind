package tools.jackson.databind.deser;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

/**
 * This unit test suite tests use of basic Annotations for
 * bean deserialization; ones that indicate (non-constructor)
 * method types, explicit deserializer annotations.
 */
@SuppressWarnings("serial")
public class BasicAnnotationsTest extends DatabindTestUtil
{
    // Class for testing {@link JsonProperty} annotations
    final static class SizeClassSetter
    {
        int _size;
        int _length;
        int _other;

        @JsonProperty public void size(int value) { _size = value; }
        @JsonProperty("length") public void foobar(int value) { _length = value; }

        // note: need not be public if annotated
        @JsonProperty protected void other(int value) { _other = value; }

        // finally: let's add a red herring that should be avoided...
        public void errorOut(int value) { throw new Error(); }
    }

    static class Issue442Bean {
        @JsonUnwrapped
        protected IntWrapper w = new IntWrapper(13);
    }

    final static class SizeClassSetter2
    {
        int _x;

        @JsonProperty public void setX(int value) { _x = value; }

        // another red herring, which shouldn't be included
        public void setXandY(int x, int y) { throw new Error(); }
    }

    /**
     * One more, but this time checking for implied setter
     * using @JsonDeserialize
     */
    final static class SizeClassSetter3
    {
        int _x;

        @JsonDeserialize public void x(int value) { _x = value; }
    }


    /// Classes for testing Setter discovery with inheritance
    static class BaseBean
    {
        int _x = 0, _y = 0;

        public void setX(int value) { _x = value; }
        @JsonProperty("y") void foobar(int value) { _y = value; }
    }

    static class BeanSubClass extends BaseBean
    {
        int _z;

        public void setZ(int value) { _z = value; }
    }

    static class BeanWithDeserialize {
        @JsonDeserialize protected int a;
    }

    @JsonAutoDetect(setterVisibility=Visibility.NONE)
    final static class Dummy { }

    final static class EmptyDummy { }

    static class AnnoBean {
        int value = 3;

        @JsonProperty("y")
        public void setX(int v) { value = v; }
    }

    enum Alpha { A, B, C; }

    public static class SimpleBean {
        public int x, y;
    }

    static class ListSubClass extends ArrayList<StringWrapper> { }

    /**
     * Map class that should behave like {@link ListSubClass}, but by
     * using annotations.
     */
    @JsonDeserialize(contentAs=StringWrapper.class)
    static class AnnotatedStringList extends ArrayList<Object> { }

    @JsonDeserialize(contentAs=BooleanElement.class)
    static class AnnotatedBooleanList extends ArrayList<Object> { }

    protected static class BooleanElement {
        public Boolean b;

        @JsonCreator
        public BooleanElement(Boolean value) { b = value; }

        @JsonValue public Boolean value() { return b; }
    }

    /*
    /**********************************************************************
    /* Other helper classes
    /**********************************************************************
     */

    final static class IntsDeserializer extends StdDeserializer<int[]>
    {
        public IntsDeserializer() { super(int[].class); }
        @Override
        public int[] deserialize(JsonParser p, DeserializationContext ctxt)
        {
            return new int[] { p.getIntValue() };
        }
    }

    /*
    /**********************************************************************
    /* Test methods, basic
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testSimpleSetter() throws Exception
    {
        SizeClassSetter result = MAPPER.readValue
            ("{ \"other\":3, \"size\" : 2, \"length\" : -999 }",
             SizeClassSetter.class);

        assertEquals(3, result._other);
        assertEquals(2, result._size);
        assertEquals(-999, result._length);
    }

    @Test
    public void testSimpleSetter2() throws Exception
    {
        SizeClassSetter2 result = MAPPER.readValue("{ \"x\": -3 }",
             SizeClassSetter2.class);
        assertEquals(-3, result._x);
    }

    @Test
    public void testSimpleSetter3() throws Exception
    {
        SizeClassSetter3 result = MAPPER.readValue
            ("{ \"x\": 128 }",
             SizeClassSetter3.class);
        assertEquals(128, result._x);
    }

    /**
     * Test for verifying that super-class setters are used as
     * expected.
     */
    @Test
    public void testSetterInheritance() throws Exception
    {
        BeanSubClass result = MAPPER.readValue
            ("{ \"x\":1, \"z\" : 3, \"y\" : 2 }",
             BeanSubClass.class);
        assertEquals(1, result._x);
        assertEquals(2, result._y);
        assertEquals(3, result._z);
    }

    @Test
    public void testImpliedProperty() throws Exception
    {
        BeanWithDeserialize bean = MAPPER.readValue("{\"a\":3}", BeanWithDeserialize.class);
        assertNotNull(bean);
        assertEquals(3, bean.a);
    }

    // [databind#442]
    @Test
    public void testIssue442PrivateUnwrapped() throws Exception
    {
        Issue442Bean bean = MAPPER.readValue("{\"i\":5}", Issue442Bean.class);
        assertEquals(5, bean.w.i);
    }

    /*
    /**********************************************************************
    /* Test methods, Collections
    /**********************************************************************
     */

    /**
     * Verifying that sub-classing works ok wrt generics information
     */
    @Test
    public void testListSubClass() throws Exception
    {
        ListSubClass result = MAPPER.readValue("[ \"123\" ]", ListSubClass.class);
        assertEquals(1, result.size());
        Object value = result.get(0);
        assertEquals(StringWrapper.class, value.getClass());
        StringWrapper bw = (StringWrapper) value;
        assertEquals("123", bw.str);
    }

    // Verifying that sub-classing works ok wrt generics information
    @Test
    public void testAnnotatedLStringList() throws Exception
    {
        AnnotatedStringList result = MAPPER.readValue("[ \"...\" ]", AnnotatedStringList.class);
        assertEquals(1, result.size());
        Object ob = result.get(0);
        assertEquals(StringWrapper.class, ob.getClass());
        assertEquals("...", ((StringWrapper) ob).str);
    }

    @Test
    public void testAnnotatedBooleanList() throws Exception
    {
        AnnotatedBooleanList result = MAPPER.readValue("[ false ]", AnnotatedBooleanList.class);
        assertEquals(1, result.size());
        Object ob = result.get(0);
        assertEquals(BooleanElement.class, ob.getClass());
        assertFalse(((BooleanElement) ob).b);
    }

    /*
    /**********************************************************************
    /* Test methods, annotations disabled
    /**********************************************************************
     */

    @Test
    public void testAnnotationsDisabled() throws Exception
    {
        // first: verify that annotation introspection is enabled by default
        assertTrue(MAPPER.deserializationConfig().isEnabled(MapperFeature.USE_ANNOTATIONS));
        // with annotations, property is renamed
        AnnoBean bean = MAPPER.readValue("{ \"y\" : 0 }", AnnoBean.class);
        assertEquals(0, bean.value);

        ObjectMapper m = jsonMapperBuilder()
                .disable(MapperFeature.USE_ANNOTATIONS)
                .build();
        // without annotations, should default to default bean-based name...
        bean = m.readValue("{ \"x\" : 0 }", AnnoBean.class);
        assertEquals(0, bean.value);
    }

    @Test
    public void testEnumsWhenDisabled() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        assertEquals(Alpha.B, m.readValue(q("B"), Alpha.class));

        m = jsonMapperBuilder()
                .disable(MapperFeature.USE_ANNOTATIONS)
                .build();
        // should still use the basic name handling here
        assertEquals(Alpha.B, m.readValue(q("B"), Alpha.class));
    }

    @Test
    public void testNoAccessOverrides() throws Exception
    {
        ObjectMapper m = jsonMapperBuilder()
                .disable(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS)
                .build();
        SimpleBean bean = m.readValue("{\"x\":1,\"y\":2}", SimpleBean.class);
        assertEquals(1, bean.x);
        assertEquals(2, bean.y);
    }
}
