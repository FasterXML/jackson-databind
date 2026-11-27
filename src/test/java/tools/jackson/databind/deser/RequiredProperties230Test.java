package tools.jackson.databind.deser;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.filter.FilteringParserDelegate;
import tools.jackson.core.filter.TokenFilter;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

class RequiredProperties230Test extends DatabindTestUtil
{
    static class RequiredBean {
        @JsonProperty(required = true)
        @JsonAlias("label")
        public String name = "initial";

        @JsonProperty(required = true, isRequired = OptBoolean.FALSE)
        public String optional;

        @JsonProperty(isRequired = OptBoolean.TRUE)
        @JsonSetter(nulls = Nulls.SKIP)
        public void setCount(Integer value) {
            count = value;
        }

        private Integer count = 7;
    }

    static class WithNestedName {
        @JsonDeserialize(using = FromActiveParser.class)
        public JsonNode child;

        @JsonProperty(required = true)
        public String name;
    }

    static class FromActiveParser extends StdDeserializer<JsonNode> {
        FromActiveParser() {
            super(JsonNode.class);
        }

        @Override
        public JsonNode deserialize(JsonParser p, DeserializationContext ctxt) {
            return ctxt.readValue(ctxt.getParser(), JsonNode.class);
        }
    }

    static class WithCreator {
        public final String id;

        @JsonProperty(required = true)
        public String value;

        @JsonCreator
        WithCreator(@JsonProperty(value = "id", required = true) String id) {
            this.id = id;
        }
    }

    @JsonDeserialize(builder = ResultBuilder.class)
    static class Result {
        public final String name;

        Result(String name) {
            this.name = name;
        }
    }

    static class ResultBuilder {
        private String name;

        @JsonProperty(value = "name", required = true)
        public ResultBuilder withName(String value) {
            name = value;
            return this;
        }

        public Result build() {
            if (name == null) {
                throw new IllegalStateException("name was not provided to the builder");
            }
            return new Result(name);
        }
    }

    @JsonDeserialize(builder = ValueWithRequiredName.Builder.class)
    static class ValueWithRequiredName {
        @JsonProperty(required = true)
        private final String name;

        @JsonProperty(required = true)
        private final String optional;

        @JsonProperty(required = true)
        private final String derived = "computed";

        ValueWithRequiredName(String name, String optional) {
            this.name = name;
            this.optional = optional;
        }

        static class Builder {
            private String name;
            private String optional;

            @JsonAlias("label")
            public Builder withName(String value) {
                name = value;
                return this;
            }

            @JsonProperty(isRequired = OptBoolean.FALSE)
            public Builder withOptional(String value) {
                optional = value;
                return this;
            }

            public ValueWithRequiredName build() {
                return new ValueWithRequiredName(name, optional);
            }
        }
    }

    static class UnwrappedBuilderValue {
        @JsonUnwrapped(prefix = "value_")
        public ValueWithRequiredName value;
    }

    @JsonDeserialize(builder = CreatorBuilderValue.Builder.class)
    record CreatorBuilderValue(@JsonProperty(required = true) String name,
            @JsonProperty(required = true) String optional) {
        @JsonPropertyOrder({ "name", "optional" })
        static class Builder {
            private final String name;
            private final String optional;

            @JsonCreator
            Builder(@JsonProperty("name") @JsonAlias("label") @JsonView(InternalView.class) String name,
                    @JsonProperty(value = "optional", isRequired = OptBoolean.FALSE) String optional) {
                this.name = name;
                this.optional = optional;
            }

            public CreatorBuilderValue build() {
                return new CreatorBuilderValue(name, optional);
            }
        }
    }

    static class PositionalCreatorBuilderValue {
        @JsonFormat(shape = JsonFormat.Shape.ARRAY)
        public CreatorBuilderValue value;
    }

    static class UnwrappedCreatorBuilderValue {
        @JsonUnwrapped(prefix = "value_")
        public CreatorBuilderValue value;
    }

    static class OuterWithInner {
        public Inner inner;

        @JsonFormat(shape = JsonFormat.Shape.ARRAY)
        public Inner positional;

        public class Inner {
            @JsonProperty(required = true)
            public String name = "initial";
        }
    }

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder({ "count", "name" })
    static class PositionalBean {
        public int count;

        @JsonProperty(required = true)
        public String name;
    }

    @JsonDeserialize(builder = PositionalValue.Builder.class)
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    record PositionalValue(int count, @JsonProperty(required = true) String name) {
        @JsonFormat(shape = JsonFormat.Shape.ARRAY)
        @JsonPropertyOrder({ "count", "name" })
        static class Builder {
            private int count;
            private String name;

            public Builder withCount(int value) {
                count = value;
                return this;
            }

            public Builder withName(String value) {
                name = value;
                return this;
            }

            public PositionalValue build() {
                return new PositionalValue(count, name);
            }
        }
    }

    static class ExternalPayload {
        public String name;
    }

    @JsonIgnoreProperties("oldItem")
    static class WithExternalPayload {
        @JsonProperty(required = true)
        @JsonAlias("oldItem")
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = "kind")
        @JsonSubTypes(@JsonSubTypes.Type(value = ExternalPayload.class, name = "payload"))
        public ExternalPayload item;
    }

    static class WithSharedExternalAlias {
        @JsonProperty(required = true)
        @JsonAlias("shared")
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = "kind")
        @JsonSubTypes(@JsonSubTypes.Type(value = ExternalPayload.class, name = "payload"))
        public ExternalPayload first;

        @JsonProperty(required = true)
        @JsonAlias("shared")
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = "kind")
        @JsonSubTypes(@JsonSubTypes.Type(value = ExternalPayload.class, name = "payload"))
        public ExternalPayload second;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes(@JsonSubTypes.Type(value = Subtype.class, name = "sub"))
    abstract static class Base { }

    static class Subtype extends Base {
        @JsonProperty(required = true)
        public String first;

        @JsonProperty(required = true)
        public String last;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY,
            property = "type")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = HiddenIdSubtype.class, name = "sub"),
        @JsonSubTypes.Type(value = SubtypeWithChild.class, name = "withChild")
    })
    abstract static class BaseWithHiddenId { }

    static class HiddenIdSubtype extends BaseWithHiddenId {
        @JsonProperty(required = true)
        public String type;

        @JsonProperty(required = true)
        public String name;
    }

    static class SubtypeWithChild extends BaseWithHiddenId {
        @JsonView(InternalView.class)
        public RequiredTypeChild child;
    }

    static class RequiredTypeChild {
        @JsonView(InternalView.class)
        @JsonProperty(required = true)
        public String type;
    }

    static class PublicView { }
    static class InternalView extends PublicView { }

    static class WithView {
        @JsonView(InternalView.class)
        @JsonProperty(required = true)
        public String internal;
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
    static class WithGeneratedId {
        public int count;
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    static class WithRequiredId {
        @JsonProperty(required = true)
        public int id;

        public int count;
    }

    static class WithoutRequiredProperties {
        public int count;
        JsonToken creatorToken;
    }

    private final JsonMapper MAPPER = newJsonMapper();

    private ObjectReader checking(Class<?> type) {
        return MAPPER.readerFor(type).with(DeserializationFeature.FAIL_ON_MISSING_REQUIRED_PROPERTIES);
    }

    @Test
    void missingFieldAndSetterRequireOptIn() {
        ObjectReader reader = MAPPER.readerFor(RequiredBean.class);
        RequiredBean withoutCheck = reader.readValue("{}");
        assertEquals("initial", withoutCheck.name);
        assertEquals(7, withoutCheck.count);

        ObjectReader checked = reader.with(DeserializationFeature.FAIL_ON_MISSING_REQUIRED_PROPERTIES);
        assertMissing(checked, """
                {"count":2}
                """, "name");
        assertMissing(checked, """
                {"name":"report"}
                """, "count");
        assertEquals("initial", ((RequiredBean) reader.readValue("{}")).name);
    }

    @Test
    void explicitNullAndNullsSkipCountAsPresent() {
        RequiredBean result = checking(RequiredBean.class).readValue("""
                {"name":null,"count":null}
                """);
        assertNull(result.name);
        assertEquals(7, result.count);
        assertNull(result.optional);
    }

    @Test
    void aliasesAndCaseInsensitiveNamesCountAsPresent() {
        ObjectReader reader = jsonMapperBuilder()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_MISSING_REQUIRED_PROPERTIES)
                .build()
                .readerFor(RequiredBean.class);
        RequiredBean result = reader.readValue("""
                {"LABEL":"report","COUNT":4}
                """);
        assertEquals("report", result.name);
        assertEquals(4, result.count);
    }

    @Test
    void nestedNameDoesNotCountForEnclosingObject() {
        ObjectReader reader = checking(WithNestedName.class);
        assertMissing(reader, """
                {"child":{"name":"nested"}}
                """, "name");
        WithNestedName value = reader.readValue("""
                {"ignored":{"name":"ignored"},"child":[{"name":"nested"}],"name":"outer"}
                """);
        assertEquals("outer", value.name);
        assertEquals("nested", value.child.get(0).get("name").asString());
    }

    @Test
    void checksBuilderBeforeCallingBuild() {
        assertMissing(checking(Result.class), "{}", "name");
        Result value = checking(Result.class).readValue("""
                {"name":"report"}
                """);
        assertEquals("report", value.name);
    }

    @Test
    void builderUsesRequiredMarkerFromMatchingValueProperty() {
        ObjectReader reader = MAPPER.readerFor(ValueWithRequiredName.class);
        ValueWithRequiredName unchecked = reader.readValue("{}");
        assertNull(unchecked.name);

        ObjectReader checked = reader.with(DeserializationFeature.FAIL_ON_MISSING_REQUIRED_PROPERTIES);
        assertMissing(checked, "{}", "name");
        ValueWithRequiredName value = checked.readValue("""
                {"label":"report"}
                """);
        assertEquals("report", value.name);
        assertNull(value.optional);
        assertEquals("computed", value.derived);
    }

    @Test
    void builderUsesValueMarkerAfterUnwrappingRenamesProperty() {
        ObjectReader reader = checking(UnwrappedBuilderValue.class);
        assertMissing(reader, """
                {"value_optional":"present"}
                """, "value_name");
        UnwrappedBuilderValue value = reader.readValue("""
                {"value_name":"report"}
                """);
        assertEquals("report", value.value.name);
    }

    @Test
    void builderCreatorUsesValueMarkerBeforeConstruction() {
        ObjectReader unchecked = MAPPER.readerFor(CreatorBuilderValue.class);
        ObjectReader checked = checking(CreatorBuilderValue.class);
        CreatorBuilderValue empty = new CreatorBuilderValue(null, null);
        assertEquals(empty, unchecked.readValue("{}"));
        assertMissingCreator(checked, "{}", "name");
        assertEquals(new CreatorBuilderValue("report", null),
                checked.readValue("{\"label\":\"report\"}"));
        assertEquals(empty, checked.readValue("{\"name\":null}"));
        assertEquals(empty, checked.withView(PublicView.class).readValue("{}"));
        assertEquals(empty, unchecked.readValue("{}"));
    }

    @Test
    void positionalBuilderCreatorUsesValueMarker() {
        ObjectReader reader = checking(PositionalCreatorBuilderValue.class);
        assertMissingCreator(reader, "{\"value\":[]}", "name");
        PositionalCreatorBuilderValue value = reader.readValue("{\"value\":[\"report\"]}");
        assertEquals(new CreatorBuilderValue("report", null), value.value);
    }

    @Test
    void unwrappedBuilderCreatorUsesValueMarker() {
        ObjectReader reader = checking(UnwrappedCreatorBuilderValue.class);
        assertMissingCreator(reader, "{\"value_optional\":\"present\"}", "value_name");
        UnwrappedCreatorBuilderValue value = reader.readValue("{\"value_name\":\"report\"}");
        assertEquals(new CreatorBuilderValue("report", null), value.value);
    }

    @Test
    void requiresSlotsInArrayShapedBeanAndBuilder() {
        ObjectReader beanReader = checking(PositionalBean.class);
        assertMissing(beanReader, "[7]", "name");
        PositionalBean bean = beanReader.readValue("[7,null]");
        assertEquals(7, bean.count);
        assertNull(bean.name);
        assertEquals(7, ((PositionalBean) beanReader.readValue(MAPPER.readTree("[7,null]"))).count);

        ObjectReader builderReader = checking(PositionalValue.class);
        assertMissing(builderReader, "[7]", "name");
        assertEquals(new PositionalValue(7, null), builderReader.readValue("[7,null]"));
    }

    @Test
    void countsOnlyArraySlotsExposedByParser() {
        TokenFilter secondValue = new TokenFilter() {
            @Override
            public TokenFilter includeElement(int index) {
                return index == 1 ? INCLUDE_ALL : null;
            }
        };
        try (JsonParser p = new FilteringParserDelegate(MAPPER.createParser("[99,7]"),
                secondValue, TokenFilter.Inclusion.INCLUDE_ALL_AND_PATH, true)) {
            MismatchedInputException e = assertThrows(MismatchedInputException.class,
                    () -> checking(PositionalBean.class).readValue(p));
            assertEquals("name", e.getPath().get(0).getPropertyName());
        }
    }

    @Test
    void requiresAnExternallyTypedValue() {
        ObjectReader reader = checking(WithExternalPayload.class);
        assertMissing(reader, "{}", "item");
        assertMissing(reader, """
                {"oldItem":{"name":"report"}}
                """, "item");

        WithExternalPayload value = reader.readValue("""
                {"item":{"name":"report"},"kind":"payload"}
                """);
        assertEquals("report", value.item.name);
        WithExternalPayload explicitNull = reader.readValue("""
                {"kind":"payload","item":null}
                """);
        assertNull(explicitNull.item);
    }

    @Test
    void countsSharedExternalAliasForBothProperties() {
        WithSharedExternalAlias value = checking(WithSharedExternalAlias.class).readValue("""
                {"kind":"payload","shared":{"name":"report"}}
                """);
        assertEquals("report", value.first.name);
        assertEquals("report", value.second.name);
    }

    @Test
    void creatorChecksAreIndependentOfRegularProperties() {
        assertMissing(checking(WithCreator.class), """
                {"id":"1"}
                """, "value");
        WithCreator bean = checking(WithCreator.class).readValue("""
                {"value":"report","id":"1"}
                """);
        assertEquals("1", bean.id);
        assertEquals("report", bean.value);

        MismatchedInputException e = assertThrows(MismatchedInputException.class,
                () -> MAPPER.readValue("""
                        {"value":"report"}
                        """, WithCreator.class));
        verifyException(e, "Missing required creator property 'id'");
    }

    @Test
    void checksPropertiesOnBothSidesOfTypeId() {
        ObjectReader reader = checking(Base.class);
        Subtype bean = reader.readValue("""
                {"first":"before","type":"sub","last":"after"}
                """);
        assertEquals("before", bean.first);
        assertEquals("after", bean.last);
        assertMissing(reader, """
                {"first":"before","type":"sub"}
                """, "last");
    }

    @Test
    void requiredTypePropertyCountsWhenConsumedForSubtypeSelection() {
        ObjectReader reader = checking(BaseWithHiddenId.class);
        HiddenIdSubtype bean = reader.readValue("""
                {"name":"report","type":"sub"}
                """);
        assertEquals("report", bean.name);
        assertNull(bean.type);

        assertMissing(reader, """
                {"type":"sub"}
                """, "name");
    }

    @Test
    void consumedParentTypeIdDoesNotCountForChildObject() {
        assertMissing(checking(BaseWithHiddenId.class).withView(InternalView.class), """
                {"type":"withChild","child":{}}
                """, "type");
    }

    @Test
    void onlyRequiresPropertiesVisibleInActiveView() {
        ObjectReader checked = checking(WithView.class);
        WithView value = checked.withView(PublicView.class).readValue("{}");
        assertNull(value.internal);
        assertMissing(checked.withView(InternalView.class), "{}", "internal");
    }

    @Test
    void existingObjectCanBeUpdatedWithoutRequiredProperties() {
        RequiredBean original = new RequiredBean();
        RequiredBean updated = MAPPER.readerForUpdating(original)
                .with(DeserializationFeature.FAIL_ON_MISSING_REQUIRED_PROPERTIES)
                .readValue("""
                        {"count":4}
                        """);
        assertSame(original, updated);
        assertEquals("initial", updated.name);
        assertEquals(4, updated.count);
    }

    @Test
    void checksPropertiesWhenCreatingNonStaticInnerClass() {
        ObjectReader reader = checking(OuterWithInner.class);
        assertMissing(reader, "{\"inner\":{}}", "name");
        OuterWithInner value = reader.readValue("{\"inner\":{\"name\":\"report\"}}");
        assertEquals("report", value.inner.name);

        OuterWithInner unchecked = MAPPER.readValue("{\"inner\":{}}", OuterWithInner.class);
        assertEquals("initial", unchecked.inner.name);
        OuterWithInner.Inner updated = MAPPER.readerForUpdating(value.inner)
                .with(DeserializationFeature.FAIL_ON_MISSING_REQUIRED_PROPERTIES).readValue("{}");
        assertSame(value.inner, updated);
        assertEquals("report", updated.name);
    }

    @Test
    void checksPositionsWhenCreatingNonStaticInnerClass() {
        ObjectReader reader = checking(OuterWithInner.class);
        assertMissing(reader, "{\"positional\":[]}", "name");
        OuterWithInner value = reader.readValue("{\"positional\":[\"report\"]}");
        assertEquals("report", value.positional.name);
    }

    @Test
    void objectIdUsesRequiredSettingFromDeclaredProperty() {
        WithGeneratedId generated = checking(WithGeneratedId.class).readValue("""
                {"count":2}
                """);
        assertEquals(2, generated.count);

        ObjectReader reader = checking(WithRequiredId.class);
        assertMissing(reader, """
                {"count":2}
                """, "id");
        WithRequiredId declared = reader.readValue("""
                {"count":2,"id":9}
                """);
        assertEquals(9, declared.id);
    }

    @Test
    void optInPreservesParserPositionForDefaultCreator() {
        SimpleModule module = new SimpleModule();
        module.addValueInstantiator(WithoutRequiredProperties.class,
                new ValueInstantiator.Base(WithoutRequiredProperties.class) {
                    @Override
                    public boolean canCreateUsingDefault() { return true; }

                    @Override
                    public Object createUsingDefault(DeserializationContext ctxt) {
                        WithoutRequiredProperties value = new WithoutRequiredProperties();
                        value.creatorToken = ctxt.getParser().currentToken();
                        return value;
                    }
                });
        JsonMapper mapper = jsonMapperBuilder().addModule(module)
                .enable(DeserializationFeature.FAIL_ON_MISSING_REQUIRED_PROPERTIES).build();
        WithoutRequiredProperties value = mapper.readValue("""
                {"count":7}
                """, WithoutRequiredProperties.class);
        assertEquals(JsonToken.START_OBJECT, value.creatorToken);
        assertEquals(7, value.count);
    }

    private void assertMissing(ObjectReader reader, String input, String name) {
        MismatchedInputException e = assertThrows(MismatchedInputException.class,
                () -> reader.readValue(input));
        verifyException(e, "Missing required property '" + name + "'");
        assertEquals(name, e.getPath().get(e.getPath().size() - 1).getPropertyName());
    }

    private void assertMissingCreator(ObjectReader reader, String input, String name) {
        MismatchedInputException e = assertThrows(MismatchedInputException.class,
                () -> reader.readValue(input));
        verifyException(e, "Missing required creator property '" + name + "'");
    }
}
