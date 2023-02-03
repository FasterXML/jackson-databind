package com.fasterxml.jackson.databind.convert;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;

import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import org.junit.Assert;

// [databind#3418]: Coercion from empty String to Collection<String>, with
// `DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY`
public class EmptyStringAsSingleValueTest extends BaseMapTest
{
    static final class StringWrapper {
        private final String s;

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public StringWrapper(String s) {
            this.s = s;
        }

        @Override
        public String toString() {
            return "StringWrapper{" + s + "}";
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof StringWrapper && ((StringWrapper) obj).s.equals(s);
        }

        @Override
        public int hashCode() {
            return s.hashCode();
        }
    }

    private final ObjectMapper NORMAL_MAPPER = jsonMapperBuilder()
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .build();

    private final ObjectMapper COERCION_MAPPER = jsonMapperBuilder()
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        // same as XmlMapper
            .withCoercionConfigDefaults(h -> {
                h.setAcceptBlankAsEmpty(true)
                    .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsEmpty);
            })
            .build();

    public void testEmptyToList() throws Exception {
        // NO coercion + empty string input + StringCollectionDeserializer
        assertEquals(Collections.singletonList(""),
                NORMAL_MAPPER.readValue("\"\"", new TypeReference<List<String>>() {}));
    }

    public void testEmptyToListWrapper() throws Exception {
        // NO coercion + empty string input + normal CollectionDeserializer
        assertEquals(Collections.singletonList(new StringWrapper("")),
                NORMAL_MAPPER.readValue("\"\"", new TypeReference<List<StringWrapper>>() {}));
    }

    public void testCoercedEmptyToList() throws Exception {
        // YES coercion + empty string input + StringCollectionDeserializer
        assertEquals(Collections.emptyList(), COERCION_MAPPER.readValue("\"\"",
                new TypeReference<List<String>>() {}));
    }

    public void testCoercedEmptyToListWrapper() throws Exception {
        // YES coercion + empty string input + normal CollectionDeserializer
        assertEquals(Collections.emptyList(),
                COERCION_MAPPER.readValue("\"\"", new TypeReference<List<StringWrapper>>() {}));
    }

    public void testCoercedListToList() throws Exception {
        // YES coercion + empty LIST input + StringCollectionDeserializer
        assertEquals(Collections.emptyList(),
                COERCION_MAPPER.readValue("[]", new TypeReference<List<String>>() {}));
    }

    public void testCoercedListToListWrapper() throws Exception {
        // YES coercion + empty LIST input + normal CollectionDeserializer
        assertEquals(Collections.emptyList(),
                COERCION_MAPPER.readValue("[]", new TypeReference<List<StringWrapper>>() {}));
    }

    public void testBlankToList() throws Exception {
        // NO coercion + empty string input + StringCollectionDeserializer
        assertEquals(Collections.singletonList(" "),
                NORMAL_MAPPER.readValue("\" \"", new TypeReference<List<String>>() {}));
    }

    public void testBlankToListWrapper() throws Exception {
        // NO coercion + empty string input + normal CollectionDeserializer
        assertEquals(Collections.singletonList(new StringWrapper(" ")),
                NORMAL_MAPPER.readValue("\" \"", new TypeReference<List<StringWrapper>>() {}));
    }

    public void testCoercedBlankToList() throws Exception {
        // YES coercion + empty string input + StringCollectionDeserializer
        assertEquals(Collections.emptyList(),
                COERCION_MAPPER.readValue("\" \"", new TypeReference<List<String>>() {}));
    }

    public void testCoercedBlankToListWrapper() throws Exception {
        // YES coercion + empty string input + normal CollectionDeserializer
        assertEquals(Collections.emptyList(),
                COERCION_MAPPER.readValue("\" \"", new TypeReference<List<StringWrapper>>() {}));
    }

    public void testEmptyToArray() throws Exception {
        // NO coercion + empty string input + StringCollectionDeserializer
        Assert.assertArrayEquals(new String[]{""},
                NORMAL_MAPPER.readValue("\"\"", new TypeReference<String[]>() {}));
    }

    public void testEmptyToArrayWrapper() throws Exception {
        // NO coercion + empty string input + normal CollectionDeserializer
        Assert.assertArrayEquals(new StringWrapper[]{new StringWrapper("")},
                NORMAL_MAPPER.readValue("\"\"", new TypeReference<StringWrapper[]>() {}));
    }

    public void testCoercedEmptyToArray() throws Exception {
        // YES coercion + empty string input + StringCollectionDeserializer
        Assert.assertArrayEquals(new String[0], COERCION_MAPPER.readValue("\"\"",
                new TypeReference<String[]>() {}));
    }

    public void testCoercedEmptyToArrayWrapper() throws Exception {
        // YES coercion + empty string input + normal CollectionDeserializer
        Assert.assertArrayEquals(new StringWrapper[0],
                COERCION_MAPPER.readValue("\"\"", new TypeReference<StringWrapper[]>() {}));
    }

    public void testCoercedListToArray() throws Exception {
        // YES coercion + empty LIST input + StringCollectionDeserializer
        Assert.assertArrayEquals(new String[0],
                COERCION_MAPPER.readValue("[]", new TypeReference<String[]>() {}));
    }

    public void testCoercedListToArrayWrapper() throws Exception {
        // YES coercion + empty LIST input + normal CollectionDeserializer
        Assert.assertArrayEquals(new StringWrapper[0],
                COERCION_MAPPER.readValue("[]", new TypeReference<StringWrapper[]>() {}));
    }

    public void testBlankToArray() throws Exception {
        // NO coercion + empty string input + StringCollectionDeserializer
        Assert.assertArrayEquals(new String[]{" "},
                NORMAL_MAPPER.readValue("\" \"", new TypeReference<String[]>() {}));
    }

    public void testBlankToArrayWrapper() throws Exception {
        // NO coercion + empty string input + normal CollectionDeserializer
        Assert.assertArrayEquals(new StringWrapper[]{new StringWrapper(" ")},
                NORMAL_MAPPER.readValue("\" \"", new TypeReference<StringWrapper[]>() {}));
    }

    public void testCoercedBlankToArray() throws Exception {
        // YES coercion + empty string input + StringCollectionDeserializer
        Assert.assertArrayEquals(new String[0],
                COERCION_MAPPER.readValue("\" \"", new TypeReference<String[]>() {}));
    }

    public void testCoercedBlankToArrayWrapper() throws Exception {
        // YES coercion + empty string input + normal CollectionDeserializer
        Assert.assertArrayEquals(new StringWrapper[0],
                COERCION_MAPPER.readValue("\" \"", new TypeReference<StringWrapper[]>() {}));
    }
}
