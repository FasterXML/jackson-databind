package tools.jackson.databind.jsontype;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#3194]: Discrepancy between Type Id inclusion on serialization vs
// expectation during deserialization of 2D arrays of final types when using
// DefaultTyping.NON_FINAL
class PolymorphicArrays3194Test extends DatabindTestUtil {
    static final class SomeBean {
        public Object[][] value;
    }

    private final ObjectMapper MAPPER = JsonMapper
            .builder()
            .activateDefaultTyping(
                    BasicPolymorphicTypeValidator.builder()
                            .allowIfSubTypeIsArray()
                            .allowIfSubType(Object.class)
                            .build(),
                    DefaultTyping.NON_FINAL)
            .build();

    // Case 1: Object[][] containing String[] elements (explicit String[] subarrays)
    @Test
    void twoDimensionalArrayObjectWithStringElements() throws Exception {
        SomeBean instance = new SomeBean();
        instance.value = new Object[][]{new String[]{"1.1", "1.2"}, new String[]{"2.1", "2.2"}};
        String json = MAPPER.writeValueAsString(instance);

        SomeBean result = MAPPER.readValue(json, SomeBean.class);
        assertEquals(Object[][].class, result.value.getClass());
        assertEquals(String[].class, result.value[0].getClass());
        assertArrayEquals(new String[]{"1.1", "1.2"}, (String[]) result.value[0]);
        assertArrayEquals(new String[]{"2.1", "2.2"}, (String[]) result.value[1]);
    }

    // Case 2: Object[][] containing Object[] elements
    @Test
    void twoDimensionalArrayObjectWithObjectElements() throws Exception {
        SomeBean instance = new SomeBean();
        instance.value = new Object[][]{{"1.1", "1.2"}, {"2.1", "2.2"}};
        String json = MAPPER.writeValueAsString(instance);

        SomeBean result = MAPPER.readValue(json, SomeBean.class);
        assertEquals(Object[][].class, result.value.getClass());
        assertEquals(Object[].class, result.value[0].getClass());
    }

    // Case 3 (the original failing case): String[][] stored in Object[][]
    @Test
    void twoDimensionalStringArrayInObjectField() throws Exception {
        SomeBean instance = new SomeBean();
        instance.value = new String[][]{{"1.1", "1.2"}, {"2.1", "2.2"}};
        String json = MAPPER.writeValueAsString(instance);

        // After the fix, inner arrays should NOT have type IDs since the outer
        // type ID "[[Ljava.lang.String;" already determines element type String[]
        // (which is final).
        SomeBean result = MAPPER.readValue(json, SomeBean.class);
        assertEquals(String[][].class, result.value.getClass());
        assertEquals(String[].class, result.value[0].getClass());
        assertArrayEquals(new String[]{"1.1", "1.2"}, result.value[0]);
        assertArrayEquals(new String[]{"2.1", "2.2"}, result.value[1]);
    }

    // Case 4: Integer[][] stored in Object[][]
    @Test
    void twoDimensionalIntegerArrayInObjectField() throws Exception {
        SomeBean instance = new SomeBean();
        instance.value = new Integer[][]{{1, 2}, {3, 4}};
        String json = MAPPER.writeValueAsString(instance);

        SomeBean result = MAPPER.readValue(json, SomeBean.class);
        assertEquals(Integer[][].class, result.value.getClass());
        assertEquals(Integer[].class, result.value[0].getClass());
        assertArrayEquals(new Integer[]{1, 2}, result.value[0]);
        assertArrayEquals(new Integer[]{3, 4}, result.value[1]);
    }

    // Case 5: handcrafted JSON without inner type IDs (should still work)
    @Test
    void twoDimensionalStringArrayFromHandcraftedJson() throws Exception {
        String handcrafted = "{\"value\":[\"[[Ljava.lang.String;\",[[\"1.1\",\"1.2\"],[\"2.1\",\"2.2\"]]]}";
        SomeBean result = MAPPER.readValue(handcrafted, SomeBean.class);
        assertEquals(String[][].class, result.value.getClass());
        assertEquals(String[].class, result.value[0].getClass());
        assertArrayEquals(new String[]{"1.1", "1.2"}, result.value[0]);
        assertArrayEquals(new String[]{"2.1", "2.2"}, result.value[1]);
    }
}
