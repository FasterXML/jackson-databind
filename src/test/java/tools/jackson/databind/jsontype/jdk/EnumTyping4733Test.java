package tools.jackson.databind.jsontype.jdk;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class EnumTyping4733Test extends DatabindTestUtil
{
    // Baseline case that already worked
    @JsonTypeInfo(use = Id.CLASS)
    @JsonSubTypes({
         @JsonSubTypes.Type(value = A_CLASS.class),
    })
    interface InterClass {
         default void yes() {}
    }

    enum A_CLASS implements InterClass {
        A1,
        A2 {
            @Override
            public void yes() { }
        };
    }

    // Failed before fix for [databind#4733]
    @JsonTypeInfo(use = Id.MINIMAL_CLASS)
    @JsonSubTypes({
         @JsonSubTypes.Type(value = A_MIN_CLASS.class),
    })
    interface InterMinimalClass {
         default void yes() {}
    }

    enum A_MIN_CLASS implements InterMinimalClass {
        A1,
        A2 {
            @Override
            public void yes() { }
        };
    }

    // Failed before fix for [databind#4733]
    @JsonTypeInfo(use = Id.NAME)
    @JsonSubTypes({
         @JsonSubTypes.Type(value = A_NAME.class),
    })
    interface InterName {
         default void yes() {}
    }

    enum A_NAME implements InterName {
        A1,
        A2 {
            @Override
            public void yes() { }
        };
    }

    // Failed before fix for [databind#4733]
    @JsonTypeInfo(use = Id.SIMPLE_NAME)
    @JsonSubTypes({
         @JsonSubTypes.Type(value = A_SIMPLE_NAME.class),
    })
    interface InterSimpleName {
         default void yes() {}
    }

    enum A_SIMPLE_NAME implements InterSimpleName {
        A1,
        A2 {
            @Override
            public void yes() { }
        };
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testIssue4733Class() throws Exception
    {
         String json1 = MAPPER.writeValueAsString(A_CLASS.A1);
         String json2 = MAPPER.writeValueAsString(A_CLASS.A2);

         assertEquals(A_CLASS.A1, MAPPER.readValue(json1, A_CLASS.class));
         assertEquals(A_CLASS.A2,  MAPPER.readValue(json2, A_CLASS.class));
    }

    @Test
    public void testIssue4733MinimalClass() throws Exception
    {
         String json1 = MAPPER.writeValueAsString(A_MIN_CLASS.A1);
         String json2 = MAPPER.writeValueAsString(A_MIN_CLASS.A2);
         assertEquals(A_MIN_CLASS.A1, MAPPER.readValue(json1, A_MIN_CLASS.class),
                 "JSON: "+json1);
         assertEquals(A_MIN_CLASS.A2,  MAPPER.readValue(json2, A_MIN_CLASS.class),
                 "JSON: "+json2);
    }

    @Test
    public void testIssue4733Name() throws Exception
    {
         String json1 = MAPPER.writeValueAsString(A_NAME.A1);
         String json2 = MAPPER.writeValueAsString(A_NAME.A2);
         assertEquals(A_NAME.A1, MAPPER.readValue(json1, A_NAME.class),
                 "JSON: "+json1);
         assertEquals(A_NAME.A2,  MAPPER.readValue(json2, A_NAME.class),
                 "JSON: "+json2);
    }

    @Test
    public void testIssue4733SimpleName() throws Exception
    {
         String json1 = MAPPER.writeValueAsString(A_SIMPLE_NAME.A1);
         String json2 = MAPPER.writeValueAsString(A_SIMPLE_NAME.A2);
         assertEquals(A_SIMPLE_NAME.A1, MAPPER.readValue(json1, A_SIMPLE_NAME.class),
                 "JSON: "+json1);
         assertEquals(A_SIMPLE_NAME.A2,  MAPPER.readValue(json2, A_SIMPLE_NAME.class),
                 "JSON: "+json2);
    }
}
