package tools.jackson.databind.records;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class RecordUpdate3079Test extends DatabindTestUtil
{
    public record IdNameRecord(int id, String name) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IgnoreAllRecord(int id) { }

    // Record with @JsonAnySetter that captures UPPER-CASE property names
    public record AnySetterRecord(int id, String name,
            @JsonAnySetter Map<String, Object> extra) { }

    static class IdNameWrapper {
        public IdNameRecord value;

        protected IdNameWrapper() { }
        public IdNameWrapper(IdNameRecord v) { value = v; }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#3079]: Should be able to update Record value directly
    @Test
    public void testDirectRecordUpdate() throws Exception
    {
        IdNameRecord orig = new IdNameRecord(123, "Bob");
        IdNameRecord result = MAPPER.updateValue(orig,
                Collections.singletonMap("id", 137));
        assertNotNull(result);
        assertEquals(137, result.id());
        assertEquals("Bob", result.name());
        assertNotSame(orig, result);
    }

    // [databind#3079]: update with all properties overridden
    @Test
    public void testDirectRecordUpdateAllProperties() throws Exception
    {
        IdNameRecord orig = new IdNameRecord(123, "Bob");
        IdNameRecord result = MAPPER.updateValue(orig,
                Collections.singletonMap("name", "Gary"));
        assertNotNull(result);
        assertNotSame(orig, result);
        assertEquals(123, result.id());
        assertEquals("Gary", result.name());
        assertNotSame(orig, result);
    }

    // [databind#3079]: update with no properties should return equivalent (but not same) Record
    @Test
    public void testDirectRecordUpdateNoProperties() throws Exception
    {
        IdNameRecord orig = new IdNameRecord(123, "Bob");
        IdNameRecord result = MAPPER.updateValue(orig,
                Collections.emptyMap());
        assertNotNull(result);
        assertNotSame(orig, result);
        assertEquals(123, result.id());
        assertEquals("Bob", result.name());

        // Same with `null`:
        result = MAPPER.updateValue(orig, null);
        assertNotNull(result);
        // actually same instance, impl detail
        assertSame(orig, result);
    }

    // [databind#3079] also: should be able to update Record valued property
    @Test
    public void testRecordAsPropertyUpdate() throws Exception
    {
        IdNameRecord origRecord = new IdNameRecord(123, "Bob");
        IdNameWrapper orig = new IdNameWrapper(origRecord);

        IdNameWrapper delta = new IdNameWrapper(new IdNameRecord(200, "Gary"));
        IdNameWrapper result = MAPPER.updateValue(orig, delta);

        assertEquals(200, result.value.id());
        assertEquals("Gary", result.value.name());
        assertSame(orig, result);
        assertNotSame(origRecord, result.value);
    }

    // [databind#3079] exercise "ignore all" path
    @Test
    public void testIgnoreAllUnknown() throws Exception {
        IgnoreAllRecord orig = new IgnoreAllRecord(1);
        IgnoreAllRecord updated = MAPPER.updateValue(orig, Collections.singletonMap("value", 123));
        assertNotNull(updated);
        assertNotSame(orig, updated);
    }

    /*
    /**********************************************************************
    /* Tests via ObjectReader (readerForUpdating)
    /**********************************************************************
     */

    // [databind#3079]: update Record via ObjectReader, partial override
    @Test
    public void testReaderForUpdatingRecordPartial() throws Exception
    {
        IdNameRecord orig = new IdNameRecord(123, "Bob");
        IdNameRecord result = MAPPER.readerForUpdating(orig)
                .readValue(a2q("{'id':137}"));
        assertNotNull(result);
        assertEquals(137, result.id());
        assertEquals("Bob", result.name());
        assertNotSame(orig, result);
    }

    // [databind#3079]: update Record via ObjectReader, all properties
    @Test
    public void testReaderForUpdatingRecordAllProps() throws Exception
    {
        IdNameRecord orig = new IdNameRecord(123, "Bob");
        IdNameRecord result = MAPPER.readerForUpdating(orig)
                .readValue(a2q("{'id':456,'name':'Gary'}"));
        assertNotNull(result);
        assertEquals(456, result.id());
        assertEquals("Gary", result.name());
        assertNotSame(orig, result);
    }

    // [databind#3079]: update Record via ObjectReader, empty JSON object
    @Test
    public void testReaderForUpdatingRecordEmpty() throws Exception
    {
        IdNameRecord orig = new IdNameRecord(123, "Bob");
        IdNameRecord result = MAPPER.readerForUpdating(orig)
                .readValue("{}");
        assertNotNull(result);
        // NOTE: will not be same instance in this particular case, but more of an impl detail
        assertEquals(123, result.id());
        assertEquals("Bob", result.name());

        // Similarly with `null`:
        result = MAPPER.readerForUpdating(orig).readValue("null");
        // NOTE: will be same instance in this particular case
        assertNotNull(result);
        assertSame(orig, result);
    }

    // [databind#3079]: update Record via ObjectReader, original unchanged
    @Test
    public void testReaderForUpdatingRecordOrigUnchanged() throws Exception
    {
        IdNameRecord orig = new IdNameRecord(123, "Bob");
        MAPPER.readerForUpdating(orig)
                .readValue(a2q("{'id':999,'name':'Zed'}"));
        assertEquals(123, orig.id());
        assertEquals("Bob", orig.name());
    }

    // [databind#3079]: update Record-valued property via ObjectReader
    @Test
    public void testReaderForUpdatingRecordProperty() throws Exception
    {
        IdNameRecord origRecord = new IdNameRecord(123, "Bob");
        IdNameWrapper orig = new IdNameWrapper(origRecord);

        IdNameWrapper result = MAPPER.readerForUpdating(orig)
                .readValue(a2q("{'value':{'id':200,'name':'Gary'}}"));
        assertEquals(200, result.value.id());
        assertEquals("Gary", result.value.name());
        assertSame(orig, result);
        assertNotSame(origRecord, result.value);
    }

    // [databind#3079]: unknown properties should be ignored when configured
    @Test
    public void testReaderForUpdatingRecordUnknownIgnored() throws Exception
    {
        ObjectMapper lenientMapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        IdNameRecord orig = new IdNameRecord(123, "Bob");
        IdNameRecord result = lenientMapper.readerForUpdating(orig)
                .readValue(a2q("{'id':137,'unknown':'value'}"));
        assertNotNull(result);
        assertEquals(137, result.id());
        assertEquals("Bob", result.name());
    }

    // [databind#3079]: @JsonAnySetter captures UPPER-CASE property names
    //   that map to existing lower-case properties via any-setter Map
    @Test
    public void testReaderForUpdatingRecordWithAnySetter() throws Exception
    {
        AnySetterRecord orig = new AnySetterRecord(123, "Bob",
                Map.of("ID", 999, "NAME", "Old"));
        AnySetterRecord result = MAPPER.readerForUpdating(orig)
                .readValue(a2q("{'ID':456,'NAME':'Gary'}"));
        assertNotNull(result);
        // Regular properties should be pre-populated from original
        assertEquals(123, result.id());
        assertEquals("Bob", result.name());
        // UPPER-CASE properties should be captured by any-setter, overriding originals
        assertEquals(456, result.extra().get("ID"));
        assertEquals("Gary", result.extra().get("NAME"));
    }
}
