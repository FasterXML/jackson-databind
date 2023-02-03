package com.fasterxml.jackson.databind.jsonschema;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.*;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Basic tests to exercise low-level support added for JSON Schema module and
 * other modules that use type introspection.
 */
public class NewSchemaTest extends BaseMapTest
{
    enum TestEnum {
        A, B, C;

        @Override
        public String toString() {
            return "ToString:"+name();
        }
    }

    enum TestEnumWithJsonValue {
        A, B, C;

        @JsonValue
        public String forSerialize() {
            return "value-"+name();
        }
    }

    // silly little class to exercise basic traversal
    static class POJO {
        public List<POJO> children;
        public POJO[] childOrdering;
        public Map<String, java.util.Date> times;
        public Map<String,Integer> conversions;

        public EnumMap<TestEnum,Double> weights;
    }

    static class POJOWithScalars {
        public boolean boo;
        public byte b;
        public char c;
        public short s;
        public int i;
        public long l;
        public float f;
        public double d;

        public byte[] arrayBoo;
        public byte[] arrayb;
        public char[] arrayc;
        public short[] arrays;
        public int[] arrayi;
        public long[] arrayl;
        public float[] arrayf;
        public double[] arrayd;

        public Boolean Boo;
        public Byte B;
        public Character C;
        public Short S;
        public Integer I;
        public Long L;
        public Float F;
        public Double D;

        public TestEnum en;
        public String str;
        public String[] strs;
        public java.util.Date date;
        public java.util.Calendar calendar;
    }

    static class POJOWithRefs {
        public AtomicReference<POJO> maybePOJO;

        public AtomicReference<String> maybeString;
    }

    // [databind#1793]
    static class POJOWithJsonValue {
        private Point[] value;

        @JsonCreator(mode=JsonCreator.Mode.DELEGATING)
        public POJOWithJsonValue(Point[] v) { value = v; }

        @JsonValue
        public Point[] serialization() { return value; }
    }

    @JsonPropertyOrder({ "dec", "bigInt" })
    static class Numbers {
        public BigDecimal dec;
        public BigInteger bigInt;
    }

    static class BogusJsonFormatVisitorWrapper
        extends JsonFormatVisitorWrapper.Base
    {
        // Implement handlers just to get more exercise...

        @Override
        public JsonObjectFormatVisitor expectObjectFormat(JavaType type) {
            return new JsonObjectFormatVisitor.Base(getProvider()) {
                @Override
                public void property(BeanProperty prop) throws JsonMappingException {
                    _visit(prop);
                }

                @Override
                public void property(String name, JsonFormatVisitable handler,
                        JavaType propertyTypeHint) { }

                @Override
                public void optionalProperty(BeanProperty prop) throws JsonMappingException {
                    _visit(prop);
                }

                @Override
                public void optionalProperty(String name, JsonFormatVisitable handler,
                        JavaType propertyTypeHint) { }

                private void _visit(BeanProperty prop) throws JsonMappingException
                {
                    if (!(prop instanceof BeanPropertyWriter)) {
                        return;
                    }
                    BeanPropertyWriter bpw = (BeanPropertyWriter) prop;
                    JsonSerializer<?> ser = bpw.getSerializer();
                    final SerializerProvider prov = getProvider();
                    if (ser == null) {
                        if (prov == null) {
                            throw new Error("SerializerProvider missing");
                        }
                        ser = prov.findValueSerializer(prop.getType(), prop);
                    }
                    // and this just for bit of extra coverage...
                    if (ser instanceof StdSerializer) {
                        @SuppressWarnings("deprecation")
                        JsonNode schemaNode = ((StdSerializer<?>) ser).getSchema(prov, prop.getType());
                        assertNotNull(schemaNode);
                    }
                    JsonFormatVisitorWrapper visitor = new JsonFormatVisitorWrapper.Base(getProvider());
                    ser.acceptJsonFormatVisitor(visitor, prop.getType());
                }
            };
        }

        @Override
        public JsonArrayFormatVisitor expectArrayFormat(JavaType type) {
            return new JsonArrayFormatVisitor.Base(getProvider());
        }

        @Override
        public JsonStringFormatVisitor expectStringFormat(JavaType type) {
            return new JsonStringFormatVisitor.Base();
        }

        @Override
        public JsonNumberFormatVisitor expectNumberFormat(JavaType type) {
            return new JsonNumberFormatVisitor.Base();
        }

        @Override
        public JsonIntegerFormatVisitor expectIntegerFormat(JavaType type) {
            return new JsonIntegerFormatVisitor.Base();
        }

        @Override
        public JsonBooleanFormatVisitor expectBooleanFormat(JavaType type) {
            return new JsonBooleanFormatVisitor.Base();
        }

        @Override
        public JsonNullFormatVisitor expectNullFormat(JavaType type) {
            return new JsonNullFormatVisitor.Base();
        }

        @Override
        public JsonAnyFormatVisitor expectAnyFormat(JavaType type) {
            return new JsonAnyFormatVisitor.Base();
        }

        @Override
        public JsonMapFormatVisitor expectMapFormat(JavaType type) {
            return new JsonMapFormatVisitor.Base();
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    /* Silly little test for simply triggering traversal, without attempting to
     * verify what is being reported. Smoke test that should trigger problems
     * if basic POJO type/serializer traversal had issues.
     */
    public void testBasicTraversal() throws Exception
    {
        MAPPER.acceptJsonFormatVisitor(POJO.class, new BogusJsonFormatVisitorWrapper());
        MAPPER.acceptJsonFormatVisitor(POJOWithScalars.class, new BogusJsonFormatVisitorWrapper());
        MAPPER.acceptJsonFormatVisitor(LinkedHashMap.class, new BogusJsonFormatVisitorWrapper());
        MAPPER.acceptJsonFormatVisitor(ArrayList.class, new BogusJsonFormatVisitorWrapper());
        MAPPER.acceptJsonFormatVisitor(EnumSet.class, new BogusJsonFormatVisitorWrapper());

        MAPPER.acceptJsonFormatVisitor(POJOWithRefs.class, new BogusJsonFormatVisitorWrapper());

        MAPPER.acceptJsonFormatVisitor(POJOWithJsonValue.class, new BogusJsonFormatVisitorWrapper());
    }

    public void testSimpleEnum() throws Exception
    {
        final Set<String> values = new TreeSet<String>();
        ObjectWriter w = MAPPER.writer(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);

        w.acceptJsonFormatVisitor(TestEnum.class, new JsonFormatVisitorWrapper.Base() {
            @Override
            public JsonStringFormatVisitor expectStringFormat(JavaType type) {
                return new JsonStringFormatVisitor() {
                    @Override
                    public void enumTypes(Set<String> enums) {
                        values.addAll(enums);
                    }

                    @Override
                    public void format(JsonValueFormat format) { }
                };
            }
        });

        assertEquals(3, values.size());
        TreeSet<String> exp = new TreeSet<String>(Arrays.asList(
                        "ToString:A",
                        "ToString:B",
                        "ToString:C"
                        ));
        assertEquals(exp, values);
    }

    public void testEnumWithJsonValue() throws Exception
    {
        final Set<String> values = new TreeSet<String>();
        MAPPER.acceptJsonFormatVisitor(TestEnumWithJsonValue.class,
                new JsonFormatVisitorWrapper.Base() {
            @Override
            public JsonStringFormatVisitor expectStringFormat(JavaType type) {
                return new JsonStringFormatVisitor() {
                    @Override
                    public void enumTypes(Set<String> enums) {
                        values.addAll(enums);
                    }

                    @Override
                    public void format(JsonValueFormat format) { }
                };
            }
        });

        assertEquals(3, values.size());
        TreeSet<String> exp = new TreeSet<String>(Arrays.asList(
                        "value-A",
                        "value-B",
                        "value-C"
                        ));
        assertEquals(exp, values);
    }

    //  Ensure JsonValueFormat serializes/deserializes as expected
    public void testJsonValueFormatHandling() throws Exception
    {
        // first: serialize using 'toString()', not name
        final String EXP = q("host-name");
        assertEquals(EXP, MAPPER.writeValueAsString(JsonValueFormat.HOST_NAME));

        // and second, deserialize ok from that as well
        assertSame(JsonValueFormat.HOST_NAME, MAPPER.readValue(EXP, JsonValueFormat.class));
    }

    // [databind#1045], regression wrt BigDecimal
    public void testSimpleNumbers() throws Exception
    {
        final StringBuilder sb = new StringBuilder();

        MAPPER.acceptJsonFormatVisitor(Numbers.class,
                new JsonFormatVisitorWrapper.Base() {
            @Override
            public JsonObjectFormatVisitor expectObjectFormat(final JavaType type) {
                return new JsonObjectFormatVisitor.Base(getProvider()) {
                    @Override
                    public void optionalProperty(BeanProperty prop) throws JsonMappingException {
                        sb.append("[optProp ").append(prop.getName()).append("(");
                        JsonSerializer<Object> ser = null;
                        if (prop instanceof BeanPropertyWriter) {
                            BeanPropertyWriter bpw = (BeanPropertyWriter) prop;
                            ser = bpw.getSerializer();
                        }
                        final SerializerProvider prov = getProvider();
                        if (ser == null) {
                            ser = prov.findValueSerializer(prop.getType(), prop);
                        }
                        ser.acceptJsonFormatVisitor(new JsonFormatVisitorWrapper.Base() {
                            @Override
                            public JsonNumberFormatVisitor expectNumberFormat(
                                    JavaType t) {
                                return new JsonNumberFormatVisitor() {
                                    @Override
                                    public void format(JsonValueFormat format) {
                                        sb.append("[numberFormat=").append(format).append("]");
                                    }

                                    @Override
                                    public void enumTypes(Set<String> enums) { }

                                    @Override
                                    public void numberType(NumberType numberType) {
                                        sb.append("[numberType=").append(numberType).append("]");
                                    }
                                };
                            }

                            @Override
                            public JsonIntegerFormatVisitor expectIntegerFormat(JavaType t) {
                                return new JsonIntegerFormatVisitor() {
                                    @Override
                                    public void format(JsonValueFormat format) {
                                        sb.append("[integerFormat=").append(format).append("]");
                                    }

                                    @Override
                                    public void enumTypes(Set<String> enums) { }

                                    @Override
                                    public void numberType(NumberType numberType) {
                                        sb.append("[numberType=").append(numberType).append("]");
                                    }
                                };
                            }
                        }, prop.getType());

                        sb.append(")]");
                    }
                };
            }
        });
        assertEquals("[optProp dec([numberType=BIG_DECIMAL])][optProp bigInt([numberType=BIG_INTEGER])]",
                sb.toString());
    }
}
