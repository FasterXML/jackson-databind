package tools.jackson.databind.deser.inject;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.InjectableValues;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;


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

    // Case 4: Field + Setter (same property, same ID) - should inject via setter only (masking)
    // The field and setter are for the same property, so ideally only one should be injected
    static class FieldSetterBean {
        @JacksonInject("id")
        public String value;

        int setterCallCount = 0;

        @JacksonInject("id")
        public void setValue(String value) {
            this.setterCallCount++;
            this.value = value;
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

    // [databind#5217]: Field + Setter (same property) - setter should mask field
    // This verifies that when both field and setter are annotated with @JacksonInject
    // for the same property, only the setter is used (higher precedence)
    @Test
    void testFieldAndSetterSameProperty() throws Exception
    {
        ObjectReader reader = newJsonMapper()
                .readerFor(FieldSetterBean.class)
                .with(INJECTABLES);

        FieldSetterBean bean = reader.readValue("{}");
        assertEquals("injectedValue", bean.value);
        assertEquals(1, bean.setterCallCount, "Should inject only once via setter");
    }

    // Case 5: Field + Setter with @JsonProperty custom name
    // Verifies that _findPropertyNameForMember correctly handles @JsonProperty
    static class FieldSetterWithJsonPropertyBean {
        @JacksonInject("id")
        @JsonProperty("customName")
        public String field;

        int setterCallCount = 0;

        @JacksonInject("id")
        public void setCustomName(String value) {
            this.setterCallCount++;
            this.field = value;
        }
    }

    // Case 6: Creator param + Field with same ID on DIFFERENT properties
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

    // Case 7: Different IDs - should inject independently
    static class DifferentIdsBean {
        @JacksonInject("id1")
        public String field1;

        @JacksonInject("id2")
        public String field2;
    }

    // Case 8: Creator param + DIFFERENT property field share same injectable ID
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

    // Case 9: Creator param + SAME property field - should inject only once (#4218 핵심)
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

    // Case 10: Two injectable setters mapped to same logical property -> error
    static class TwoSettersSamePropertyBean {
        public String value;

        @JacksonInject("id")
        @JsonProperty("value")
        public void setValue(String v) {
            value = v;
        }

        @JacksonInject("id")
        @JsonProperty("value")
        public void setOtherValue(String v) {
            value = v;
        }
    }

    // [databind#5217]: Field + Setter with @JsonProperty - verifies property name lookup
    @Test
    void testFieldSetterWithJsonProperty() throws Exception
    {
        ObjectReader reader = newJsonMapper()
                .readerFor(FieldSetterWithJsonPropertyBean.class)
                .with(INJECTABLES);

        FieldSetterWithJsonPropertyBean bean = reader.readValue("{}");
        assertEquals("injectedValue", bean.field);
        assertEquals(1, bean.setterCallCount, "Should inject only once via setter");
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

    @Test
    void testTwoInjectableSettersSamePropertyFails() throws Exception
    {
        ObjectReader reader = newJsonMapper()
            .readerFor(TwoSettersSamePropertyBean.class)
            .with(INJECTABLES);

        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class,
            () -> reader.readValue("{}"));
        verifyException(e, "multiple setters");
    }

    // [databind#5217]: Core regression test - without InjectableValues configured,
    // field-field should NOT fail with "Duplicate injectable value" error.
    // This directly verifies the original issue's "Expected behavior: The same error
    // should be made in all cases."
    @Test
    void testFieldFieldWithoutInjectableValuesShouldNotFailWithDuplicate() throws Exception
    {
        ObjectReader reader = newJsonMapper().readerFor(FieldFieldBean.class);
        // NO .with(INJECTABLES) - this is the key point

        Exception e = assertThrows(Exception.class, () -> reader.readValue("{}"));

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

        Exception e = assertThrows(Exception.class, () -> reader.readValue("{}"));

        // Should also NOT be "Duplicate injectable value"
        String msg = e.getMessage();
        if (msg != null && msg.contains("Duplicate injectable")) {
            fail("Should not fail with 'Duplicate injectable value' error. Got: " + msg);
        }
    }
}
