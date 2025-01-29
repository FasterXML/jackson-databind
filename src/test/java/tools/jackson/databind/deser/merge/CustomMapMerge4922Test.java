package tools.jackson.databind.deser.merge;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonMerge;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("serial")
public class CustomMapMerge4922Test
    extends DatabindTestUtil
{
    // [databind#4922]
    interface MyMap4922<K, V> extends Map<K, V> {}

    static class MapImpl<K, V> extends HashMap<K, V> implements MyMap4922<K, V> {}

    static class MergeMap4922 {
        @JsonMerge // either here
        public MyMap4922<Integer, String> map = new MapImpl<>();
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#4922]: Merge for custom maps fails
    @Test
    void testJDKMapperReading() throws Exception {
        MergeMap4922 input = new MergeMap4922();
        input.map.put(3, "ADS");

        String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(input);
        MergeMap4922 merge2 = MAPPER.readValue(json, MergeMap4922.class);
        assertNotNull(merge2);
    }

}
