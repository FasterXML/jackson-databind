package tools.jackson.databind.deser.jdk;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import tools.jackson.core.JsonParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

class SetDuplicateElements6177Test extends DatabindTestUtil
{
    private static final DeserializationFeature FEATURE =
            DeserializationFeature.FAIL_ON_DUPLICATE_SET_ELEMENTS;

    private static final TypeReference<Set<String>> STRING_SET = new TypeReference<>() { };
    private static final TypeReference<Set<Integer>> INT_SET = new TypeReference<>() { };

    private final ObjectMapper MAPPER = newJsonMapper();
    private final ObjectReader STRICT_READER = MAPPER.reader().with(FEATURE);

    enum Color {
        @JsonAlias("rouge")
        RED
    }

    static class StringValues {
        @JsonSetter(contentNulls = Nulls.SKIP)
        public Set<String> values;

        @JsonDeserialize(contentUsing = LowerCaseDeserializer.class)
        public Set<String> normalized;
    }

    static class LowerCaseDeserializer extends StdDeserializer<String> {
        LowerCaseDeserializer() { super(String.class); }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) {
            return p.getString().toLowerCase(Locale.ROOT);
        }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    static class Item {
        public int id;
    }

    static class References {
        public Set<Item> values;
        public Item target;
    }

    @Test
    void duplicatesAllowedByDefault() {
        assertEquals(Set.of("a"), MAPPER.readValue("[\"a\",\"a\"]", STRING_SET));
        assertEquals(Set.of(1), MAPPER.readValue("[1,1]", INT_SET));
        assertEquals(EnumSet.of(Color.RED), MAPPER.readValue("[\"RED\",\"rouge\"]",
                new TypeReference<EnumSet<Color>>() { }));
    }

    @Test
    void duplicatesInListsArePreserved() {
        assertEquals(List.of("a", "a"), STRICT_READER.forType(new TypeReference<List<String>>() { })
                .readValue("[\"a\",\"a\"]"));
        assertEquals(List.of(1, 1), STRICT_READER.forType(new TypeReference<List<Integer>>() { })
                .readValue("[1,1]"));
    }

    @Test
    void duplicateAfterCoercion() {
        ObjectReader reader = STRICT_READER.forType(INT_SET);
        assertEquals(Set.of(1, 2), reader.readValue("[1,2]"));
        _assertDuplicate(reader, "[1,\"1\"]", 1);
        assertEquals(Set.of(1), reader.without(FEATURE).readValue("[1,\"1\"]"));
    }

    @Test
    void duplicateIndexWithExceptionWrappingDisabled() {
        ObjectReader reader = STRICT_READER.forType(INT_SET)
                .without(DeserializationFeature.WRAP_EXCEPTIONS);
        _assertDuplicateMessage(reader, "[1,1]", 1);
    }

    @Test
    void duplicateNullElement() {
        _assertDuplicate(STRICT_READER.forType(INT_SET), "[null,null]", 1);
    }

    @Test
    void skippedNullStillCountsTowardArrayIndex() {
        ObjectReader reader = STRICT_READER.forType(StringValues.class);
        StringValues unique = reader.readValue("{\"values\":[\"a\",null,\"b\"]}");
        assertEquals(Set.of("a", "b"), unique.values);

        MismatchedInputException e = _assertDuplicate(reader,
                "{\"values\":[\"a\",null,\"a\"]}", 2);
        assertEquals("values", e.getPath().get(0).getPropertyName());
    }

    @Test
    void duplicateAfterCustomStringDeserialization() {
        _assertDuplicate(STRICT_READER.forType(StringValues.class),
                "{\"normalized\":[\"A\",\"a\"]}", 1);
    }

    @Test
    void duplicateEnumConstantThroughAlias() {
        _assertDuplicate(STRICT_READER.forType(new TypeReference<EnumSet<Color>>() { }),
                "[\"RED\",\"rouge\"]", 1);
    }

    @Test
    void duplicateAccordingToExistingSetsComparator() {
        SortedSet<String> existing = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        existing.add("a");
        ObjectReader reader = STRICT_READER.forType(STRING_SET).withValueToUpdate(existing);
        _assertDuplicate(reader, "[\"A\"]", 0);
    }

    @Test
    void duplicateForwardReference() {
        _assertDuplicateMessage(STRICT_READER.forType(References.class),
                "{\"values\":[1,1],\"target\":{\"id\":1}}", 1);
    }

    @Test
    void duplicateBufferedBehindForwardReference() {
        _assertDuplicateMessage(STRICT_READER.forType(References.class),
                "{\"values\":[1,{\"id\":2},2],\"target\":{\"id\":1}}", 2);
    }

    private MismatchedInputException _assertDuplicate(ObjectReader reader, String json, int index) {
        MismatchedInputException e = _assertDuplicateMessage(reader, json, index);
        assertEquals(index, e.getPath().get(e.getPath().size() - 1).getIndex());
        return e;
    }

    private MismatchedInputException _assertDuplicateMessage(ObjectReader reader, String json, int index) {
        MismatchedInputException e = assertThrows(MismatchedInputException.class,
                () -> reader.readValue(json));
        verifyException(e, "Duplicate element for Set at array index " + index);
        return e;
    }
}
