package tools.jackson.databind.records;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class RecordWithReadOnlyTest extends DatabindTestUtil
{
    record RecordWithReadOnly(int id, @JsonProperty(access = Access.READ_ONLY) String name) {
    }

    record RecordWithReadOnlyNamedProperty(int id,
            @JsonProperty(value = "name", access = Access.READ_ONLY) String name) {
    }

    record RecordWithReadOnlyAccessor(int id, String name) {

        @JsonProperty(access = Access.READ_ONLY)
        @Override
        public String name() {
            return name;
        }
    }

    record RecordWithReadOnlyComponentOverriddenAccessor(int id,
            @JsonProperty(access = Access.READ_ONLY) String name)
    {
        // @JsonProperty on overridden method is not automatically inherited by overriding method
        @Override
        public String name() {
            return name;
        }
    }

    record RecordWithReadOnlyPrimitiveType(@JsonProperty(access = Access.READ_ONLY) int id, String name) {
    }

    record RecordWithReadOnlyAll(@JsonProperty(access = Access.READ_ONLY) int id,
                                 @JsonProperty(access = Access.READ_ONLY) String name) {
    }

    record RecordWithReadOnlyAllAndNoArgConstructor(@JsonProperty(access = Access.READ_ONLY) int id,
                                                    @JsonProperty(access = Access.READ_ONLY) String name) {

        public RecordWithReadOnlyAllAndNoArgConstructor() {
            this(-1, "no-arg");
        }
    }

    static class ReadOnly5049Pojo
    {
        protected String a, b;

        ReadOnly5049Pojo(
                @JsonProperty(value = "a", access = JsonProperty.Access.READ_ONLY) String a,
                @JsonProperty(value = "b", access = JsonProperty.Access.READ_ONLY) String b) {
            this.a = a;
            this.b = b;
        }

        public String getA() { return a; }
        public String getB() { return b; }
    }

    record ReadOnly5049Record(
            @JsonProperty(value = "a", access = JsonProperty.Access.READ_ONLY) String a,
            @JsonProperty(value = "b", access = JsonProperty.Access.READ_ONLY) String b) {
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .build();

    /*
    /**********************************************************************
    /* Test methods, JsonProperty.access=READ_ONLY
    /**********************************************************************
     */

    @Test
    public void testSerializeReadOnlyProperty() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordWithReadOnly(123, "Bob"));
        assertEquals(a2q("{'id':123,'name':'Bob'}"), json);
    }

    @Test
    public void testDeserializeReadOnlyProperty() throws Exception {
        RecordWithReadOnly value = MAPPER.readValue(a2q("{'id':123,'name':'Bob'}"), RecordWithReadOnly.class);
        assertEquals(new RecordWithReadOnly(123, null), value);
    }

    /*
    /**********************************************************************
    /* Test methods, JsonProperty.access=READ_ONLY + JsonProperty.value=...
    /**********************************************************************
     */

    // [databind#4826]
    @Test
    public void testSerializeReadOnlyNamedProperty() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordWithReadOnlyNamedProperty(123, "Bob"));
        assertEquals(a2q("{'id':123,'name':'Bob'}"), json);
    }

    // [databind#4826]
    @Test
    public void testDeserializeReadOnlyNamedProperty() throws Exception {
        RecordWithReadOnlyNamedProperty value = MAPPER.readValue(a2q("{'id':123,'name':'Bob'}"),
                RecordWithReadOnlyNamedProperty.class);
        assertEquals(new RecordWithReadOnlyNamedProperty(123, null), value);
    }

    // [databind#5049]
    @Test
    void testRoundtripPOJO5049() throws Exception
    {
        String json = MAPPER.writeValueAsString(new ReadOnly5049Pojo("hello", "world"));
        ReadOnly5049Pojo pojo = MAPPER.readerFor(ReadOnly5049Pojo.class).readValue(json);
        assertNotNull(pojo);
        assertNull(pojo.a);
        assertNull(pojo.b);
    }

    // [databind#5049]
    @Test
    void testRoundtripRecord5049() throws Exception
    {
        String json = MAPPER.writeValueAsString(new ReadOnly5049Record("hello", "world"));
        ReadOnly5049Record record = MAPPER.readValue(json, ReadOnly5049Record.class);
        assertNotNull(record);
        assertNull(record.a());
        assertNull(record.b());
    }

    /*
    /**********************************************************************
    /* Test methods, JsonProperty.access=READ_ONLY accessor
    /**********************************************************************
     */

    @Test
    public void testSerializeReadOnlyAccessor() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordWithReadOnlyAccessor(123, "Bob"));
        assertEquals(a2q("{'id':123,'name':'Bob'}"), json);
    }

    @Test
    public void testDeserializeReadOnlyAccessor() throws Exception {
        RecordWithReadOnlyAccessor expected = new RecordWithReadOnlyAccessor(123, null);

        assertEquals(expected, MAPPER.readValue(a2q("{'id':123}"), RecordWithReadOnlyAccessor.class));
        assertEquals(expected, MAPPER.readValue(a2q("{'id':123,'name':null}"), RecordWithReadOnlyAccessor.class));
        assertEquals(expected, MAPPER.readValue(a2q("{'id':123,'name':'Bob'}"), RecordWithReadOnlyAccessor.class));
    }

    /*
    /**********************************************************************
    /* Test methods, JsonProperty.access=READ_ONLY component, but accessor
     * method was overridden without re-annotating with JsonProperty.access=READ_ONLY
    /**********************************************************************
     */

    @Test
    public void testSerializeReadOnlyComponentOverrideAccessor() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordWithReadOnlyComponentOverriddenAccessor(123, "Bob"));
        assertEquals(a2q("{'id':123,'name':'Bob'}"), json);
    }

    @Test
    public void testDeserializeReadOnlyComponentOverrideAccessor() throws Exception {
        RecordWithReadOnlyComponentOverriddenAccessor expected = new RecordWithReadOnlyComponentOverriddenAccessor(123, null);

        assertEquals(expected, MAPPER.readValue(a2q("{'id':123}"), RecordWithReadOnlyComponentOverriddenAccessor.class));
        assertEquals(expected, MAPPER.readValue(a2q("{'id':123,'name':null}"), RecordWithReadOnlyComponentOverriddenAccessor.class));
        assertEquals(expected, MAPPER.readValue(a2q("{'id':123,'name':'Bob'}"), RecordWithReadOnlyComponentOverriddenAccessor.class));
    }

    /*
    /**********************************************************************
    /* Test methods, JsonProperty.access=READ_ONLY parameter of primitive type
    /**********************************************************************
     */

    @Test
    public void testSerializeReadOnlyPrimitiveTypeProperty() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordWithReadOnlyPrimitiveType(123, "Bob"));
        assertEquals(a2q("{'id':123,'name':'Bob'}"), json);
    }

    @Test
    public void testDeserializeReadOnlyPrimitiveTypeProperty() throws Exception {
        RecordWithReadOnlyPrimitiveType value = MAPPER.readValue(a2q("{'id':123,'name':'Bob'}"),
                RecordWithReadOnlyPrimitiveType.class);
        assertEquals(new RecordWithReadOnlyPrimitiveType(0, "Bob"), value);
    }

    /*
    /**********************************************************************
    /* Test methods, JsonProperty.access=READ_ONLY all parameters
    /**********************************************************************
     */

    @Test
    public void testSerializeReadOnlyAllProperties() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordWithReadOnlyAll(123, "Bob"));
        assertEquals(a2q("{'id':123,'name':'Bob'}"), json);
    }

    @Test
    public void testDeserializeReadOnlyAllProperties() throws Exception {
        RecordWithReadOnlyAll value = MAPPER.readValue(a2q("{'id':123,'name':'Bob'}"),
                RecordWithReadOnlyAll.class);
        assertEquals(new RecordWithReadOnlyAll(0, null), value);
    }

    @Test
    public void testSerializeReadOnlyAllProperties_WithNoArgConstructor() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordWithReadOnlyAllAndNoArgConstructor(123, "Bob"));
        assertEquals(a2q("{'id':123,'name':'Bob'}"), json);
    }

    @Test
    public void testDeserializeReadOnlyAllProperties_WithNoArgConstructor() throws Exception {
        RecordWithReadOnlyAllAndNoArgConstructor value = MAPPER.readValue(a2q("{'id':123,'name':'Bob'}"),
                RecordWithReadOnlyAllAndNoArgConstructor.class);
        assertEquals(new RecordWithReadOnlyAllAndNoArgConstructor(0, null), value);
    }
}
