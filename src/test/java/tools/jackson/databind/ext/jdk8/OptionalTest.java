package tools.jackson.databind.ext.jdk8;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.deser.std.StdScalarDeserializer;
import tools.jackson.databind.ser.std.StdScalarSerializer;

public class OptionalTest extends BaseMapTest
{
    private static final TypeReference<Optional<String>> OPTIONAL_STRING_TYPE = new TypeReference<Optional<String>>() {};
    private static final TypeReference<Optional<TestBean>> OPTIONAL_BEAN_TYPE = new TypeReference<Optional<TestBean>>() {};

    public static class TestBean
    {
        public int foo;
        public String bar;

        @JsonCreator
        public TestBean(@JsonProperty("foo") int foo, @JsonProperty("bar") String bar)
        {
            this.foo = foo;
            this.bar = bar;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj.getClass() != getClass()) {
                return false;
            }
            TestBean castObj = (TestBean) obj;
            return castObj.foo == foo && Objects.equals(castObj.bar, bar);
        }

        @Override
        public int hashCode() {
            return foo ^ bar.hashCode();
        }
    }

    static class OptionalStringBean {
        public Optional<String> value;

        public OptionalStringBean() { }
        OptionalStringBean(String str) {
            value = Optional.ofNullable(str);
        }
    }

    // [datatype-jdk8#4]
    static class Issue4Entity {
        private final Optional<String> data;

        @JsonCreator
        public Issue4Entity(@JsonProperty("data") Optional<String> data) {
            this.data = Objects.requireNonNull(data, "data");
        }

        @JsonProperty ("data")
        public Optional<String> data() {
            return data;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Issue4Entity entity = (Issue4Entity) o;
            return data.equals(entity.data);
        }
    }

    static class CaseChangingStringWrapper {
        @JsonSerialize(contentUsing=UpperCasingSerializer.class)
        @JsonDeserialize(contentUsing=LowerCasingDeserializer.class)
        public Optional<String> value;

        CaseChangingStringWrapper() { }
        public CaseChangingStringWrapper(String s) { value = Optional.ofNullable(s); }
    }

    public static class UpperCasingSerializer extends StdScalarSerializer<String>
    {
        public UpperCasingSerializer() { super(String.class); }

        @Override
        public void serialize(String value, JsonGenerator gen,
                SerializerProvider provider) {
            gen.writeString(value.toUpperCase());
        }
    }

    public static class LowerCasingDeserializer extends StdScalarDeserializer<String>
    {
        public LowerCasingDeserializer() { super(String.class); }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt)
        {
            return p.getText().toLowerCase();
        }
    }

    private ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testStringAbsent() throws Exception
    {
        assertFalse(roundtrip(Optional.empty(), OPTIONAL_STRING_TYPE).isPresent());
    }

    public void testStringPresent() throws Exception
    {
        assertEquals("test", roundtrip(Optional.of("test"), OPTIONAL_STRING_TYPE).get());
    }

    public void testBeanAbsent() throws Exception
    {
        assertFalse(roundtrip(Optional.empty(), OPTIONAL_BEAN_TYPE).isPresent());
    }

    public void testBeanPresent() throws Exception
    {
        final TestBean bean = new TestBean(Integer.MAX_VALUE, "woopwoopwoopwoopwoop");
        assertEquals(bean, roundtrip(Optional.of(bean), OPTIONAL_BEAN_TYPE).get());
    }

    public void testBeanWithCreator() throws Exception
    {
        final Issue4Entity emptyEntity = new Issue4Entity(Optional.empty());
        final String json = MAPPER.writeValueAsString(emptyEntity);

        final Issue4Entity deserialisedEntity = MAPPER.readValue(json, Issue4Entity.class);
        if (!deserialisedEntity.equals(emptyEntity)) {
            throw new IOException("Entities not equal");
        }
    }

    public void testOptionalStringInBean() throws Exception
    {
        OptionalStringBean bean = MAPPER.readValue("{\"value\":\"xyz\"}", OptionalStringBean.class);
        assertNotNull(bean.value);
        assertEquals("xyz", bean.value.get());
    }

    // To support [datatype-jdk8#8]
    public void testExcludeIfOptionalAbsent() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                .build();
        assertEquals(a2q("{'value':'foo'}"),
                mapper.writeValueAsString(new OptionalStringBean("foo")));
        // absent is not strictly null so
        assertEquals(a2q("{'value':null}"),
                mapper.writeValueAsString(new OptionalStringBean(null)));

        // however:
        mapper = jsonMapperBuilder()
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_ABSENT))
                .build();
        assertEquals(a2q("{'value':'foo'}"),
                mapper.writeValueAsString(new OptionalStringBean("foo")));
        assertEquals(a2q("{}"),
                mapper.writeValueAsString(new OptionalStringBean(null)));
    }

    public void testWithCustomDeserializer() throws Exception
    {
        CaseChangingStringWrapper w = MAPPER.readValue(a2q("{'value':'FoobaR'}"),
                CaseChangingStringWrapper.class);
        assertEquals("foobar", w.value.get());
    }

    // [modules-java8#36]
    public void testWithCustomDeserializerIfOptionalAbsent() throws Exception
    {
        // 10-Aug-2017, tatu: Actually this is not true: missing value does not trigger
        //    specific handling
        /*
        assertEquals(Optional.empty(), MAPPER.readValue("{}",
                CaseChangingStringWrapper.class).value);
                */

        assertEquals(Optional.empty(), MAPPER.readValue(a2q("{'value':null}"),
                CaseChangingStringWrapper.class).value);
    }

    public void testCustomSerializer() throws Exception
    {
        final String VALUE = "fooBAR";
        String json = MAPPER.writeValueAsString(new CaseChangingStringWrapper(VALUE));
        assertEquals(json, a2q("{'value':'FOOBAR'}"));
    }

    public void testCustomSerializerIfOptionalAbsent() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                .build();
        assertEquals(a2q("{'value':'FOO'}"),
                mapper.writeValueAsString(new CaseChangingStringWrapper("foo")));
        // absent is not strictly null so
        assertEquals(a2q("{'value':null}"),
                mapper.writeValueAsString(new CaseChangingStringWrapper(null)));

        // however:
        mapper = jsonMapperBuilder()
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_ABSENT))
                .build();
        assertEquals(a2q("{'value':'FOO'}"),
                mapper.writeValueAsString(new CaseChangingStringWrapper("foo")));
        assertEquals(a2q("{}"),
                mapper.writeValueAsString(new CaseChangingStringWrapper(null)));
    }

    // [modules-java8#33]: Verify against regression...
    public void testOtherRefSerializers() throws Exception
    {
        String json = MAPPER.writeValueAsString(new AtomicReference<String>("foo"));
        assertEquals(q("foo"), json);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private <T> Optional<T> roundtrip(Optional<T> obj, TypeReference<Optional<T>> type) throws IOException
    {
        return MAPPER.readValue(MAPPER.writeValueAsBytes(obj), type);
    }
}
