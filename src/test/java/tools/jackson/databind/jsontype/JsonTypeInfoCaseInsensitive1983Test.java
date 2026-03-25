package tools.jackson.databind.jsontype;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.InvalidTypeIdException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Tests wrt [databind#1983]
public class JsonTypeInfoCaseInsensitive1983Test extends DatabindTestUtil
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
            property = "Operation")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = Equal.class, name = "eq"),
            @JsonSubTypes.Type(value = NotEqual.class, name = "notEq"),
    })
    public static abstract class Filter {
    }

    public static class Equal extends Filter { }

    public static class NotEqual extends Filter { }

    // verify failures when exact matching required:
    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void readMixedCaseSubclass() throws Exception
    {
        final String serialised = "{\"Operation\":\"NoTeQ\"}";

        // first: mismatch with value unless case-sensitivity disabled:
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> MAPPER.readValue(serialised, Filter.class));
        verifyException(e, "Could not resolve type id 'NoTeQ'");

        ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES)
                .build();
        // Type id ("value") mismatch, should work now:
        Filter result = mapper.readValue(serialised, Filter.class);

        assertEquals(NotEqual.class, result.getClass());
    }

    @Test
    void readMixedCasePropertyName() throws Exception
    {
        final String serialised = "{\"oPeRaTioN\":\"notEq\"}";
        // first: mismatch with property name unless case-sensitivity disabled:
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> MAPPER.readValue(serialised, Filter.class));
        verifyException(e, "missing type id property");

        ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                .build();
        // Type property name mismatch (but value match); should work:
        Filter result = mapper.readValue(serialised, Filter.class);

        assertEquals(NotEqual.class, result.getClass());
    }
}
