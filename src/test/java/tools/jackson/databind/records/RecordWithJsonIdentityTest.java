package tools.jackson.databind.records;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#4729] Object ID handling tries (unnecessarily) to set id value on a record
// [databind#5238] immutable classes with @JsonIdentityInfo can be deserialized; records cannot
public class RecordWithJsonIdentityTest extends DatabindTestUtil
{
    // [databind#4729]
    @JsonIdentityInfo(property = "id", generator = ObjectIdGenerators.PropertyGenerator.class)
    record Device(String id) { }

    record Activity(String id,
            @JsonIdentityReference(alwaysAsId = true) List<Device> participants) { }

    record Configuration(List<Device> devices, List<Activity> activities) { }

    // [databind#5238]
    record ExampleRecord(List<ThingRecord> allThings, ThingRecord selected) { }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    record ThingRecord(int id, String name) { }

    static class ExamplePojo {
        public List<ThingPojo> allThings;
        public ThingPojo selected;

        @JsonCreator
        public ExamplePojo(
                @JsonProperty("allThings") List<ThingPojo> allThings,
                @JsonProperty("selected") ThingPojo selected) {
            this.allThings = allThings;
            this.selected = selected;
        }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    static class ThingPojo {
        public final int id;
        public final String name;

        @JsonCreator
        public ThingPojo(@JsonProperty("id") int id, @JsonProperty("name") String name) {
            this.id = id;
            this.name = name;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************************
    /* Test methods, @JsonIdentityReference [databind#4729]
    /**********************************************************************
     */

    // [databind#4729]
    @Test
    void testRecordWithPropertyGeneratorAndIdentityReference() throws Exception
    {
        String input = a2q("{"
                + "'devices': [{'id': 'Arris'}],"
                + "'activities': [{'id': 'TV', 'participants': ['Arris']}]"
                + "}");

        Configuration result = MAPPER.readValue(input, Configuration.class);

        assertEquals(1, result.devices().size());
        assertEquals("Arris", result.devices().get(0).id());
        assertEquals(1, result.activities().size());
        assertEquals("TV", result.activities().get(0).id());
        assertEquals(1, result.activities().get(0).participants().size());
        assertSame(result.devices().get(0), result.activities().get(0).participants().get(0));
    }

    @Test
    void testRecordWithPropertyGeneratorRoundTrip() throws Exception
    {
        Device arris = new Device("Arris");
        Device roku = new Device("Roku");
        Activity tv = new Activity("TV", List.of(arris, roku));
        Configuration input = new Configuration(List.of(arris, roku), List.of(tv));

        String json = MAPPER.writeValueAsString(input);
        Configuration result = MAPPER.readValue(json, Configuration.class);

        assertEquals(2, result.devices().size());
        assertEquals("Arris", result.devices().get(0).id());
        assertEquals("Roku", result.devices().get(1).id());
        assertEquals(1, result.activities().size());
        assertEquals(2, result.activities().get(0).participants().size());
        // Verify identity: participants should be same instances as devices
        assertSame(result.devices().get(0), result.activities().get(0).participants().get(0));
        assertSame(result.devices().get(1), result.activities().get(0).participants().get(1));
    }

    /*
    /**********************************************************************
    /* Test methods, records vs POJOs [databind#5238]
    /**********************************************************************
     */

    // [databind#5238]
    @Test
    void testIdentityWithPojo() throws Exception {
        ThingPojo t1 = new ThingPojo(1, "a");
        ThingPojo t2 = new ThingPojo(2, "b");
        ExamplePojo input = new ExamplePojo(List.of(t1, t2), t2);

        String json = MAPPER.writeValueAsString(input);

        ExamplePojo result = MAPPER.readValue(json, ExamplePojo.class);
        assertEquals(input.allThings.size(), result.allThings.size());
        assertEquals(input.selected.id, result.selected.id);
        assertEquals(input.selected.name, result.selected.name);
    }

    @Test
    void testIdentityWithRecord() throws Exception {
        ThingRecord t1 = new ThingRecord(1, "a");
        ThingRecord t2 = new ThingRecord(2, "b");
        ExampleRecord input = new ExampleRecord(List.of(t1, t2), t2);

        String json = MAPPER.writeValueAsString(input);
        ExampleRecord result = MAPPER.readValue(json, ExampleRecord.class);

        assertEquals(input.allThings.size(), result.allThings.size());
        assertEquals(input.selected.id, result.selected.id);
        assertEquals(input.selected.name, result.selected.name);
    }
}
