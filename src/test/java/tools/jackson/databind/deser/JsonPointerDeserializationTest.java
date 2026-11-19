package tools.jackson.databind.deser;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPointer;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonPointerDeserializationTest
{
    private final ObjectMapper MAPPER = JsonMapper.builder().build();

    static class FieldBean {
        public String name;

        @JsonPointer("/employee/details/departmentId")
        public Integer departmentId = 99;
    }

    static class SetterBean {
        private String value;

        @JsonPointer("/data/a~1b/c~0d")
        public void setValue(String value) {
            this.value = value;
        }
    }

    static class InvalidPointerBean {
        @JsonPointer("not-a-pointer")
        public String value;
    }

    @Test
    public void testPointerBoundField() throws Exception {
        FieldBean bean = MAPPER.readValue(
                "{\"name\":\"Bob\",\"employee\":{\"details\":{\"departmentId\":123}}}",
                FieldBean.class);

        assertEquals("Bob", bean.name);
        assertEquals(123, bean.departmentId);
    }

    @Test
    public void testPointerEscapingOnSetter() throws Exception {
        SetterBean bean = MAPPER.readValue(
                "{\"data\":{\"a/b\":{\"c~d\":\"found\"}}}", SetterBean.class);

        assertEquals("found", bean.value);
    }

    @Test
    public void testMissingPointerLeavesPropertyAbsent() throws Exception {
        FieldBean bean = MAPPER.readValue("{\"name\":\"Bob\"}", FieldBean.class);

        assertEquals(99, bean.departmentId);
    }

    @Test
    public void testExplicitNullIsBound() throws Exception {
        FieldBean bean = MAPPER.readValue(
                "{\"employee\":{\"details\":{\"departmentId\":null}}}", FieldBean.class);

        assertNull(bean.departmentId);
    }

    @Test
    public void testPointerBoundFieldWhenUpdating() throws Exception {
        FieldBean bean = new FieldBean();
        bean.name = "before";

        FieldBean updated = MAPPER.readerForUpdating(bean).readValue(
                "{\"name\":\"after\",\"employee\":{\"details\":{\"departmentId\":123}}}");

        assertEquals("after", updated.name);
        assertEquals(123, updated.departmentId);
    }

    @Test
    public void testInvalidPointerReportsDefinitionProblem() {
        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class,
                () -> MAPPER.readValue("{}", InvalidPointerBean.class));

        assertTrue(e.getMessage().contains("Invalid @JsonPointer value 'not-a-pointer'"));
    }

    @Test
    public void testPointerPropertyFailureHasPropertyPath() {
        InvalidFormatException e = assertThrows(InvalidFormatException.class,
                () -> MAPPER.readValue(
                        "{\"employee\":{\"details\":{\"departmentId\":\"not-an-integer\"}}}",
                        FieldBean.class));

        assertEquals("departmentId", e.getPath().get(0).getPropertyName());
    }

    @Test
    public void testUnrelatedUnknownPropertyStillFails() {
        assertThrows(UnrecognizedPropertyException.class,
                () -> MAPPER.readerFor(FieldBean.class)
                        .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                        .readValue("{\"unknown\":true}"));
    }
}
