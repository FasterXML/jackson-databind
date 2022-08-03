package com.fasterxml.jackson.databind.deser.std;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class EmptyStringAsSingleValueTest {
    private static ObjectMapper normalMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        return mapper;
    }

    private static ObjectMapper coercionMapper() {
        ObjectMapper mapper = normalMapper();
        // same as XmlMapper
        mapper.coercionConfigDefaults()
                .setAcceptBlankAsEmpty(true)
                .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsEmpty);
        return mapper;
    }

    @Test
    public void emptyToList() throws JsonProcessingException {
        // NO coercion + empty string input + StringCollectionDeserializer
        Assert.assertEquals(Collections.singletonList(""), normalMapper().readValue("\"\"", new TypeReference<List<String>>() {}));
    }

    @Test
    public void emptyToListWrapper() throws JsonProcessingException {
        // NO coercion + empty string input + normal CollectionDeserializer
        Assert.assertEquals(Collections.singletonList(new StringWrapper("")), normalMapper().readValue("\"\"", new TypeReference<List<StringWrapper>>() {}));
    }

    @Test
    public void coercedEmptyToList() throws JsonProcessingException {
        // YES coercion + empty string input + StringCollectionDeserializer
        Assert.assertEquals(Collections.emptyList(), coercionMapper().readValue("\"\"", new TypeReference<List<String>>() {}));
    }

    @Test
    public void coercedEmptyToListWrapper() throws JsonProcessingException {
        // YES coercion + empty string input + normal CollectionDeserializer
        Assert.assertEquals(Collections.emptyList(), coercionMapper().readValue("\"\"", new TypeReference<List<StringWrapper>>() {}));
    }

    @Test
    public void coercedListToList() throws JsonProcessingException {
        // YES coercion + empty LIST input + StringCollectionDeserializer
        Assert.assertEquals(Collections.emptyList(), coercionMapper().readValue("[]", new TypeReference<List<String>>() {}));
    }

    @Test
    public void coercedListToListWrapper() throws JsonProcessingException {
        // YES coercion + empty LIST input + normal CollectionDeserializer
        Assert.assertEquals(Collections.emptyList(), coercionMapper().readValue("[]", new TypeReference<List<StringWrapper>>() {}));
    }

    @Test
    public void blankToList() throws JsonProcessingException {
        // NO coercion + empty string input + StringCollectionDeserializer
        Assert.assertEquals(Collections.singletonList(" "), normalMapper().readValue("\" \"", new TypeReference<List<String>>() {}));
    }

    @Test
    public void blankToListWrapper() throws JsonProcessingException {
        // NO coercion + empty string input + normal CollectionDeserializer
        Assert.assertEquals(Collections.singletonList(new StringWrapper(" ")), normalMapper().readValue("\" \"", new TypeReference<List<StringWrapper>>() {}));
    }

    @Test
    public void coercedBlankToList() throws JsonProcessingException {
        // YES coercion + empty string input + StringCollectionDeserializer
        Assert.assertEquals(Collections.emptyList(), coercionMapper().readValue("\" \"", new TypeReference<List<String>>() {}));
    }

    @Test
    public void coercedBlankToListWrapper() throws JsonProcessingException {
        // YES coercion + empty string input + normal CollectionDeserializer
        Assert.assertEquals(Collections.emptyList(), coercionMapper().readValue("\" \"", new TypeReference<List<StringWrapper>>() {}));
    }

    private static final class StringWrapper {
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
}
