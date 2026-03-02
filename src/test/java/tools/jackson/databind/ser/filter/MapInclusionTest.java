package tools.jackson.databind.ser.filter;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;

import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class MapInclusionTest extends DatabindTestUtil
{
    // [databind#588]
    static class NoEmptiesMapContainer {
        @JsonInclude(value=JsonInclude.Include.NON_EMPTY,
                content=JsonInclude.Include.NON_EMPTY)
        public Map<String,String> stuff = new LinkedHashMap<>();

        public NoEmptiesMapContainer add(String key, String value) {
            stuff.put(key, value);
            return this;
        }
    }

    static class NoNullsMapContainer {
        @JsonInclude(value=JsonInclude.Include.NON_NULL,
                content=JsonInclude.Include.NON_NULL)
        public Map<String,String> stuff = new LinkedHashMap<>();

        public NoNullsMapContainer add(String key, String value) {
            stuff.put(key, value);
            return this;
        }
    }

    static class NoNullsNotEmptyMapContainer {
        @JsonInclude(value=JsonInclude.Include.NON_EMPTY,
                content=JsonInclude.Include.NON_NULL)
        public Map<String,String> stuff = new LinkedHashMap<>();

        public NoNullsNotEmptyMapContainer add(String key, String value) {
            stuff.put(key, value);
            return this;
        }
    }

    // [databind#2909]
    static class Wrapper2909 {
        @JsonValue
        public Map<String, String> values = new HashMap<>();
    }

    static class TopLevel2909 {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public Wrapper2909 nested = new Wrapper2909();
    }

    // [databind#2572]
    @JsonPropertyOrder({ "model", "properties" })
    static class Car {
        public String model;
        public Map<String, Integer> properties;
    }

    // [databind#1649]
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_EMPTY)
    static class Bean1649 {
        public Map<String, String> map;

        public Bean1649(String key, String value) {
            map = new LinkedHashMap<>();
            map.put(key, value);
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************************
    /* Test methods, NON_EMPTY / NON_NULL on Map value and content
    /**********************************************************************
     */

    // [databind#588]
    @Test
    public void testNonEmptyValueMapViaProp() throws Exception
    {
        assertEquals(a2q("{}"),
                MAPPER.writeValueAsString(new NoEmptiesMapContainer().add("a", null).add("b", "")));
    }

    @Test
    public void testNoNullsMap() throws Exception
    {
        assertEquals(a2q("{'stuff':{'b':''}}"),
                MAPPER.writeValueAsString(new NoNullsMapContainer().add("a", null).add("b", "")));
    }

    @Test
    public void testNonEmptyNoNullsMap() throws Exception
    {
        assertEquals(a2q("{'stuff':{'b':''}}"),
                MAPPER.writeValueAsString(new NoNullsNotEmptyMapContainer().add("a", null).add("b", "")));

        assertEquals(a2q("{}"),
                MAPPER.writeValueAsString(new NoNullsNotEmptyMapContainer().add("a", null).add("b", null)));
    }

    // [databind#2909]
    @Test
    public void testMapViaJsonValue() throws Exception
    {
        assertEquals(a2q("{}"), MAPPER.writeValueAsString(new TopLevel2909()));
    }

    /*
    /**********************************************************************
    /* Test methods, config-override for Map content inclusion [databind#2572]
    /**********************************************************************
     */

    // [databind#2572]
    @Test
    public void test2572MapDefault() throws Exception
    {
        final JsonInclude.Value BOTH_NON_NULL = JsonInclude.Value.construct(
                JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL);
        final Map<String, Integer> carProperties = new LinkedHashMap<>();
        carProperties.put("Speed", 100);
        carProperties.put("Weight", null);
        final Car car = new Car();
        car.model = "F60";
        car.properties = carProperties;

        ObjectMapper mapper = JsonMapper.builder()
                .changeDefaultPropertyInclusion(incl -> BOTH_NON_NULL)
                .build();
        assertEquals(a2q("{'Speed':100}"),
                mapper.writeValueAsString(carProperties));
        assertEquals(a2q("{'model':'F60','properties':{'Speed':100}}"),
                mapper.writeValueAsString(car));
    }

    @Test
    public void test2572MapOverrideUseDefaults() throws Exception
    {
        final JsonInclude.Value BOTH_NON_NULL = JsonInclude.Value.construct(
                JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL);
        final Map<String, Integer> carProperties = new LinkedHashMap<>();
        carProperties.put("Speed", 100);
        carProperties.put("Weight", null);
        final Car car = new Car();
        car.model = "F60";
        car.properties = carProperties;

        ObjectMapper mapper = JsonMapper.builder()
                .changeDefaultPropertyInclusion(incl -> BOTH_NON_NULL)
                .withConfigOverride(Map.class,
                        o -> o.setInclude(JsonInclude.Value.construct(JsonInclude.Include.USE_DEFAULTS,
                        JsonInclude.Include.USE_DEFAULTS)))
                .build();
        assertEquals(a2q("{'Speed':100}"),
                mapper.writeValueAsString(carProperties));
        assertEquals(a2q("{'model':'F60','properties':{'Speed':100}}"),
                mapper.writeValueAsString(car));
    }

    @Test
    public void test2572MapOverrideInclAlways() throws Exception
    {
        final JsonInclude.Value BOTH_NON_NULL = JsonInclude.Value.construct(
                JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL);
        final Map<String, Integer> carProperties = new LinkedHashMap<>();
        carProperties.put("Speed", 100);
        carProperties.put("Weight", null);
        final Car car = new Car();
        car.model = "F60";
        car.properties = carProperties;

        ObjectMapper mapper = JsonMapper.builder()
                .changeDefaultPropertyInclusion(incl -> BOTH_NON_NULL)
                .withConfigOverride(Map.class,
                        o -> o.setInclude(JsonInclude.Value.construct(JsonInclude.Include.ALWAYS,
                        JsonInclude.Include.ALWAYS)))
                .build();
        assertEquals(a2q("{'Speed':100,'Weight':null}"),
                mapper.writeValueAsString(carProperties));
        assertEquals(a2q("{'model':'F60','properties':{'Speed':100,'Weight':null}}"),
                mapper.writeValueAsString(car));
    }

    /*
    /**********************************************************************
    /* Test methods, class-level NON_EMPTY on Map value and content [databind#1649]
    /**********************************************************************
     */

    // [databind#1649]
    @Test
    public void testNonEmptyViaClass() throws Exception {
        assertEquals(a2q("{'map':{'a':'b'}}"),
                MAPPER.writeValueAsString(new Bean1649("a", "b")));
        // null value → map content excluded → empty map → map itself excluded
        assertEquals(a2q("{}"),
                MAPPER.writeValueAsString(new Bean1649("a", null)));
        // empty string value → excluded by NON_EMPTY
        assertEquals(a2q("{}"),
                MAPPER.writeValueAsString(new Bean1649("a", "")));
    }
}
