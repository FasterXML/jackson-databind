package tools.jackson.databind.ext.jdk8;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
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

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

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

    public void testExcludeEmptyStringViaOptional() throws Exception
    {
        String json = MAPPER.writeValueAsString(new OptionalNonEmptyStringBean("x"));
        assertEquals("{\"value\":\"x\"}", json);
        json = MAPPER.writeValueAsString(new OptionalNonEmptyStringBean(null));
        assertEquals("{}", json);
        json = MAPPER.writeValueAsString(new OptionalNonEmptyStringBean(""));
        assertEquals("{}", json);
    }

    public void testSerPropInclusionAlways() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultPropertyInclusion(incl ->
                    JsonInclude.Value.construct(JsonInclude.Include.NON_ABSENT, JsonInclude.Include.ALWAYS))
                .build();
        assertEquals("{\"myData\":true}",
                mapper.writeValueAsString(OptionalGenericData.construct(Boolean.TRUE)));
    }

    public void testSerPropInclusionNonNull() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder().changeDefaultPropertyInclusion(
                    i -> JsonInclude.Value.construct(JsonInclude.Include.NON_ABSENT, JsonInclude.Include.NON_NULL))
                .build();
        assertEquals("{\"myData\":true}",
                mapper.writeValueAsString(OptionalGenericData.construct(Boolean.TRUE)));
    }

    public void testSerPropInclusionNonAbsent() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultPropertyInclusion(
                        i -> JsonInclude.Value.construct(JsonInclude.Include.NON_ABSENT, JsonInclude.Include.NON_ABSENT))
                .build();
        assertEquals("{\"myData\":true}",
                mapper.writeValueAsString(OptionalGenericData.construct(Boolean.TRUE)));
    }

    public void testSerPropInclusionNonEmpty() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultPropertyInclusion(
                        i -> JsonInclude.Value.construct(JsonInclude.Include.NON_ABSENT, JsonInclude.Include.NON_EMPTY))
                .build();
        assertEquals("{\"myData\":true}",
                mapper.writeValueAsString(OptionalGenericData.construct(Boolean.TRUE)));
    }
}
