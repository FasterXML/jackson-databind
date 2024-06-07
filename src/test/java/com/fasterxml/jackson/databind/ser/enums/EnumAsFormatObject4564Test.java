package com.fasterxml.jackson.databind.ser.enums;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#4564] Fix Enum-asJSON-Object serialization with self as field.
public class EnumAsFormatObject4564Test
{

    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public enum Level {
        LEVEL1("level1"),
        LEVEL2("level2"),
        LEVEL3("level3", Level.LEVEL1);

        public String label;
        public Level sublevel;

        Level(String level2) {
            this.label = level2;
        }

        Level(String level3, Level level) {
            this.label = level3;
            this.sublevel = level;
        }
    }

    private final ObjectMapper MAPPER = new JsonMapper();

    @Test
    public void testEnumAsFormatObject() throws JsonProcessingException {
        List<Level> levels = new ArrayList<>();
        levels.add(Level.LEVEL1);
        levels.add(Level.LEVEL2);
        levels.add(Level.LEVEL3);

        String JSON = MAPPER.writerFor(new TypeReference<List<Level>>() {
        }).writeValueAsString(levels);

        // Fails, because we get [{"label":"level1"},{"label":"level2"},{"label":"level3"}]
        assertEquals(
                "["
                        + "{\"label\":\"level1\"},"
                        + "{\"label\":\"level2\"},"
                        + "{\"label\":\"level3\",\"sublevel\":{\"label\":\"level1\"}}"
                        + "]",
                JSON);
    }
}
