package tools.jackson.databind.jsontype;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#1410]: Bean property name overlapping with class-level type id
//   property should fail with helpful message
public class TypeIdPropertyDup1410Test extends DatabindTestUtil
{
    enum EnvironmentEventSource { BACKEND; }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "source")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = BackendEvent.class, name = "BACKEND")
    })
    static abstract class EnvironmentEvent {
        private String environmentName;
        private String message;

        protected EnvironmentEvent() {
        } // for deserializer

        protected EnvironmentEvent(String env, String msg) {
            environmentName = env;
            message = msg;
        }

        public String getEnvironmentName() {
            return environmentName;
        }

        public abstract EnvironmentEventSource getSource();

        public String getMessage() {
            return message;
        }
    }

    static class BackendEvent extends EnvironmentEvent {
        private String status;

        private Object resultData;

        protected BackendEvent() {
        } // for deserializer

        public BackendEvent(String envName, String message, String status, Object results) {
            super(envName, message);
            this.status = status;
            resultData = results;
        }

        @Override
        public EnvironmentEventSource getSource() {
            return EnvironmentEventSource.BACKEND;
        }

        public String getStatus() {
            return status;
        }

        public Object getResultData() {
            return resultData;
        }

        @Override
        public String toString() {
            return String.format("(%s): %s", status, getMessage());
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#1410]
    @Test
    void dupPropsFailsWithHelpfulMessage() throws Exception {
        EnvironmentEvent event = new BackendEvent("foo", "hello", "bar", null);
        InvalidDefinitionException ex = assertThrows(
                InvalidDefinitionException.class,
                () -> MAPPER.writeValueAsString(event));
        verifyException(ex, "Conflict between type id property");
        verifyException(ex, "EXISTING_PROPERTY");
    }
}
