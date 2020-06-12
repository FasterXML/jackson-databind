package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.junit.Assert;

import java.util.List;


/**
 * Tests for sub-types id user uses,
 *      1. only names in all sub-types
 *      2. 'names' for multi-keys while 'name' for singular keys
 *
 * [ annotations#171 ]
 * As per this feature, keys holding similar java object types can be incorporated
 * within the same annotation.
 * See this <a href="https://github.com/FasterXML/jackson-annotations/issues/171">issue/improvement</a>
 */
public class TestMultipleTypeNames extends BaseMapTest {

    // serializer-deserializer
    private final ObjectMapper MAPPER = objectMapper();


    // common classes
    static class MultiTypeName { }

    static class A extends MultiTypeName {
        private long x;
        public long getX() { return x; }
    }

    static class B extends MultiTypeName {
        private float y;
        public float getY() { return y; }
    }





    // data for test 1
    static class WrapperForNamesTest {
        private List<BaseForNamesTest> base;
        public List<BaseForNamesTest> getBase() { return base; }
    }

    static class BaseForNamesTest {
        private String type;
        public String getType() { return type; }

        @JsonTypeInfo (
                use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = "type"
        )
        @JsonSubTypes (value = {
                @JsonSubTypes.Type(value = A.class, names = "a"),
                @JsonSubTypes.Type(value = B.class, names = {"b","c"}),
        })
        private MultiTypeName data;
        public MultiTypeName getData() { return data; }
    }

    public void testOnlyNames () throws Exception {
        String json;
        WrapperForNamesTest w;

        // TC 1 : all KV serialisation
        json = "{\"base\": [{\"type\":\"a\", \"data\": {\"x\": 5}}, {\"type\":\"b\", \"data\": {\"y\": 3.1}}, {\"type\":\"c\", \"data\": {\"y\": 33.8}}]}";
        w = MAPPER.readValue(json, WrapperForNamesTest.class);
        Assert.assertNotNull(w);
        Assert.assertEquals(3, w.base.size());
        Assert.assertTrue(w.base.get(0).data instanceof A);
        Assert.assertEquals(5l, ((A) w.base.get(0).data).x);
        Assert.assertTrue(w.base.get(1).data instanceof B);
        Assert.assertEquals(3.1f, ((B) w.base.get(1).data).y, 0);
        Assert.assertTrue(w.base.get(2).data instanceof B);
        Assert.assertEquals(33.8f, ((B) w.base.get(2).data).y, 0);


        // TC 2 : incorrect serialisation
        json = "{\"data\": [{\"type\":\"a\", \"data\": {\"x\": 2.2}}, {\"type\":\"b\", \"data\": {\"y\": 5.3}}, {\"type\":\"c\", \"data\": {\"y\": 9.8}}]}";
        try  {
            MAPPER.readValue(json, WrapperForNamesTest.class);
            Assert.fail("This serialisation should fail 'coz of x being float");
        } catch (UnrecognizedPropertyException e) {}
    }





    // data for test 2
    static class WrapperForNameAndNamesTest {
        private List<BaseForNameAndNamesTest> base;
        public List<BaseForNameAndNamesTest> getBase() { return base; }
    }

    static class BaseForNameAndNamesTest {
        private String type;
        public String getType() { return type; }

        @JsonTypeInfo (
                use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = "type"
        )
        @JsonSubTypes (value = {
                @JsonSubTypes.Type(value = A.class, name = "a"),
                @JsonSubTypes.Type(value = B.class, names = {"b","c"}),
        })
        private MultiTypeName data;
        public MultiTypeName getData() { return data; }
    }

    public void testNameAndNames () throws Exception {
        String json;
        WrapperForNameAndNamesTest w;

        // TC 1 : all KV serialisation
        json = "{\"base\": [{\"type\":\"a\", \"data\": {\"x\": 5}}, {\"type\":\"b\", \"data\": {\"y\": 3.1}}, {\"type\":\"c\", \"data\": {\"y\": 33.8}}]}";
        w = MAPPER.readValue(json, WrapperForNameAndNamesTest.class);
        Assert.assertNotNull(w);
        Assert.assertEquals(3, w.base.size());
        Assert.assertTrue(w.base.get(0).data instanceof A);
        Assert.assertEquals(5l, ((A) w.base.get(0).data).x);
        Assert.assertTrue(w.base.get(1).data instanceof B);
        Assert.assertEquals(3.1f, ((B) w.base.get(1).data).y, 0);
        Assert.assertTrue(w.base.get(2).data instanceof B);
        Assert.assertEquals(33.8f, ((B) w.base.get(2).data).y, 0);


        // TC 2 : incorrect serialisation
        json = "{\"data\": [{\"type\":\"a\", \"data\": {\"x\": 2.2}}, {\"type\":\"b\", \"data\": {\"y\": 5.3}}, {\"type\":\"c\", \"data\": {\"y\": 9.8}}]}";
        try  {
            MAPPER.readValue(json, WrapperForNameAndNamesTest.class);
            Assert.fail("This serialisation should fail 'coz of x being float");
        } catch (UnrecognizedPropertyException e) {}
    }

}
