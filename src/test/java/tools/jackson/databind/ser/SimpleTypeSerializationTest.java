package tools.jackson.databind.ser;

import java.io.*;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import tools.jackson.core.*;
import tools.jackson.core.io.ContentReference;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonAppend;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.BeanPropertyDefinition;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.util.Annotations;
import tools.jackson.databind.util.TokenBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for verifying serialization of simple basic non-structured
 * types; primitives (and/or their wrappers), Strings, arrays,
 * and Jackson-specific types; also field-backed properties, auto-detect
 * configuration, cyclic type handling, and virtual property appending.
 */
public class SimpleTypeSerializationTest
    extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();
    private final ObjectWriter WRITER = MAPPER.writer();

    /*
    /**********************************************************************
    /* Helper classes: field serialization
    /**********************************************************************
     */

    static class SimpleFieldBean
    {
        public int x, y;

        // not auto-detectable, not public
        int z;

        // ignored, not detectable either
        @JsonIgnore public int a;
    }

    static class SimpleFieldBean2
    {
        @JsonSerialize String[] values;

        // note: this annotation should not matter for serialization:
        @JsonDeserialize int dummy;
    }

    static class TransientBean
    {
        public int a;
        // transients should not be included
        public transient int b;
        // or statics
        public static int c;
    }

    @JsonAutoDetect(setterVisibility=Visibility.PUBLIC_ONLY, fieldVisibility=Visibility.NONE)
    static class NoAutoDetectBean
    {
        // not auto-detectable any more
        public int x;

        @JsonProperty("z")
        public int _z;
    }

    /**
     * Let's test invalid bean too: can't have 2 logical properties
     * with same name.
     *<p>
     * 21-Feb-2010, tatus: That is, not within same class.
     *    As per [JACKSON-226] it is acceptable to "override"
     *    field definitions in sub-classes.
     */
    static class DupFieldBean
    {
        @JsonProperty("foo")
        public int _z = 1;

        @JsonSerialize
        private int foo = 2;
    }

    static class DupFieldBean2
    {
        public int z = 3;

        @JsonProperty("z")
        public int _z = 4;
    }

    static class OkDupFieldBean
        extends SimpleFieldBean
    {
        @JsonProperty("x")
        protected int myX;

        public int y;

        public OkDupFieldBean(int x, int y) {
            this.myX = x;
            this.y = y;
        }
    }

    /**
     * It is ok to have a method-based and field-based property
     * introspectable: only one should be serialized, and since
     * methods have precedence, it should be the method one.
     */
    static class FieldAndMethodBean
    {
        @JsonProperty public int z;

        @JsonProperty("z") public int getZ() { return z+1; }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    static class Item240 {
        @JsonProperty
        private String id;
        // only include annotation to ensure it won't override settings
        @JsonSerialize(typing=JsonSerialize.Typing.STATIC)
        private String state;

        public Item240(String id, String state) {
            this.id = id;
            this.state = state;
        }
    }

    /*
    /**********************************************************
    /* Helper classes: auto-detect configuration
    /**********************************************************
     */

    static class FieldBean
    {
        public String p1 = "public";
        protected String p2 = "protected";
        @SuppressWarnings("unused")
        private String p3 = "private";
    }

    @JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.PROTECTED_AND_PUBLIC)
    static class ProtFieldBean extends FieldBean { }

    static class MethodBean
    {
        public String getA() { return "a"; }
        protected String getB() { return "b"; }
        @SuppressWarnings("unused")
        private String getC() { return "c"; }
    }

    @JsonAutoDetect(getterVisibility=JsonAutoDetect.Visibility.PROTECTED_AND_PUBLIC)
    static class ProtMethodBean extends MethodBean { }

    final static class FieldBeanWithStatic
    {
        public int x = 1;

        public static int y = 2;

        // not even @JsonProperty should make statics usable...
        @JsonProperty public static int z = 3;
    }

    final static class GetterBeanWithStatic
    {
        public int getX() { return 3; }

        public static int getA() { return -3; }

        // not even @JsonProperty should make statics usable...
        @JsonProperty public static int getFoo() { return 123; }
    }

    /*
    /**********************************************************************
    /* Helper classes: cyclic type handling
    /**********************************************************************
     */

    static class CyclicBean
    {
        CyclicBean _next;
        final String _name;

        public CyclicBean(CyclicBean next, String name) {
            _next = next;
            _name = name;
        }

        public CyclicBean getNext() { return _next; }
        public String getName() { return _name; }

        public void assignNext(CyclicBean n) { _next = n; }
    }

    @JsonPropertyOrder({ "id", "parent" })
    static class Selfie2501 {
        public int id;

        public Selfie2501 parent;

        public Selfie2501(int id) { this.id = id; }
    }

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    static class Selfie2501AsArray extends Selfie2501 {
        public Selfie2501AsArray(int id) { super(id); }
    }

    /*
    /**********************************************************************
    /* Helper classes: virtual properties (JsonAppend)
    /**********************************************************************
     */

    @JsonAppend(attrs={ @JsonAppend.Attr("id"),
        @JsonAppend.Attr(value="internal", propName="extra", required=true)
    })
    static class VPropSimpleBean
    {
        public int value = 13;
    }

    @JsonAppend(prepend=true, attrs={ @JsonAppend.Attr("id"),
            @JsonAppend.Attr(value="internal", propName="extra")
        })
    static class VPropSimpleBeanPrepend
    {
        public int value = 13;
    }

    enum VPropABC {
        A, B, C;
    }

    @JsonAppend(attrs=@JsonAppend.Attr(value="desc", include=JsonInclude.Include.NON_EMPTY))
    static class OptionalsBean
    {
        public int value = 28;
    }

    static class CustomVProperty
        extends VirtualBeanPropertyWriter
    {
        private CustomVProperty() { super(); }

        private CustomVProperty(BeanPropertyDefinition propDef,
                Annotations ctxtAnn, JavaType type) {
            super(propDef, ctxtAnn, type);
        }

        @Override
        protected Object value(Object bean, JsonGenerator jgen, SerializationContext prov) {
            if (_name.toString().equals("id")) {
                return "abc123";
            }
            if (_name.toString().equals("extra")) {
                return new int[] { 42 };
            }
            return "???";
        }

        @Override
        public VirtualBeanPropertyWriter withConfig(MapperConfig<?> config,
                AnnotatedClass declaringClass, BeanPropertyDefinition propDef,
                JavaType type)
        {
            return new CustomVProperty(propDef, declaringClass.getAnnotations(), type);
        }
    }

    @JsonAppend(prepend=true, props={ @JsonAppend.Prop(value=CustomVProperty.class, name="id"),
            @JsonAppend.Prop(value=CustomVProperty.class, name="extra")
        })
    static class CustomVBean
    {
        public int value = 72;
    }

    /*
    /**********************************************************************
    /* Test methods, simple types
    /**********************************************************************
     */

    @Test
    public void testBoolean() throws Exception
    {
        assertEquals("true", MAPPER.writeValueAsString(Boolean.TRUE));
        assertEquals("false", MAPPER.writeValueAsString(Boolean.FALSE));
    }

    @Test
    public void testBooleanArray() throws Exception
    {
        assertEquals("[true,false]", MAPPER.writeValueAsString(new boolean[] { true, false} ));
        assertEquals("[true,false]", MAPPER.writeValueAsString(new Boolean[] { Boolean.TRUE, Boolean.FALSE} ));
    }

    @Test
    public void testByteArray() throws Exception
    {
        byte[] data = { 1, 17, -3, 127, -128 };
        Byte[] data2 = new Byte[data.length];
        for (int i = 0; i < data.length; ++i) {
            data2[i] = data[i]; // auto-boxing
        }
        // For this we need to deserialize, to get base64 codec
        String str1 = MAPPER.writeValueAsString(data);
        String str2 = MAPPER.writeValueAsString(data2);
        assertArrayEquals(data, MAPPER.readValue(str1, byte[].class));
        assertArrayEquals(data2, MAPPER.readValue(str2, Byte[].class));
    }

    // as per [Issue#42], allow Base64 variant use as well
    @Test
    public void testBase64Variants() throws Exception
    {
        final byte[] INPUT = "abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890X".getBytes("UTF-8");

        // default encoding is "MIME, no linefeeds", so:
        assertEquals(q("YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwYWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwWA=="), MAPPER.writeValueAsString(INPUT));
        assertEquals(q("YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwYWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwWA=="),
                MAPPER.writer(Base64Variants.MIME_NO_LINEFEEDS).writeValueAsString(INPUT));

        // but others should be slightly different
        assertEquals(q("YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwYWJjZGVmZ2hpamtsbW5vcHFyc3R1\\ndnd4eXoxMjM0NTY3ODkwWA=="),
                MAPPER.writer(Base64Variants.MIME).writeValueAsString(INPUT));
        assertEquals(q("YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwYWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwWA"), // no padding or LF
                MAPPER.writer(Base64Variants.MODIFIED_FOR_URL).writeValueAsString(INPUT));
        // PEM mandates 64 char lines:
        assertEquals(q("YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwYWJjZGVmZ2hpamts\\nbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwWA=="),
                MAPPER.writer(Base64Variants.PEM).writeValueAsString(INPUT));
    }

    @Test
    public void testClass() throws Exception
    {
        String result = MAPPER.writeValueAsString(java.util.List.class);
        assertEquals("\"java.util.List\"", result);
    }

    /*
    /**********************************************************************
    /* Test methods, array types
    /**********************************************************************
     */

    @Test
    public void testLongStringArray() throws Exception
    {
        final int SIZE = 40000;

        StringBuilder sb = new StringBuilder(SIZE*2);
        for (int i = 0; i < SIZE; ++i) {
            sb.append((char) i);
        }
        String str = sb.toString();
        byte[] data = MAPPER.writeValueAsBytes(new String[] { "abc", str, null, str });
        JsonParser p = MAPPER.createParser(data);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("abc", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        String actual = p.getString();
        assertEquals(str.length(), actual.length());
        assertEquals(str, actual);
        assertToken(JsonToken.VALUE_NULL, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(str, p.getString());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
        p.close();
    }

    @Test
    public void testIntArray() throws Exception
    {
        String json = MAPPER.writeValueAsString(new int[] { 1, 2, 3, -7 });
        assertEquals("[1,2,3,-7]", json);
    }

    @Test
    public void testBigIntArray() throws Exception
    {
        final int SIZE = 99999;
        int[] ints = new int[SIZE];
        for (int i = 0; i < ints.length; ++i) {
            ints[i] = i;
        }

        // Let's try couple of times, to ensure that state is handled
        // correctly by ObjectMapper (wrt buffer recycling used
        // with 'writeAsBytes()')
        for (int round = 0; round < 3; ++round) {
            byte[] data = MAPPER.writeValueAsBytes(ints);
            JsonParser p = MAPPER.createParser(data);
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            for (int i = 0; i < SIZE; ++i) {
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(i, p.getIntValue());
            }
            assertToken(JsonToken.END_ARRAY, p.nextToken());
            p.close();
        }
    }

    @Test
    public void testLongArray() throws Exception
    {
        String json = MAPPER.writeValueAsString(new long[] { Long.MIN_VALUE, 0, Long.MAX_VALUE });
        assertEquals("["+Long.MIN_VALUE+",0,"+Long.MAX_VALUE+"]", json);
    }

    @Test
    public void testStringArray() throws Exception
    {
        assertEquals("[\"a\",\"\\\"foo\\\"\",null]",
                MAPPER.writeValueAsString(new String[] { "a", "\"foo\"", null }));
        assertEquals("[]",
                MAPPER.writeValueAsString(new String[] { }));
    }

    @Test
    public void testDoubleArray() throws Exception
    {
        String json = MAPPER.writeValueAsString(new double[] { 1.01, 2.0, -7, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY });
        assertEquals("[1.01,2.0,-7.0,\"NaN\",\"-Infinity\",\"Infinity\"]", json);
    }

    @Test
    public void testFloatArray() throws Exception
    {
        String json = MAPPER.writeValueAsString(new float[] { 1.01f, 2.0f, -7f, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY });
        assertEquals("[1.01,2.0,-7.0,\"NaN\",\"-Infinity\",\"Infinity\"]", json);
    }

    /*
    /**********************************************************************
    /* Test methods, Jackson types
    /**********************************************************************
     */

    @Test
    public void testLocation() throws IOException
    {
        File f = new File("/tmp/test.json");
        TokenStreamLocation loc = new TokenStreamLocation(ContentReference.rawReference(f),
                -1, 100, 13);
        Map<String,Object> result = writeAndMap(MAPPER, loc);
        assertEquals(Integer.valueOf(-1), result.get("charOffset"));
        assertEquals(Integer.valueOf(-1), result.get("byteOffset"));
        assertEquals(Integer.valueOf(100), result.get("lineNr"));
        assertEquals(Integer.valueOf(13), result.get("columnNr"));
        assertEquals(4, result.size());
    }

    /**
     * Verify that {@link TokenBuffer} can be properly serialized
     * automatically, using the "standard" JSON sample document
     */
    @Test
    public void testTokenBuffer() throws Exception
    {
        // First, copy events from known good source (StringReader)
        JsonParser p = createParserUsingReader(SAMPLE_DOC_JSON_SPEC);
        TokenBuffer tb = TokenBuffer.forGeneration();
        while (p.nextToken() != null) {
            tb.copyCurrentEvent(p);
        }
        p.close();
        // Then serialize as String
        String str = MAPPER.writeValueAsString(tb);
        tb.close();
        // and verify it looks ok
        verifyJsonSpecSampleDoc(createParserUsingReader(str), true);
    }

    /*
    /**********************************************************************
    /* Test methods, field serialization
    /**********************************************************************
     */

    @Test
    public void testSimpleAutoDetect() throws Exception
    {
        SimpleFieldBean bean = new SimpleFieldBean();
        // let's set x, leave y as is
        bean.x = 13;
        Map<String,Object> result = writeAndMap(MAPPER, bean);
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(13), result.get("x"));
        assertEquals(Integer.valueOf(0), result.get("y"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSimpleAnnotation() throws Exception
    {
        SimpleFieldBean2 bean = new SimpleFieldBean2();
        bean.values = new String[] { "a", "b" };
        Map<String,Object> result = writeAndMap(MAPPER, bean);
        assertEquals(1, result.size());
        List<String> values = (List<String>) result.get("values");
        assertEquals(2, values.size());
        assertEquals("a", values.get(0));
        assertEquals("b", values.get(1));
    }

    @Test
    public void testTransientAndStatic() throws Exception
    {
        TransientBean bean = new TransientBean();
        Map<String,Object> result = writeAndMap(MAPPER, bean);
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(0), result.get("a"));
    }

    @Test
    public void testNoAutoDetect() throws Exception
    {
        NoAutoDetectBean bean = new NoAutoDetectBean();
        bean._z = -4;
        Map<String,Object> result = writeAndMap(MAPPER, bean);
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(-4), result.get("z"));
    }

    /**
     * Unit test that verifies that if both a field and a getter
     * method exist for a logical property (which is allowed),
     * getter has precedence over field.
     */
    @Test
    public void testMethodPrecedence() throws Exception
    {
        FieldAndMethodBean bean = new FieldAndMethodBean();
        bean.z = 9;
        assertEquals(10, bean.getZ());
        assertEquals("{\"z\":10}", MAPPER.writeValueAsString(bean));
    }

    /**
     * Testing [JACKSON-226]: it is ok to have "field override",
     * as long as there are no intra-class conflicts.
     */
    @Test
    public void testOkDupFields() throws Exception
    {
        OkDupFieldBean bean = new OkDupFieldBean(1, 2);
        Map<String,Object> json = writeAndMap(MAPPER, bean);
        assertEquals(2, json.size());
        assertEquals(Integer.valueOf(1), json.get("x"));
        assertEquals(Integer.valueOf(2), json.get("y"));
    }

    @Test
    public void testIssue240() throws Exception
    {
        Item240 bean = new Item240("a12", null);
        assertEquals(MAPPER.writeValueAsString(bean), "{\"id\":\"a12\"}");
    }

    @Test
    public void testFailureDueToDupField1() throws Exception
    {
        try {
            final String json = MAPPER.writeValueAsString(new DupFieldBean());
            fail("Should not pass, got: "+json);
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Multiple fields representing");
        }
    }

    @Test
    public void testResolvedDuplicate() throws Exception
    {
        String json = MAPPER.writeValueAsString(new DupFieldBean2());
        assertEquals(json, a2q("{'z':4}"));
    }

    /*
    /**********************************************************************
    /* Test methods, auto-detect configuration
    /**********************************************************************
     */

    @Test
    public void testDefaults() throws Exception
    {
        // by default, only public fields and getters are detected
        assertEquals("{\"p1\":\"public\"}",
                MAPPER.writeValueAsString(new FieldBean()));
        assertEquals("{\"a\":\"a\"}",
                MAPPER.writeValueAsString(new MethodBean()));
    }

    @Test
    public void testProtectedViaAnnotations() throws Exception
    {
        Map<String,Object> result = writeAndMap(MAPPER, new ProtFieldBean());
        assertEquals(2, result.size());
        assertEquals("public", result.get("p1"));
        assertEquals("protected", result.get("p2"));
        assertNull(result.get("p3"));

        result = writeAndMap(MAPPER, new ProtMethodBean());
        assertEquals(2, result.size());
        assertEquals("a", result.get("a"));
        assertEquals("b", result.get("b"));
        assertNull(result.get("c"));
    }

    @Test
    public void testStaticFields() throws Exception
    {
        Map<String,Object> result = writeAndMap(MAPPER, new FieldBeanWithStatic());
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(1), result.get("x"));
    }

    @Test
    public void testStaticMethods() throws Exception
    {
        Map<String,Object> result = writeAndMap(MAPPER, new GetterBeanWithStatic());
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(3), result.get("x"));
    }

    @Test
    public void testPrivateUsingGlobals() throws Exception
    {
        ObjectMapper m = jsonMapperBuilder()
                .changeDefaultVisibility(vc ->
                    vc.withFieldVisibility(JsonAutoDetect.Visibility.ANY))
                .build();

        Map<String,Object> result = writeAndMap(m, new FieldBean());
        assertEquals(3, result.size());
        assertEquals("public", result.get("p1"));
        assertEquals("protected", result.get("p2"));
        assertEquals("private", result.get("p3"));

        m = jsonMapperBuilder()
                .changeDefaultVisibility(vc ->
                    vc.withGetterVisibility(JsonAutoDetect.Visibility.ANY)
                    )
                .build();
        result = writeAndMap(m, new MethodBean());
        assertEquals(3, result.size());
        assertEquals("a", result.get("a"));
        assertEquals("b", result.get("b"));
        assertEquals("c", result.get("c"));
    }

    @Test
    public void testBasicSetup() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultVisibility(vc ->
                    vc.with(JsonAutoDetect.Visibility.ANY))
                .build();
        Map<String,Object> result = writeAndMap(mapper, new FieldBean());
        assertEquals(3, result.size());
        assertEquals("public", result.get("p1"));
        assertEquals("protected", result.get("p2"));
        assertEquals("private", result.get("p3"));
    }

    @Test
    public void testMapperShortcutMethods() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultVisibility(vc -> vc
                        .withVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY))
                .build();

        Map<String,Object> result = writeAndMap(mapper, new FieldBean());
        assertEquals(3, result.size());
        assertEquals("public", result.get("p1"));
        assertEquals("protected", result.get("p2"));
        assertEquals("private", result.get("p3"));
    }

    /*
    /**********************************************************************
    /* Test methods, cyclic types
    /**********************************************************************
     */

    @Test
    public void testLinkedButNotCyclic() throws Exception
    {
        CyclicBean last = new CyclicBean(null, "last");
        CyclicBean first = new CyclicBean(last, "first");
        Map<String,Object> map = writeAndMap(MAPPER, first);

        assertEquals(2, map.size());
        assertEquals("first", map.get("name"));

        @SuppressWarnings("unchecked")
        Map<String,Object> map2 = (Map<String,Object>) map.get("next");
        assertNotNull(map2);
        assertEquals(2, map2.size());
        assertEquals("last", map2.get("name"));
        assertNull(map2.get("next"));
    }

    @Test
    public void testSimpleDirectSelfReference() throws Exception
    {
        CyclicBean selfRef = new CyclicBean(null, "self-refs");
        CyclicBean first = new CyclicBean(selfRef, "first");
        selfRef.assignNext(selfRef);
        CyclicBean[] wrapper = new CyclicBean[] { first };
        try {
            writeAndMap(MAPPER, wrapper);
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Direct self-reference leading to cycle");
        }
    }

    // [databind#2501]: Should be possible to replace null cyclic ref
    @Test
    public void testReplacedCycle() throws Exception
    {
        Selfie2501 self1 = new Selfie2501(1);
        self1.parent = self1;
        ObjectWriter w = MAPPER.writer()
                .without(SerializationFeature.FAIL_ON_SELF_REFERENCES)
                .with(SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL)
                ;
        assertEquals(a2q("{'id':1,'parent':null}"), w.writeValueAsString(self1));

        // Also consider a variant of cyclic POJO in container
        Selfie2501AsArray self2 = new Selfie2501AsArray(2);
        self2.parent = self2;
        assertEquals(a2q("[2,null]"), w.writeValueAsString(self2));
    }

    /*
    /**********************************************************************
    /* Test methods, virtual properties (JsonAppend)
    /**********************************************************************
     */

    @Test
    public void testAttributeProperties() throws Exception
    {
        Map<String,Object> stuff = new LinkedHashMap<>();
        stuff.put("x", 3);
        stuff.put("y", VPropABC.B);

        String json = WRITER.withAttribute("id", "abc123")
                .withAttribute("internal", stuff)
                .writeValueAsString(new VPropSimpleBean());
        assertEquals(a2q("{'value':13,'id':'abc123','extra':{'x':3,'y':'B'}}"), json);

        json = WRITER.withAttribute("id", "abc123")
                .withAttribute("internal", stuff)
                .writeValueAsString(new VPropSimpleBeanPrepend());
        assertEquals(a2q("{'id':'abc123','extra':{'x':3,'y':'B'},'value':13}"), json);
    }

    @Test
    public void testAttributePropInclusion() throws Exception
    {
        // first, with desc
        String json = WRITER.withAttribute("desc", "nice")
                .writeValueAsString(new OptionalsBean());
        assertEquals(a2q("{'value':28,'desc':'nice'}"), json);

        // then with null (not defined)
        json = WRITER.writeValueAsString(new OptionalsBean());
        assertEquals(a2q("{'value':28}"), json);

        // and finally "empty"
        json = WRITER.withAttribute("desc", "")
                .writeValueAsString(new OptionalsBean());
        assertEquals(a2q("{'value':28}"), json);
    }

    @Test
    public void testCustomProperties() throws Exception
    {
        String json = WRITER.withAttribute("desc", "nice")
                .writeValueAsString(new CustomVBean());
        assertEquals(a2q("{'id':'abc123','extra':[42],'value':72}"), json);
    }
}
