package tools.jackson.databind.deser.inject;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.exc.MissingInjectableValueExcepion;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;


// [databind#5217]: Multiple injections of same value should work consistently
class JacksonInject5217Test extends DatabindTestUtil
{
    // Case 1: Field & Field - both fields use same injectable type
    static class FieldFieldBean {
        @JacksonInject("id")
        public String field1;

        @JacksonInject("id")
        public String field2;
    }

    // Case 2: Parameter & Parameter - both constructor params use same injectable type
    static class ParamParamBean {
        final String param1;
        final String param2;

        @JsonCreator
        ParamParamBean(
                @JacksonInject("id") @JsonProperty("param1") String param1,
                @JacksonInject("id") @JsonProperty("param2") String param2
        ) {
            this.param1 = param1;
            this.param2 = param2;
        }
    }

    // Case 3: Parameter & Field - constructor param and field use same injectable type
    // When both param and field have same injectable id, the field injection is skipped
    // (per issue #4218) and the value comes from constructor assignment
    static class ParamFieldBean {
        @JacksonInject("id")
        public String field;

        final String param;

        @JsonCreator
        ParamFieldBean(
                @JacksonInject("id") @JsonProperty("param") String param
        ) {
            this.param = param;
            this.field = param; // Constructor assigns param to field
        }
    }

    // Two fields with same default injectable ID (type-based)
    static class TwoFieldsSameTypeDto {
        @JacksonInject
        public String first;

        @JacksonInject
        public String second;
    }

    // Creator param + Field with same ID on DIFFERENT properties
    // When on different properties, both should be injected
    static class CreatorParamFieldBean {
        @JacksonInject("id")
        public String field;

        final String param;

        @JsonCreator
        CreatorParamFieldBean(
                @JacksonInject("id") @JsonProperty("param") String param
        ) {
            this.param = param;
            // Note: field is NOT assigned here to verify injection behavior
        }
    }

    // Different IDs - should inject independently
    static class DifferentIdsBean {
        @JacksonInject("id1")
        public String field1;

        @JacksonInject("id2")
        public String field2;
    }

    // Creator param + DIFFERENT property field share same injectable ID
    // Both should be injected (not under-injection) - P0 test for #4218 fix
    static class CreatorPlusDifferentPropertyFieldBean {
        @JacksonInject("id")
        public String fieldB;  // Different property from creator param

        final String paramA;

        @JsonCreator
        CreatorPlusDifferentPropertyFieldBean(
                @JacksonInject("id") @JsonProperty("paramA") String paramA
        ) {
            this.paramA = paramA;
        }
    }

    // Creator param + SAME property field - should inject only once (#4218)
    static class CreatorSamePropertyFieldBean {
        @JacksonInject("id")
        public String id;  // Same property as creator param!

        @JsonCreator
        CreatorSamePropertyFieldBean(
                @JacksonInject("id") @JsonProperty("id") String id
        ) {
            // When field injection is skipped (same property), constructor must assign
            this.id = id;
        }
    }

    private final InjectableValues INJECTABLES = new InjectableValues.Std()
            .addValue("id", "injectedValue");

    // [databind#5217]: Field & Field - was failing with "Duplicate injectable value" error
    @Test
    void testFieldAndFieldInjection() throws Exception
    {
        ObjectReader reader = newJsonMapper()
                .readerFor(FieldFieldBean.class)
                .with(INJECTABLES);

        FieldFieldBean bean = reader.readValue("{}");
        assertEquals("injectedValue", bean.field1);
        assertEquals("injectedValue", bean.field2);
    }

    // [databind#5217]: Parameter & Parameter - was already working
    @Test
    void testParamAndParamInjection() throws Exception
    {
        ObjectReader reader = newJsonMapper()
                .readerFor(ParamParamBean.class)
                .with(INJECTABLES);

        ParamParamBean bean = reader.readValue("{}");
        assertEquals("injectedValue", bean.param1);
        assertEquals("injectedValue", bean.param2);
    }

    // [databind#5217]: Parameter & Field - was already working
    @Test
    void testParamAndFieldInjection() throws Exception
    {
        ObjectReader reader = newJsonMapper()
                .readerFor(ParamFieldBean.class)
                .with(INJECTABLES);

        ParamFieldBean bean = reader.readValue("{}");
        assertEquals("injectedValue", bean.param);
        assertEquals("injectedValue", bean.field);
    }

    // [databind#4218]: Creator param + field with same ID on DIFFERENT properties
    // When creator param and field are on DIFFERENT properties, both should be injected.
    // The #4218 fix only prevents duplicate injection for the SAME property.
    @Test
    void testCreatorParamDoesNotDuplicateFieldInjection() throws Exception
    {
        ObjectReader reader = newJsonMapper()
                .readerFor(CreatorParamFieldBean.class)
                .with(INJECTABLES);

        CreatorParamFieldBean bean = reader.readValue("{}");
        assertEquals("injectedValue", bean.param);
        // Field is on a DIFFERENT property ("field" vs "param"), so it should also be injected
        assertEquals("injectedValue", bean.field,
                "Field on different property should also be injected");
    }

    // [databind#5217]: Different IDs - existing functionality should work
    @Test
    void testDifferentIdsStillWork() throws Exception
    {
        InjectableValues injectables = new InjectableValues.Std()
                .addValue("id1", "value1")
                .addValue("id2", "value2");

        ObjectReader reader = newJsonMapper()
                .readerFor(DifferentIdsBean.class)
                .with(injectables);

        DifferentIdsBean bean = reader.readValue("{}");
        assertEquals("value1", bean.field1);
        assertEquals("value2", bean.field2);
    }

    // [databind#4218]: P0 test - Creator param + DIFFERENT property field with same ID
    // Both should be injected - verifies the fix doesn't cause under-injection
    @Test
    void testCreatorAndDifferentPropertyFieldBothInjected() throws Exception
    {
        ObjectReader reader = newJsonMapper()
                .readerFor(CreatorPlusDifferentPropertyFieldBean.class)
                .with(INJECTABLES);

        CreatorPlusDifferentPropertyFieldBean bean = reader.readValue("{}");

        // P0: Both should be injected!
        assertEquals("injectedValue", bean.paramA, "Creator param should be injected");
        assertEquals("injectedValue", bean.fieldB, "Different property field should ALSO be injected");
    }

    // [databind#4218]: Creator param + SAME property field - no duplicate injection
    @Test
    void testCreatorParamAndSamePropertyFieldNoDuplicate() throws Exception
    {
        ObjectReader reader = newJsonMapper()
                .readerFor(CreatorSamePropertyFieldBean.class)
                .with(INJECTABLES);

        CreatorSamePropertyFieldBean bean = reader.readValue("{}");
        assertEquals("injectedValue", bean.id, "Value should be injected");
    }

    // [databind#5217]: Core regression test - without InjectableValues configured,
    // field-field and param-param should fail with the SAME exception type,
    // NOT "Duplicate injectable value" error.
    @Test
    void testFieldFieldWithoutInjectableValuesShouldNotFailWithDuplicate() throws Exception
    {
        ObjectReader reader = newJsonMapper().readerFor(FieldFieldBean.class);
        // NO .with(INJECTABLES) - this is the key point

        // Should throw MissingInjectableValueExcepion (same as param-param case)
        MissingInjectableValueExcepion e = assertThrows(MissingInjectableValueExcepion.class,
            () -> reader.readValue("{}"));

        // Must NOT be "Duplicate injectable value" error - that was the bug
        String msg = e.getMessage();
        if (msg != null && msg.contains("Duplicate injectable")) {
            fail("Should not fail with 'Duplicate injectable value' error. Got: " + msg);
        }
    }

    @Test
    void testParamParamWithoutInjectableValues() throws Exception
    {
        ObjectReader reader = newJsonMapper().readerFor(ParamParamBean.class);
        // NO .with(INJECTABLES)

        // Should throw MissingInjectableValueExcepion (same as field-field case)
        MissingInjectableValueExcepion e = assertThrows(MissingInjectableValueExcepion.class,
            () -> reader.readValue("{}"));

        // Should also NOT be "Duplicate injectable value"
        String msg = e.getMessage();
        if (msg != null && msg.contains("Duplicate injectable")) {
            fail("Should not fail with 'Duplicate injectable value' error. Got: " + msg);
        }
    }

    // [databind#5217/#4218]: Record with @JsonProperty rename + @JacksonInject
    // Verifies Rule 3 works correctly even when logical property name differs from field name
    record RecordWithRenamedInject(
            @JacksonInject("id") @JsonProperty("renamed") String original
    ) {}

    @Test
    void testRecordWithRenamedInjectableProperty() throws Exception
    {
        ObjectReader reader = newJsonMapper()
                .readerFor(RecordWithRenamedInject.class)
                .with(INJECTABLES);

        RecordWithRenamedInject bean = reader.readValue("{}");

        // Creator param should be injected via constructor
        assertEquals("injectedValue", bean.original(),
                "Record component should be injected via creator param");
    }

    // Record with renamed injectable + different property field (same ID)
    // Verifies that renaming doesn't break Rule 1 (multiple targets allowed)
    static class RecordPlusDifferentFieldBean {
        @JacksonInject("id")
        public String otherField;

        final String recordValue;

        @JsonCreator
        RecordPlusDifferentFieldBean(
                @JacksonInject("id") @JsonProperty("renamed") String recordValue
        ) {
            this.recordValue = recordValue;
        }
    }

    @Test
    void testRecordRenamedPlusDifferentFieldBothInjected() throws Exception
    {
        ObjectReader reader = newJsonMapper()
                .readerFor(RecordPlusDifferentFieldBean.class)
                .with(INJECTABLES);

        RecordPlusDifferentFieldBean bean = reader.readValue("{}");

        // Both should be injected - different properties with same ID
        assertEquals("injectedValue", bean.recordValue,
                "Renamed creator param should be injected");
        assertEquals("injectedValue", bean.otherField,
                "Different property field should ALSO be injected");
    }

    /*
    /**********************************************************************
    /* New tests per plan: default-id field-field (#5217 original reproduction)
    /**********************************************************************
     */

    // Test 1 — Original reproduction (regression prevention)
    // Default id (= type name) + field-field same type, NO InjectableValues configured
    // Must NOT fail with "Duplicate injectable value" — #5217 inconsistency fix
    @Test
    void testDefaultIdFieldFieldNoDuplicateError() throws Exception
    {
        ObjectReader reader = newJsonMapper().readerFor(TwoFieldsSameTypeDto.class);
        // InjectableValues not configured — intentional

        MissingInjectableValueExcepion ex = assertThrows(
            MissingInjectableValueExcepion.class,
            () -> reader.readValue("{}")
        );

        String msg = ex.getMessage();
        assertNotNull(msg);
        // Key: must NOT be "Duplicate injectable value" — that was the #5217 bug
        assertFalse(msg.contains("Duplicate injectable value"),
            "Should not get duplicate injectable error but got: " + msg);
    }

    // Test 2 — Behavior verification: both fields injected with same value
    @Test
    void testDefaultIdFieldFieldBothInjected() throws Exception
    {
        // Register for both possible default-id formats (Class object and class name string)
        InjectableValues injectables = new InjectableValues.Std()
            .addValue(String.class, "INJECTED")
            .addValue(String.class.getName(), "INJECTED");

        ObjectReader reader = newJsonMapper()
            .readerFor(TwoFieldsSameTypeDto.class)
            .with(injectables);

        TwoFieldsSameTypeDto result = reader.readValue("{}");

        assertEquals("INJECTED", result.first);
        assertEquals("INJECTED", result.second);
    }

    // Test 3 — API shape: findAllInjectables() returns multiple members for same default id
    @Test
    void testFindAllInjectablesMultipleMembersForDefaultId() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        BeanDescription desc = ObjectMapperTestAccess.beanDescriptionForDeser(mapper, TwoFieldsSameTypeDto.class);

        Map<Object, List<AnnotatedMember>> all = desc.findAllInjectables();
        assertFalse(all.isEmpty(), "Should have at least one injectable entry");

        // Key: at least one ID with 2+ members (format-independent count check)
        boolean foundMultiple = all.values().stream()
            .anyMatch(members -> members.size() >= 2);
        assertTrue(foundMultiple, "Expected at least one ID with 2+ members");
    }
}
