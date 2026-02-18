package tools.jackson.databind.tofix;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.Nulls;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.*;

// [databind#3084] Deserializing empty JSON object into Record with List<OtherRecord>
// throws InvalidDefinitionException when Nulls.AS_EMPTY is configured for content nulls
class RecordWithListOfRecordAsEmpty3084Test extends DatabindTestUtil {

    record Bar(String name, String value) {}

    record Foo(List<Bar> list) {}

    // [databind#3084]
    @JacksonTestFailureExpected
    @Test
    void emptyObjectIntoRecordWithListOfRecord() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultNullHandling(h -> h.withContentNulls(Nulls.AS_EMPTY))
                .build();
        Foo foo = mapper.readValue("{}", Foo.class);
        assertNull(foo.list());
    }
}
