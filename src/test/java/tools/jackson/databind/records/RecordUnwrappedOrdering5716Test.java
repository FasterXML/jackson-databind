package tools.jackson.databind.records;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#5716] @JsonUnwrapped record properties should stay at declaration position
public class RecordUnwrappedOrdering5716Test extends DatabindTestUtil
{
    record Name(String first, String last) { }
    record Row(long time, @JsonUnwrapped Name name, double score) { }

    private final ObjectMapper MAPPER = JsonMapper.builder()
            .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(MapperFeature.SORT_CREATOR_PROPERTIES_FIRST)
            .build();

    @Test
    void unwrappedRecordShouldKeepDeclarationOrder() throws Exception
    {
        Row input = new Row(1L, new Name("a", "b"), 2.5d);

        String json = MAPPER.writeValueAsString(input);

        assertEquals(a2q("{'time':1,'first':'a','last':'b','score':2.5}"), json);
    }
}
