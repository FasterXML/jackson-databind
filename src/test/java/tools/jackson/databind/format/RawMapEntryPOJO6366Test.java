package tools.jackson.databind.format;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

class RawMapEntryPOJO6366Test extends DatabindTestUtil
{
    @JsonFormat(shape = JsonFormat.Shape.POJO)
    interface AsPOJO { }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .withConfigOverride(Map.Entry.class,
                    o -> o.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.POJO)))
            .build();

    @Test
    void rawEntryUsesUntypedKeyAndValue() throws Exception {
        Map.Entry<?, ?> result = MAPPER.readValue(
                a2q("{'key':'answer','value':42}"), Map.Entry.class);

        assertEquals("answer", result.getKey());
        assertEquals(42, result.getValue());
    }

    @Test
    void rawEntryRetainsStructuredValues() throws Exception {
        Map.Entry<?, ?> result = MAPPER.readValue(
                a2q("{'key':[1,2],'value':{'nested':true}}"), Map.Entry.class);

        assertEquals(List.of(1, 2), result.getKey());
        assertEquals(Map.of("nested", true), result.getValue());
    }

    @Test
    void rawEntryCanUsePOJOMixIn() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder().addMixIn(Map.Entry.class, AsPOJO.class).build();
        Map.Entry<?, ?> result = mapper.readValue(
                a2q("{'key':'answer','value':42}"), Map.Entry.class);

        assertEquals("answer", result.getKey());
        assertEquals(42, result.getValue());
    }

    @Test
    void typedEntryPreservesValueType() throws Exception {
        Map.Entry<String, Integer> result = MAPPER.readValue(
                a2q("{'key':'answer','value':'42'}"), new TypeReference<>() { });

        assertEquals("answer", result.getKey());
        assertEquals(Integer.valueOf(42), result.getValue());
    }

    @Test
    void rawEntryInNaturalShapeStillReportsMissingTypeInformation() {
        assertThrows(InvalidDefinitionException.class,
                () -> newJsonMapper().readValue(a2q("{'answer':42}"), Map.Entry.class));
    }
}
