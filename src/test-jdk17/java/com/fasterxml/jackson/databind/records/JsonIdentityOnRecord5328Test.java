package com.fasterxml.jackson.databind.records;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JsonIdentityOnRecord5328Test {

    private final ObjectMapper MAPPER = new ObjectMapper();

    // ✅ Working test with POJO
    @Test
    void testIdentityWithPojo() throws Exception {
        ThingPojo t1 = new ThingPojo(1, "a");
        ThingPojo t2 = new ThingPojo(2, "b");

        ExamplePojo input = new ExamplePojo(List.of(t1, t2), t2);

        String json = MAPPER.writeValueAsString(input);
        ExamplePojo result = MAPPER.readValue(json, ExamplePojo.class);

        assertEquals(input.selected.id, result.selected.id);
        assertEquals(input.selected.name, result.selected.name);
    }

    // ❌ Failing test with record
    @Test
    void testIdentityWithRecord() throws Exception {
        ThingRecord t1 = new ThingRecord(1, "a");
        ThingRecord t2 = new ThingRecord(2, "b");

        ExampleRecord input = new ExampleRecord(List.of(t1, t2), t2);

        String json = MAPPER.writeValueAsString(input);

        Exception ex = assertThrows(Exception.class, () ->
                MAPPER.readValue(json, ExampleRecord.class));

        System.out.println("Expected failure:");
        ex.printStackTrace();
    }

    // Record-based data
    record ExampleRecord(List<ThingRecord> allThings, ThingRecord selected) {}

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    record ThingRecord(int id, String name) {}

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
        public ThingPojo(@JsonProperty("id") int id, @JsonProperty("name") String name) {
            this.id = id;
            this.name = name;
        }
    }
}