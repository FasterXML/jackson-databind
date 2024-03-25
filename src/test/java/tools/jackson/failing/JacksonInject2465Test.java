package tools.jackson.failing;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// [databind#2465]
// NOTE: passes for Jackson 3.0, for reasons unknown
public class JacksonInject2465Test extends DatabindTestUtil
{
    // [databind#2465]
    public static final class TestCase2465 {
        // 17-Apr-2020, tatu: Forcing this to be ignored will work around the
        //   problem, but this really should not be necessary.
//        @JsonIgnore
        private final Internal2465 str;
        private final int id;

        @JsonCreator
        public TestCase2465(@JacksonInject(useInput = OptBoolean.FALSE) Internal2465 str,
                            @JsonProperty("id") int id) {
            this.str = str;
            this.id = id;
        }

        public int fetchId() {
            return id;
        }

        public Internal2465 fetchInternal() {
            return str;
        }
    }

    public static final class Internal2465 {
        final String val;

        public Internal2465(String val) {
            this.val = val;
        }
    }

    // [databind#2465]
    @Test
    void injectWithCreator() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultNullHandling(h -> JsonSetter.Value.construct(Nulls.AS_EMPTY, Nulls.AS_EMPTY))
                .changeDefaultVisibility(h -> h.withVisibility(PropertyAccessor.FIELD,
                        JsonAutoDetect.Visibility.ANY))
                .build();

        final Internal2465 injected = new Internal2465("test");
        TestCase2465 o = mapper.readerFor(TestCase2465.class)
                .with(new InjectableValues.Std().addValue(Internal2465.class, injected))
                .readValue("{\"id\":3}");
        assertEquals(3, o.fetchId());
        assertNotNull(o.fetchInternal());
    }
}
