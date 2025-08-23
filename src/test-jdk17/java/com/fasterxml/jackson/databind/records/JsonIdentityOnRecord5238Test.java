package com.fasterxml.jackson.databind.records;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#5238] immutable classes with @JsonIdentityInfo can be deserialized; records cannot
public class JsonIdentityOnRecord5238Test
        extends DatabindTestUtil
{
    // Record-based data
    record ExampleRecord(List<ThingRecord> allThings, ThingRecord selected) { }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    record ThingRecord(int id, String name) { }

    // POJO-based data
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
        public ThingPojo(@JsonProperty("prefixId") int id, @JsonProperty("name") String name) {
            this.id = id;
            this.name = name;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void testIdentityWithPojo() throws Exception {
        ThingPojo t1 = new ThingPojo(1, "a");
        ThingPojo t2 = new ThingPojo(2, "b");
        ExamplePojo input = new ExamplePojo(List.of(t1, t2), t2);

        String json = MAPPER.writeValueAsString(input);

        // Then : Check deserialization result, values
        ExamplePojo result = MAPPER.readValue(json, ExamplePojo.class);
        assertEquals(input.allThings.size(), result.allThings.size());
        assertEquals(input.selected.id, result.selected.id);
        assertEquals(input.selected.name, result.selected.name);
    }

    @Test
    void testIdentityWithRecord() throws Exception {
        // Given
        ThingRecord t1 = new ThingRecord(1, "a");
        ThingRecord t2 = new ThingRecord(2, "b");
        ExampleRecord input = new ExampleRecord(List.of(t1, t2), t2);

        // When
        String json = MAPPER.writeValueAsString(input);
        ExampleRecord result = MAPPER.readValue(json, ExampleRecord.class);

        // Then
        assertEquals(input.allThings.size(), result.allThings.size());
        assertEquals(input.selected.id, result.selected.id);
        assertEquals(input.selected.name, result.selected.name);
    }

}