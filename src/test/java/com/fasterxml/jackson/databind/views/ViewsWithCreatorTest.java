package com.fasterxml.jackson.databind.views;

import static com.fasterxml.jackson.databind.BaseMapTest.newJsonMapper;
import static org.junit.Assert.assertEquals;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.junit.Test;

public class ViewsWithCreatorTest
{

    public static class View { }
    public static class View1 extends View { }
    public static class View2 extends View { }

    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE,
            getterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ObjWithCreator {

        public String a;

        @JsonView(View1.class)
        public String b;

        @JsonView(View2.class)
        public String c;

        public ObjWithCreator() { }

        @JsonCreator
        public ObjWithCreator(@JsonProperty("a") String a, @JsonProperty("b") String b, @JsonProperty("c") String c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }

        @Override
        public String toString() {
            return String.format("%s-%s-%s", a, b, c);
        }
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE,
            getterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ObjWithoutCreator {

        public String a;

        @JsonView(View1.class)
        public String b;

        @JsonView(View2.class)
        public String c;

        public ObjWithoutCreator() { }

        public ObjWithoutCreator(String a, String b, String c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }

        @Override
        public String toString() {
            return String.format("%s-%s-%s", a, b, c);
        }
    }

    @Test
    public void testWithJsonCreator() throws Exception {
        ObjectMapper mapper = newJsonMapper();
        ObjectReader reader = mapper.readerFor(ObjWithCreator.class).withView(View1.class);

        // serialize first,
        String JSON = mapper.writeValueAsString(new ObjWithCreator("a", "b", "c"));
        // then back
        assertEquals("a-b-null", reader.readValue(JSON).toString());
    }

    @Test
    public void testWithoutJsonCreator() throws Exception {
        ObjectMapper mapper = newJsonMapper();
        ObjectReader reader = mapper.readerFor(ObjWithoutCreator.class).withView(View1.class);

        // serialize first,
        String JSON = mapper.writeValueAsString(new ObjWithoutCreator("a", "b", "c"));
        // then back
        assertEquals("a-b-null", reader.readValue(JSON).toString());
    }
}
