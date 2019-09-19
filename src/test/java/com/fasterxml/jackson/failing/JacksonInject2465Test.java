package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.annotation.PropertyAccessor;

import com.fasterxml.jackson.databind.*;

public class JacksonInject2465Test extends BaseMapTest
{
    public static final class TestCase2465 {
        private final Internal2465 str;
        private final int id;

        @JsonCreator
        public TestCase2465(@JacksonInject Internal2465 str, @JsonProperty("id") int id) {
            this.str = str;
            this.id = id;
        }

        public int fetchId() { return id; }
        public Internal2465 fetchInternal() { return str; }
    }

    public static final class Internal2465 {
        final String val;

        public Internal2465(String val) {
            this.val = val;
        }
    }

    // [databind#2465]
    public void testInjectWithCreator() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .defaultSetterInfo(JsonSetter.Value.construct(Nulls.AS_EMPTY, Nulls.AS_EMPTY))
                .build();
        mapper.setVisibility(mapper.getVisibilityChecker().withVisibility(PropertyAccessor.FIELD,
                JsonAutoDetect.Visibility.ANY));

        final Internal2465 injected = new Internal2465("test");
        TestCase2465 o = mapper.readerFor(TestCase2465.class)
                .with(new InjectableValues.Std().addValue(Internal2465.class, injected))
                .readValue("{\"id\":3}");
        assertEquals(3, o.fetchId());
        assertNotNull(o.fetchInternal());
    }
}
