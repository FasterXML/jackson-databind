package tools.jackson.databind.ser.enums;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

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
    public void testEnumAsFormatObject() throws Exception {
        List<Level> levels = new ArrayList<>();
        levels.add(Level.LEVEL1);
        levels.add(Level.LEVEL2);
        levels.add(Level.LEVEL3);

        String JSON = MAPPER.writeValueAsString(levels);

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

