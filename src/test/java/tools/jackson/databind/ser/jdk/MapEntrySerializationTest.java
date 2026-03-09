package tools.jackson.databind.ser.jdk;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.NoCheckSubTypeValidator;

import static org.junit.jupiter.api.Assertions.*;

// [databind#565]: Support ser/deser of Map.Entry
public class MapEntrySerializationTest extends DatabindTestUtil
{
    static class StringIntMapEntry implements Map.Entry<String,Integer> {
        public final String k;
        public final Integer v;
        public StringIntMapEntry(String k, Integer v) {
            this.k = k;
            this.v = v;
        }

        @Override
        public String getKey() { return k; }

        @Override
        public Integer getValue() { return v; }

        @Override
        public Integer setValue(Integer value) {
            throw new UnsupportedOperationException();
        }
    }

    static class StringIntMapEntryWrapper {
        public StringIntMapEntry value;

        public StringIntMapEntryWrapper(String k, Integer v) {
            value = new StringIntMapEntry(k, v);
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // [Databind#565]
    @Test
    public void testMapEntry() throws IOException
    {
        StringIntMapEntry input = new StringIntMapEntry("answer", 42);
        String json = MAPPER.writeValueAsString(input);
        assertEquals(a2q("{'answer':42}"), json);

        StringIntMapEntry[] array = new StringIntMapEntry[] { input };
        json = MAPPER.writeValueAsString(array);
        assertEquals(a2q("[{'answer':42}]"), json);

        // and maybe with bit of extra typing?
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(NoCheckSubTypeValidator.instance,
                        DefaultTyping.NON_FINAL)
                .build();
        json = mapper.writeValueAsString(input);
        assertEquals(a2q("['"+StringIntMapEntry.class.getName()+"',{'answer':42}]"),
                json);
    }

    @Test
    public void testMapEntryWrapper() throws IOException
    {
        StringIntMapEntryWrapper input = new StringIntMapEntryWrapper("answer", 42);
        String json = MAPPER.writeValueAsString(input);
        assertEquals(a2q("{'value':{'answer':42}}"), json);
    }
}
