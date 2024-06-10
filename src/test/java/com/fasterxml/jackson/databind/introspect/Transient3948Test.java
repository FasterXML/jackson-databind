package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serializable;

// With [databind#3948] we should not drop `@JsonIgnore` regardless
// of "transient" keyword.
public class Transient3948Test extends BaseMapTest {

    @JsonPropertyOrder(alphabetic = true)
    static class Obj implements Serializable {

        private static final long serialVersionUID = -1L;

        private String a = "hello";

        @JsonIgnore
        private transient String b = "world";

        @JsonProperty("cat")
        private String c = "jackson";

        @JsonProperty("dog")
        private transient String d = "databind";

        public String getA() {
            return a;
        }

        public String getB() {
            return b;
        }

        public String getC() {
            return c;
        }

        public String getD() {
            return d;
        }
    }

    private final ObjectMapper DEFAULT_MAPPER = newJsonMapper();

    private final ObjectMapper MAPPER_TRANSIENT = jsonMapperBuilder()
            .configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
            .build();

    public void testJsonIgnoreSerialization() throws Exception {
        Obj obj1 = new Obj();

        String json = DEFAULT_MAPPER.writeValueAsString(obj1);

        assertEquals(a2q("{'a':'hello','cat':'jackson','dog':'databind'}"), json);
    }

    public void testJsonIgnoreSerializationTransient() throws Exception {
        Obj obj1 = new Obj();

        String json = MAPPER_TRANSIENT.writeValueAsString(obj1);

        assertEquals(a2q("{'a':'hello','cat':'jackson','dog':'databind'}"), json);
    }
}
