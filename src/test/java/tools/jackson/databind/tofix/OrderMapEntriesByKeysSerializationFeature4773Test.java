package tools.jackson.databind.tofix;

import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// [databind#4773] Test to verify `SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS` behavior
//     when serializing `Map` instances with un-comparable keys.
public class OrderMapEntriesByKeysSerializationFeature4773Test
        extends DatabindTestUtil
{

    public static class IncomparableContainer4773 {
        public Map<Currency, String> exampleMap = new HashMap<>();
    }

    public static class ObjectContainer4773 {
        public Map<Object, String> exampleMap = new HashMap<>();
    }

    private final ObjectMapper objectMapper = jsonMapperBuilder()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .build();

    @JacksonTestFailureExpected
    @Test
    void testSerializationWithIncomparableKeys()
            throws Exception
    {
        // Given
        IncomparableContainer4773 entity = new IncomparableContainer4773();
        entity.exampleMap.put(Currency.getInstance("GBP"), "GBP_TEXT");
        entity.exampleMap.put(Currency.getInstance("AUD"), "AUD_TEXT");

        // When : Throws exception
        // com.fasterxml.jackson.databind.JsonMappingException: class java.util.Currency cannot be cast to class java.lang.Comparable
        String jsonResult = objectMapper.writeValueAsString(entity);

        // Then : Order should not matter, just plain old serialize
        assertTrue(jsonResult.contains("GBP"));
        assertTrue(jsonResult.contains("AUD"));
    }

    @Test
    void testSerializationWithGenericObjectKeys()
            throws Exception
    {
        // Given
        ObjectContainer4773 entity = new ObjectContainer4773();
        entity.exampleMap.put(5, "N_TEXT");
        entity.exampleMap.put(1, "GBP_TEXT");
        entity.exampleMap.put(3, "T_TEXT");
        entity.exampleMap.put(4, "AUD_TEXT");
        entity.exampleMap.put(2, "KRW_TEXT");

        // When
        String jsonResult = objectMapper.writeValueAsString(entity);

        // Then
        assertEquals(a2q("{'exampleMap':{" +
                "'1':'GBP_TEXT'," +
                "'2':'KRW_TEXT'," +
                "'3':'T_TEXT'," +
                "'4':'AUD_TEXT'," +
                "'5':'N_TEXT'}}"), jsonResult);
    }

}
