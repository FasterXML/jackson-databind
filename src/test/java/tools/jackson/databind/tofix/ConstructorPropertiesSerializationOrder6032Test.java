package tools.jackson.databind.tofix;

import java.beans.ConstructorProperties;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#6032]: unannotated constructor parameter is treated as
// creator property for serialization ordering.
class ConstructorPropertiesSerializationOrder6032Test extends DatabindTestUtil
{
    static final class WithoutAnnotation {
        private final String aa = "a";
        private final String bb;

        public WithoutAnnotation(String bb) {
            this.bb = bb;
        }

        public String getAa() {
            return aa;
        }

        public String getBb() {
            return bb;
        }
    }

    static final class WithAnnotation {
        private final String aa = "a";
        private final String bb;

        @ConstructorProperties({ "bb" })
        public WithAnnotation(String bb) {
            this.bb = bb;
        }

        public String getAa() {
            return aa;
        }

        public String getBb() {
            return bb;
        }
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .enable(MapperFeature.SORT_CREATOR_PROPERTIES_FIRST)
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .build();

    @Test
    void annotatedConstructorPropertyShouldBeSortedFirst() throws Exception
    {
        assertEquals(a2q("{'bb':'b','aa':'a'}"),
                MAPPER.writeValueAsString(new WithAnnotation("b")));
    }

    @JacksonTestFailureExpected
    @Test
    void unannotatedConstructorShouldNotChangePropertyOrder() throws Exception
    {
        assertEquals(a2q("{'aa':'a','bb':'b'}"),
                MAPPER.writeValueAsString(new WithoutAnnotation("b")));
    }
}
