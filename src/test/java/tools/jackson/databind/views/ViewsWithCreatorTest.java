package tools.jackson.databind.views;

import static org.junit.Assert.assertEquals;
import static tools.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;

public class ViewsWithCreatorTest
{
    static class View { }
    static class View1 extends View { }
    static class View2 extends View { }

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

    private final ObjectMapper MAPPER = newJsonMapper();
    
    // [databind#1172]
    @Test
    public void testWithJsonCreator() throws Exception
    {
        ObjectReader reader = MAPPER.readerFor(ObjWithCreator.class).withView(View1.class);

        // serialize first,
        String json = MAPPER.writeValueAsString(new ObjWithCreator("a", "b", "c"));
        // then back
        assertEquals("a-b-null", reader.readValue(json).toString());
    }

    // [databind#1172]
    @Test
    public void testWithoutJsonCreator() throws Exception
    {
        ObjectReader reader = MAPPER.readerFor(ObjWithoutCreator.class)
            .without(DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES)
            .withView(View1.class);

        // serialize first,
        String json = MAPPER.writeValueAsString(new ObjWithoutCreator("a", "b", "c"));
        // then back
        assertEquals("a-b-null", reader.readValue(json).toString());
    }
}
