package tools.jackson.databind.deser;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

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

    private final ObjectMapper MAPPER = newJsonMapper();

    private final String BAD_ALIASED = """
            {"alt":"notAnInt"}
            """;

    private final String BAD_NESTED_ALIASED = """
            {"pt":{"xx":"notAnInt"}}
            """;

    @Test
    void beanVanilla() throws Exception
    {
        _verifyPath(MAPPER.readerFor(AliasedBean.class), BAD_ALIASED, "alt");
        _verifyPath(MAPPER.readerFor(AliasedBean.class), BAD_NESTED_ALIASED, "pt", "xx");
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

    @Test
    void recordProperty() throws Exception
    {
        _verifyPath(MAPPER.readerFor(AliasedRecord.class), BAD_ALIASED, "alt");
    }

    @Test
    void builderVanilla() throws Exception
    {
        _verifyPath(MAPPER.readerFor(AliasedBuilderValue.class), BAD_ALIASED, "alt");
    }

    @Test
    void builderWithView() throws Exception
    {
        _verifyPath(MAPPER.readerWithView(Object.class).forType(AliasedBuilderValue.class),
                BAD_ALIASED, "alt");
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
