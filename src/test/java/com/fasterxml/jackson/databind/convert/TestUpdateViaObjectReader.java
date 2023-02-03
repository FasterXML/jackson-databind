package com.fasterxml.jackson.databind.convert;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

import static org.junit.Assert.assertArrayEquals;

/**
 * Unit tests for verifying that "updating reader" works as
 * expected.
 */
@SuppressWarnings("serial")
public class TestUpdateViaObjectReader extends BaseMapTest
{
    static class Bean {
        public String a = "a";
        public String b = "b";

        public int[] c = new int[] { 1, 2, 3 };

        public Bean child = null;
    }

    static class XYBean {
        public int x, y;
    }

    public class TextView {}
    public class NumView {}

    public class Updateable {
        @JsonView(NumView.class)
        public int num;

        @JsonView(TextView.class)
        public String str;
    }

    // for [databind#744]
    static class DataA {
        public int i = 1;
        public int j = 2;

    }

    static class DataB {
        public DataA da = new DataA();
        public int k = 3;
    }

    static class DataADeserializer extends StdDeserializer<DataA> {
        private static final long serialVersionUID = 1L;

        DataADeserializer() {
            super(DataA.class);
        }

        @Override
        public DataA deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.currentToken() != JsonToken.START_OBJECT) {
                ctxt.reportInputMismatch(DataA.class,
                        "Wrong current token, expected START_OBJECT, got: "
                        +p.currentToken());
                // never gets here
            }
            /*JsonNode node =*/ p.readValueAsTree();

            DataA da = new DataA();
            da.i = 5;
            return da;
        }
    }

    // [databind#1831]
    @JsonTypeInfo(use = Id.NAME)
    @JsonSubTypes({  @JsonSubTypes.Type(value = Cat.class) })
    static abstract public class AbstractAnimal { }

    @JsonDeserialize(using = AnimalWrapperDeserializer.class)
    static class AnimalWrapper {
        @JsonUnwrapped
        protected AbstractAnimal animal;

        public void setAnimal(AbstractAnimal animal) {
            this.animal = animal;
        }
    }

    static class Cat extends AbstractAnimal { }

    static class AnimalWrapperDeserializer extends StdDeserializer<AnimalWrapper> {
        public AnimalWrapperDeserializer() {
            super(AnimalWrapper.class);
        }

        @Override
        public AnimalWrapper deserialize(JsonParser json, DeserializationContext context) throws IOException {
            AnimalWrapper msg = new AnimalWrapper();
            msg.setAnimal(json.readValueAs(AbstractAnimal.class));
            return msg;
        }

        @Override
        public AnimalWrapper deserialize(JsonParser json, DeserializationContext context, AnimalWrapper intoValue) throws IOException {
            intoValue.setAnimal(json.readValueAs(AbstractAnimal.class));
            return intoValue;
        }
    }

    /*
    /********************************************************
    /* Test methods
    /********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testBeanUpdate() throws Exception
    {
        Bean bean = new Bean();
        assertEquals("b", bean.b);
        assertEquals(3, bean.c.length);
        assertNull(bean.child);

        Object ob = MAPPER.readerForUpdating(bean).readValue("{ \"b\":\"x\", \"c\":[4,5], \"child\":{ \"a\":\"y\"} }");
        assertSame(ob, bean);

        assertEquals("a", bean.a);
        assertEquals("x", bean.b);
        assertArrayEquals(new int[] { 4, 5 }, bean.c);

        Bean child = bean.child;
        assertNotNull(child);
        assertEquals("y", child.a);
        assertEquals("b", child.b);
        assertArrayEquals(new int[] { 1, 2, 3 }, child.c);
        assertNull(child.child);

        // 30-Oct-2022, tatu: Also test that `null` is ok for "readerForUpdating()" to
        //    to remove "to-update" Object

        Bean b2 = MAPPER.readerForUpdating(null)
                .forType(Bean.class)
                .readValue(a2q("{'b':'abc'}"));
        assertEquals("abc", b2.b);

        // and similarly via ObjectReader
        Bean b3 = MAPPER.readerForUpdating(b2)
                .withValueToUpdate(null)
                .readValue(a2q("{'b':'xyz'}"));
        assertNotSame(b2, b3);
        assertEquals("xyz", b3.b);
    }

    public void testListUpdate() throws Exception
    {
        List<String> strs = new ArrayList<String>();
        strs.add("a");
        // for lists, we will be appending entries
        Object ob = MAPPER.readerForUpdating(strs).readValue("[ \"b\", \"c\", \"d\" ]");
        assertSame(strs, ob);
        assertEquals(4, strs.size());
        assertEquals("a", strs.get(0));
        assertEquals("b", strs.get(1));
        assertEquals("c", strs.get(2));
        assertEquals("d", strs.get(3));
    }

    public void testMapUpdate() throws Exception
    {
        Map<String,String> strs = new HashMap<String,String>();
        strs.put("a", "a");
        strs.put("b", "b");
        // for maps, we will be adding and/or overwriting entries
        Object ob = MAPPER.readerForUpdating(strs).readValue("{ \"c\" : \"c\", \"a\" : \"z\" }");
        assertSame(strs, ob);
        assertEquals(3, strs.size());
        assertEquals("z", strs.get("a"));
        assertEquals("b", strs.get("b"));
        assertEquals("c", strs.get("c"));
    }

    // Test for [JACKSON-717] -- ensure 'readValues' also does update
    @SuppressWarnings("resource")
    public void testUpdateSequence() throws Exception
    {
        XYBean toUpdate = new XYBean();
        Iterator<XYBean> it = MAPPER.readerForUpdating(toUpdate).readValues(
                "{\"x\":1,\"y\":2}\n{\"x\":16}{\"y\":37}");

        assertTrue(it.hasNext());
        XYBean value = it.next();
        assertSame(toUpdate, value);
        assertEquals(1, value.x);
        assertEquals(2, value.y);

        assertTrue(it.hasNext());
        value = it.next();
        assertSame(toUpdate, value);
        assertEquals(16, value.x);
        assertEquals(2, value.y); // unchanged

        assertTrue(it.hasNext());
        value = it.next();
        assertSame(toUpdate, value);
        assertEquals(16, value.x); // unchanged
        assertEquals(37, value.y);

        assertFalse(it.hasNext());
    }

    // [JACKSON-824]
    public void testUpdatingWithViews() throws Exception
    {
        Updateable bean = new Updateable();
        bean.num = 100;
        bean.str = "test";
        Updateable result = MAPPER.readerForUpdating(bean)
                .withView(TextView.class)
                .readValue("{\"num\": 10, \"str\":\"foobar\"}");
        assertSame(bean, result);

        assertEquals(100, bean.num);
        assertEquals("foobar", bean.str);
    }

    // [databind#744]
    public void testIssue744() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(DataA.class, new DataADeserializer());
        mapper.registerModule(module);

        DataB db = new DataB();
        db.da.i = 11;
        db.k = 13;
        String jsonBString = mapper.writeValueAsString(db);
        JsonNode jsonBNode = mapper.valueToTree(db);

        // create parent
        DataB dbNewViaString = mapper.readValue(jsonBString, DataB.class);
        assertEquals(5, dbNewViaString.da.i);
        assertEquals(13, dbNewViaString.k);

        DataB dbNewViaNode = mapper.treeToValue(jsonBNode, DataB.class);
        assertEquals(5, dbNewViaNode.da.i);
        assertEquals(13, dbNewViaNode.k);

        // update parent
        DataB dbUpdViaString = new DataB();
        DataB dbUpdViaNode = new DataB();

        assertEquals(1, dbUpdViaString.da.i);
        assertEquals(3, dbUpdViaString.k);
        mapper.readerForUpdating(dbUpdViaString).readValue(jsonBString);
        assertEquals(5, dbUpdViaString.da.i);
        assertEquals(13, dbUpdViaString.k);

        assertEquals(1, dbUpdViaNode.da.i);
        assertEquals(3, dbUpdViaNode.k);

        mapper.readerForUpdating(dbUpdViaNode).readValue(jsonBNode);
        assertEquals(5, dbUpdViaNode.da.i);
        assertEquals(13, dbUpdViaNode.k);
    }

    // [databind#1831]
    public void test1831UsingNode() throws IOException {
        String catJson = MAPPER.writeValueAsString(new Cat());
        JsonNode jsonNode = MAPPER.readTree(catJson);
        AnimalWrapper optionalCat = new AnimalWrapper();
        ObjectReader r = MAPPER.readerForUpdating(optionalCat);
        AnimalWrapper result = r.readValue(jsonNode);
        assertSame(optionalCat, result);
    }

    public void test1831UsingString() throws IOException {
        String catJson = MAPPER.writeValueAsString(new Cat());
        AnimalWrapper optionalCat = new AnimalWrapper();
        AnimalWrapper result = MAPPER.readerForUpdating(optionalCat).readValue(catJson);
        assertSame(optionalCat, result);
    }
}
