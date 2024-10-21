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
    void emptyToList() throws Exception {
        // NO coercion + empty string input + StringCollectionDeserializer
        assertEquals(Collections.singletonList(""),
                NORMAL_MAPPER.readValue("\"\"", new TypeReference<List<String>>() {}));
    }

    @Test
    void emptyToListWrapper() throws Exception {
        // NO coercion + empty string input + normal CollectionDeserializer
        assertEquals(Collections.singletonList(new StringWrapper("")),
                NORMAL_MAPPER.readValue("\"\"", new TypeReference<List<StringWrapper>>() {}));
    }

    @Test
    void coercedEmptyToList() throws Exception {
        // YES coercion + empty string input + StringCollectionDeserializer
        assertEquals(Collections.emptyList(), COERCION_MAPPER.readValue("\"\"",
                new TypeReference<List<String>>() {}));
    }

    @Test
    void coercedEmptyToListWrapper() throws Exception {
        // YES coercion + empty string input + normal CollectionDeserializer
        assertEquals(Collections.emptyList(),
                COERCION_MAPPER.readValue("\"\"", new TypeReference<List<StringWrapper>>() {}));
    }

    @Test
    void coercedListToList() throws Exception {
        // YES coercion + empty LIST input + StringCollectionDeserializer
        assertEquals(Collections.emptyList(),
                COERCION_MAPPER.readValue("[]", new TypeReference<List<String>>() {}));
    }

    @Test
    void coercedListToListWrapper() throws Exception {
        // YES coercion + empty LIST input + normal CollectionDeserializer
        assertEquals(Collections.emptyList(),
                COERCION_MAPPER.readValue("[]", new TypeReference<List<StringWrapper>>() {}));
    }

    @Test
    void blankToList() throws Exception {
        // NO coercion + empty string input + StringCollectionDeserializer
        assertEquals(Collections.singletonList(" "),
                NORMAL_MAPPER.readValue("\" \"", new TypeReference<List<String>>() {}));
    }

    @Test
    void blankToListWrapper() throws Exception {
        // NO coercion + empty string input + normal CollectionDeserializer
        assertEquals(Collections.singletonList(new StringWrapper(" ")),
                NORMAL_MAPPER.readValue("\" \"", new TypeReference<List<StringWrapper>>() {}));
    }

    @Test
    void coercedBlankToList() throws Exception {
        // YES coercion + empty string input + StringCollectionDeserializer
        assertEquals(Collections.emptyList(),
                COERCION_MAPPER.readValue("\" \"", new TypeReference<List<String>>() {}));
    }

    @Test
    void coercedBlankToListWrapper() throws Exception {
        // YES coercion + empty string input + normal CollectionDeserializer
        assertEquals(Collections.emptyList(),
                COERCION_MAPPER.readValue("\" \"", new TypeReference<List<StringWrapper>>() {}));
    }

    @Test
    void emptyToArray() throws Exception {
        // NO coercion + empty string input + StringCollectionDeserializer
        assertArrayEquals(new String[]{""},
                NORMAL_MAPPER.readValue("\"\"", new TypeReference<String[]>() {}));
    }

    @Test
    void emptyToArrayWrapper() throws Exception {
        // NO coercion + empty string input + normal CollectionDeserializer
        assertArrayEquals(new StringWrapper[]{new StringWrapper("")},
                NORMAL_MAPPER.readValue("\"\"", new TypeReference<StringWrapper[]>() {}));
    }

    @Test
    void coercedEmptyToArray() throws Exception {
        // YES coercion + empty string input + StringCollectionDeserializer
        assertArrayEquals(new String[0], COERCION_MAPPER.readValue("\"\"",
                new TypeReference<String[]>() {}));
    }

    @Test
    void coercedEmptyToArrayWrapper() throws Exception {
        // YES coercion + empty string input + normal CollectionDeserializer
        assertArrayEquals(new StringWrapper[0],
                COERCION_MAPPER.readValue("\"\"", new TypeReference<StringWrapper[]>() {}));
    }

    @Test
    void coercedListToArray() throws Exception {
        // YES coercion + empty LIST input + StringCollectionDeserializer
        assertArrayEquals(new String[0],
                COERCION_MAPPER.readValue("[]", new TypeReference<String[]>() {}));
    }

    @Test
    void coercedListToArrayWrapper() throws Exception {
        // YES coercion + empty LIST input + normal CollectionDeserializer
        assertArrayEquals(new StringWrapper[0],
                COERCION_MAPPER.readValue("[]", new TypeReference<StringWrapper[]>() {}));
    }

    @Test
    void blankToArray() throws Exception {
        // NO coercion + empty string input + StringCollectionDeserializer
        assertArrayEquals(new String[]{" "},
                NORMAL_MAPPER.readValue("\" \"", new TypeReference<String[]>() {}));
    }

    @Test
    void blankToArrayWrapper() throws Exception {
        // NO coercion + empty string input + normal CollectionDeserializer
        assertArrayEquals(new StringWrapper[]{new StringWrapper(" ")},
                NORMAL_MAPPER.readValue("\" \"", new TypeReference<StringWrapper[]>() {}));
    }

    @Test
    void coercedBlankToArray() throws Exception {
        // YES coercion + empty string input + StringCollectionDeserializer
        assertArrayEquals(new String[0],
                COERCION_MAPPER.readValue("\" \"", new TypeReference<String[]>() {}));
    }

    @Test
    void coercedBlankToArrayWrapper() throws Exception {
        // YES coercion + empty string input + normal CollectionDeserializer
        assertArrayEquals(new StringWrapper[0],
                COERCION_MAPPER.readValue("\" \"", new TypeReference<StringWrapper[]>() {}));
    }
}
