package tools.jackson.databind.struct;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;
import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#5911]: `@JsonAlias` on property inside a `@JsonUnwrapped` type
public class UnwrappedWithAlias5911Test extends DatabindTestUtil {
    record AorC(@JsonAlias("a") String c) {}
    record OuterRecord(@JsonUnwrapped AorC aOrC, String b) {}

    static class Inner {
        @JsonAlias({"a", "aa"})
        public String c;
    }
    static class Outer {
        @JsonUnwrapped
        public Inner inner;
        public String b;
    }

    private final JsonMapper MAPPER = JsonMapper.builder().build();

    @Test
    void testRecordAlias() throws Exception {
        OuterRecord b = MAPPER.readValue(
            "{ \"a\": \"Hello\", \"b\": \"World!\" }", OuterRecord.class);
        assertEquals("Hello", b.aOrC.c);
        assertEquals("World!", b.b);
    }

    @Test
    void testPojoAlias() throws Exception {
        Outer o = MAPPER.readValue(
            "{ \"a\": \"Hello\", \"b\": \"World!\" }", Outer.class);
        assertEquals("Hello", o.inner.c);
        assertEquals("World!", o.b);
    }

    @Test
    void testPojoSecondAlias() throws Exception {
        Outer o = MAPPER.readValue(
            "{ \"aa\": \"Hi\", \"b\": \"there\" }", Outer.class);
        assertEquals("Hi", o.inner.c);
        assertEquals("there", o.b);
    }

    static class CreatorInner {
        final String c;
        @JsonCreator
        CreatorInner(@JsonProperty("c") @JsonAlias("a") String c) {
            this.c = c;
        }
    }
    static class CreatorOuter {
        @JsonUnwrapped
        public CreatorInner inner;
        public String b;
    }

    @Test
    void testCreatorPojoAlias() throws Exception {
        CreatorOuter o = MAPPER.readValue(
            "{ \"a\": \"Hello\", \"b\": \"World!\" }", CreatorOuter.class);
        assertEquals("Hello", o.inner.c);
        assertEquals("World!", o.b);
    }

    @Test
    void testPojoPrimaryName() throws Exception {
        Outer o = MAPPER.readValue(
            "{ \"c\": \"direct\", \"b\": \"x\" }", Outer.class);
        assertEquals("direct", o.inner.c);
        assertEquals("x", o.b);
    }
}
