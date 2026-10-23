package tools.jackson.databind.records;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonKey;
import com.fasterxml.jackson.annotation.JsonValue;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for {@link JsonKey} annotation with Java Records.
 * Verifies that @JsonKey works correctly when used with Record types,
 * ensuring feature parity with POJO implementation.
 *
 * @see <a href="https://github.com/FasterXML/jackson-databind/issues/5559">Issue #5559</a>
 */
public class RecordMapKeyAnnotationsTest extends DatabindTestUtil
{
    // [databind#5559]
    record InnerRecord(@JsonKey String key, @JsonValue String value) { }

    // [databind#5559]
    record OuterRecord(@JsonKey @JsonValue InnerRecord inner) { }

    // [databind#5559]
    record NoKeyOuterRecord(@JsonValue InnerRecord inner) { }

    // [databind#5559]
    record SimpleKeyRecord(@JsonKey String id) { }

    // [databind#3063]
    record GetLocations3063(@JsonValue Map<String, String> nameToLocation)
    {
        @JsonCreator
        public GetLocations3063(Map<String, String> nameToLocation)
        {
            this.nameToLocation = nameToLocation;
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#5559]: Test basic @JsonKey with nested @JsonValue in records
    @Test
    public void testRecordAsKey() throws Exception {
        OuterRecord outer = new OuterRecord(new InnerRecord("innerKey", "innerValue"));
        Map<OuterRecord, String> map = Collections.singletonMap(outer, "value");
        String actual = MAPPER.writeValueAsString(map);
        assertEquals("{\"innerKey\":\"value\"}", actual);
    }

    // [databind#5559]: Test @JsonValue without @JsonKey in records
    @Test
    public void testRecordAsValue() throws Exception {
        Map<String, OuterRecord> map = Collections.singletonMap("key",
                new OuterRecord(new InnerRecord("innerKey", "innerValue")));
        String actual = MAPPER.writeValueAsString(map);
        assertEquals("{\"key\":\"innerValue\"}", actual);
    }

    // [databind#5559]: Test record with @JsonValue but no @JsonKey
    @Test
    public void testNoKeyOuterRecord() throws Exception {
        Map<String, NoKeyOuterRecord> map = Collections.singletonMap("key",
                new NoKeyOuterRecord(new InnerRecord("innerKey", "innerValue")));
        String actual = MAPPER.writeValueAsString(map);
        assertEquals("{\"key\":\"innerValue\"}", actual);
    }

    // [databind#5559]: Test simple record with just @JsonKey
    @Test
    public void testSimpleRecordAsKey() throws Exception {
        SimpleKeyRecord keyRecord = new SimpleKeyRecord("myKey");
        Map<SimpleKeyRecord, String> map = Collections.singletonMap(keyRecord, "myValue");
        String actual = MAPPER.writeValueAsString(map);
        assertEquals("{\"myKey\":\"myValue\"}", actual);
    }

    // [databind#3063]: @JsonValue Map serialization on record
    @Test
    public void testRecordWithJsonValue3063() throws Exception
    {
        Map<String, String> locations = Collections.singletonMap("a", "locationA");
        String json = MAPPER.writeValueAsString(new GetLocations3063(locations));

        assertNotNull(json);
    }
}
