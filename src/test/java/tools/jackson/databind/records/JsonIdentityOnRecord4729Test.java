package tools.jackson.databind.records;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#4729] Object ID handling tries (unnecessarily) to set id value on a record
public class JsonIdentityOnRecord4729Test
        extends DatabindTestUtil
{
    @JsonIdentityInfo(property = "id", generator = ObjectIdGenerators.PropertyGenerator.class)
    record Device(String id) { }

    record Activity(String id,
            @JsonIdentityReference(alwaysAsId = true) List<Device> participants) { }

    record Configuration(List<Device> devices, List<Activity> activities) { }

    private final ObjectMapper MAPPER = newJsonMapper();

    // Reproduces the exact scenario from the issue report: records with
    // @JsonIdentityInfo(PropertyGenerator) and @JsonIdentityReference(alwaysAsId=true)
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

    // Also verify round-trip works
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
}
