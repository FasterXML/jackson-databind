package tools.jackson.databind.jsontype.deftyping;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.core.type.TypeReference;

import tools.jackson.databind.*;
import tools.jackson.databind.jsontype.DefaultBaseTypeLimitingValidator;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.NoCheckSubTypeValidator;

import static org.junit.jupiter.api.Assertions.*;

public class TestDefaultForEnums
    extends DatabindTestUtil
{
    public enum TestEnum {
        A, B;
    }

    static final class EnumHolder
    {
        public Object value; // "untyped"

        public EnumHolder() { }
        public EnumHolder(TestEnum e) { value = e; }
    }

    protected static class TimeUnitBean {
        public TimeUnit timeUnit;
    }

    static class Foo3569<T> {
        public T item;
    }

    enum Bar3569 {
        ENABLED, DISABLED, HIDDEN;

        @JsonCreator
        public static Bar3569 fromValue(String value) {
            String upperVal = value.toUpperCase();
            for (Bar3569 enumValue : Bar3569.values()) {
                if (enumValue.name().equals(upperVal)) {
                    return enumValue;
                }
            }
            throw new IllegalArgumentException("Bad input [" + value + "]");
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper DEFTYPING_MAPPER = jsonMapperBuilder()
            .activateDefaultTyping(NoCheckSubTypeValidator.instance)
            .build();

    @Test
    public void testSimpleEnumBean() throws Exception
    {
        TimeUnitBean bean = new TimeUnitBean();
        bean.timeUnit = TimeUnit.SECONDS;

        // First, without type info
        ObjectMapper m = new ObjectMapper();
        String json = m.writeValueAsString(bean);
        TimeUnitBean result = m.readValue(json, TimeUnitBean.class);
        assertEquals(TimeUnit.SECONDS, result.timeUnit);

        // then with type info
        json = DEFTYPING_MAPPER.writeValueAsString(bean);
        result = DEFTYPING_MAPPER.readValue(json, TimeUnitBean.class);

        assertEquals(TimeUnit.SECONDS, result.timeUnit);
    }

    @Test
    public void testSimpleEnumsInObjectArray() throws Exception
    {
        // Typing is needed for enums
        String json = DEFTYPING_MAPPER.writeValueAsString(new Object[] { TestEnum.A });
        assertEquals("[[\"tools.jackson.databind.jsontype.deftyping.TestDefaultForEnums$TestEnum\",\"A\"]]", json);

        // and let's verify we get it back ok as well:
        Object[] value = DEFTYPING_MAPPER.readValue(json, Object[].class);
        assertEquals(1, value.length);
        assertSame(TestEnum.A, value[0]);
    }

    @Test
    public void testSimpleEnumsAsField() throws Exception
    {
        String json = DEFTYPING_MAPPER.writeValueAsString(new EnumHolder(TestEnum.B));
        assertEquals("{\"value\":[\"tools.jackson.databind.jsontype.deftyping.TestDefaultForEnums$TestEnum\",\"B\"]}", json);
        EnumHolder holder = DEFTYPING_MAPPER.readValue(json, EnumHolder.class);
        assertSame(TestEnum.B, holder.value);
    }

    /**
     * [databind#3569]: Unable to deserialize enum object with default-typed
     * {@link com.fasterxml.jackson.annotation.JsonTypeInfo.As#WRAPPER_ARRAY} and {@link JsonCreator} together,
     */
    @Test
    public void testEnumAsWrapperArrayWithCreator() throws Exception
    {
        ObjectMapper objectMapper = jsonMapperBuilder()
                .activateDefaultTyping(
                        new DefaultBaseTypeLimitingValidator(),
                        DefaultTyping.NON_FINAL_AND_ENUMS,
                        JsonTypeInfo.As.WRAPPER_ARRAY)
                .build();

        Foo3569<Bar3569> expected = new Foo3569<>();
        expected.item = Bar3569.ENABLED;

        // First, serialize
        String serialized = objectMapper.writeValueAsString(expected);

        // Then, deserialize with TypeReference
        assertNotNull(objectMapper.readValue(serialized, new TypeReference<Foo3569<Bar3569>>() {}));
        // And, also try as described in [databind#3569]
        JavaType javaType = objectMapper.getTypeFactory().constructParametricType(Foo3569.class, new Class[]{Bar3569.class});
        assertNotNull(objectMapper.readValue(serialized, javaType));
    }
}
