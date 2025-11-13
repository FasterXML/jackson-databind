package tools.jackson.databind.deser.creators;

import java.util.Objects;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.AnnotatedMethod;
import tools.jackson.databind.introspect.AnnotatedParameter;
import tools.jackson.databind.introspect.AnnotatedWithParams;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DelegatingCreatorImplicitNamesTest
    extends DatabindTestUtil
{
    // [databind#1001]
    static class D
    {
        private String raw1 = "";
        private String raw2 = "";

        private D(String raw1, String raw2) {
            this.raw1 = raw1;
            this.raw2 = raw2;
        }

        // not needed strictly speaking, but added for good measure
        @JsonCreator
        public static D make(String value) {
            String[] split = value.split(":");
            return new D(split[0], split[1]);
        }

        @JsonValue
        public String getMyValue() {
            return raw1 + ":" + raw2;
        }

        @Override
        public String toString() {
            return getMyValue();
        }

        @Override
        public boolean equals(Object o) {
            D other = (D) o;
            return other.raw1.equals(raw1)
                    && other.raw2.equals(raw2);
        }
    }

    // To test equivalent of parameter-names, let's use this one
    protected static class CreatorNameIntrospector1001 extends JacksonAnnotationIntrospector
    {
        private static final long serialVersionUID = 1L;

        @Override
        public String findImplicitPropertyName(MapperConfig<?> config, AnnotatedMember member) {
            if (member instanceof AnnotatedParameter ap) {
                AnnotatedWithParams owner = ap.getOwner();
                if (owner instanceof AnnotatedMethod) {
                    if (ap.getIndex() == 0) {
                        return "value";
                    }
                }
            }
            return super.findImplicitPropertyName(config, member);
        }
    }

    static class Data2543 {

        final String part1;
        final String part2;

        // this creator is considered a source of settable bean properties,
        // used during deserialization
        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public Data2543(@JsonProperty("part1") String part1,
                    @JsonProperty("part2") String part2) {
            this.part1 = part1;
            this.part2 = part2;
        }

        // no properties should be collected from this creator,
        // even though it has an argument with an implicit name
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static Data2543 fromFullData(String fullData) {
            String[] parts = fullData.split("\\s+", 2);
            return new Data2543(parts[0], parts[1]);
        }
    }

    static class DelegatingCreatorNamedArgumentIntrospector2543
            extends JacksonAnnotationIntrospector
    {
        private static final long serialVersionUID = 1L;

        @Override
        public String findImplicitPropertyName(MapperConfig<?> config, AnnotatedMember member) {
            if (member instanceof AnnotatedParameter ap) {
                AnnotatedWithParams owner = ap.getOwner();
                if (owner instanceof AnnotatedMethod method) {
                    if (Objects.requireNonNull(method.getAnnotation(JsonCreator.class)).mode() == JsonCreator.Mode.DELEGATING)
                        return "fullData";
                }
            }
            return super.findImplicitPropertyName(config, member);
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    private final ObjectMapper MAPPER_2543 = JsonMapper.builder()
            .annotationIntrospector(new DelegatingCreatorNamedArgumentIntrospector2543())
            .build();

    // [databind#1001]

    @Test
    public void testWithoutNamedParameters1001() throws Exception
    {
        D d = D.make("abc:def");

        String actualJson = MAPPER.writeValueAsString(d);
        D actualD = MAPPER.readValue(actualJson, D.class);

        assertEquals("\"abc:def\"", actualJson);
        assertEquals(d, actualD);
    }

    @Test
    public void testWithNamedParameters1001() throws Exception
    {
        ObjectMapper sut = jsonMapperBuilder()
            .annotationIntrospector(new CreatorNameIntrospector1001())
            .build();

        D d = D.make("abc:def");

        String actualJson = sut.writeValueAsString(d);
        D actualD = sut.readValue(actualJson, D.class);

        assertEquals("\"abc:def\"", actualJson);
        assertEquals(d, actualD);
    }

    // [databind#2543]
    @Test
    public void testDeserialization2543() throws Exception {
        Data2543 data = MAPPER_2543.readValue(a2q("{'part1':'a','part2':'b'}"), Data2543.class);

        assertThat(data.part1).isEqualTo("a");
        assertThat(data.part2).isEqualTo("b");
    }

    @Test
    public void testDelegatingDeserialization2543() throws Exception {
        Data2543 data = MAPPER_2543.readValue(a2q("'a b'"), Data2543.class);

        assertThat(data.part1).isEqualTo("a");
        assertThat(data.part2).isEqualTo("b");
    }

}
