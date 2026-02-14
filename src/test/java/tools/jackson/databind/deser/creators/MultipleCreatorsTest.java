package tools.jackson.databind.deser.creators;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.assertj.core.api.Assertions.assertThat;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MultipleCreatorsTest
    extends DatabindTestUtil
{
    // [databind#4602]
    static class OuterBean4602 {
        private final Bean4602 bean;

        @JsonCreator
        public OuterBean4602(@JsonProperty("bean") Bean4602 bean) {
            this.bean = bean;
        }

        public Bean4602 getBean() {
            return bean;
        }
    }

    // [databind#4602]
    static class Bean4602 {
        private final List<String> list;

        private final InnerBean4602 inner;

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public Bean4602(@JsonProperty("list") List<String> list, @JsonProperty("inner") InnerBean4602 inner) {
            this.list = list;
            this.inner = inner;
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        private static Bean4602 of(final List<String> list) {
            return new Bean4602(list, new InnerBean4602("default"));
        }

        public List<String> getList() {
            return list;
        }

        public InnerBean4602 getInner() {
            return inner;
        }
    }

    // [databind#4602]
    static class InnerBean4602 {
        private final String name;

        @JsonCreator
        public InnerBean4602(@JsonProperty("name") String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    // [databind#2962]
    static class ExampleDto2962
    {
        final int version;

        ExampleDto2962(int version) {
            this.version = version;
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        static ExampleDto2962 fromJson(Json2962 json) {
            return new ExampleDto2962(json.version);
        }

        static class Json2962 {
            public int version;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#4602]
    @Test
    public void testThatPropertiesCreatorIsUsed() throws Exception
    {
        final String json = "{ \"bean\":{ \"list\":[ \"a\", \"b\", \"c\"], \"inner\":{ \"name\": \"inner\" }}}";

        OuterBean4602 result = MAPPER.readValue(json, OuterBean4602.class);

        assertThat(result).isNotNull();
        assertThat(result.getBean()).isNotNull();
        assertThat(result.getBean().getInner()).isNotNull();
        assertThat(result.getBean().getInner().getName()).isEqualTo("inner");
        assertThat(result.getBean().getList()).containsExactly("a", "b", "c");
    }

    // [databind#4602]
    @Test
    public void testThatDelegatingCreatorIsUsed() throws Exception
    {
        OuterBean4602 result = MAPPER.readValue("{ \"bean\": [ \"a\", \"b\", \"c\"] }",
                OuterBean4602.class);

        assertThat(result).isNotNull();
        assertThat(result.getBean()).isNotNull();
        assertThat(result.getBean().getInner()).isNotNull();
        assertThat(result.getBean().getInner().getName()).isEqualTo("default");
        assertThat(result.getBean().getList()).containsExactly("a", "b", "c");
    }

    // [databind#2962]
    @Test
    public void testImplicitCtorExplicitFactory() throws Exception
    {
        ExampleDto2962 result = MAPPER.readValue("42", ExampleDto2962.class);
        assertEquals(42, result.version);
    }
}
