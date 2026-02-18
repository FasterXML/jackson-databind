package tools.jackson.databind.records;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.Nulls;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#3084] Deserializing empty JSON object into Record with List<OtherRecord>
// throws InvalidDefinitionException when Nulls.AS_EMPTY is configured for content nulls
class RecordWithListOfRecordNullHandling3084Test extends DatabindTestUtil {

    record Bar(String name, String value) {}

    record Foo(List<Bar> list) {}

    // [databind#3084]: deserializing {} into record with List<OtherRecord> should not
    // throw eagerly when content nulls are configured AS_EMPTY
    @Test
    void emptyObjectIntoRecordWithListOfRecord() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultNullHandling(h -> h.withContentNulls(Nulls.AS_EMPTY))
                .build();
        Foo foo = mapper.readValue("{}", Foo.class);
        assertNull(foo.list());
    }

    // [databind#3084]: non-empty list should still deserialize normally
    @Test
    void nonEmptyListIntoRecordWithListOfRecord() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultNullHandling(h -> h.withContentNulls(Nulls.AS_EMPTY))
                .build();
        Foo foo = mapper.readValue("{\"list\":[{\"name\":\"a\",\"value\":\"b\"}]}", Foo.class);
        assertNotNull(foo.list());
        assertEquals(1, foo.list().size());
        assertEquals("a", foo.list().get(0).name());
        assertEquals("b", foo.list().get(0).value());
    }
}
