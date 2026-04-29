package tools.jackson.databind.views;

import com.fasterxml.jackson.annotation.JsonApplyView;
import com.fasterxml.jackson.annotation.JsonView;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import java.io.StringWriter;
import java.util.Map;

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
    static class ViewBB extends ViewB { }

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
        @JsonApplyView
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

    @Test
    public void testJsonApplyView() {
        // Then with "ViewA", just one property
        Bean2 bean2 = new Bean2();

        StringWriter sw = new StringWriter();
        MAPPER.writerWithView(ViewA.class).writeValue(sw, bean2);

        Map<String,Object> bean2Map = MAPPER.readValue(sw.toString(), Map.class);
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

    @Test
    public void testJsonApplyViewNested() {
        // Then with "ViewAA", just three properties
        Bean3 bean3 = new Bean3();

        StringWriter sw = new StringWriter();
        MAPPER.writerWithView(ViewAA.class).writeValue(sw, bean3);

        Map<String,Object> bean3Map = MAPPER.readValue(sw.toString(), Map.class);
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

        Map<String,Object> bean2WithApplyViewBMap = (Map<String, Object>) bean3Map.get("bean2WithApplyViewB");
        assertEquals(0, bean2WithApplyViewBMap.size());
    }

}
