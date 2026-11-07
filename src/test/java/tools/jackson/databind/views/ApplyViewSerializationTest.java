package tools.jackson.databind.views;

import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonApplyView;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonView;

import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;
import tools.jackson.databind.ser.std.SimpleFilterProvider;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

//[databind#5745] : allow overriding JsonView

/**
 * Unit tests for verifying JSON apply view functionality.
 */
public class ApplyViewSerializationTest extends DatabindTestUtil
{
    // Classes that represent views
    static class ViewA { }
    static class ViewAA extends ViewA { }
    static class ViewB { }

    static class Bean
    {
        @JsonView(ViewA.class)
        public String a = "1";

        @JsonView({ViewAA.class, ViewB.class})
        public String aa = "2";

        @JsonView(ViewB.class)
        public String getB() { return "3"; }
    }

    static class Bean2 {

        @JsonView(ViewA.class)
        @JsonApplyView(ViewB.class)
        public Bean beanWithApplyViewB = new Bean();

        @JsonView(ViewA.class)
        @JsonApplyView(JsonApplyView.NONE.class)
        public Bean beanWithApplyNoneView = new Bean();
    }

    static class Bean3 {

        @JsonView(ViewA.class)
        public Bean bean = new Bean();

        @JsonView(ViewA.class)
        public Bean2 bean2 = new Bean2();

        @JsonView(ViewA.class)
        @JsonApplyView(ViewB.class)
        public Bean2 bean2WithApplyViewB = new Bean2();
    }

    // [databind#5745]: @JsonApplyView on an @JsonUnwrapped property, which is
    // serialized by UnwrappingBeanPropertyWriter instead of BeanPropertyWriter
    static class UnwrappedBean {
        @JsonView(ViewA.class)
        @JsonApplyView(ViewB.class)
        @JsonUnwrapped
        public Bean bean = new Bean();
    }

    static class UnwrappedNoneBean {
        @JsonView(ViewA.class)
        @JsonApplyView(JsonApplyView.NONE.class)
        @JsonUnwrapped
        public Bean bean = new Bean();
    }

    // [databind#5745]: @JsonApplyView on an @JsonAnyGetter accessor, which is
    // serialized by AnyGetterWriter
    static class AnyGetterBean {
        @JsonApplyView(ViewB.class)
        @JsonAnyGetter
        public Map<String, Bean> extra() {
            return Collections.singletonMap("bean", new Bean());
        }
    }

    @JsonFilter("anyFilter")
    static class FilteredAnyGetterBean {
        @JsonApplyView(ViewB.class)
        @JsonAnyGetter
        public Map<String, Bean> extra() {
            return Collections.singletonMap("bean", new Bean());
        }
    }

    // Used to verify that an inner @JsonApplyView overrides an outer one:
    // outer writer activates ViewB; this field's @JsonView(ViewB) lets it through,
    // and its @JsonApplyView(ViewA) switches the active view to ViewA for the
    // nested Bean2 — without which both Bean2 fields (each @JsonView(ViewA))
    // would be filtered out by ViewB.
    static class Bean4 {

        @JsonView(ViewB.class)
        @JsonApplyView(ViewA.class)
        public Bean2 wrapped = new Bean2();
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    // Ensure `MapperFeature.DEFAULT_VIEW_INCLUSION` is enabled
    // (its default differs b/w Jackson 2.x and 3.x)
    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .build();

    // Mapper using the 3.x default (DEFAULT_VIEW_INCLUSION disabled), used to
    // confirm @JsonApplyView semantics do not depend on this feature flag.
    private final ObjectMapper MAPPER_NO_DEFAULT_INCLUSION = jsonMapperBuilder()
            .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .build();

    @SuppressWarnings("unchecked")
    @Test
    public void testJsonApplyView() {
        // With "ViewA" active: both Bean2 properties are @JsonView(ViewA), so both
        // serialize, but each applies its own override view to the nested Bean.
        Bean2 bean2 = new Bean2();

        StringWriter sw = new StringWriter();
        MAPPER.writerWithView(ViewA.class).writeValue(sw, bean2);

        Map<?,?> bean2Map = MAPPER.readValue(sw.toString(), Map.class);
        assertEquals(2, bean2Map.size());

        Map<String,Object> beanWithApplyViewBMap = (Map<String, Object>) bean2Map.get("beanWithApplyViewB");
        assertEquals(2, beanWithApplyViewBMap.size());
        assertFalse(beanWithApplyViewBMap.containsKey("a"));
        assertEquals("2", beanWithApplyViewBMap.get("aa"));
        assertEquals("3", beanWithApplyViewBMap.get("b"));

        Map<String,Object> beanWithApplyNoneViewMap = (Map<String, Object>) bean2Map.get("beanWithApplyNoneView");
        assertEquals(3, beanWithApplyNoneViewMap.size());
        assertEquals("1", beanWithApplyNoneViewMap.get("a"));
        assertEquals("2", beanWithApplyNoneViewMap.get("aa"));
        assertEquals("3", beanWithApplyNoneViewMap.get("b"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testJsonApplyViewNested() {
        // With "ViewAA" active (extends ViewA): all three Bean3 properties are
        // @JsonView(ViewA), so all serialize; verify nested @JsonApplyView still applies.
        Bean3 bean3 = new Bean3();

        StringWriter sw = new StringWriter();
        MAPPER.writerWithView(ViewAA.class).writeValue(sw, bean3);

        Map<?,?> bean3Map = MAPPER.readValue(sw.toString(), Map.class);
        assertEquals(3, bean3Map.size());

        Map<String,Object> beanMap = (Map<String, Object>) bean3Map.get("bean");
        assertEquals(2, beanMap.size());
        assertEquals("1", beanMap.get("a"));
        assertEquals("2", beanMap.get("aa"));
        assertFalse(beanMap.containsKey("b"));

        Map<String,Object> bean2Map = (Map<String, Object>) bean3Map.get("bean2");
        assertEquals(2, bean2Map.size());
        Map<String,Object> nestedBeanWithApplyViewBMap = (Map<String, Object>) bean2Map.get("beanWithApplyViewB");
        assertEquals(2, nestedBeanWithApplyViewBMap.size());
        assertFalse(nestedBeanWithApplyViewBMap.containsKey("a"));
        assertEquals("2", nestedBeanWithApplyViewBMap.get("aa"));
        assertEquals("3", nestedBeanWithApplyViewBMap.get("b"));

        // NONE applyView reached via nested Bean2: all three Bean properties present
        Map<String,Object> nestedBeanWithApplyNoneViewMap = (Map<String, Object>) bean2Map.get("beanWithApplyNoneView");
        assertEquals(3, nestedBeanWithApplyNoneViewMap.size());
        assertEquals("1", nestedBeanWithApplyNoneViewMap.get("a"));
        assertEquals("2", nestedBeanWithApplyNoneViewMap.get("aa"));
        assertEquals("3", nestedBeanWithApplyNoneViewMap.get("b"));

        Map<?,?> bean2WithApplyViewBMap = (Map<?,?>) bean3Map.get("bean2WithApplyViewB");
        assertEquals(0, bean2WithApplyViewBMap.size());
    }

    // Verifies an inner @JsonApplyView overrides an outer one: writer is ViewB,
    // Bean4.wrapped switches the active view to ViewA, then Bean2's own
    // @JsonApplyView re-fires for the deepest Bean.
    @SuppressWarnings("unchecked")
    @Test
    public void testJsonApplyViewOverridesOuter() {
        Bean4 bean4 = new Bean4();

        StringWriter sw = new StringWriter();
        MAPPER.writerWithView(ViewB.class).writeValue(sw, bean4);

        Map<?,?> bean4Map = MAPPER.readValue(sw.toString(), Map.class);
        assertEquals(1, bean4Map.size());

        Map<String,Object> wrappedMap = (Map<String, Object>) bean4Map.get("wrapped");
        // Both Bean2 fields are @JsonView(ViewA): they only survive because Bean4
        // applied ViewA, not the writer's ViewB.
        assertEquals(2, wrappedMap.size());

        // Inner @JsonApplyView(ViewB) wins over outer ViewA for the deepest Bean
        Map<String,Object> deepWithApplyB = (Map<String, Object>) wrappedMap.get("beanWithApplyViewB");
        assertEquals(2, deepWithApplyB.size());
        assertFalse(deepWithApplyB.containsKey("a"));
        assertEquals("2", deepWithApplyB.get("aa"));
        assertEquals("3", deepWithApplyB.get("b"));

        // Inner @JsonApplyView(NONE) wins over outer ViewA: all three properties
        Map<String,Object> deepWithApplyNone = (Map<String, Object>) wrappedMap.get("beanWithApplyNoneView");
        assertEquals(3, deepWithApplyNone.size());
        assertEquals("1", deepWithApplyNone.get("a"));
        assertEquals("2", deepWithApplyNone.get("aa"));
        assertEquals("3", deepWithApplyNone.get("b"));
    }

    // [databind#5745]: @JsonApplyView was silently ignored for @JsonUnwrapped
    // properties (UnwrappingBeanPropertyWriter), so ViewA-only properties of the
    // unwrapped Bean leaked although ViewB was applied
    @Test
    public void jsonApplyViewOnUnwrapped() {
        StringWriter sw = new StringWriter();
        MAPPER.writerWithView(ViewA.class).writeValue(sw, new UnwrappedBean());

        Map<?,?> map = MAPPER.readValue(sw.toString(), Map.class);
        // Bean serialized under ViewB: "aa" and "b" visible, "a" (ViewA-only) not
        assertEquals(2, map.size());
        assertFalse(map.containsKey("a"));
        assertEquals("2", map.get("aa"));
        assertEquals("3", map.get("b"));
    }

    @Test
    public void jsonApplyViewNoneOnUnwrapped() {
        StringWriter sw = new StringWriter();
        MAPPER.writerWithView(ViewA.class).writeValue(sw, new UnwrappedNoneBean());

        Map<?,?> map = MAPPER.readValue(sw.toString(), Map.class);
        // NONE disables view processing for the unwrapped Bean: all 3 properties
        assertEquals(3, map.size());
        assertEquals("1", map.get("a"));
        assertEquals("2", map.get("aa"));
        assertEquals("3", map.get("b"));
    }

    // [databind#5745]: @JsonApplyView was silently ignored for @JsonAnyGetter
    // accessors (AnyGetterWriter), on both the plain and the filtered path
    @Test
    public void jsonApplyViewOnAnyGetter() {
        StringWriter sw = new StringWriter();
        MAPPER.writerWithView(ViewA.class).writeValue(sw, new AnyGetterBean());

        Map<?,?> map = MAPPER.readValue(sw.toString(), Map.class);
        assertEquals(1, map.size());
        Map<?,?> beanMap = (Map<?,?>) map.get("bean");
        // entry value serialized under ViewB: "aa" and "b" visible, "a" not
        assertEquals(2, beanMap.size());
        assertFalse(beanMap.containsKey("a"));
        assertEquals("2", beanMap.get("aa"));
        assertEquals("3", beanMap.get("b"));
    }

    @Test
    public void jsonApplyViewOnFilteredAnyGetter() {
        StringWriter sw = new StringWriter();
        MAPPER.writer(new SimpleFilterProvider()
                        .addFilter("anyFilter", SimpleBeanPropertyFilter.serializeAll()))
                .withView(ViewA.class)
                .writeValue(sw, new FilteredAnyGetterBean());

        Map<?,?> map = MAPPER.readValue(sw.toString(), Map.class);
        assertEquals(1, map.size());
        Map<?,?> beanMap = (Map<?,?>) map.get("bean");
        assertEquals(2, beanMap.size());
        assertFalse(beanMap.containsKey("a"));
        assertEquals("2", beanMap.get("aa"));
        assertEquals("3", beanMap.get("b"));
    }

    // Same scenario as testJsonApplyView but with DEFAULT_VIEW_INCLUSION disabled
    // (3.x default). All test properties are explicitly @JsonView-annotated, so
    // the feature must not affect outcomes; in particular NONE still yields all 3.
    @SuppressWarnings("unchecked")
    @Test
    public void testJsonApplyViewWithoutDefaultInclusion() {
        Bean2 bean2 = new Bean2();

        StringWriter sw = new StringWriter();
        MAPPER_NO_DEFAULT_INCLUSION.writerWithView(ViewA.class).writeValue(sw, bean2);

        Map<?,?> bean2Map = MAPPER_NO_DEFAULT_INCLUSION.readValue(sw.toString(), Map.class);
        assertEquals(2, bean2Map.size());

        Map<String,Object> beanWithApplyViewBMap = (Map<String, Object>) bean2Map.get("beanWithApplyViewB");
        assertEquals(2, beanWithApplyViewBMap.size());
        assertFalse(beanWithApplyViewBMap.containsKey("a"));
        assertEquals("2", beanWithApplyViewBMap.get("aa"));
        assertEquals("3", beanWithApplyViewBMap.get("b"));

        Map<String,Object> beanWithApplyNoneViewMap = (Map<String, Object>) bean2Map.get("beanWithApplyNoneView");
        assertEquals(3, beanWithApplyNoneViewMap.size());
        assertEquals("1", beanWithApplyNoneViewMap.get("a"));
        assertEquals("2", beanWithApplyNoneViewMap.get("aa"));
        assertEquals("3", beanWithApplyNoneViewMap.get("b"));
    }
}
