package tools.jackson.databind.deser.creators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tools.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES;
import static tools.jackson.databind.testutil.DatabindTestUtil.jsonMapperBuilder;
import static tools.jackson.databind.testutil.JacksonTestUtilBase.a2q;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import tools.jackson.databind.json.JsonMapper;

// [databind#5734]
class CreatorAbsentPrimitivesTest {

    record PrimitiveRecord(int id, boolean enabled) {
    }

    @ParameterizedTest
    @MethodSource("absentPrimitivesForRecordArguments")
    void absentPrimitivesForRecordArguments(String json, PrimitiveRecord expected) {
        JsonMapper mapper = jsonMapperBuilder()
                .configure(FAIL_ON_NULL_FOR_PRIMITIVES, true) // Our values are not null, they are absent.
                .build();

        PrimitiveRecord actual = mapper.readValue(json, PrimitiveRecord.class);

        assertEquals(expected, actual);
    }

    static Stream<Arguments> absentPrimitivesForRecordArguments() {
        return Stream.of(
                Arguments.of(a2q("{ }"), new PrimitiveRecord(0, false)),
                Arguments.of(a2q("{'id': 42}"), new PrimitiveRecord(42, false)),
                Arguments.of(a2q("{'enabled': true}"), new PrimitiveRecord(0, true))
        );
    }
}
