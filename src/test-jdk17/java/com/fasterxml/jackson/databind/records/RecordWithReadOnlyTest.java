package com.fasterxml.jackson.databind.records;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RecordWithReadOnlyTest extends DatabindTestUtil {

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

    record RecordWithReadOnlyComponentOverriddenAccessor(int id, @JsonProperty(access = Access.READ_ONLY) String name) {

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

    private final ObjectMapper MAPPER = newJsonMapper();

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

    @Test
    public void testSerializeReadOnlyNamedProperty() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordWithReadOnlyNamedProperty(123, "Bob"));
        assertEquals(a2q("{'id':123,'name':'Bob'}"), json);
    }

    /**
     * Currently documents a bug where a property was NOT ignored during deserialization if given an explicit name.
     * Also reproducible in 2.14.x.
     */
    @Test
    public void testDeserializeReadOnlyNamedProperty() throws Exception {
        RecordWithReadOnlyNamedProperty value = MAPPER.readValue(a2q("{'id':123,'name':'Bob'}"), RecordWithReadOnlyNamedProperty.class);
        assertEquals(new RecordWithReadOnlyNamedProperty(123, "Bob"), value); // BUG: should be `null` instead of "Bob"
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
    /* Test methods, JsonProperty.access=READ_ONLY component, but accessor method was overridden without re-annotating with JsonProperty.access=READ_ONLY
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
        RecordWithReadOnlyPrimitiveType value = MAPPER.readValue(a2q("{'id':123,'name':'Bob'}"), RecordWithReadOnlyPrimitiveType.class);
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
        RecordWithReadOnlyAll value = MAPPER.readValue(a2q("{'id':123,'name':'Bob'}"), RecordWithReadOnlyAll.class);
        assertEquals(new RecordWithReadOnlyAll(0, null), value);
    }

    @Test
    public void testSerializeReadOnlyAllProperties_WithNoArgConstructor() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordWithReadOnlyAllAndNoArgConstructor(123, "Bob"));
        assertEquals(a2q("{'id':123,'name':'Bob'}"), json);
    }

    @Test
    public void testDeserializeReadOnlyAllProperties_WithNoArgConstructor() throws Exception {
        RecordWithReadOnlyAllAndNoArgConstructor value = MAPPER.readValue(a2q("{'id':123,'name':'Bob'}"), RecordWithReadOnlyAllAndNoArgConstructor.class);
        assertEquals(new RecordWithReadOnlyAllAndNoArgConstructor(0, null), value);
    }
}
