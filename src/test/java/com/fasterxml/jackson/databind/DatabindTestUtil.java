package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class containing test utility methods.
 * The methods are migrated from {@link BaseMapTest} and {@link BaseTest},
 * as part of JUnit 5 migration.
 *
 * @since 2.17
 */
public class DatabindTestUtil
{

    /*
    /**********************************************************
    /* Helper methods, serialization
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    public static Map<String,Object> writeAndMap(ObjectMapper m, Object value)
            throws IOException
    {
        String str = m.writeValueAsString(value);
        return (Map<String,Object>) m.readValue(str, LinkedHashMap.class);
    }

    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */

    public static ObjectMapper newJsonMapper() {
        return new JsonMapper();
    }

    public static JsonMapper.Builder jsonMapperBuilder() {
        return JsonMapper.builder();
    }

    /*
    /**********************************************************
    /* Encoding or String representations
    /**********************************************************
     */

    public static String a2q(String json) {
        return json.replace("'", "\"");
    }
}
