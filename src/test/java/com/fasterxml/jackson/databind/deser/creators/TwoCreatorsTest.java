package com.fasterxml.jackson.databind.deser.creators;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TwoCreatorsTest {

    @Test
    public void testThatPropertiesCreatorIsUsed() throws JsonProcessingException {
        final String json = "{ \"bean\":{ \"list\":[ \"a\", \"b\", \"c\"], \"inner\":{ \"name\": \"inner\" }}}";

        final ObjectMapper mapper = new ObjectMapper();

        OuterBean result = mapper.readValue(json, OuterBean.class);

        assertThat(result)
                .isNotNull();

        assertThat(result.getBean())
                .isNotNull();

        assertThat(result.getBean().getInner())
                .isNotNull();

        assertThat(result.getBean().getInner().getName())
                .isEqualTo("inner");

        assertThat(result.getBean().getList())
                .containsExactly("a", "b", "c");
    }

    @Test
    public void testThatDelegatingCreatorIsUsed() throws JsonProcessingException {
        final String json = "{ \"bean\": [ \"a\", \"b\", \"c\"] }";

        final ObjectMapper mapper = new ObjectMapper();

        OuterBean result = mapper.readValue(json, OuterBean.class);

        assertThat(result)
                .isNotNull();

        assertThat(result.getBean())
                .isNotNull();

        assertThat(result.getBean().getInner())
                .isNotNull();

        assertThat(result.getBean().getInner().getName())
                .isEqualTo("default");

        assertThat(result.getBean().getList())
                .containsExactly("a", "b", "c");
    }

    public static class OuterBean {
        private final Bean bean;

        @JsonCreator
        public OuterBean(@JsonProperty("bean") Bean bean) {
            this.bean = bean;
        }

        public Bean getBean() {
            return bean;
        }
    }

    public static class Bean {
        private final List<String> list;

        private final InnerBean inner;

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public Bean(@JsonProperty("list") List<String> list, @JsonProperty("inner") InnerBean inner) {
            this.list = list;
            this.inner = inner;
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        private static Bean of(final List<String> list) {
            return new Bean(list, new InnerBean("default"));
        }

        public List<String> getList() {
            return list;
        }

        public InnerBean getInner() {
            return inner;
        }
    }

    public static class InnerBean {
        private final String name;

        @JsonCreator
        public InnerBean(@JsonProperty("name") String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
