package tools.jackson.databind.ser.jdk;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.json.JsonWriteFeature;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializerProvider;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.StdScalarSerializer;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for verifying serialization of {@link java.util.concurrent.atomic.AtomicReference}
 * and other atomic types, via various settings.
 */
public class AtomicTypeSerializationTest
    extends DatabindTestUtil
{
    static class UpperCasingSerializer extends StdScalarSerializer<String>
    {
        public UpperCasingSerializer() { super(String.class); }

        @Override
        public void serialize(String value, JsonGenerator gen,
                SerializerProvider provider) {
            gen.writeString(value.toUpperCase());
        }
    }

    static class UCStringWrapper {
        @JsonSerialize(contentUsing=UpperCasingSerializer.class)
        public AtomicReference<String> value;

        public UCStringWrapper(String s) { value = new AtomicReference<String>(s); }
    }

    // [datatypes-java8#17]
    @JsonPropertyOrder({ "date1", "date2", "date" })
    static class ContextualOptionals
    {
        public AtomicReference<Date> date;

        @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy+MM+dd")
        public AtomicReference<Date> date1;

        @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy*MM*dd")
        public AtomicReference<Date> date2;
    }

    // [databind#1673]
    static class ContainerA {
        public AtomicReference<Strategy> strategy =
                new AtomicReference<>((Strategy) new Foo(42));
    }

    static class ContainerB {
        public AtomicReference<List<Strategy>> strategy;
        {
            List<Strategy> list = new ArrayList<>();
            list.add(new Foo(42));
            strategy = new AtomicReference<>(list);
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes({ @JsonSubTypes.Type(name = "Foo", value = Foo.class) })
    interface Strategy { }

    static class Foo implements Strategy {
        public int foo;

        @JsonCreator
        Foo(@JsonProperty("foo") int foo) {
            this.foo = foo;
        }
    }

    // [databind#2565]: problems with JsonUnwrapped, non-unwrappable type
    static class MyBean2565 {
        @JsonUnwrapped
        public AtomicReference<String> maybeText = new AtomicReference<>("value");
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testAtomicBoolean() throws Exception
    {
        assertEquals("true", MAPPER.writeValueAsString(new AtomicBoolean(true)));
        assertEquals("false", MAPPER.writeValueAsString(new AtomicBoolean(false)));
    }

    @Test
    public void testAtomicInteger() throws Exception
    {
        assertEquals("1", MAPPER.writeValueAsString(new AtomicInteger(1)));
        assertEquals("-9", MAPPER.writeValueAsString(new AtomicInteger(-9)));
    }

    @Test
    public void testAtomicLong() throws Exception
    {
        assertEquals("0", MAPPER.writeValueAsString(new AtomicLong(0)));
    }

    @Test
    public void testAtomicReference() throws Exception
    {
        String[] strs = new String[] { "abc" };
        assertEquals("[\"abc\"]", MAPPER.writeValueAsString(new AtomicReference<String[]>(strs)));
    }

    @Test
    public void testCustomSerializer() throws Exception
    {
        final String VALUE = "fooBAR";
        String json = MAPPER.writeValueAsString(new UCStringWrapper(VALUE));
        assertEquals(json, a2q("{'value':'FOOBAR'}"));
    }

    @Test
    public void testContextualAtomicReference() throws Exception
    {
        SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));

        ObjectMapper mapper = jsonMapperBuilder()
                .disable(JsonWriteFeature.ESCAPE_FORWARD_SLASHES)
                .defaultDateFormat(df)
                .build();

        ContextualOptionals input = new ContextualOptionals();
        input.date = new AtomicReference<>(new Date(0L));
        input.date1 = new AtomicReference<>(new Date(0L));
        input.date2 = new AtomicReference<>(new Date(0L));
        final String json = mapper.writeValueAsString(input);
        assertEquals(a2q(
                "{'date1':'1970+01+01','date2':'1970*01*01','date':'1970/01/01'}"),
                json);
    }

    // [databind#1673]
    @Test
    public void testPolymorphicReferenceSimple() throws Exception
    {
        final String EXPECTED = "{\"type\":\"Foo\",\"foo\":42}";
        String json = MAPPER.writeValueAsString(new ContainerA());
        assertEquals("{\"strategy\":" + EXPECTED + "}", json);
    }

    // [databind#1673]
    @Test
    public void testPolymorphicReferenceListOf() throws Exception
    {
        final String EXPECTED = "{\"type\":\"Foo\",\"foo\":42}";
        // Reproduction of issue seen with scala.Option and java8 Optional types:
        // https://github.com/FasterXML/jackson-module-scala/issues/346#issuecomment-336483326
        String json = MAPPER.writeValueAsString(new ContainerB());
        assertEquals("{\"strategy\":[" + EXPECTED + "]}", json);
    }

    // [databind#2565]: problems with JsonUnwrapped, non-unwrappable type
    @Test
    public void testWithUnwrappableUnwrapped() throws Exception
    {
        assertEquals(a2q("{'maybeText':'value'}"),
                MAPPER.writeValueAsString(new MyBean2565()));
    }
}
