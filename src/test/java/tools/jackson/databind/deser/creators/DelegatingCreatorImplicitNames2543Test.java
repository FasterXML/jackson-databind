package tools.jackson.databind.deser.creators;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.DELEGATING;
import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;

public class DelegatingCreatorImplicitNames2543Test
    extends DatabindTestUtil
{
    static class Data {

        final String part1;
        final String part2;

        // this creator is considered a source of settable bean properties,
        // used during deserialization
        @JsonCreator(mode = PROPERTIES)
        public Data(@JsonProperty("part1") String part1,
                    @JsonProperty("part2") String part2) {
            this.part1 = part1;
            this.part2 = part2;
        }

        // no properties should be collected from this creator,
        // even though it has an argument with an implicit name
        @JsonCreator(mode = DELEGATING)
        public static Data fromFullData(String fullData) {
            String[] parts = fullData.split("\\s+", 2);
            return new Data(parts[0], parts[1]);
        }
    }

    static class DelegatingCreatorNamedArgumentIntrospector
            extends JacksonAnnotationIntrospector {
        private static final long serialVersionUID = 1L;

        @Override
        public String findImplicitPropertyName(MapperConfig<?> config, AnnotatedMember member) {
            if (member instanceof AnnotatedParameter) {
                AnnotatedWithParams owner = ((AnnotatedParameter) member).getOwner();
                if (owner instanceof AnnotatedMethod) {
                    AnnotatedMethod method = (AnnotatedMethod) owner;
                    if (Objects.requireNonNull(method.getAnnotation(JsonCreator.class)).mode() == DELEGATING)
                        return "fullData";
                }
            }
            return super.findImplicitPropertyName(config, member);
        }
    }

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .annotationIntrospector(new DelegatingCreatorNamedArgumentIntrospector())
            .build();

    @Test
    public void testDeserialization() throws Exception {
        Data data = MAPPER.readValue(a2q("{'part1':'a','part2':'b'}"), Data.class);

        assertThat(data.part1).isEqualTo("a");
        assertThat(data.part2).isEqualTo("b");
    }

    @Test
    public void testDelegatingDeserialization() throws Exception {
        Data data = MAPPER.readValue(a2q("'a b'"), Data.class);

        assertThat(data.part1).isEqualTo("a");
        assertThat(data.part2).isEqualTo("b");
    }
}
