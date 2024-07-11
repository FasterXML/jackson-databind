package com.fasterxml.jackson.databind.deser.creators;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// for [databind#4602]
public class TwoCreators4602Test
    extends DatabindTestUtil
{
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

    private final ObjectMapper MAPPER = newJsonMapper();

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
}
