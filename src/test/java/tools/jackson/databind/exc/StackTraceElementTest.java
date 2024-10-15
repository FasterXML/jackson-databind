package tools.jackson.databind.exc;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import static tools.jackson.databind.testutil.DatabindTestUtil.jsonMapperBuilder;

// for [databind#1794]
public class StackTraceElementTest
{
    public static class ErrorObject {

        public String throwable;
        public String message;

//        @JsonDeserialize(contentUsing = StackTraceElementDeserializer.class)
        public StackTraceElement[] stackTrace;

        ErrorObject() {}

        public ErrorObject(Throwable throwable) {
            this.throwable = throwable.getClass().getName();
            message = throwable.getMessage();
            stackTrace = throwable.getStackTrace();
        }
    }

    // for [databind#1794] where extra `declaringClass` is serialized from private field.
    @Test
    public void testCustomStackTraceDeser() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultVisibility(vc ->
                    vc.withVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY))
                .build();

        String json = mapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(new ErrorObject(new Exception("exception message")));

        ErrorObject result = mapper.readValue(json, ErrorObject.class);
        assertNotNull(result);
    }
}
