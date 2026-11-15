package tools.jackson.databind.deser.merge;

import java.util.EnumMap;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonMerge;

import tools.jackson.databind.*;

import static org.junit.jupiter.api.Assertions.*;

import static tools.jackson.databind.testutil.DatabindTestUtil.*;

// [databind#6445]: `@JsonMerge` did not merge existing values for `EnumMap`
// properties: values were always re-constructed instead of updated in place
// (unlike `MapDeserializer` which merges into the existing value).
public class EnumMapMerge6445Test
{
    enum ChannelKey { INPUT_1, INPUT_2 }

    // Value type with no default constructor (nor any Creator):
    // it can only be populated by merging into an existing instance
    static class Channel
    {
        public String name;
        public int level;

        public Channel(String name, int level) {
            this.name = name;
            this.level = level;
        }

        // Second constructor: prevents implicit Creator detection
        public Channel(String name) {
            this(name, 0);
        }
    }

    static class ModelWithChannels
    {
        @JsonMerge
        public EnumMap<ChannelKey, Channel> channels;

        protected ModelWithChannels() {
            channels = new EnumMap<>(ChannelKey.class);
            channels.put(ChannelKey.INPUT_1, new Channel("input", 1));
        }
    }

    // Value type with a default constructor, for merge/replace checks
    static class Bean
    {
        public String a;
        public String b;
    }

    static class ModelWithBeans
    {
        @JsonMerge
        public EnumMap<ChannelKey, Bean> beans;

        protected ModelWithBeans() {
            beans = new EnumMap<>(ChannelKey.class);
            Bean bean = new Bean();
            bean.a = "a";
            bean.b = "b";
            beans.put(ChannelKey.INPUT_1, bean);
        }
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            // Make sure we'll report merge problems by default
            .disable(MapperFeature.IGNORE_MERGE_FOR_UNMERGEABLE)
            .build();

    @Test
    public void mergeIntoExistingEnumMapValue() throws Exception
    {
        // Without merging, deserialization fails because `Channel`
        // has no Creators; with merging, the existing instance is updated
        ModelWithChannels result = MAPPER.readValue(
                a2q("{'channels':{'INPUT_1':{'level':5}}}"),
                ModelWithChannels.class);
        assertEquals(1, result.channels.size());
        Channel ch = result.channels.get(ChannelKey.INPUT_1);
        assertEquals("input", ch.name);
        assertEquals(5, ch.level);
    }

    @Test
    public void mergeAddsNewKeysAndMergesExisting() throws Exception
    {
        ModelWithBeans result = MAPPER.readValue(
                a2q("{'beans':{'INPUT_1':{'a':'A'},'INPUT_2':{'a':'x','b':'y'}}}"),
                ModelWithBeans.class);
        assertEquals(2, result.beans.size());
        // Existing value merged, not replaced
        assertEquals("A", result.beans.get(ChannelKey.INPUT_1).a);
        assertEquals("b", result.beans.get(ChannelKey.INPUT_1).b);
        // New key constructed normally
        assertEquals("x", result.beans.get(ChannelKey.INPUT_2).a);
        assertEquals("y", result.beans.get(ChannelKey.INPUT_2).b);
    }

    @Test
    public void updatingReaderMergesIntoSameInstance() throws Exception
    {
        ModelWithBeans input = new ModelWithBeans();
        Bean original = input.beans.get(ChannelKey.INPUT_1);
        ModelWithBeans result = MAPPER.readerForUpdating(input)
                .readValue(a2q("{'beans':{'INPUT_1':{'a':'A'}}}"));
        assertSame(original, result.beans.get(ChannelKey.INPUT_1));
        assertEquals("A", original.a);
        assertEquals("b", original.b);
    }

    // [databind#3205]: content type with `mergeable = false` must not merge
    @Test
    public void contentMergeDisabledByType() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .withConfigOverride(Bean.class,
                        o -> o.setMergeable(false))
                .build();
        ModelWithBeans input = new ModelWithBeans();
        Bean original = input.beans.get(ChannelKey.INPUT_1);
        ModelWithBeans result = mapper.readerForUpdating(input)
                .readValue(a2q("{'beans':{'INPUT_1':{'a':'A'}}}"));
        Bean updated = result.beans.get(ChannelKey.INPUT_1);
        // Replaced, not merged
        assertNotSame(original, updated);
        assertEquals("A", updated.a);
        assertNull(updated.b);
    }
}
