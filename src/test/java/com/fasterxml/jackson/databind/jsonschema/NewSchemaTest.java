package com.fasterxml.jackson.databind.jsonschema;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.*;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;

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

    @JsonPropertyOrder({ "dec", "bigInt" })
    static class Numbers {
        public BigDecimal dec;
        public BigInteger bigInt;
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */
    
    private final ObjectMapper MAPPER = new ObjectMapper();

    /* Silly little test for simply triggering traversal, without attempting to
     * verify what is being reported. Smoke test that should trigger problems
     * if basic POJO type/serializer traversal had issues.
     */
    public void testBasicTraversal() throws Exception
    {
        MAPPER.acceptJsonFormatVisitor(POJO.class, new JsonFormatVisitorWrapper.Base());
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
        MAPPER.acceptJsonFormatVisitor(TestEnumWithJsonValue.class, new JsonFormatVisitorWrapper.Base() {
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

    // [2.7]: Ensure JsonValueFormat serializes/deserializes as expected
    public void testJsonValueFormatHandling() throws Exception
    {
        // first: serialize using 'toString()', not name
        final String EXP = quote("host-name");
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
                                    JavaType t) throws JsonMappingException {
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
                            public JsonIntegerFormatVisitor expectIntegerFormat(JavaType t) throws JsonMappingException {
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
