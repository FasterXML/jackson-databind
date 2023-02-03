package com.fasterxml.jackson.databind.misc;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

public class CaseInsensitiveDeserTest extends BaseMapTest
{
    // [databind#1036]
    static class BaseResponse {
        public int errorCode;
        public String debugMessage;
    }

    static class Issue476Bean {
        public Issue476Type value1, value2;
    }
    static class Issue476Type {
        public String name, value;
    }

    // [databind#1232]: allow per-property case-insensitivity
    static class Role {
        public String ID;
        public String Name;
    }

    static class CaseInsensitiveRoleWrapper
    {
        @JsonFormat(with={ JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES })
        public Role role;
    }

    // [databind#1438]
    static class InsensitiveCreator
    {
        int v;

        @JsonCreator
        public InsensitiveCreator(@JsonProperty("value") int v0) {
            v = v0;
        }
    }

    // [databind#1854]
    static class Obj1854 {
        private final int id;

        private final List<ChildObj1854> items;

        public Obj1854(int id, List<ChildObj1854> items) {
            this.id = id;
            this.items = items;
        }

        @JsonCreator
        public static Obj1854 fromJson(@JsonProperty("ID") int id,
                @JsonProperty("Items") List<ChildObj1854> items) {
            return new Obj1854(id, items);
        }

        public int getId() {
            return id;
        }

        public List<ChildObj1854> getItems() {
            return items;
        }

    }

    // [databind#1854]
    static class ChildObj1854 {
        private final String childId;

        private ChildObj1854(String id) {
            this.childId = id;
        }

        @JsonCreator
        public static ChildObj1854 fromJson(@JsonProperty("ChildID") String cid) {
            return new ChildObj1854(cid);
        }

        public String getId() {
            return childId;
        }
    }

    // [databind#1886]: allow case-insensitivity by default on a class
    @JsonFormat(with={ JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES })
    static class CaseInsensitiveRole {
        public String ID;
        public String Name;
    }

    // [databind#1886]: allow case-insensitivity by default on a class
    static class CaseInsensitiveRoleContainer {
        public CaseInsensitiveRole role;
    }

    // [databind#1886]: ... but also overrides
    static class CaseSensitiveRoleContainer {
        @JsonFormat(without={ JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES })
        public CaseInsensitiveRole role;
    }

    /*
    /********************************************************
    /* Test methods
    /********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();
    private final ObjectMapper INSENSITIVE_MAPPER = jsonMapperBuilder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
            .build();

    // [databind#566]
    public void testCaseInsensitiveDeserialization() throws Exception
    {
        final String JSON = "{\"Value1\" : {\"nAme\" : \"fruit\", \"vALUe\" : \"apple\"}, \"valUE2\" : {\"NAME\" : \"color\", \"value\" : \"red\"}}";

        // first, verify default settings which do not accept improper case
        ObjectMapper mapper = new ObjectMapper();
        assertFalse(mapper.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES));
        try {
            mapper.readValue(JSON, Issue476Bean.class);

            fail("Should not accept improper case properties by default");
        } catch (UnrecognizedPropertyException e) {
            verifyException(e, "Unrecognized field");
            assertValidLocation(e.getLocation());
        }

        // Definitely not OK to enable dynamically - the BeanPropertyMap (which is the consumer of this particular feature) gets cached.
        ObjectReader r = INSENSITIVE_MAPPER.readerFor(Issue476Bean.class);
        Issue476Bean result = r.readValue(JSON);
        assertEquals(result.value1.name, "fruit");
        assertEquals(result.value1.value, "apple");
    }

    // [databind#1036]
    public void testCaseInsensitive1036() throws Exception
    {
        final String json = "{\"ErrorCode\":2,\"DebugMessage\":\"Signature not valid!\"}";
//        final String json = "{\"errorCode\":2,\"debugMessage\":\"Signature not valid!\"}";

        BaseResponse response = INSENSITIVE_MAPPER.readValue(json, BaseResponse.class);
        assertEquals(2, response.errorCode);
        assertEquals("Signature not valid!", response.debugMessage);
    }

    // [databind#1232]: allow per-property case-insensitivity
    public void testCaseInsensitiveWithFormat() throws Exception {
        CaseInsensitiveRoleWrapper w = MAPPER.readValue
                (a2q("{'role':{'id':'12','name':'Foo'}}"),
                        CaseInsensitiveRoleWrapper.class);
        assertNotNull(w);
        assertEquals("12", w.role.ID);
        assertEquals("Foo", w.role.Name);
    }

    // [databind#1438]
    public void testCreatorWithInsensitive() throws Exception
    {
        final String json = a2q("{'VALUE':3}");
        InsensitiveCreator bean = INSENSITIVE_MAPPER.readValue(json, InsensitiveCreator.class);
        assertEquals(3, bean.v);
    }

    // And allow config overrides too
    public void testCaseInsensitiveViaConfigOverride() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configOverride(Role.class)
            .setFormat(JsonFormat.Value.empty()
                    .withFeature(JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES));
        Role role = mapper.readValue
                (a2q("{'id':'12','name':'Foo'}"),
                        Role.class);
        assertNotNull(role);
        assertEquals("12", role.ID);
        assertEquals("Foo", role.Name);
    }

    public void testIssue1854() throws Exception
    {
        final String DOC = a2q("{'ID': 1, 'Items': [ { 'ChildID': 10 } ]}");
        Obj1854 result = INSENSITIVE_MAPPER.readValue(DOC, Obj1854.class);
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertNotNull(result.getItems());
        assertEquals(1, result.getItems().size());
    }


    // [databind#1886]: allow case-insensitivity by default on a class
    public void testCaseInsensitiveViaClassAnnotation() throws Exception
    {
        final String CONTAINED = a2q("{'role': {'id':'3','name':'Bob'}}");

        // First: via wrapper/container:
        CaseInsensitiveRoleContainer cont = MAPPER.readValue(CONTAINED,
                        CaseInsensitiveRoleContainer.class);
        assertEquals("3", cont.role.ID);
        assertEquals("Bob", cont.role.Name);

        // second: directly as root value
        CaseInsensitiveRole role = MAPPER.readValue
                (a2q("{'id':'12','name':'Billy'}"),
                        CaseInsensitiveRole.class);
        assertEquals("12", role.ID);
        assertEquals("Billy", role.Name);

        // and finally, more complicated; should be possible to force sensitivity:
        try {
            /*CaseSensitiveRoleContainer r =*/ MAPPER.readValue(CONTAINED,
                    CaseSensitiveRoleContainer.class);
            fail("Should not pass");
        } catch (UnrecognizedPropertyException e) {
            verifyException(e, "Unrecognized ");
            verifyException(e, "\"id\"");
        }
    }
}
