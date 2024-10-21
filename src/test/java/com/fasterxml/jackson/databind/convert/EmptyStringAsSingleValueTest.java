package com.fasterxml.jackson.databind.convert;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;

import static org.junit.jupiter.api.Assertions.*;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.*;

// [databind#3418]: Coercion from empty String to Collection<String>, with
// `DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY`
class EmptyStringAsSingleValueTest
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

    @Test
    void testEmptyToList() throws Exception {
        // NO coercion + empty string input + StringCollectionDeserializer
        assertEquals(Collections.singletonList(""),
                NORMAL_MAPPER.readValue("\"\"", new TypeReference<List<String>>() {}));
    }

    @Test
    void testEmptyToListWrapper() throws Exception {
        // NO coercion + empty string input + normal CollectionDeserializer
        assertEquals(Collections.singletonList(new StringWrapper("")),
                NORMAL_MAPPER.readValue("\"\"", new TypeReference<List<StringWrapper>>() {}));
    }

    @Test
    void testCoercedEmptyToList() throws Exception {
        // YES coercion + empty string input + StringCollectionDeserializer
        assertEquals(Collections.emptyList(), COERCION_MAPPER.readValue("\"\"",
                new TypeReference<List<String>>() {}));
    }

    @Test
    void testCoercedEmptyToListWrapper() throws Exception {
        // YES coercion + empty string input + normal CollectionDeserializer
        assertEquals(Collections.emptyList(),
                COERCION_MAPPER.readValue("\"\"", new TypeReference<List<StringWrapper>>() {}));
    }

    @Test
    void testCoercedListToList() throws Exception {
        // YES coercion + empty LIST input + StringCollectionDeserializer
        assertEquals(Collections.emptyList(),
                COERCION_MAPPER.readValue("[]", new TypeReference<List<String>>() {}));
    }

    @Test
    void testCoercedListToListWrapper() throws Exception {
        // YES coercion + empty LIST input + normal CollectionDeserializer
        assertEquals(Collections.emptyList(),
                COERCION_MAPPER.readValue("[]", new TypeReference<List<StringWrapper>>() {}));
    }

    @Test
    void testBlankToList() throws Exception {
        // NO coercion + empty string input + StringCollectionDeserializer
        assertEquals(Collections.singletonList(" "),
                NORMAL_MAPPER.readValue("\" \"", new TypeReference<List<String>>() {}));
    }

    @Test
    void testBlankToListWrapper() throws Exception {
        // NO coercion + empty string input + normal CollectionDeserializer
        assertEquals(Collections.singletonList(new StringWrapper(" ")),
                NORMAL_MAPPER.readValue("\" \"", new TypeReference<List<StringWrapper>>() {}));
    }

    @Test
    void testCoercedBlankToList() throws Exception {
        // YES coercion + empty string input + StringCollectionDeserializer
        assertEquals(Collections.emptyList(),
                COERCION_MAPPER.readValue("\" \"", new TypeReference<List<String>>() {}));
    }

    @Test
    void testCoercedBlankToListWrapper() throws Exception {
        // YES coercion + empty string input + normal CollectionDeserializer
        assertEquals(Collections.emptyList(),
                COERCION_MAPPER.readValue("\" \"", new TypeReference<List<StringWrapper>>() {}));
    }

    @Test
    void testEmptyToArray() throws Exception {
        // NO coercion + empty string input + StringCollectionDeserializer
        assertArrayEquals(new String[]{""},
                NORMAL_MAPPER.readValue("\"\"", new TypeReference<String[]>() {}));
    }

    @Test
    void testEmptyToArrayWrapper() throws Exception {
        // NO coercion + empty string input + normal CollectionDeserializer
        assertArrayEquals(new StringWrapper[]{new StringWrapper("")},
                NORMAL_MAPPER.readValue("\"\"", new TypeReference<StringWrapper[]>() {}));
    }

    @Test
    void testCoercedEmptyToArray() throws Exception {
        // YES coercion + empty string input + StringCollectionDeserializer
        assertArrayEquals(new String[0], COERCION_MAPPER.readValue("\"\"",
                new TypeReference<String[]>() {}));
    }

    @Test
    void testCoercedEmptyToArrayWrapper() throws Exception {
        // YES coercion + empty string input + normal CollectionDeserializer
        assertArrayEquals(new StringWrapper[0],
                COERCION_MAPPER.readValue("\"\"", new TypeReference<StringWrapper[]>() {}));
    }

    @Test
    void testCoercedListToArray() throws Exception {
        // YES coercion + empty LIST input + StringCollectionDeserializer
        assertArrayEquals(new String[0],
                COERCION_MAPPER.readValue("[]", new TypeReference<String[]>() {}));
    }

    @Test
    void testCoercedListToArrayWrapper() throws Exception {
        // YES coercion + empty LIST input + normal CollectionDeserializer
        assertArrayEquals(new StringWrapper[0],
                COERCION_MAPPER.readValue("[]", new TypeReference<StringWrapper[]>() {}));
    }

    @Test
    void testBlankToArray() throws Exception {
        // NO coercion + empty string input + StringCollectionDeserializer
        assertArrayEquals(new String[]{" "},
                NORMAL_MAPPER.readValue("\" \"", new TypeReference<String[]>() {}));
    }

    @Test
    void testBlankToArrayWrapper() throws Exception {
        // NO coercion + empty string input + normal CollectionDeserializer
        assertArrayEquals(new StringWrapper[]{new StringWrapper(" ")},
                NORMAL_MAPPER.readValue("\" \"", new TypeReference<StringWrapper[]>() {}));
    }

    @Test
    void testCoercedBlankToArray() throws Exception {
        // YES coercion + empty string input + StringCollectionDeserializer
        assertArrayEquals(new String[0],
                COERCION_MAPPER.readValue("\" \"", new TypeReference<String[]>() {}));
    }

    @Test
    void testCoercedBlankToArrayWrapper() throws Exception {
        // YES coercion + empty string input + normal CollectionDeserializer
        assertArrayEquals(new StringWrapper[0],
                COERCION_MAPPER.readValue("\" \"", new TypeReference<StringWrapper[]>() {}));
    }
}
