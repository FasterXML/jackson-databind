package com.fasterxml.jackson.databind.views;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonView;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

public class FailOnUnexpectedView6077Test extends DatabindTestUtil
{
    static class PublicView { }
    static class InternalView extends PublicView { }

    private final ObjectMapper FAIL_ON_MAPPER = jsonMapperBuilder()
            .enable(DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES)
            .build();

    private final ObjectMapper FAIL_OFF_MAPPER = jsonMapperBuilder()
            .disable(DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES)
            .build();

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder({ "name", "secret" })
    static class ArrayBean {
        @JsonView(PublicView.class)
        public String name;

        @JsonView(InternalView.class)
        public String secret;

        public ArrayBean() { }

        public ArrayBean(String name, String secret) {
            this.name = name;
            this.secret = secret;
        }
    }

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder({ "name", "secret" })
    static class ArrayCreatorBean {
        final String name;
        final String secret;

        @JsonCreator
        public ArrayCreatorBean(
                @JsonProperty("name") @JsonView(PublicView.class) String name,
                @JsonProperty("secret") @JsonView(InternalView.class) String secret) {
            this.name = name;
            this.secret = secret;
        }
    }

    @JsonDeserialize(builder = ArrayBuilderBean.Builder.class)
    static class ArrayBuilderBean {
        final String name;
        final String secret;

        ArrayBuilderBean(String name, String secret) {
            this.name = name;
            this.secret = secret;
        }

        @JsonPOJOBuilder(withPrefix = "")
        @JsonFormat(shape = JsonFormat.Shape.ARRAY)
        @JsonPropertyOrder({ "name", "secret" })
        static class Builder {
            String name;
            String secret;

            @JsonView(PublicView.class)
            public Builder name(String value) {
                name = value;
                return this;
            }

            @JsonView(InternalView.class)
            public Builder secret(String value) {
                secret = value;
                return this;
            }

            public ArrayBuilderBean build() {
                return new ArrayBuilderBean(name, secret);
            }
        }
    }

    @JsonDeserialize(builder = ArrayCreatorBuilderBean.Builder.class)
    static class ArrayCreatorBuilderBean {
        final String name;
        final String secret;

        ArrayCreatorBuilderBean(String name, String secret) {
            this.name = name;
            this.secret = secret;
        }

        @JsonPOJOBuilder(withPrefix = "")
        @JsonFormat(shape = JsonFormat.Shape.ARRAY)
        @JsonPropertyOrder({ "name", "secret" })
        static class Builder {
            String name;
            String secret;

            @JsonCreator
            public Builder(@JsonProperty("name") @JsonView(PublicView.class) String name) {
                this.name = name;
            }

            @JsonView(InternalView.class)
            public Builder secret(String value) {
                secret = value;
                return this;
            }

            public ArrayCreatorBuilderBean build() {
                return new ArrayCreatorBuilderBean(name, secret);
            }
        }
    }

    static class CreatorBean {
        final String name;
        final String secret;

        @JsonCreator
        public CreatorBean(
                @JsonProperty("name") @JsonView(PublicView.class) String name,
                @JsonProperty("secret") @JsonView(InternalView.class) String secret) {
            this.name = name;
            this.secret = secret;
        }
    }

    static class CreatorWithBufferedPropertyBean {
        final String name;

        @JsonView(InternalView.class)
        public String secret;

        @JsonCreator
        public CreatorWithBufferedPropertyBean(
                @JsonProperty("name") @JsonView(PublicView.class) String name) {
            this.name = name;
        }
    }

    @JsonDeserialize(builder = BuilderCreatorBean.Builder.class)
    static class BuilderCreatorBean {
        final String name;
        final String secret;

        BuilderCreatorBean(String name, String secret) {
            this.name = name;
            this.secret = secret;
        }

        @JsonPOJOBuilder(withPrefix = "")
        static class Builder {
            String name;
            String secret;

            @JsonCreator
            public Builder(
                    @JsonProperty("name") @JsonView(PublicView.class) String name,
                    @JsonProperty("secret") @JsonView(InternalView.class) String secret) {
                this.name = name;
                this.secret = secret;
            }

            public BuilderCreatorBean build() {
                return new BuilderCreatorBean(name, secret);
            }
        }
    }

    @JsonDeserialize(builder = BuilderWithBufferedPropertyBean.Builder.class)
    static class BuilderWithBufferedPropertyBean {
        final String name;
        final String secret;

        BuilderWithBufferedPropertyBean(String name, String secret) {
            this.name = name;
            this.secret = secret;
        }

        @JsonPOJOBuilder(withPrefix = "")
        static class Builder {
            String name;
            String secret;

            @JsonCreator
            public Builder(@JsonProperty("name") @JsonView(PublicView.class) String name) {
                this.name = name;
            }

            @JsonView(InternalView.class)
            public Builder secret(String value) {
                secret = value;
                return this;
            }

            public BuilderWithBufferedPropertyBean build() {
                return new BuilderWithBufferedPropertyBean(name, secret);
            }
        }
    }

    static class Details {
        @JsonView(PublicView.class)
        public String note;
    }

    static class UnwrappedBean {
        @JsonView(PublicView.class)
        public String name;

        @JsonView(InternalView.class)
        public String secret;

        @JsonUnwrapped
        @JsonView(PublicView.class)
        public Details details;
    }

    @JsonDeserialize(builder = UnwrappedBuilderBean.Builder.class)
    static class UnwrappedBuilderBean {
        final String name;
        final String secret;
        final Details details;

        UnwrappedBuilderBean(String name, String secret, Details details) {
            this.name = name;
            this.secret = secret;
            this.details = details;
        }

        @JsonPOJOBuilder(withPrefix = "")
        static class Builder {
            String name;
            String secret;
            Details details;

            @JsonView(PublicView.class)
            public Builder name(String value) {
                name = value;
                return this;
            }

            @JsonView(InternalView.class)
            public Builder secret(String value) {
                secret = value;
                return this;
            }

            @JsonUnwrapped
            @JsonView(PublicView.class)
            public Builder details(Details value) {
                details = value;
                return this;
            }

            public UnwrappedBuilderBean build() {
                return new UnwrappedBuilderBean(name, secret, details);
            }
        }
    }

    interface Payload { }

    static class TextPayload implements Payload {
        public String value;
    }

    static class ExternalTypeBean {
        @JsonView(PublicView.class)
        public String name;

        @JsonView(InternalView.class)
        public String secret;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
        @JsonSubTypes({ @JsonSubTypes.Type(value = TextPayload.class, name = "text") })
        @JsonView(PublicView.class)
        public Payload payload;
    }

    static class ExternalTypeCreatorBean {
        final String name;
        final String secret;

        @JsonView(InternalView.class)
        public String note;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
        @JsonSubTypes({ @JsonSubTypes.Type(value = TextPayload.class, name = "text") })
        @JsonView(PublicView.class)
        public Payload payload;

        @JsonCreator
        public ExternalTypeCreatorBean(
                @JsonProperty("name") @JsonView(PublicView.class) String name,
                @JsonProperty("secret") @JsonView(InternalView.class) String secret) {
            this.name = name;
            this.secret = secret;
        }
    }

    static class ExternalTypeIdCreatorBean {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "kind",
                visible = true)
        @JsonSubTypes({ @JsonSubTypes.Type(value = TextPayload.class, name = "text") })
        @JsonView(PublicView.class)
        public Payload payload;

        final String name;
        final String kind;

        @JsonCreator
        public ExternalTypeIdCreatorBean(
                @JsonProperty("name") @JsonView(PublicView.class) String name,
                @JsonProperty("kind") @JsonView(InternalView.class) String kind,
                @JsonProperty("payload") @JsonView(PublicView.class) Payload payload) {
            this.name = name;
            this.kind = kind;
            this.payload = payload;
        }
    }

    @JsonDeserialize(builder = ExternalTypeBuilderBean.Builder.class)
    static class ExternalTypeBuilderBean {
        final String name;
        final String secret;
        final Payload payload;

        ExternalTypeBuilderBean(String name, String secret, Payload payload) {
            this.name = name;
            this.secret = secret;
            this.payload = payload;
        }

        @JsonPOJOBuilder(withPrefix = "")
        static class Builder {
            String name;
            String secret;
            Payload payload;

            @JsonView(PublicView.class)
            public Builder name(String value) {
                name = value;
                return this;
            }

            @JsonView(InternalView.class)
            public Builder secret(String value) {
                secret = value;
                return this;
            }

            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                    include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
            @JsonSubTypes({ @JsonSubTypes.Type(value = TextPayload.class, name = "text") })
            @JsonView(PublicView.class)
            public Builder payload(Payload value) {
                payload = value;
                return this;
            }

            public ExternalTypeBuilderBean build() {
                return new ExternalTypeBuilderBean(name, secret, payload);
            }
        }
    }

    @Test
    public void pojoAsArrayCreateFailsWhenEnabled() throws Exception {
        expectUnexpectedView(FAIL_ON_MAPPER.readerWithView(PublicView.class)
                .forType(ArrayBean.class), "[\"name\",\"secret\"]");
    }

    @Test
    public void pojoAsArrayCreateSkipsWhenDisabled() throws Exception {
        ArrayBean result = FAIL_OFF_MAPPER.readerWithView(PublicView.class)
                .forType(ArrayBean.class)
                .readValue("[\"name\",\"secret\"]");

        assertEquals("name", result.name);
        assertNull(result.secret);
    }

    @Test
    public void pojoAsArrayUpdateSkipsHiddenWhenDisabled() throws Exception {
        ArrayBean input = new ArrayBean("old", "unchanged");
        Object result = FAIL_OFF_MAPPER.readerForUpdating(input)
                .withView(PublicView.class)
                .readValue("[\"new\",\"modified\"]");

        assertSame(input, result);
        assertEquals("new", input.name);
        assertEquals("unchanged", input.secret);
    }

    @Test
    public void pojoAsArrayUpdateFailsWhenEnabled() throws Exception {
        ArrayBean input = new ArrayBean("old", "unchanged");
        expectUnexpectedView(FAIL_ON_MAPPER.readerForUpdating(input)
                .withView(PublicView.class), "[\"new\",\"modified\"]");
    }

    @Test
    public void pojoAsArrayPropertyBasedCreatorFailsWhenEnabled() throws Exception {
        expectUnexpectedView(FAIL_ON_MAPPER.readerWithView(PublicView.class)
                .forType(ArrayCreatorBean.class), "[\"name\",\"secret\"]");
    }

    @Test
    public void builderAsArraySetterFailsWhenEnabled() throws Exception {
        expectUnexpectedView(FAIL_ON_MAPPER.readerWithView(PublicView.class)
                .forType(ArrayBuilderBean.class), "[\"name\",\"secret\"]");
    }

    @Test
    public void builderAsArrayPropertyBasedCreatorFailsWhenEnabled() throws Exception {
        expectUnexpectedView(FAIL_ON_MAPPER.readerWithView(PublicView.class)
                .forType(ArrayCreatorBuilderBean.class), "[\"name\",\"secret\"]");
    }

    @Test
    public void creatorPropertyFailsWhenEnabled() throws Exception {
        expectUnexpectedView(FAIL_ON_MAPPER.readerWithView(PublicView.class)
                .forType(CreatorBean.class), "{\"name\":\"name\",\"secret\":\"secret\"}");
    }

    @Test
    public void bufferedPropertyFailsWhenEnabled() throws Exception {
        expectUnexpectedView(FAIL_ON_MAPPER.readerWithView(PublicView.class)
                .forType(CreatorWithBufferedPropertyBean.class),
                "{\"secret\":\"secret\",\"name\":\"name\"}");
    }

    @Test
    public void builderCreatorPropertyFailsWhenEnabled() throws Exception {
        expectUnexpectedView(FAIL_ON_MAPPER.readerWithView(PublicView.class)
                .forType(BuilderCreatorBean.class), "{\"name\":\"name\",\"secret\":\"secret\"}");
    }

    @Test
    public void builderBufferedPropertyFailsWhenEnabled() throws Exception {
        expectUnexpectedView(FAIL_ON_MAPPER.readerWithView(PublicView.class)
                .forType(BuilderWithBufferedPropertyBean.class),
                "{\"secret\":\"secret\",\"name\":\"name\"}");
    }

    @Test
    public void unwrappedPropertyFailsWhenEnabled() throws Exception {
        expectUnexpectedView(FAIL_ON_MAPPER.readerWithView(PublicView.class)
                .forType(UnwrappedBean.class),
                "{\"name\":\"name\",\"secret\":\"secret\",\"note\":\"note\"}");
    }

    @Test
    public void unwrappedBuilderPropertyFailsWhenEnabled() throws Exception {
        expectUnexpectedView(FAIL_ON_MAPPER.readerWithView(PublicView.class)
                .forType(UnwrappedBuilderBean.class),
                "{\"name\":\"name\",\"secret\":\"secret\",\"note\":\"note\"}");
    }

    @Test
    public void externalTypeIdPropertyFailsWhenEnabled() throws Exception {
        expectUnexpectedView(FAIL_ON_MAPPER.readerWithView(PublicView.class)
                .forType(ExternalTypeBean.class),
                "{\"name\":\"name\",\"secret\":\"secret\",\"type\":\"text\",\"payload\":{\"value\":\"v\"}}");
    }

    @Test
    public void externalTypeIdCreatorPropertyFailsWhenEnabled() throws Exception {
        expectUnexpectedView(FAIL_ON_MAPPER.readerWithView(PublicView.class)
                .forType(ExternalTypeCreatorBean.class),
                "{\"name\":\"name\",\"secret\":\"secret\",\"type\":\"text\",\"payload\":{\"value\":\"v\"}}");
    }

    @Test
    public void hiddenExternalTypeIdCreatorPropertyFailsWhenEnabled() throws Exception {
        expectUnexpectedView(FAIL_ON_MAPPER.readerWithView(PublicView.class)
                .forType(ExternalTypeIdCreatorBean.class),
                "{\"name\":\"name\",\"kind\":\"text\",\"payload\":{\"value\":\"v\"}}");
    }

    @Test
    public void externalTypeIdBufferedPropertyFailsWhenEnabled() throws Exception {
        expectUnexpectedView(FAIL_ON_MAPPER.readerWithView(PublicView.class)
                .forType(ExternalTypeCreatorBean.class),
                "{\"name\":\"name\",\"note\":\"note\",\"type\":\"text\",\"payload\":{\"value\":\"v\"}}");
    }

    @Test
    public void externalTypeIdBuilderPropertyFailsWhenEnabled() throws Exception {
        expectUnexpectedView(FAIL_ON_MAPPER.readerWithView(PublicView.class)
                .forType(ExternalTypeBuilderBean.class),
                "{\"name\":\"name\",\"secret\":\"secret\",\"type\":\"text\",\"payload\":{\"value\":\"v\"}}");
    }

    private void expectUnexpectedView(ObjectReader reader, String json) throws Exception {
        try {
            reader.readValue(json);
            fail("Expected MismatchedInputException for view-hidden property");
        } catch (MismatchedInputException e) {
            verifyException(e, "Input mismatch while deserializing");
            verifyException(e, "is not part of current active view");
        }
    }
}
