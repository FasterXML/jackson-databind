package com.fasterxml.jackson.databind.tofix;

import java.beans.ConstructorProperties;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import com.fasterxml.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertNotNull;

// [databind#2686]: Not sure if this can ever be solved as
//  setter/builder is on Builder class, but it is called after
//  building completes, i.e. on wrong class.

class BuilderWithBackRef2686Test extends DatabindTestUtil {
    // [databind#2686]
    public static class Container {
        Content forward;

        String containerValue;

        @JsonManagedReference
        public Content getForward() {
            return forward;
        }

        @JsonManagedReference
        public void setForward(Content forward) {
            this.forward = forward;
        }

        public String getContainerValue() {
            return containerValue;
        }

        public void setContainerValue(String containerValue) {
            this.containerValue = containerValue;
        }
    }

    @JsonDeserialize(builder = Content.Builder.class) // Works when removing this, i.e. using the constructor
    public static class Content {
        //         @JsonBackReference
        private Container back;

        private String contentValue;

        @ConstructorProperties({"back", "contentValue"})
        public Content(Container back, String contentValue) {
            this.back = back;
            this.contentValue = contentValue;
        }

        public String getContentValue() {
            return contentValue;
        }

        @JsonBackReference
        public Container getBack() {
            return back;
        }

        @JsonPOJOBuilder(withPrefix = "")
        public static class Builder {
            private Container back;
            private String contentValue;

            @JsonBackReference
            Builder back(Container b) {
                this.back = b;
                return this;
            }

            Builder contentValue(String cv) {
                this.contentValue = cv;
                return this;
            }

            Content build() {
                return new Content(back, contentValue);
            }
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @JacksonTestFailureExpected
    @Test
    void buildWithBackRefs2686() throws Exception {
        Container container = new Container();
        container.containerValue = "containerValue";
        Content content = new Content(container, "contentValue");
        container.forward = content;

        String json = MAPPER.writeValueAsString(container);
        Container result = MAPPER.readValue(json, Container.class); // Exception here

        assertNotNull(result);
    }
}
