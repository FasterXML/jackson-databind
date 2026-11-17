package tools.jackson.databind.ext.jdk8;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class OptionalnclusionTest
    extends DatabindTestUtil
{
    @JsonAutoDetect(fieldVisibility=Visibility.ANY)
    public static final class OptionalData {
        public Optional<String> myString = Optional.empty();
    }

    // for [datatype-jdk8#18]
    static class OptionalNonEmptyStringBean {
        @JsonInclude(value=Include.NON_EMPTY, content=Include.NON_EMPTY)
        public Optional<String> value;

        public OptionalNonEmptyStringBean() { }
        OptionalNonEmptyStringBean(String str) {
            value = Optional.ofNullable(str);
        }
    }

    public static final class OptionalGenericData<T> {
        public Optional<T> myData;
        public static <T> OptionalGenericData<T> construct(T data) {
            OptionalGenericData<T> ret = new OptionalGenericData<T>();
            ret.myData = Optional.of(data);
            return ret;
        }
    }

    // Optional-wrapped empty Collections/Maps: value inclusion only
    // considers the Optional itself, not its contents
    @JsonInclude(Include.NON_EMPTY)
    record OptionalContainersValueOnly(Optional<List<String>> list,
            Optional<Set<String>> set, Optional<Map<String, String>> map,
            Optional<String> str) { }

    @JsonInclude(value = Include.NON_EMPTY, content = Include.NON_EMPTY)
    record OptionalContainersWithContent(Optional<List<String>> list,
            Optional<Set<String>> set, Optional<Map<String, String>> map,
            Optional<String> str) { }

    record OptionalContainers(Optional<List<String>> list,
            Optional<Set<String>> set, Optional<Map<String, String>> map,
            Optional<String> str) { }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testSerOptNonEmpty() throws Exception
    {
        OptionalData data = new OptionalData();
        data.myString = null;
        String value = jsonMapperBuilder()
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_EMPTY))
                .build()
                .writeValueAsString(data);
        assertEquals("{}", value);
    }

    @Test
    public void testSerOptNonDefault() throws Exception
    {
        OptionalData data = new OptionalData();
        data.myString = null;
        String value = jsonMapperBuilder()
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_DEFAULT))
                .build()
                .writeValueAsString(data);
        assertEquals("{}", value);
    }

    @Test
    public void testSerOptNonAbsent() throws Exception
    {
        OptionalData data = new OptionalData();
        data.myString = null;
        String value = jsonMapperBuilder()
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_ABSENT))
                .build()
                .writeValueAsString(data);
        assertEquals("{}", value);
    }

    @Test
    public void testExcludeEmptyStringViaOptional() throws Exception
    {
        String json = MAPPER.writeValueAsString(new OptionalNonEmptyStringBean("x"));
        assertEquals("{\"value\":\"x\"}", json);
        json = MAPPER.writeValueAsString(new OptionalNonEmptyStringBean(null));
        assertEquals("{}", json);
        json = MAPPER.writeValueAsString(new OptionalNonEmptyStringBean(""));
        assertEquals("{}", json);
    }

    @Test
    public void testSerPropInclusionAlways() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultPropertyInclusion(incl ->
                    JsonInclude.Value.construct(JsonInclude.Include.NON_ABSENT, JsonInclude.Include.ALWAYS))
                .build();
        assertEquals("{\"myData\":true}",
                mapper.writeValueAsString(OptionalGenericData.construct(Boolean.TRUE)));
    }

    @Test
    public void testSerPropInclusionNonNull() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder().changeDefaultPropertyInclusion(
                    i -> JsonInclude.Value.construct(JsonInclude.Include.NON_ABSENT, JsonInclude.Include.NON_NULL))
                .build();
        assertEquals("{\"myData\":true}",
                mapper.writeValueAsString(OptionalGenericData.construct(Boolean.TRUE)));
    }

    @Test
    public void testSerPropInclusionNonAbsent() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultPropertyInclusion(
                        i -> JsonInclude.Value.construct(JsonInclude.Include.NON_ABSENT, JsonInclude.Include.NON_ABSENT))
                .build();
        assertEquals("{\"myData\":true}",
                mapper.writeValueAsString(OptionalGenericData.construct(Boolean.TRUE)));
    }

    @Test
    public void testSerPropInclusionNonEmpty() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultPropertyInclusion(
                        i -> JsonInclude.Value.construct(JsonInclude.Include.NON_ABSENT, JsonInclude.Include.NON_EMPTY))
                .build();
        assertEquals("{\"myData\":true}",
                mapper.writeValueAsString(OptionalGenericData.construct(Boolean.TRUE)));
    }

    // Value inclusion of NON_EMPTY alone only excludes absent Optionals;
    // Optionals with empty contents are still written
    @Test
    public void nonEmptyValueOnlyKeepsOptionalOfEmptyContainers() throws Exception
    {
        assertEquals("""
                {"list":[],"set":[],"map":{},"str":""}""",
                MAPPER.writeValueAsString(new OptionalContainersValueOnly(
                        Optional.of(List.of()), Optional.of(Set.of()),
                        Optional.of(Map.of()), Optional.of(""))));
        assertEquals("{}",
                MAPPER.writeValueAsString(new OptionalContainersValueOnly(
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())));
    }

    // Content inclusion of NON_EMPTY also excludes Optionals with empty contents
    @Test
    public void nonEmptyContentExcludesOptionalOfEmptyContainers() throws Exception
    {
        assertEquals("{}",
                MAPPER.writeValueAsString(new OptionalContainersWithContent(
                        Optional.of(List.of()), Optional.of(Set.of()),
                        Optional.of(Map.of()), Optional.of(""))));
        assertEquals("{}",
                MAPPER.writeValueAsString(new OptionalContainersWithContent(
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())));
        assertEquals("""
                {"list":["a"],"set":["b"],"map":{"k":"v"},"str":"x"}""",
                MAPPER.writeValueAsString(new OptionalContainersWithContent(
                        Optional.of(List.of("a")), Optional.of(Set.of("b")),
                        Optional.of(Map.of("k", "v")), Optional.of("x"))));
    }

    // Same as above but via mapper-wide default inclusion
    @Test
    public void defaultNonEmptyContentExcludesOptionalOfEmptyContainers() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultPropertyInclusion(incl -> incl
                        .withValueInclusion(Include.NON_EMPTY)
                        .withContentInclusion(Include.NON_EMPTY))
                .build();
        assertEquals("{}",
                mapper.writeValueAsString(new OptionalContainers(
                        Optional.of(List.of()), Optional.of(Set.of()),
                        Optional.of(Map.of()), Optional.of(""))));
        assertEquals("""
                {"list":["a"],"set":["b"],"map":{"k":"v"},"str":"x"}""",
                mapper.writeValueAsString(new OptionalContainers(
                        Optional.of(List.of("a")), Optional.of(Set.of("b")),
                        Optional.of(Map.of("k", "v")), Optional.of("x"))));
        // but class-level `@JsonInclude` overrides it: `content` defaults to ALWAYS
        assertEquals("""
                {"list":[],"set":[],"map":{},"str":""}""",
                mapper.writeValueAsString(new OptionalContainersValueOnly(
                        Optional.of(List.of()), Optional.of(Set.of()),
                        Optional.of(Map.of()), Optional.of(""))));
    }
}
