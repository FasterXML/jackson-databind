package tools.jackson.databind.failing;

import java.util.Collections;

import tools.jackson.databind.*;

// 01-Dec-2022, tatu: Alas, fails on JDK 17
// see related passing test in RecordUpdate3079Test
public class RecordUpdate3079FailingTest extends BaseMapTest
{
    public record IdNameRecord(int id, String name) { }

    static class IdNameWrapper {
        public IdNameRecord value;

        protected IdNameWrapper() { }
        public IdNameWrapper(IdNameRecord v) { value = v; }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#3079]: Should be able to Record value directly
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
}
