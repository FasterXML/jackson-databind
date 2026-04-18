package tools.jackson.databind.records;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// [databind#3178]: records variant — exercises PropertyBasedCreator rename path
public class RecordUnwrappedWithPrefix3178Test extends DatabindTestUtil
{
    record Location(int x, int y) { }

    record Inner(String name, Location location) { }

    record WithPrefix(@JsonUnwrapped(prefix = "_") Inner unwrapped) { }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testPrefixedUnwrappingWithRecord() throws Exception {
        WithPrefix source = new WithPrefix(new Inner("Bubba", new Location(2, 3)));
        String json = MAPPER.writeValueAsString(source);
        assertEquals("{\"_name\":\"Bubba\",\"_location\":{\"x\":2,\"y\":3}}", json);
        WithPrefix bean = MAPPER.readValue(json, WithPrefix.class);
        assertNotNull(bean.unwrapped());
        assertNotNull(bean.unwrapped().location());
        assertEquals(source.unwrapped().name(), bean.unwrapped().name());
        assertEquals(source.unwrapped().location().x(), bean.unwrapped().location().x());
        assertEquals(source.unwrapped().location().y(), bean.unwrapped().location().y());
    }
}
