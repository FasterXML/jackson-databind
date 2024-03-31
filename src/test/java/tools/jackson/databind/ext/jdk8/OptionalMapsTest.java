package tools.jackson.databind.ext.jdk8;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class OptionalMapsTest
    extends DatabindTestUtil
{
    static final class OptMapBean {
        public Map<String, Optional<?>> values;

        public OptMapBean(String key, Optional<?> v) {
            values = new LinkedHashMap<>();
            values.put(key, v);
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    public void testMapElementInclusion() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder().changeDefaultPropertyInclusion(
                incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL)
                    .withContentInclusion(JsonInclude.Include.NON_ABSENT))
                .build();
        // first: Absent entry/-ies should NOT be included
        assertEquals("{\"values\":{}}",
                mapper.writeValueAsString(new OptMapBean("key", Optional.empty())));
        // but non-empty should
        assertEquals("{\"values\":{\"key\":\"value\"}}",
                mapper.writeValueAsString(new OptMapBean("key", Optional.of("value"))));
        // and actually even empty
        assertEquals("{\"values\":{\"key\":\"\"}}",
                mapper.writeValueAsString(new OptMapBean("key", Optional.of(""))));
    }

}
