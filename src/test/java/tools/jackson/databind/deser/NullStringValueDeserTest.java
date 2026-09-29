package tools.jackson.databind.deser;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.util.JsonParserDelegate;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that a parser reporting {@code VALUE_STRING} but returning
 * {@code null} as its text (like Ion does for corrupt content, e.g. a SYMBOL
 * whose text cannot be resolved) results in a {@link MismatchedInputException}
 * and not a {@link NullPointerException}.
 */
public class NullStringValueDeserTest extends DatabindTestUtil
{
    enum ABC { A, B, C }

    static class IntBean {
        public int x;
    }

    static class DateBean {
        @JsonFormat(pattern = "yyyy-MM-dd")
        public Date date;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_ARRAY)
    @JsonSubTypes(@JsonSubTypes.Type(value = Impl.class, name = "impl"))
    static class Base { }

    static class Impl extends Base { }

    // Mimics Ion parser behavior for corrupt content (e.g. a SYMBOL whose text
    // cannot be resolved): reports VALUE_STRING but returns null text
    static class NullStringParser extends JsonParserDelegate
    {
        public NullStringParser(JsonParser p) {
            super(p);
        }

        @Override
        public String getString() {
            return hasToken(JsonToken.VALUE_STRING) ? null : delegate.getString();
        }

        @Override
        public String getValueAsString() {
            return hasToken(JsonToken.VALUE_STRING) ? null : delegate.getValueAsString();
        }

        @Override
        public String getValueAsString(String defaultValue) {
            return hasToken(JsonToken.VALUE_STRING) ? null : delegate.getValueAsString(defaultValue);
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    private final ObjectMapper WRAPPING_MAPPER = jsonMapperBuilder()
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .build();

    // Containers

    @Test
    public void nullStringAsMap() throws Exception {
        _verifyFails(MAPPER, Map.class);
    }

    @Test
    public void nullStringAsEnumMap() throws Exception {
        _verifyFails(MAPPER, MAPPER.getTypeFactory().constructMapType(EnumMap.class, ABC.class, String.class));
    }

    @Test
    public void nullStringAsList() throws Exception {
        _verifyFails(MAPPER, List.class);
    }

    @Test
    public void nullStringAsIntegerList() throws Exception {
        _verifyFails(MAPPER, MAPPER.getTypeFactory().constructCollectionType(List.class, Integer.class));
    }

    // Scalars

    @Test
    public void nullStringAsNumbers() throws Exception {
        for (Class<?> type : new Class<?>[] {
                Boolean.TYPE, Boolean.class,
                Byte.TYPE, Byte.class, Short.TYPE, Short.class,
                Integer.TYPE, Integer.class, Long.TYPE, Long.class,
                Float.TYPE, Float.class, Double.TYPE, Double.class,
                Character.TYPE, Character.class, Number.class,
                BigInteger.class, BigDecimal.class
        }) {
            _verifyFails(MAPPER, type);
        }
    }

    @Test
    public void nullStringAsIntProperty() throws Exception {
        _verifyFails(MAPPER, MAPPER.constructType(IntBean.class), "{'x':'abc'}");
    }

    @Test
    public void nullStringAsDate() throws Exception {
        _verifyFails(MAPPER, Date.class);
        _verifyFails(MAPPER, MAPPER.constructType(DateBean.class), "{'date':'abc'}");
    }

    @Test
    public void nullStringAsEnum() throws Exception {
        _verifyFails(MAPPER, ABC.class);
        _verifyFails(MAPPER, MAPPER.getTypeFactory().constructCollectionType(EnumSet.class, ABC.class),
                "['abc']");
    }

    @Test
    public void nullStringAsPath() throws Exception {
        _verifyFails(MAPPER, Path.class);
    }

    // Arrays

    @Test
    public void nullStringAsCharArrayElement() throws Exception {
        _verifyFails(MAPPER, MAPPER.constructType(char[].class), "['a','b']");
    }

    @Test
    public void nullStringAsSingleValueArray() throws Exception {
        _verifyFails(WRAPPING_MAPPER, int[].class);
        _verifyFails(WRAPPING_MAPPER, Integer[].class);
    }

    @Test
    public void nullStringAsSingleValueNullableContainers() throws Exception {
        // String (and untyped) values may be null, so these bind null element
        assertArrayEquals(new String[] { null },
                (String[]) _read(WRAPPING_MAPPER, WRAPPING_MAPPER.constructType(String[].class), "'abc'"));
        assertArrayEquals(new Object[] { null },
                (Object[]) _read(WRAPPING_MAPPER, WRAPPING_MAPPER.constructType(Object[].class), "'abc'"));
        assertEquals(Collections.singletonList(null),
                _read(WRAPPING_MAPPER, WRAPPING_MAPPER.getTypeFactory()
                        .constructCollectionType(List.class, String.class), "'abc'"));
    }

    // Polymorphic

    @Test
    public void nullStringAsWrapperArrayTypeId() throws Exception {
        _verifyFails(MAPPER, MAPPER.constructType(Base.class), "['impl',{}]");
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private void _verifyFails(ObjectMapper mapper, Class<?> type) throws Exception {
        _verifyFails(mapper, mapper.constructType(type));
    }

    private void _verifyFails(ObjectMapper mapper, JavaType type) throws Exception {
        _verifyFails(mapper, type, "'abc'");
    }

    private void _verifyFails(ObjectMapper mapper, JavaType type, String json) throws Exception {
        try {
            _read(mapper, type, json);
            fail("Should not pass, got result for " + type);
        } catch (MismatchedInputException e) {
            // expected
        }
    }

    private Object _read(ObjectMapper mapper, JavaType type, String json) throws Exception {
        try (JsonParser p = new NullStringParser(mapper.createParser(a2q(json)))) {
            return mapper.readValue(p, type);
        }
    }
}
