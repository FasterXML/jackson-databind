package tools.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.AnnotatedParameter;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class CreatorImplicitNameTest
    extends DatabindTestUtil
{
    // Simple introspector that gives generated "ctorN" names for constructor
    // parameters
    static class ConstructorNameAI extends JacksonAnnotationIntrospector
    {
        private static final long serialVersionUID = 1L;

        @Override
        public String findImplicitPropertyName(MapperConfig<?> config, AnnotatedMember member) {
            if (member instanceof AnnotatedParameter ap) {
                return "ctor%d".formatted(ap.getIndex());
            }
            return super.findImplicitPropertyName(config, member);
        }
    }

    @JsonPropertyOrder({ "first" ,"second", "other" })
    static class Issue792Bean
    {
        String value;

        public Issue792Bean(@JsonProperty("first") String a,
                @JsonProperty("second") String b) {
            value = a;
            // ignore second arg
        }

        public String getCtor0() { return value; }

        public int getOther() { return 3; }
    }

    static class Bean2
    {
        int x = 3;

        @JsonProperty("stuff")
        private void setValue(int i) { x = i; }

        public int getValue() { return x; }
    }

    // Bean that should only serialize 'value', but deserialize both
    static class PasswordBean
    {
        @JsonProperty(access=JsonProperty.Access.WRITE_ONLY)
        private String password;

        private int value;

        public int getValue() { return value; }
        public String getPassword() { return password; }

        public String asString() {
            return "[password='%s',value=%d]".formatted(password, value);
        }
    }

    // [databind#4545]
    static class Payload4545 {
        private final String key1;
        private final String key2;

        @JsonCreator
        public Payload4545(
                @ImplicitName("key1")
                @JsonProperty("key")
                String key1, // NOTE: the mismatch `key` / `key1` is important

                @ImplicitName("key2")
                @JsonProperty("key2")
                String key2
        ) {
            this.key1 = key1;
            this.key2 = key2;
        }

        public String getKey1() {
            return key1;
        }

        public String getKey2() {
            return key2;
        }
    }

    // [databind#4810]
    static class DataClass4810 {
        private String x;

        private DataClass4810(String x) {
            this.x = x;
        }

        @JsonProperty("bar")
        public String getFoo() {
            return x;
        }

        // NOTE: mode-less, should be properly detected as properties-based
        @JsonCreator
        public static DataClass4810 create(@ImplicitName("bar") String bar) {
            return new DataClass4810(bar);
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = sharedMapper();

    @Test
    public void testBindingOfImplicitCreatorNames() throws Exception
    {
        ObjectMapper m = jsonMapperBuilder()
                .annotationIntrospector(new ConstructorNameAI())
                .build();
        String json = m.writeValueAsString(new Issue792Bean("a", "b"));
        assertEquals(a2q("{'first':'a','other':3}"), json);
    }

    @Test
    public void testImplicitWithSetterGetter() throws Exception
    {
        String json = MAPPER.writeValueAsString(new Bean2());
        assertEquals(a2q("{'stuff':3}"), json);
    }

    @Test
    public void testWriteOnly() throws Exception
    {
        PasswordBean bean = MAPPER.readValue(a2q("{'value':7,'password':'foo'}"),
                PasswordBean.class);
        assertEquals("[password='foo',value=7]", bean.asString());
        String json = MAPPER.writeValueAsString(bean);
        assertEquals("{\"value\":7}", json);
    }

    // [databind#4545]
    @Test
    public void testCreatorWithRename4545() throws Exception
    {
        final ObjectMapper mapper4545 = jsonMapperBuilder()
                .disable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS)
                .annotationIntrospector(new ImplicitNameIntrospector())
                .build();
        String jsonPayload = a2q("{ 'key1': 'val1', 'key2': 'val2'}");

        try {
            mapper4545.readerFor(Payload4545.class)
                    .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(jsonPayload);
            fail("Should not pass");
        } catch (UnrecognizedPropertyException e) {
            verifyException(e, "Unrecognized");
            verifyException(e, "key1");
        }
    }

    // [databind#4810]
    @Test
    void testShouldSupportPropertyRenaming4810() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .annotationIntrospector(new ImplicitNameIntrospector())
                .build();

        JsonNode serializationResult = mapper.valueToTree(DataClass4810.create("42"));

        assertEquals(a2q("{'bar':'42'}"), serializationResult.toString());

        DataClass4810 deserializationResult = mapper.treeToValue(serializationResult, DataClass4810.class);

        assertEquals("42", deserializationResult.getFoo());
    }
}
