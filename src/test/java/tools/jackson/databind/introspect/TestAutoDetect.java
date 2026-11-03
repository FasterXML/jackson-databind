package tools.jackson.databind.introspect;

import java.util.Objects;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class TestAutoDetect extends DatabindTestUtil
{
    // 21-Sep-2017, tatu: With 2.x, private delegating ctor was acceptable; with 3.x
    //    must be non-private OR annotated
    static class ProtectedBean {
        String _a;

        protected ProtectedBean(String a) { this._a = a; }
    }

    // Private scalar constructor ok, but only if annotated (or level changed)
    static class PrivateBeanAnnotated {
        String a;

        @JsonCreator
        private PrivateBeanAnnotated(String a) { this.a = a; }
    }

    static class PrivateBeanNonAnnotated {
        String a;
        private PrivateBeanNonAnnotated(String a) { this.a = a; }
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

    @Test
    public void testProtectedDelegatingCtor() throws Exception
    {
        // first, default settings, with which construction works ok
        ProtectedBean bean = MAPPER.readValue(q("abc"), ProtectedBean.class);
        assertEquals("abc", bean._a);

        // then by increasing visibility requirement:
        ObjectMapper m = jsonMapperBuilder()
                .changeDefaultVisibility(vc -> vc.withScalarConstructorVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY))
                .build();
        JacksonException e = assertThrows(JacksonException.class,
                () -> m.readValue("\"abc\"", ProtectedBean.class));
        verifyException(e, InvalidDefinitionException.class, "no String-argument constructor/factory");
    }

    @Test
    public void testPrivateDelegatingCtor() throws Exception
    {
        // first, default settings, with which construction works ok
        PrivateBeanAnnotated bean = MAPPER.readValue(q("abc"), PrivateBeanAnnotated.class);
        assertEquals("abc", bean.a);

        // but not so much without
        JacksonException e = assertThrows(JacksonException.class,
                () -> MAPPER.readValue("\"abc\"", PrivateBeanNonAnnotated.class));
        verifyException(e, InvalidDefinitionException.class, "no String-argument constructor/factory");

        // except if we lower requirement
        ObjectMapper m = jsonMapperBuilder()
                .changeDefaultVisibility(vc -> vc.withScalarConstructorVisibility(JsonAutoDetect.Visibility.ANY))
                .build();
        bean = m.readValue(q("xyz"), PrivateBeanAnnotated.class);
        assertEquals("xyz", bean.a);
    }

    // [databind#1347]
    @Test
    public void testVisibilityConfigOverridesForSer() throws Exception
    {
        // first, by default, both field/method should be visible
        final Feature1347SerBean input = new Feature1347SerBean();
        assertEquals(a2q("{'field':2,'value':3}"),
                MAPPER.writeValueAsString(input));

        ObjectMapper mapper = jsonMapperBuilder()
                .withConfigOverride(Feature1347SerBean.class,
                        o -> o.setVisibility(JsonAutoDetect.Value.construct(PropertyAccessor.GETTER,
                            Visibility.NONE)))
                .build();
        assertEquals(a2q("{'field':2}"),
                mapper.writeValueAsString(input));
    }

    // [databind#1347]
    @Test
    public void testVisibilityConfigOverridesForDeser() throws Exception
    {
        final String JSON = a2q("{'value':3}");

        // by default, should throw exception
        DatabindException e = assertThrows(DatabindException.class,
                () -> MAPPER.readValue(JSON, Feature1347DeserBean.class));
        verifyException(e, "Should NOT get called");

        // but when instructed to ignore setter, should work
        // [databind#1947]
        ObjectMapper mapper = jsonMapperBuilder()
                .withConfigOverride(Feature1347DeserBean.class,
                        o -> o.setVisibility(JsonAutoDetect.Value.construct(PropertyAccessor.SETTER,
                                Visibility.NONE)))
                .build();
        Feature1347DeserBean result = mapper.readValue(JSON, Feature1347DeserBean.class);
        assertEquals(3, result.value);
    }

    // [databind#2789]
    @Test
    public void testAnnotatedFieldIssue2789() throws Exception {
        final String json = MAPPER.writeValueAsString(new DataClassA());
        final DataParent2789 copy = MAPPER.readValue(json, DataParent2789.class);
        assertEquals(DataType2789.CLASS_A, copy.getType());
    }
}
