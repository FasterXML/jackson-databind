package tools.jackson.databind.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.*;

// https://github.com/FasterXML/jackson-databind/issues/1497
public class UnwrappedWithCreator1497Test extends DatabindTestUtil
{
    static class Location {
        public final int latitude;
        public final int longitude;

        public Location(int latitude, int longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    static class Place {
        public final String name;

        @JsonUnwrapped
        public final Location location;

        @JsonCreator
        public Place(@JsonProperty("name") String name,
                     @JsonProperty("latitude") int latitude,
                     @JsonProperty("longitude") int longitude) {
            this.name = name;
            this.location = new Location(latitude, longitude);
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @JacksonTestFailureExpected
    @Test
    public void testJsonCreatorWithUnwrappedField() throws Exception
    {
        String json = a2q("{'name':'Home','latitude':37,'longitude':127}");
        Place place = MAPPER.readValue(json, Place.class);

        assertEquals("Home", place.name);
        assertNotNull(place.location);
        assertEquals(37, place.location.latitude);
        assertEquals(127, place.location.longitude);
    }

    @Test
    public void testJsonCreatorWithUnwrappedFieldSerialization() throws Exception
    {
        String expected = a2q("{'name':'Home','latitude':37,'longitude':127}");
        Place place = new Place("Home", 37, 127);
        String json = MAPPER.writeValueAsString(place);

        // The JSON should have unwrapped properties (not nested)
        assertEquals(expected, json);
    }
}

