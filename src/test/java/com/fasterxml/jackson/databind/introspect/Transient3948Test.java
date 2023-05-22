package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serial;
import java.io.Serializable;

public class Transient3948Test extends BaseMapTest {

    public static class Obj implements Serializable {

        @Serial
        private static final long serialVersionUID = -1L;

        private String a;

        @JsonIgnore
        private transient String b;

        @JsonProperty("cat")
        private String c;

        @JsonProperty("dog")
        private transient String d;

        public String getA() {
            return a;
        }

        public void setA(String a) {
            this.a = a;
        }

        public String getB() {
            return b;
        }

        public void setB(String b) {
            this.b = b;
        }

        public String getC() {
            return c;
        }

        public void setC(String c) {
            this.c = c;
        }

        public String getD() {
            return d;
        }

        public void setD(String d) {
            this.d = d;
        }
    }

    final ObjectMapper DEFAULT_MAPPER = newJsonMapper();

    final ObjectMapper MAPPER_TRANSIENT = jsonMapperBuilder()
            .configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
            .build();

    public void testJsonIgnoreSerialization() throws Exception {
        var obj1 = new Obj();
        obj1.setA("hello");
        obj1.setB("world");
        obj1.setC("jackson");
        obj1.setD("databind");

        String json = DEFAULT_MAPPER.writeValueAsString(obj1);

        assertEquals(a2q("{'a':'hello','b':'world','cat':'jackson','dog':'databind'}"), json);
    }

    public void testJsonIgnoreSerializationTransient() throws Exception {
        var obj1 = new Obj();
        obj1.setA("hello");
        obj1.setB("world");
        obj1.setC("jackson");
        obj1.setD("databind");


        String json = MAPPER_TRANSIENT.writeValueAsString(obj1);

        assertEquals(a2q("{'a':'hello','cat':'jackson','dog':'databind'}"), json);
    }
}
