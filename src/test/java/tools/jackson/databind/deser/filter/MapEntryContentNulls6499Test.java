package tools.jackson.databind.deser.filter;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidNullException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#6499]: "content nulls" for `Map.Entry` apply to entry value,
// but not to content of a nested container value
public class MapEntryContentNulls6499Test extends DatabindTestUtil
{
    static class EntryFail {
        @JsonSetter(contentNulls = Nulls.FAIL)
        public Map.Entry<String, List<String>> v;
    }

    static class EntrySkip {
        @JsonSetter(contentNulls = Nulls.SKIP)
        public Map.Entry<String, List<String>> v;
    }

    static class EntryAsEmpty {
        @JsonSetter(contentNulls = Nulls.AS_EMPTY)
        public Map.Entry<String, List<String>> v;
    }

    static class PojoEntryFail {
        @JsonSetter(contentNulls = Nulls.FAIL)
        @JsonFormat(shape = JsonFormat.Shape.POJO)
        public Map.Entry<String, List<String>> v;
    }

    static class PojoEntryListKeyFail {
        @JsonSetter(contentNulls = Nulls.FAIL)
        @JsonFormat(shape = JsonFormat.Shape.POJO)
        public Map.Entry<List<String>, String> v;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void failForNullEntryValue() throws Exception
    {
        InvalidNullException e = assertThrows(InvalidNullException.class,
                () -> MAPPER.readValue("""
                        {"v":{"a":null}}
                        """, EntryFail.class));
        verifyException(e, "Invalid `null` value");
        assertEquals(List.class, e.getTargetType());
    }

    @Test
    public void failNotPropagatedToNestedContainer() throws Exception
    {
        EntryFail result = MAPPER.readValue("""
                {"v":{"a":[null]}}
                """, EntryFail.class);
        assertEquals("a", result.v.getKey());
        assertEquals(Collections.singletonList(null), result.v.getValue());
    }

    // Entry cannot be skipped; value is left as `null`
    @Test
    public void skipForNullEntryValue() throws Exception
    {
        EntrySkip result = MAPPER.readValue("""
                {"v":{"a":null}}
                """, EntrySkip.class);
        assertEquals("a", result.v.getKey());
        assertNull(result.v.getValue());

        // and nested container not affected either
        result = MAPPER.readValue("""
                {"v":{"a":[null]}}
                """, EntrySkip.class);
        assertEquals(Collections.singletonList(null), result.v.getValue());
    }

    @Test
    public void asEmptyForNullEntryValue() throws Exception
    {
        EntryAsEmpty result = MAPPER.readValue("""
                {"v":{"a":null}}
                """, EntryAsEmpty.class);
        assertEquals("a", result.v.getKey());
        assertEquals(Collections.emptyList(), result.v.getValue());
    }

    @Test
    public void pojoShapeFailForNullEntryValue() throws Exception
    {
        InvalidNullException e = assertThrows(InvalidNullException.class,
                () -> MAPPER.readValue("""
                        {"v":{"key":"a","value":null}}
                        """, PojoEntryFail.class));
        verifyException(e, "Invalid `null` value");
        assertEquals(List.class, e.getTargetType());

        PojoEntryFail result = MAPPER.readValue("""
                {"v":{"key":"a","value":[null]}}
                """, PojoEntryFail.class);
        assertEquals("a", result.v.getKey());
        assertEquals(Collections.singletonList(null), result.v.getValue());
    }

    // Content nulls do not apply to content of a container-valued key either
    @Test
    public void pojoShapeFailNotPropagatedToKey() throws Exception
    {
        PojoEntryListKeyFail result = MAPPER.readValue("""
                {"v":{"key":[null],"value":"x"}}
                """, PojoEntryListKeyFail.class);
        assertEquals(Collections.singletonList(null), result.v.getKey());
        assertEquals("x", result.v.getValue());
    }
}
