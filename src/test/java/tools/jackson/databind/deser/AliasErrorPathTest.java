package tools.jackson.databind.deser;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.util.JsonParserDelegate;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that the path of an exception for a failing property value
 * uses the property name as found in input (which may be an alias), and not
 * the canonical name, regardless of the processing path taken.
 */
public class AliasErrorPathTest extends DatabindTestUtil
{
    static class Point {
        @JsonAlias("xx")
        @JsonView(Object.class)
        public int x;
    }

    static class AliasedBean {
        @JsonAlias("alt")
        @JsonView(Object.class)
        public int value;

        @JsonAlias("pt")
        @JsonView(Object.class)
        public Point point;
    }

    static class AliasedCreatorBean {
        final int value;

        @JsonCreator
        AliasedCreatorBean(@JsonProperty("value") @JsonAlias("alt") int value) {
            this.value = value;
        }
    }

    static class CreatorWithAliasedProperty {
        @JsonAlias("alt")
        public int value;

        final int id;

        @JsonCreator
        CreatorWithAliasedProperty(@JsonProperty("id") int id) {
            this.id = id;
        }
    }

    record AliasedRecord(@JsonAlias("alt") int value) { }

    @JsonDeserialize(builder = AliasedBuilderValue.Builder.class)
    static class AliasedBuilderValue {
        final int value;

        AliasedBuilderValue(int value) {
            this.value = value;
        }

        static class Builder {
            int value;

            @JsonAlias("alt")
            @JsonView(Object.class)
            public Builder withValue(int v) {
                value = v;
                return this;
            }

            public AliasedBuilderValue build() {
                return new AliasedBuilderValue(value);
            }
        }
    }

    // No views, used with `DEFAULT_VIEW_INCLUSION` enabled, for "vanilla" processing
    static class VanillaPoint {
        @JsonAlias("xx")
        public int x;
    }

    static class VanillaBean {
        @JsonAlias("a1") public int a;
        @JsonAlias("b1") public int b;
        @JsonAlias("c1") public int c;
        @JsonAlias("d1") public int d;
        @JsonAlias("e1") public int e;
        @JsonAlias("pt") public VanillaPoint point;
    }

    @JsonDeserialize(builder = VanillaBuilderValue.Builder.class)
    static class VanillaBuilderValue {
        static class Builder {
            @JsonAlias("alt")
            public Builder withValue(int v) {
                return this;
            }

            public VanillaBuilderValue build() {
                return new VanillaBuilderValue();
            }
        }
    }

    static class Inner {
        public int inner;
    }

    static class UnwrappedBean {
        @JsonAlias("alt")
        public int value;

        @JsonUnwrapped
        public Inner unwrapped;
    }

    static class UnwrappedCreatorBean {
        @JsonAlias("alt")
        public int value;

        @JsonUnwrapped
        public Inner unwrapped;

        @JsonCreator
        UnwrappedCreatorBean(@JsonProperty("id") int id) { }
    }

    static class ExternalIdBean {
        @JsonAlias("alt")
        public int value;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
        @JsonSubTypes(@JsonSubTypes.Type(value = Inner.class, name = "inner"))
        public Object ext;
    }

    static class NestedCreatorBean {
        @JsonCreator
        NestedCreatorBean(@JsonProperty("point") @JsonAlias("pt") VanillaPoint point) { }
    }

    @JsonDeserialize(builder = CreatorBuilderValue.Builder.class)
    static class CreatorBuilderValue {
        static class Builder {
            @JsonCreator
            Builder(@JsonProperty("id") int id) { }

            @JsonAlias("alt")
            public Builder withValue(int v) {
                return this;
            }

            public CreatorBuilderValue build() {
                return new CreatorBuilderValue();
            }
        }
    }

    @JsonDeserialize(builder = UnwrappedBuilderValue.Builder.class)
    static class UnwrappedBuilderValue {
        static class Builder {
            @JsonAlias("alt")
            public Builder withValue(int v) {
                return this;
            }

            @JsonUnwrapped
            public Builder withUnwrapped(Inner v) {
                return this;
            }

            public UnwrappedBuilderValue build() {
                return new UnwrappedBuilderValue();
            }
        }
    }

    @JsonDeserialize(builder = UnwrappedCreatorBuilderValue.Builder.class)
    static class UnwrappedCreatorBuilderValue {
        static class Builder {
            @JsonCreator
            Builder(@JsonProperty("id") int id) { }

            @JsonAlias("alt")
            public Builder withValue(int v) {
                return this;
            }

            @JsonUnwrapped
            public Builder withUnwrapped(Inner v) {
                return this;
            }

            public UnwrappedCreatorBuilderValue build() {
                return new UnwrappedCreatorBuilderValue();
            }
        }
    }

    @JsonDeserialize(builder = ExternalIdBuilderValue.Builder.class)
    static class ExternalIdBuilderValue {
        static class Builder {
            @JsonAlias("alt")
            public Builder withValue(int v) {
                return this;
            }

            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                    include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
            @JsonSubTypes(@JsonSubTypes.Type(value = Inner.class, name = "inner"))
            public Builder withExt(Object v) {
                return this;
            }

            public ExternalIdBuilderValue build() {
                return new ExternalIdBuilderValue();
            }
        }
    }

    // Parser that does not expose parent's name at START_OBJECT/START_ARRAY,
    // same as Avro backend
    static class NoNameAtStartParser extends JsonParserDelegate {
        NoNameAtStartParser(JsonParser p) {
            super(p);
        }

        @Override
        public String currentName() {
            JsonToken t = currentToken();
            return (t == JsonToken.START_OBJECT || t == JsonToken.START_ARRAY)
                    ? null : super.currentName();
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    private final ObjectMapper VANILLA_MAPPER = jsonMapperBuilder()
            .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .build();

    private final String BAD_ALIASED = """
            {"alt":"notAnInt"}
            """;

    private final String BAD_NESTED_ALIASED = """
            {"pt":{"xx":"notAnInt"}}
            """;

    @Test
    void beanDefault() throws Exception
    {
        _verifyPath(MAPPER.readerFor(AliasedBean.class), BAD_ALIASED, "alt");
        _verifyPath(MAPPER.readerFor(AliasedBean.class), BAD_NESTED_ALIASED, "pt", "xx");
    }

    // Each of the unrolled loop's positions, plus the one after that
    @Test
    void beanVanilla() throws Exception
    {
        ObjectReader r = VANILLA_MAPPER.readerFor(VanillaBean.class);
        _verifyPath(r, """
                {"a1":"bad"}
                """, "a1");
        _verifyPath(r, """
                {"a1":1,"b1":"bad"}
                """, "b1");
        _verifyPath(r, """
                {"a1":1,"b1":2,"c1":"bad"}
                """, "c1");
        _verifyPath(r, """
                {"a1":1,"b1":2,"c1":3,"d1":"bad"}
                """, "d1");
        _verifyPath(r, """
                {"a1":1,"b1":2,"c1":3,"d1":4,"e1":"bad"}
                """, "e1");
        _verifyPath(r, BAD_NESTED_ALIASED, "pt", "xx");
    }

    // Separate loop when parser already points to the first property name
    @Test
    void beanVanillaFromPropertyName() throws Exception
    {
        ObjectReader r = VANILLA_MAPPER.readerFor(VanillaBean.class);
        _verifyPathFromPropertyName(r, """
                {"a1":"bad"}
                """, "a1");
        _verifyPathFromPropertyName(r, """
                {"a1":1,"b1":"bad"}
                """, "b1");
        _verifyPathFromPropertyName(r, """
                {"a1":1,"b1":2,"c1":"bad"}
                """, "c1");
    }

    @Test
    void beanVanillaAfterUnknown() throws Exception
    {
        ObjectReader r = VANILLA_MAPPER.readerFor(VanillaBean.class)
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        _verifyPath(r, """
                {"unknown":0,"a1":"bad"}
                """, "a1");
    }

    @Test
    void beanUpdateVanilla() throws Exception
    {
        _verifyPath(VANILLA_MAPPER.readerForUpdating(new VanillaBean()), """
                {"a1":"bad"}
                """, "a1");
    }

    @Test
    void beanWithView() throws Exception
    {
        ObjectReader r = MAPPER.readerWithView(Object.class).forType(AliasedBean.class);
        _verifyPath(r, BAD_ALIASED, "alt");
        _verifyPath(r, BAD_NESTED_ALIASED, "pt", "xx");
    }

    @Test
    void beanForUpdate() throws Exception
    {
        ObjectReader r = MAPPER.readerForUpdating(new AliasedBean());
        _verifyPath(r, BAD_ALIASED, "alt");
        _verifyPath(r, BAD_NESTED_ALIASED, "pt", "xx");
    }

    @Test
    void beanFromTree() throws Exception
    {
        InvalidFormatException e = assertThrows(InvalidFormatException.class,
                () -> MAPPER.treeToValue(MAPPER.readTree(BAD_NESTED_ALIASED), AliasedBean.class));
        assertEquals(List.of("pt", "xx"), _path(e));
    }

    @Test
    void creatorProperty() throws Exception
    {
        _verifyPath(MAPPER.readerFor(AliasedCreatorBean.class), BAD_ALIASED, "alt");
    }

    // Non-creator property seen before creator is complete, so value gets buffered
    @Test
    void propertyBufferedForCreator() throws Exception
    {
        _verifyPath(MAPPER.readerFor(CreatorWithAliasedProperty.class), """
                {"alt":"notAnInt","id":1}
                """, "alt");
    }

    // Non-creator property seen after creator is complete
    @Test
    void propertyAfterCreator() throws Exception
    {
        _verifyPath(MAPPER.readerFor(CreatorWithAliasedProperty.class), """
                {"id":1,"alt":"notAnInt"}
                """, "alt");
    }

    @Test
    void unwrapped() throws Exception
    {
        _verifyPath(MAPPER.readerFor(UnwrappedBean.class), BAD_ALIASED, "alt");
    }

    @Test
    void unwrappedForUpdate() throws Exception
    {
        _verifyPath(MAPPER.readerForUpdating(new UnwrappedBean()), BAD_ALIASED, "alt");
    }

    @Test
    void unwrappedAfterCreator() throws Exception
    {
        _verifyPath(MAPPER.readerFor(UnwrappedCreatorBean.class), """
                {"id":1,"alt":"notAnInt"}
                """, "alt");
    }

    @Test
    void externalTypeId() throws Exception
    {
        _verifyPath(MAPPER.readerFor(ExternalIdBean.class), BAD_ALIASED, "alt");
    }

    @Test
    void caseInsensitiveMatch() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                .build();
        _verifyPath(mapper.readerFor(AliasedBean.class), """
                {"VALUE":"notAnInt"}
                """, "VALUE");
    }

    @Test
    void recordProperty() throws Exception
    {
        _verifyPath(MAPPER.readerFor(AliasedRecord.class), BAD_ALIASED, "alt");
    }

    @Test
    void builderDefault() throws Exception
    {
        _verifyPath(MAPPER.readerFor(AliasedBuilderValue.class), BAD_ALIASED, "alt");
    }

    @Test
    void builderWithView() throws Exception
    {
        _verifyPath(MAPPER.readerWithView(Object.class).forType(AliasedBuilderValue.class),
                BAD_ALIASED, "alt");
    }

    @Test
    void builderVanilla() throws Exception
    {
        _verifyPath(VANILLA_MAPPER.readerFor(VanillaBuilderValue.class), BAD_ALIASED, "alt");
    }

    @Test
    void builderAfterCreator() throws Exception
    {
        _verifyPath(MAPPER.readerFor(CreatorBuilderValue.class), """
                {"id":1,"alt":"notAnInt"}
                """, "alt");
    }

    @Test
    void builderUnwrapped() throws Exception
    {
        _verifyPath(MAPPER.readerFor(UnwrappedBuilderValue.class), BAD_ALIASED, "alt");
    }

    @Test
    void builderUnwrappedAfterCreator() throws Exception
    {
        _verifyPath(MAPPER.readerFor(UnwrappedCreatorBuilderValue.class), """
                {"id":1,"alt":"notAnInt"}
                """, "alt");
    }

    @Test
    void builderExternalTypeId() throws Exception
    {
        _verifyPath(MAPPER.readerFor(ExternalIdBuilderValue.class), BAD_ALIASED, "alt");
    }

    // Name of a property with Object value unknown: must use logical name
    // (not empty String), but Creator properties do know name from before value
    @Test
    void parserWithoutNameAtStartObject() throws Exception
    {
        _verifyPathNoNameAtStart(MAPPER.readerFor(AliasedBean.class), "point", "xx");
        _verifyPathNoNameAtStart(VANILLA_MAPPER.readerFor(VanillaBean.class), "point", "xx");
        _verifyPathNoNameAtStart(VANILLA_MAPPER.readerForUpdating(new VanillaBean()), "point", "xx");
        _verifyPathNoNameAtStart(MAPPER.readerFor(NestedCreatorBean.class), "pt", "xx");
    }

    private void _verifyPathNoNameAtStart(ObjectReader r, String... expPath) throws Exception
    {
        try (JsonParser p = new NoNameAtStartParser(MAPPER.createParser(BAD_NESTED_ALIASED))) {
            InvalidFormatException e = assertThrows(InvalidFormatException.class,
                    () -> r.readValue(p));
            assertEquals(List.of(expPath), _path(e));
        }
    }

    private void _verifyPathFromPropertyName(ObjectReader r, String json, String... expPath)
        throws Exception
    {
        try (JsonParser p = MAPPER.createParser(json)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            InvalidFormatException e = assertThrows(InvalidFormatException.class,
                    () -> r.readValue(p));
            assertEquals(List.of(expPath), _path(e));
        }
    }

    private void _verifyPath(ObjectReader r, String json, String... expPath)
    {
        InvalidFormatException e = assertThrows(InvalidFormatException.class,
                () -> r.readValue(json));
        assertEquals(List.of(expPath), _path(e));
    }

    private List<String> _path(DatabindException e) {
        return e.getPath().stream()
                .map(ref -> ref.getPropertyName())
                .toList();
    }
}
