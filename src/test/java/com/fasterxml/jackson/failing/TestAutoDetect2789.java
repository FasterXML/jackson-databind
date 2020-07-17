package com.fasterxml.jackson.failing;

import java.util.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

/**
 * Unit tests for checking extended auto-detect configuration,
 * in context of serialization
 */
public class TestAutoDetect2789
    extends BaseMapTest
{
    // For [databind#2789]

    @SuppressWarnings("unused")
    @JsonAutoDetect(
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type",
        visible = true)
    @JsonSubTypes({
        @JsonSubTypes.Type(name = "CLASS_A", value = DataClassA.class)
    })
    private static abstract class DataParent2789 {

        @JsonProperty("type")
        @JsonTypeId
        private final DataType2789 type;

        DataParent2789() {
            super();
            this.type = null;
        }

        DataParent2789(final DataType2789 type) {
            super();
            this.type = Objects.requireNonNull(type);
        }

        public DataType2789 getType() {
            return this.type;
        }
    }

    private static final class DataClassA extends DataParent2789 {
        DataClassA() {
            super(DataType2789.CLASS_A);
        }
    }

    private enum DataType2789 {
        CLASS_A;
    }

    /*
    /*********************************************************
    /* Test methods
    /*********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#2789]

    public void testAnnotatedFieldIssue2789() throws Exception {
        final DataParent2789 test = new DataClassA();

        final String json = MAPPER.writeValueAsString(test);

        final DataParent2789 copy = MAPPER.readValue(json, DataParent2789.class);
        assertEquals(DataType2789.CLASS_A, copy.getType());
    }
}
