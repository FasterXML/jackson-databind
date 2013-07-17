package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Tests for github issues #138
 * https://github.com/FasterXML/jackson-databind/issues/138
 */
public class TestObjectIdSerialization extends BaseMapTest {

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    static protected class Obj {

        public int id;
        @JsonIdentityReference(alwaysAsId = true)
        public SetContainer objGroup;

        public Obj() {
        }

        public Obj(int id) {
            this.id = id;
        }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    static protected class SetContainer {

        public int id;

        @JsonIdentityReference(alwaysAsId = true)
        public Set<Obj> objs = new LinkedHashSet<Obj>();

        public SetContainer() { }

        public SetContainer(int id) {
            this.id = id;
        }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    static protected class ArrContainer {

        public int id;
        @JsonIdentityReference(alwaysAsId = true)
        public Obj[] objs;

        public ArrContainer() {
        }

        public ArrContainer(int id) {
            this.id = id;
        }
    }
    private final ObjectMapper MAPPER = new ObjectMapper();
    private final static String EXP_ARR_SET = "{\"id\":4,\"objs\":[1,7]}";

    public void testArraySerialization() throws Exception {
        Obj o = new Obj(1);
        Obj o2 = new Obj(7);
        ArrContainer oa = new ArrContainer(4);
        oa.objs = new Obj[]{o, o2};
        String json = MAPPER.writeValueAsString(oa);
        assertEquals(EXP_ARR_SET, json);
    }

    public void testCollectionSerialization() throws Exception {
        Obj o = new Obj(1);
        Obj o2 = new Obj(7);
        SetContainer os = new SetContainer(4);
        os.objs.add(o);
        os.objs.add(o2);
        String json = MAPPER.writeValueAsString(os);
        assertEquals(EXP_ARR_SET, json);
    }

    public void testDeserialization() throws Exception {
        Obj o = new Obj(1);
        o.objGroup = new SetContainer(4);
        String json = MAPPER.writeValueAsString(o);
        Obj deser = MAPPER.readValue(json, Obj.class);
        assertEquals(deser.id, 1);
        assertEquals(deser.objGroup.id, 4);
    }
}
