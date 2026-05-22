package tools.jackson.databind.views;

import java.io.*;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for verifying JSON view functionality: ability to declaratively
 * suppress subset of properties from being serialized.
 */
public class ViewSerializationTest extends DatabindTestUtil
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

    /**
     * Bean with mix of explicitly annotated
     * properties, and implicit ones that may or may
     * not be included in views.
     */
    static class MixedBean
    {
        @JsonView(ViewA.class)
        public String a = "1";

        public String getB() { return "2"; }
    }

    /**
     * As indicated by [JACKSON-261], @JsonView should imply
     * that associated element (method, field) is to be considered
     * a property
     */
    static class ImplicitBean {
        @JsonView(ViewA.class)
        private int a = 1;
    }

    static class VisibilityBean {
        @JsonProperty protected String id = "id";

        @JsonView(ViewA.class)
        public String value = "x";
    }

    public static class WebView { }
    public static class OtherView { }
    public static class Foo {
        @JsonView(WebView.class)
        public int getFoo() { return 3; }
    }

    // [databind#5937]
    static class Bean5937 { }

    static class Views
    {
        public interface View { }
        public interface ExtendedView  extends View { }
    }

    static class ComplexTestData
    {
        String nameNull = null;
        String nameComplex = "complexValue";
        String nameComplexHidden = "nameComplexHiddenValue";
        SimpleTestData testData = new SimpleTestData();
        SimpleTestData[] testDataArray = new SimpleTestData[] { new SimpleTestData(), null };

        @JsonView( Views.View.class )
        public String getNameNull() { return nameNull; }
        public void setNameNull( String nameNull ) { this.nameNull = nameNull; }

        @JsonView( Views.View.class )
        public String getNameComplex() { return nameComplex; }
        public void setNameComplex( String nameComplex ) { this.nameComplex = nameComplex; }

        public String getNameComplexHidden() { return nameComplexHidden; }
        public void setNameComplexHidden( String nameComplexHidden ) { this.nameComplexHidden = nameComplexHidden; }

        @JsonView( Views.View.class )
        public SimpleTestData getTestData() { return testData; }
        public void setTestData( SimpleTestData testData ) { this.testData = testData; }

        @JsonView( Views.View.class )
        public SimpleTestData[] getTestDataArray() { return testDataArray; }
        public void setTestDataArray( SimpleTestData[] testDataArray ) { this.testDataArray = testDataArray; }
    }

    static class SimpleTestData
    {
        String name = "shown";
        String nameHidden = "hidden";

        @JsonView( Views.View.class )
        public String getName() { return name; }
        public void setName( String name ) { this.name = name; }

        public String getNameHidden( ) { return nameHidden; }
        public void setNameHidden( String nameHidden ) { this.nameHidden = nameHidden; }
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

    @SuppressWarnings("unchecked")
    @Test
    public void simple() throws Exception
    {
        StringWriter sw = new StringWriter();
        // Ok, first, using no view whatsoever; all 3
        Bean bean = new Bean();
        Map<String,Object> map = writeAndMap(MAPPER, bean);
        assertEquals(3, map.size());

        // Then with "ViewA", just one property
        sw = new StringWriter();
        MAPPER.writerWithView(ViewA.class).writeValue(sw, bean);
        map = MAPPER.readValue(sw.toString(), Map.class);
        assertEquals(1, map.size());
        assertEquals("1", map.get("a"));

        // "ViewAA", 2 properties
        sw = new StringWriter();
        MAPPER.writerWithView(ViewAA.class).writeValue(sw, bean);
        map = MAPPER.readValue(sw.toString(), Map.class);
        assertEquals(2, map.size());
        assertEquals("1", map.get("a"));
        assertEquals("2", map.get("aa"));

        // "ViewB", 2 prop2
        String json = MAPPER.writerWithView(ViewB.class).writeValueAsString(bean);
        map = MAPPER.readValue(json, Map.class);
        assertEquals(2, map.size());
        assertEquals("2", map.get("aa"));
        assertEquals("3", map.get("b"));

        // and "ViewBB", 2 as well
        json = MAPPER.writerWithView(ViewBB.class).writeValueAsString(bean);
        map = MAPPER.readValue(json, Map.class);
        assertEquals(2, map.size());
        assertEquals("2", map.get("aa"));
        assertEquals("3", map.get("b"));

        // and finally, without view.
        json = MAPPER.writerWithView(null).writeValueAsString(bean);
        map = MAPPER.readValue(json, Map.class);
        assertEquals(3, map.size());
    }

    /**
     * Unit test to verify implementation of [JACKSON-232], to
     * allow "opt-in" handling for JSON Views: that is, that
     * default for properties is to exclude unless included in
     * a view.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void defaultExclusion() throws Exception
    {
        MixedBean bean = new MixedBean();

        // default setting: both fields will get included
        String json = MAPPER.writerWithView(ViewA.class).writeValueAsString(bean);
        Map<String,Object> map = MAPPER.readValue(json, Map.class);
        assertEquals(2, map.size());
        assertEquals("1", map.get("a"));
        assertEquals("2", map.get("b"));

        // but can also change (but not necessarily on the fly...)
        ObjectMapper mapper = jsonMapperBuilder()
                .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                .build();

                // with this setting, only explicit inclusions count:
        json = mapper.writerWithView(ViewA.class).writeValueAsString(bean);
        map = mapper.readValue(json, Map.class);
        assertEquals(1, map.size());
        assertEquals("1", map.get("a"));
        assertNull(map.get("b"));

        // but without view, view processing disabled:
        json = mapper.writer().withView(null).writeValueAsString(bean);
        map = mapper.readValue(json, Map.class);
        assertEquals(2, map.size());
        assertEquals("1", map.get("a"));
        assertEquals("2", map.get("b"));
    }

    /**
     * As per [JACKSON-261], @JsonView annotation should imply that associated
     * method/field does indicate a property.
     */
    @Test
    public void implicitAutoDetection() throws Exception
    {
        assertEquals("{\"a\":1}",
                MAPPER.writeValueAsString(new ImplicitBean()));
    }

    @Test
    public void visibility() throws Exception
    {
        VisibilityBean bean = new VisibilityBean();
        // Without view setting, should only see "id"
        String json = MAPPER.writerWithView(Object.class).writeValueAsString(bean);
        //json = mapper.writeValueAsString(bean);
        assertEquals("{\"id\":\"id\"}", json);
    }

    // [JACKSON-868]
    @Test
    public void issue868() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_DEFAULT))
                .build();
        assertEquals("{}",
                mapper.writerWithView(OtherView.class).writeValueAsString(new Foo()));
    }

    // [databind#5937]
    @Test
    public void withActiveView() throws Exception
    {
        final Class<?>[] insideView = new Class<?>[1];
        final Class<?>[] afterView = new Class<?>[1];

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule()
                        .addSerializer(Bean5937.class, new StdSerializer<Bean5937>(Bean5937.class) {
                            @Override
                            public void serialize(Bean5937 value, JsonGenerator g,
                                    SerializationContext ctxt) {
                                ctxt.withActiveView(ViewA.class, () -> {
                                    insideView[0] = ctxt.getActiveView();
                                });
                                afterView[0] = ctxt.getActiveView();
                                g.writeStartObject();
                                g.writeEndObject();
                            }
                        }))
                .build();

        // No initial view: inside == ViewA, after reverts to null
        mapper.writeValueAsString(new Bean5937());
        assertSame(ViewA.class, insideView[0]);
        assertNull(afterView[0]);

        // With initial view ViewB: inside == ViewA, after reverts to ViewB
        mapper.writerWithView(ViewB.class).writeValueAsString(new Bean5937());
        assertSame(ViewA.class, insideView[0]);
        assertSame(ViewB.class, afterView[0]);
    }

    // [databind#5937]: active view must be reverted even if callback throws
    @Test
    public void withActiveViewRevertsOnThrow() throws Exception
    {
        final Class<?>[] afterView = new Class<?>[1];
        final RuntimeException boom = new RuntimeException("boom");

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule()
                        .addSerializer(Bean5937.class, new StdSerializer<Bean5937>(Bean5937.class) {
                            @Override
                            public void serialize(Bean5937 value, JsonGenerator g,
                                    SerializationContext ctxt) {
                                try {
                                    ctxt.withActiveView(ViewA.class, () -> { throw boom; });
                                } catch (RuntimeException e) {
                                    if (e != boom) throw e;
                                }
                                afterView[0] = ctxt.getActiveView();
                                g.writeStartObject();
                                g.writeEndObject();
                            }
                        }))
                .build();

        mapper.writerWithView(ViewB.class).writeValueAsString(new Bean5937());
        assertSame(ViewB.class, afterView[0]);
    }

    // [databind#5937]: nested withActiveView calls must each revert to the
    //   view in effect at their entry
    @Test
    public void withActiveViewNested() throws Exception
    {
        final Class<?>[] innerView = new Class<?>[1];
        final Class<?>[] betweenView = new Class<?>[1];
        final Class<?>[] afterView = new Class<?>[1];

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule()
                        .addSerializer(Bean5937.class, new StdSerializer<Bean5937>(Bean5937.class) {
                            @Override
                            public void serialize(Bean5937 value, JsonGenerator g,
                                    SerializationContext ctxt) {
                                ctxt.withActiveView(ViewA.class, () -> {
                                    ctxt.withActiveView(ViewAA.class, () -> {
                                        innerView[0] = ctxt.getActiveView();
                                    });
                                    betweenView[0] = ctxt.getActiveView();
                                });
                                afterView[0] = ctxt.getActiveView();
                                g.writeStartObject();
                                g.writeEndObject();
                            }
                        }))
                .build();

        mapper.writerWithView(ViewB.class).writeValueAsString(new Bean5937());
        assertSame(ViewAA.class, innerView[0]);
        assertSame(ViewA.class, betweenView[0]);
        assertSame(ViewB.class, afterView[0]);
    }

    // Nested/array data binding with a view: only view-annotated properties serialized
    @Test
    public void dataBindingUsage() throws Exception
    {
        ObjectMapper mapper = createNonNullMapper();
        String result = mapper.writerWithView(Views.View.class).writeValueAsString(new ComplexTestData());
        assertEquals(-1, result.indexOf( "nameHidden" ));
    }

    @Test
    public void dataBindingUsageWithoutView() throws Exception
    {
        ObjectMapper mapper = createNonNullMapper();
        String json = mapper.writerWithView(null).writeValueAsString(new ComplexTestData());
        assertTrue(json.indexOf( "nameHidden" ) > 0);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private ObjectMapper createNonNullMapper()
    {
        return jsonMapperBuilder()
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .build();
    }
}
