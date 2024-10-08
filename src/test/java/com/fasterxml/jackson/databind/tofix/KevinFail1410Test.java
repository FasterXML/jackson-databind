package com.fasterxml.jackson.databind.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import com.fasterxml.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class KevinFail1410Test extends DatabindTestUtil {
    enum EnvironmentEventSource {BACKEND;}

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

        public static BackendEvent create(String environmentName, String message,
                                          String status, Object results) {
            return new BackendEvent(environmentName, message,
                    status, results);
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

    @JacksonTestFailureExpected
    @Test
    void dupProps() throws Exception {
        ObjectMapper mapper = newJsonMapper();
        EnvironmentEvent event = new BackendEvent("foo", "hello", "bar", null);
        String ser = mapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(event);
        mapper.readValue(ser, EnvironmentEvent.class);
        assertNotNull(ser);
    }
}
