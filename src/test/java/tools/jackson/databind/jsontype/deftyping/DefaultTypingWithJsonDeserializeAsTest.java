package tools.jackson.databind.jsontype.deftyping;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.NoCheckSubTypeValidator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A type deserializer for a property must be built for the type the property is deserialized
 * as, after `@JsonDeserialize(as/keyAs/contentAs)` has refined the declared type, or the type
 * resolved from a type id is specialized from the declared type instead and the refinement
 * is lost.
 */
public class DefaultTypingWithJsonDeserializeAsTest
    extends DatabindTestUtil
{
    static class IntKeyMapHolder {
        @JsonDeserialize(keyAs = Integer.class)
        public Map<Object, String> map;
    }

    static class IntContentListHolder {
        @JsonDeserialize(contentAs = Integer.class)
        public List<Object> list;
    }

    static class IntContentMapHolder {
        @JsonDeserialize(contentAs = Integer.class)
        public Map<String, Object> map;
    }

    static class IntKeyMapCreatorHolder {
        public final Map<Object, String> map;

        @JsonCreator
        public IntKeyMapCreatorHolder(@JsonProperty("map") @JsonDeserialize(keyAs = Integer.class) Map<Object, String> map) {
            this.map = map;
        }
    }

    private ObjectMapper mapperWith(DefaultTyping typing, JsonTypeInfo.As as) {
        return jsonMapperBuilder()
                .activateDefaultTyping(NoCheckSubTypeValidator.instance, typing, as)
                .build();
    }

    private static final DefaultTyping[] TYPINGS = {
        DefaultTyping.NON_FINAL, DefaultTyping.OBJECT_AND_NON_CONCRETE
    };
    private static final JsonTypeInfo.As[] INCLUSIONS = {
        JsonTypeInfo.As.WRAPPER_ARRAY, JsonTypeInfo.As.PROPERTY, JsonTypeInfo.As.WRAPPER_OBJECT
    };

    @Test
    public void testKeyAsWithDefaultTyping() throws Exception
    {
        for (DefaultTyping typing : TYPINGS) {
            for (JsonTypeInfo.As as : INCLUSIONS) {
                ObjectMapper mapper = mapperWith(typing, as);
                IntKeyMapHolder holder = new IntKeyMapHolder();
                holder.map = new HashMap<>();
                holder.map.put(1, "a");
                String json = mapper.writeValueAsString(holder);

                IntKeyMapHolder result = mapper.readValue(json, IntKeyMapHolder.class);
                assertEquals(1, result.map.size(), json);
                Object key = result.map.keySet().iterator().next();
                assertEquals(Integer.class, key.getClass(), json);
                assertEquals("a", result.map.get(1), json);
            }
        }
    }

    @Test
    public void testKeyAsOnCreatorWithDefaultTyping() throws Exception
    {
        for (DefaultTyping typing : TYPINGS) {
            for (JsonTypeInfo.As as : INCLUSIONS) {
                ObjectMapper mapper = mapperWith(typing, as);
                Map<Object, String> map = new HashMap<>();
                map.put(1, "a");
                String json = mapper.writeValueAsString(new IntKeyMapCreatorHolder(map));

                IntKeyMapCreatorHolder result = mapper.readValue(json, IntKeyMapCreatorHolder.class);
                Object key = result.map.keySet().iterator().next();
                assertEquals(Integer.class, key.getClass(), json);
            }
        }
    }

    // contentAs narrows the content to a type Default Typing does not apply to, so the elements
    // are read without a type id
    @Test
    public void testContentAsWithDefaultTyping() throws Exception
    {
        for (DefaultTyping typing : TYPINGS) {
            for (JsonTypeInfo.As as : INCLUSIONS) {
                ObjectMapper mapper = mapperWith(typing, as);

                IntContentListHolder listHolder = new IntContentListHolder();
                listHolder.list = new ArrayList<>(Arrays.asList(1, 2));
                String json = mapper.writeValueAsString(listHolder);
                IntContentListHolder listResult = mapper.readValue(json, IntContentListHolder.class);
                assertEquals(Arrays.asList(1, 2), listResult.list, json);

                IntContentMapHolder mapHolder = new IntContentMapHolder();
                mapHolder.map = new HashMap<>();
                mapHolder.map.put("a", 1);
                json = mapper.writeValueAsString(mapHolder);
                IntContentMapHolder mapResult = mapper.readValue(json, IntContentMapHolder.class);
                assertEquals(Integer.valueOf(1), mapResult.map.get("a"), json);
            }
        }
    }
}
