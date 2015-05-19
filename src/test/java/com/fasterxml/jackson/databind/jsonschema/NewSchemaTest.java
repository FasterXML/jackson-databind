package com.fasterxml.jackson.databind.jsonschema;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonStringFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonValueFormat;

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
}
