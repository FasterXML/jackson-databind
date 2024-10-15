package tools.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.AnnotatedMethod;
import tools.jackson.databind.introspect.AnnotatedParameter;
import tools.jackson.databind.introspect.AnnotatedWithParams;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static tools.jackson.databind.testutil.DatabindTestUtil.jsonMapperBuilder;

public class DelegatingCreatorImplicitNames1001Test
{
    static class D
    {
        private String raw1 = "";
        private String raw2 = "";

        private D(String raw1, String raw2) {
            this.raw1 = raw1;
            this.raw2 = raw2;
        }

        // not needed strictly speaking, but added for good measure
        @JsonCreator
        public static D make(String value) {
            String[] split = value.split(":");
            return new D(split[0], split[1]);
        }

        @JsonValue
        public String getMyValue() {
            return raw1 + ":" + raw2;
        }

        @Override
        public String toString() {
            return getMyValue();
        }

        @Override
        public boolean equals(Object o) {
            D other = (D) o;
            return other.raw1.equals(raw1)
                    && other.raw2.equals(raw2);
        }
    }

    // To test equivalent of parameter-names, let's use this one
    protected static class CreatorNameIntrospector extends JacksonAnnotationIntrospector
    {
        private static final long serialVersionUID = 1L;

        @Override
        public String findImplicitPropertyName(MapperConfig<?> config, AnnotatedMember member) {
            if (member instanceof AnnotatedParameter) {
                AnnotatedParameter p = (AnnotatedParameter) member;
                AnnotatedWithParams owner = p.getOwner();
                if (owner instanceof AnnotatedMethod) {
                    if (p.getIndex() == 0) {
                        return "value";
                    }
                }
            }
            return super.findImplicitPropertyName(config, member);
        }
    }

    // Baseline test to show how things should work
    @Test
    public void testWithoutNamedParameters() throws Exception
    {
        ObjectMapper sut = new ObjectMapper();

        D d = D.make("abc:def");

        String actualJson = sut.writeValueAsString(d);
        D actualD = sut.readValue(actualJson, D.class);

        assertEquals("\"abc:def\"", actualJson);
        assertEquals(d, actualD);
    }

    // And then case that fails with [databind#1001]
    @Test
    public void testWithNamedParameters() throws Exception
    {
        ObjectMapper sut = jsonMapperBuilder()
            .annotationIntrospector(new CreatorNameIntrospector())
            .build();

        D d = D.make("abc:def");

        String actualJson = sut.writeValueAsString(d);
        D actualD = sut.readValue(actualJson, D.class);

        assertEquals("\"abc:def\"", actualJson);
        assertEquals(d, actualD);
    }
}
