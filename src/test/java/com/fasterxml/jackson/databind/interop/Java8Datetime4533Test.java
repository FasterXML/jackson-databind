package com.fasterxml.jackson.databind.interop;

import java.time.LocalDate;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public class Java8Datetime4533Test
        extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    private final ObjectMapper LENIENT_MAPPER = JsonMapper.builder()
            .disable(MapperFeature.REQUIRE_HANDLERS_FOR_JAVA8_TIMES)
            .disable(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS)
            .build();

    @Test
    public void testPreventSerialization() throws Exception
    {
        // [databind#4533]: prevent accidental serialization of Java 8 date/time types
        // as POJOs, without Java 8 date/time module:
        _testPreventSerialization(java.time.LocalDateTime.now());
        _testPreventSerialization(java.time.LocalDate.now());
        _testPreventSerialization(java.time.LocalTime.now());
        _testPreventSerialization(java.time.OffsetDateTime.now());
        _testPreventSerialization(java.time.ZonedDateTime.now());
    }

    private void _testPreventSerialization(Object value) throws Exception
    {
        try {
            String json = MAPPER.writeValueAsString(value);
            fail("Should not pass, wrote out as\n: "+json);
        } catch (com.fasterxml.jackson.databind.exc.InvalidDefinitionException e) {
            verifyException(e, "Java 8 date/time type `"+value.getClass().getName()
                    +"` not supported by default");
            verifyException(e, "add Module \"com.fasterxml.jackson.datatype:jackson-datatype-jsr310\"");
            verifyException(e, "(or disable `MapperFeature.%s`)",
                    MapperFeature.REQUIRE_HANDLERS_FOR_JAVA8_TIMES.name());
        }
    }

    @Test
    public void testPreventDeserialization() throws Exception
    {
        // [databind#4533]: prevent accidental deserialization of Java 8 date/time types
        // as POJOs, without Java 8 date/time module:
        _testPreventDeserialization(java.time.LocalDateTime.class);
        _testPreventDeserialization(java.time.LocalDate.class);
        _testPreventDeserialization(java.time.LocalTime.class);
        _testPreventDeserialization(java.time.OffsetDateTime.class);
        _testPreventDeserialization(java.time.ZonedDateTime.class);
    }

    private void _testPreventDeserialization(Class<?> value) throws Exception
    {
        try {
            Object result = MAPPER.readValue(" 0 ", value);
            fail("Not expecting to pass, resulted in: "+result);
        } catch (com.fasterxml.jackson.databind.exc.InvalidDefinitionException e) {
            verifyException(e, "Java 8 date/time type `"+value.getName()
                    +"` not supported by default");
            verifyException(e, "add Module \"com.fasterxml.jackson.datatype:jackson-datatype-jsr310\"");
            verifyException(e, "(or disable `MapperFeature.%s`)",
                    MapperFeature.REQUIRE_HANDLERS_FOR_JAVA8_TIMES.name());
        }
    }

    @Test
    public void testLenientSerailization() throws Exception
    {
        _testLenientSerialization(java.time.LocalDateTime.now());
        _testLenientSerialization(java.time.LocalDate.now());
        _testLenientSerialization(java.time.LocalTime.now());
        _testLenientSerialization(java.time.OffsetDateTime.now());

        // Except ZonedDateTime serialization fails with ...
        try {
            _testLenientSerialization(java.time.ZonedDateTime.now());
            fail("Should not pass, wrote out as\n: "+java.time.ZonedDateTime.now());
        } catch (JsonMappingException e) {
            verifyException(e, "Class com.fasterxml.jackson.databind.ser.BeanPropertyWriter");
            verifyException(e, "with modifiers \"public\"");
        }
    }

    private void _testLenientSerialization(Object value) throws Exception
    {
        String json = LENIENT_MAPPER.writeValueAsString(value);
        assertThat(json).isNotNull();
    }

    @Test
    public void testAllowDeserializationWithFeature() throws Exception
    {
        _testAllowDeserializationLenient(java.time.LocalDateTime.class);
        _testAllowDeserializationLenient(java.time.LocalDate.class);
        _testAllowDeserializationLenient(java.time.LocalTime.class);
        _testAllowDeserializationLenient(java.time.OffsetDateTime.class);
        _testAllowDeserializationLenient(java.time.ZonedDateTime.class);
    }

    private void _testAllowDeserializationLenient(Class<?> target) throws Exception {
        JacksonException e = assertThrows(JacksonException.class, () ->
                LENIENT_MAPPER.readValue("{}", target));
        Assertions.assertThat(e).hasMessageContaining("Cannot construct instance of `"+target.getName()+"`");
    }

}
