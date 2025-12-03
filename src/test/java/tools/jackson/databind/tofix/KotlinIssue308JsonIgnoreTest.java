package tools.jackson.databind.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.*;
import static tools.jackson.databind.testutil.DatabindTestUtil.*;

// From https://github.com/FasterXML/jackson-module-kotlin/issues/308
/**
 * Test for verifying that {@link JsonIgnore} on a field combined with
 * {@link JsonProperty} on a private setter method works correctly.
 * The private method "unpacks" the JSON property value to a different field.
 */
public class KotlinIssue308JsonIgnoreTest
{
    static class TestDto
    {
        @JsonIgnore
        Integer id;

        Integer cityId;

        @JsonCreator
        public TestDto(Integer id, Integer cityId) {
            this.id = id;
            this.cityId = cityId;
        }

        @JsonProperty("id")
        void unpackId(Integer idObj) {
            cityId = idObj;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @JacksonTestFailureExpected
    @Test
    public void testJsonIgnoreWithJsonPropertyUnpacker() throws Exception
    {
        TestDto dto = MAPPER.readValue("{\"id\":12345}", TestDto.class);

        assertNotNull(dto);
        assertEquals(Integer.valueOf(12345), dto.cityId);
        assertNull(dto.id);
    }
}
