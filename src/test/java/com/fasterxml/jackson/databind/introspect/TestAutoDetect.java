package com.fasterxml.jackson.databind.introspect;

import java.util.Objects;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

public class TestAutoDetect
    extends BaseMapTest
{
    static class PrivateBean {
        String a;

        private PrivateBean() { }

        private PrivateBean(String a) { this.a = a; }
    }

    // test for [databind#1347], config overrides for visibility
    @JsonPropertyOrder(alphabetic=true)
    static class Feature1347SerBean {
        public int field = 2;

        public int getValue() { return 3; }
    }

    // let's promote use of fields; but not block setters yet
    @JsonAutoDetect(fieldVisibility=Visibility.NON_PRIVATE)
    static class Feature1347DeserBean {
        int value;

        public void setValue(int x) {
            throw new IllegalArgumentException("Should NOT get called");
        }
    }

    // [databind#1947]
    static class Entity1947 {
        public int shouldBeDetected;
        public String shouldNotBeDetected;

        @JsonProperty
        public int getShouldBeDetected() {
            return shouldBeDetected;
        }

        public void setShouldBeDetected(int shouldBeDetected) {
            this.shouldBeDetected = shouldBeDetected;
        }

        public String getShouldNotBeDetected() {
            return shouldNotBeDetected;
        }

        public void setShouldNotBeDetected(String shouldNotBeDetected) {
            this.shouldNotBeDetected = shouldNotBeDetected;
        }
    }

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
    /********************************************************
    /* Unit tests
    /********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testPrivateCtor() throws Exception
    {
        // first, default settings, with which construction works ok
        ObjectMapper m = new ObjectMapper();
        PrivateBean bean = m.readValue("\"abc\"", PrivateBean.class);
        assertEquals("abc", bean.a);

        // then by increasing visibility requirement:
        m = new ObjectMapper();
        VisibilityChecker<?> vc = m.getVisibilityChecker();
        vc = vc.withCreatorVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY);
        m.setVisibility(vc);
        try {
            m.readValue("\"abc\"", PrivateBean.class);
            fail("Expected exception for missing constructor");
        } catch (MismatchedInputException e) {
            verifyException(e, "no String-argument constructor/factory");
        }
    }

    // [databind#1347]
    public void testVisibilityConfigOverridesForSer() throws Exception
    {
        // first, by default, both field/method should be visible
        final Feature1347SerBean input = new Feature1347SerBean();
        assertEquals(a2q("{'field':2,'value':3}"),
                MAPPER.writeValueAsString(input));

        ObjectMapper mapper = new ObjectMapper();
        mapper.configOverride(Feature1347SerBean.class)
            .setVisibility(JsonAutoDetect.Value.construct(PropertyAccessor.GETTER,
                            Visibility.NONE));
        assertEquals(a2q("{'field':2}"),
                mapper.writeValueAsString(input));
    }

    // [databind#1347]
    public void testVisibilityConfigOverridesForDeser() throws Exception
    {
        final String JSON = a2q("{'value':3}");

        // by default, should throw exception
        try {
            /*Feature1347DeserBean bean =*/
            MAPPER.readValue(JSON, Feature1347DeserBean.class);
            fail("Should not pass");
        } catch (DatabindException e) { // should probably be something more specific but...
            verifyException(e, "Should NOT get called");
        }

        // but when instructed to ignore setter, should work
        ObjectMapper mapper = new ObjectMapper();
        mapper.configOverride(Feature1347DeserBean.class)
            .setVisibility(JsonAutoDetect.Value.construct(PropertyAccessor.SETTER,
                        Visibility.NONE));
        Feature1347DeserBean result = mapper.readValue(JSON, Feature1347DeserBean.class);
        assertEquals(3, result.value);
    }

    // [databind#1947]
    public void testDisablingAll() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .disable(MapperFeature.AUTO_DETECT_SETTERS)
                .disable(MapperFeature.AUTO_DETECT_FIELDS)
                .disable(MapperFeature.AUTO_DETECT_GETTERS)
                .disable(MapperFeature.AUTO_DETECT_CREATORS)
                .disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
                .build();
        String json = mapper.writeValueAsString(new Entity1947());
        JsonNode n = mapper.readTree(json);
        assertEquals(1, n.size());
        assertTrue(n.has("shouldBeDetected"));
        assertFalse(n.has("shouldNotBeDetected"));
    }

    // [databind#2789]
    public void testAnnotatedFieldIssue2789() throws Exception {
        final String json = MAPPER.writeValueAsString(new DataClassA());
        final DataParent2789 copy = MAPPER.readValue(json, DataParent2789.class);
        assertEquals(DataType2789.CLASS_A, copy.getType());
    }
}
