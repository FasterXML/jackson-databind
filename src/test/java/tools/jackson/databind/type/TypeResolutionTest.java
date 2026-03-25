package tools.jackson.databind.type;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("serial")
public class TypeResolutionTest extends DatabindTestUtil
{
    public static class LongValuedMap<K> extends HashMap<K, Long> { }

    static class GenericList<X> extends ArrayList<X> { }
    static class GenericList2<Y> extends GenericList<Y> { }

    static class LongList extends GenericList2<Long> { }
    static class MyLongList<T> extends LongList { }

    static class Range<E extends Comparable<E>> implements Serializable
    {
         public Range(E start, E end) { }
    }

    static class DoubleRange extends Range<Double> {
        public DoubleRange() { super(null, null); }
        public DoubleRange(Double s, Double e) { super(s, e); }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    @Test
    public void testMaps()
    {
        TypeFactory tf = defaultTypeFactory();
        JavaType t = tf.constructType(new TypeReference<LongValuedMap<String>>() { });
        MapType type = (MapType) t;
        assertSame(LongValuedMap.class, type.getRawClass());
        assertEquals(tf.constructType(String.class), type.getKeyType());
        assertEquals(tf.constructType(Long.class), type.getContentType());
    }

    @Test
    public void testListViaTypeRef()
    {
        TypeFactory tf = defaultTypeFactory();
        JavaType t = tf.constructType(new TypeReference<MyLongList<Integer>>() {});
        CollectionType type = (CollectionType) t;
        assertSame(MyLongList.class, type.getRawClass());
        assertEquals(tf.constructType(Long.class), type.getContentType());
    }

    @Test
    public void testListViaClass()
    {
        TypeFactory tf = defaultTypeFactory();
        JavaType t = tf.constructType(LongList.class);
        JavaType type = (CollectionType) t;
        assertSame(LongList.class, type.getRawClass());
        assertEquals(tf.constructType(Long.class), type.getContentType());
    }

    @Test
    public void testGeneric()
    {
        TypeFactory tf = defaultTypeFactory();

        // First, via simple sub-class
        JavaType t = tf.constructType(DoubleRange.class);
        JavaType rangeParams = t.findSuperType(Range.class);
        assertEquals(1, rangeParams.containedTypeCount());
        assertEquals(Double.class, rangeParams.containedType(0).getRawClass());

        // then using TypeRef
        t = tf.constructType(new TypeReference<DoubleRange>() { });
        rangeParams = t.findSuperType(Range.class);
        assertEquals(1, rangeParams.containedTypeCount());
        assertEquals(Double.class, rangeParams.containedType(0).getRawClass());
    }

    /*
    /**********************************************************
    /* Unit tests: wildcard bound resolution [databind#5285]
    /**********************************************************
     */

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = EmailSettings.class, name = "EMAIL"),
        @JsonSubTypes.Type(value = PhoneSettings.class, name = "PHONE")
    })
    interface Settings { }

    record EmailSettings(String email) implements Settings { }

    record PhoneSettings(String phoneNumber) implements Settings { }

    record MessageWrapper<T extends Settings>(T settings, String message) { }

    // Container to obtain ParameterizedType via field reflection
    static class Holder5285 {
        MessageWrapper<?> wildcardWrapper;
        MessageWrapper<EmailSettings> specificWrapper;
    }

    static class Box5285<T extends Number> {
        public T value;
    }

    static class BoxHolder5285 {
        Box5285<?> wildcardBox;
    }

    static class Pair5285<L, R> {
        public L left;
        public R right;
    }

    @SuppressWarnings("unused")
    static class PairHolder5285 {
        Pair5285<String, ?> wildcardPair;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // Core issue: wildcard type resolution should use declared bound
    // [databind#5285]
    @Test
    void wildcardResolvesToDeclaredBound() throws Exception {
        Type genericType = Holder5285.class.getDeclaredField("wildcardWrapper").getGenericType();
        JavaType jacksonType = MAPPER.constructType(genericType);

        // Should resolve to MessageWrapper<Settings>, NOT MessageWrapper<Object>
        assertEquals(MessageWrapper.class, jacksonType.getRawClass());
        assertEquals(1, jacksonType.containedTypeCount());
        assertEquals(Settings.class, jacksonType.containedType(0).getRawClass());
    }

    // [databind#5285]
    @Test
    void specificTypeUnchanged() throws Exception {
        Type genericType = Holder5285.class.getDeclaredField("specificWrapper").getGenericType();
        JavaType jacksonType = MAPPER.constructType(genericType);

        assertEquals(MessageWrapper.class, jacksonType.getRawClass());
        assertEquals(1, jacksonType.containedTypeCount());
        assertEquals(EmailSettings.class, jacksonType.containedType(0).getRawClass());
    }

    // [databind#5285]
    @Test
    void serializationPreservesTypeInfo() throws Exception {
        MessageWrapper<EmailSettings> wrapper = new MessageWrapper<>(
                new EmailSettings("me@me.com"), "Sample Message");
        Type genericType = Holder5285.class.getDeclaredField("wildcardWrapper").getGenericType();
        JavaType jacksonType = MAPPER.constructType(genericType);
        String json = MAPPER.writerFor(jacksonType).writeValueAsString(wrapper);

        assertTrue(json.contains("\"type\":\"EMAIL\""),
                "JSON should contain type discriminator, got: " + json);
        assertTrue(json.contains("\"email\":\"me@me.com\""),
                "JSON should contain email field, got: " + json);
    }

    // [databind#5285]
    @Test
    void wildcardResolvesToNumberBound() throws Exception {
        Type genericType = BoxHolder5285.class.getDeclaredField("wildcardBox").getGenericType();
        JavaType jacksonType = MAPPER.constructType(genericType);

        assertEquals(Box5285.class, jacksonType.getRawClass());
        assertEquals(1, jacksonType.containedTypeCount());
        assertEquals(Number.class, jacksonType.containedType(0).getRawClass());
    }

    // [databind#5285]
    @Test
    void unboundedTypeParamStaysObject() throws Exception {
        Type genericType = PairHolder5285.class.getDeclaredField("wildcardPair").getGenericType();
        JavaType jacksonType = MAPPER.constructType(genericType);

        assertEquals(Pair5285.class, jacksonType.getRawClass());
        assertEquals(2, jacksonType.containedTypeCount());
        assertEquals(String.class, jacksonType.containedType(0).getRawClass());
        // R has no explicit bound, so ? should stay as Object
        assertEquals(Object.class, jacksonType.containedType(1).getRawClass());
    }
}
