package tools.jackson.databind.jsontype.deftyping;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

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
 * is lost. But whether type ids are expected at all is decided by the declared type, same as
 * on serialization.
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

    static class LongContentListHolder {
        @JsonDeserialize(contentAs = Long.class)
        public List<Object> list;
    }

    static class IntKeyMapCreatorHolder {
        public final Map<Object, String> map;

        @JsonCreator
        public IntKeyMapCreatorHolder(@JsonProperty("map") @JsonDeserialize(keyAs = Integer.class) Map<Object, String> map) {
            this.map = map;
        }
    }

    static class CustomMap<K, V> extends HashMap<K, V> { }

    static abstract class Base {
        public int x;
    }

    static class Impl extends Base { }

    static class AbstractAsConcreteHolder {
        @JsonDeserialize(as = Impl.class)
        public Base value;
    }

    static class ObjectAsConcreteHolder {
        @JsonDeserialize(as = Impl.class)
        public Object value;
    }

    static class ContentAsConcreteHolder {
        @JsonDeserialize(contentAs = Impl.class)
        public List<Base> list;
    }

    static class ContentAsConcreteAtomicRefHolder {
        @JsonDeserialize(contentAs = Impl.class)
        public AtomicReference<Base> ref;
    }

    static class ContentAsConcreteOptionalHolder {
        @JsonDeserialize(contentAs = Impl.class)
        public Optional<Base> opt;
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
    public void keyAsWithDefaultTyping()
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

    // Map types other than the few `TypeFactory` has short-cuts for (like `HashMap`) are
    // specialized from type bindings, not from (refined) key type
    @Test
    public void keyAsWithDefaultTypingForOtherMapTypes()
    {
        for (DefaultTyping typing : TYPINGS) {
            for (JsonTypeInfo.As as : INCLUSIONS) {
                ObjectMapper mapper = mapperWith(typing, as);
                IntKeyMapHolder holder = new IntKeyMapHolder();

                holder.map = new ConcurrentHashMap<>(Map.of(1, "a"));
                String json = mapper.writeValueAsString(holder);
                IntKeyMapHolder result = mapper.readValue(json, IntKeyMapHolder.class);
                assertEquals(ConcurrentHashMap.class, result.map.getClass(), json);
                assertEquals(Integer.class, result.map.keySet().iterator().next().getClass(), json);

                CustomMap<Object, String> custom = new CustomMap<>();
                custom.put(1, "a");
                holder.map = custom;
                json = mapper.writeValueAsString(holder);
                result = mapper.readValue(json, IntKeyMapHolder.class);
                assertEquals(CustomMap.class, result.map.getClass(), json);
                assertEquals(Integer.class, result.map.keySet().iterator().next().getClass(), json);
            }
        }
    }

    @Test
    public void keyAsOnCreatorWithDefaultTyping()
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

    // Integer values are written without type id ("natural" type)
    @Test
    public void contentAsWithDefaultTyping()
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

    // Long is not a "natural" type: elements are written with type ids (for declared `Object`
    // content) that must still be read, even though Default Typing does not apply to `Long`
    @Test
    public void contentAsLongWithDefaultTyping()
    {
        for (DefaultTyping typing : TYPINGS) {
            for (JsonTypeInfo.As as : INCLUSIONS) {
                ObjectMapper mapper = mapperWith(typing, as);
                LongContentListHolder holder = new LongContentListHolder();
                holder.list = new ArrayList<>(Arrays.asList(1L, 2L));
                String json = mapper.writeValueAsString(holder);

                LongContentListHolder result = mapper.readValue(json, LongContentListHolder.class);
                assertEquals(Arrays.asList(1L, 2L), result.list, json);
            }
        }
    }

    // Narrowing to a concrete type must not drop type ids that are written for the
    // declared (abstract, or `Object`) type
    @Test
    public void asConcreteForAbstractWithDefaultTyping()
    {
        for (DefaultTyping typing : new DefaultTyping[] { DefaultTyping.NON_FINAL,
                DefaultTyping.OBJECT_AND_NON_CONCRETE, DefaultTyping.NON_CONCRETE_AND_ARRAYS }) {
            for (JsonTypeInfo.As as : INCLUSIONS) {
                ObjectMapper mapper = mapperWith(typing, as);
                AbstractAsConcreteHolder holder = new AbstractAsConcreteHolder();
                Impl impl = new Impl();
                impl.x = 3;
                holder.value = impl;
                String json = mapper.writeValueAsString(holder);

                AbstractAsConcreteHolder result = mapper.readValue(json, AbstractAsConcreteHolder.class);
                assertEquals(Impl.class, result.value.getClass(), json);
                assertEquals(3, result.value.x, json);
            }
        }
    }

    @Test
    public void asConcreteForObjectWithDefaultTyping()
    {
        for (DefaultTyping typing : new DefaultTyping[] { DefaultTyping.NON_FINAL,
                DefaultTyping.OBJECT_AND_NON_CONCRETE, DefaultTyping.JAVA_LANG_OBJECT }) {
            for (JsonTypeInfo.As as : INCLUSIONS) {
                ObjectMapper mapper = mapperWith(typing, as);
                ObjectAsConcreteHolder holder = new ObjectAsConcreteHolder();
                Impl impl = new Impl();
                impl.x = 3;
                holder.value = impl;
                String json = mapper.writeValueAsString(holder);

                ObjectAsConcreteHolder result = mapper.readValue(json, ObjectAsConcreteHolder.class);
                assertEquals(Impl.class, result.value.getClass(), json);
                assertEquals(3, ((Impl) result.value).x, json);
            }
        }
    }

    // Reference types go through the same content type deserializer handling as containers
    @Test
    public void contentAsConcreteForReferenceWithDefaultTyping()
    {
        for (DefaultTyping typing : TYPINGS) {
            for (JsonTypeInfo.As as : INCLUSIONS) {
                ObjectMapper mapper = mapperWith(typing, as);
                Impl impl = new Impl();
                impl.x = 3;

                ContentAsConcreteAtomicRefHolder refHolder = new ContentAsConcreteAtomicRefHolder();
                refHolder.ref = new AtomicReference<>(impl);
                String json = mapper.writeValueAsString(refHolder);
                ContentAsConcreteAtomicRefHolder refResult = mapper.readValue(json,
                        ContentAsConcreteAtomicRefHolder.class);
                assertEquals(Impl.class, refResult.ref.get().getClass(), json);
                assertEquals(3, refResult.ref.get().x, json);

                ContentAsConcreteOptionalHolder optHolder = new ContentAsConcreteOptionalHolder();
                optHolder.opt = Optional.of(impl);
                json = mapper.writeValueAsString(optHolder);
                ContentAsConcreteOptionalHolder optResult = mapper.readValue(json,
                        ContentAsConcreteOptionalHolder.class);
                assertEquals(Impl.class, optResult.opt.get().getClass(), json);
                assertEquals(3, optResult.opt.get().x, json);
            }
        }
    }

    @Test
    public void contentAsConcreteForAbstractWithDefaultTyping()
    {
        for (DefaultTyping typing : TYPINGS) {
            for (JsonTypeInfo.As as : INCLUSIONS) {
                ObjectMapper mapper = mapperWith(typing, as);
                ContentAsConcreteHolder holder = new ContentAsConcreteHolder();
                Impl impl = new Impl();
                impl.x = 3;
                holder.list = new ArrayList<>(List.of(impl));
                String json = mapper.writeValueAsString(holder);

                ContentAsConcreteHolder result = mapper.readValue(json, ContentAsConcreteHolder.class);
                assertEquals(1, result.list.size(), json);
                assertEquals(Impl.class, result.list.get(0).getClass(), json);
                assertEquals(3, result.list.get(0).x, json);
            }
        }
    }
}
