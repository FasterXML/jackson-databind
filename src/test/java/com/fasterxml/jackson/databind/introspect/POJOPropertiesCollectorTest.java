package com.fasterxml.jackson.databind.introspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class POJOPropertiesCollectorTest
    extends BaseMapTest
{
    static class Simple {
        public int value;

        @JsonProperty("value")
        public void valueSetter(int v) { value = v; }

        @JsonProperty("value")
        public int getFoobar() { return value; }
    }

    static class SimpleFieldDeser
    {
        @JsonDeserialize String[] values;
    }

    static class SimpleGetterVisibility {
        public int getA() { return 0; }
        protected int getB() { return 1; }
        @SuppressWarnings("unused")
        private int getC() { return 2; }
    }

    // Class for testing 'shared ignore'
    static class Empty {
        public int value;

        public void setValue(int v) { value = v; }

        @JsonIgnore
        public int getValue() { return value; }
    }

    static class IgnoredSetter {
        @JsonProperty
        public int value;

        @JsonIgnore
        public void setValue(int v) { value = v; }

        public int getValue() { return value; }
    }

    static class ImplicitIgnores {
        @JsonIgnore public int a;
        @JsonIgnore public void setB(int b) { }
        public int c;
    }

    // Should find just one setter for "y", due to partial ignore
    static class IgnoredRenamedSetter {
        @JsonIgnore public void setY(int value) { }
        @JsonProperty("y") void foobar(int value) { }
    }

    // should produce a single property, "x"
    static class RenamedProperties {
        @JsonProperty("x")
        public int value;

        public void setValue(int v) { value = v; }

        public int getX() { return value; }
    }

    static class RenamedProperties2
    {
        @JsonProperty("renamed")
        public int getValue() { return 1; }
        public void setValue(int x) { }
    }

    // Testing that we can "merge" properties with renaming
    static class MergedProperties {
        public int x;

        @JsonProperty("x")
        public void setFoobar(int v) { x = v; }
    }

    // Testing that property order is obeyed, even for deserialization purposes
    @JsonPropertyOrder({"a", "b", "c", "d"})
    static class SortedProperties
    {
        public int b;
        public int c;

        public void setD(int value) { }
        public void setA(int value) { }
    }

    // [JACKSON-700]: test property type detection, selection
    static class TypeTestBean
    {
        protected Long value;

        @JsonCreator
        public TypeTestBean(@JsonProperty("value") String value) { }

        // If you remove this method, the test will pass
        public Integer getValue() { return 0; }
    }

    static class Jackson703
    {
        private List<FoodOrgLocation> location = new ArrayList<FoodOrgLocation>();

        {
            location.add(new FoodOrgLocation());
        }

        public List<FoodOrgLocation> getLocation() { return location; }
    }

    static class FoodOrgLocation
    {
        protected Long id;
        public String name;
        protected Location location;

        public FoodOrgLocation() {
            location = new Location();
        }

        public FoodOrgLocation(final Location foodOrg) { }

        public FoodOrgLocation(final Long id, final String name, final Location location) { }

        public Location getLocation() { return location; }
    }

    static class Location {
        public BigDecimal lattitude;
        public BigDecimal longitude;

        public Location() { }

        public Location(final BigDecimal lattitude, final BigDecimal longitude) { }
    }

    class Issue701Bean { // important: non-static!
        private int i;

        // annotation does not matter -- just need one on the last argument
        public Issue701Bean(@JsonProperty int i) { this.i = i; }

        public int getX() { return i; }
    }

    static class Issue744Bean
    {
        protected Map<String,Object> additionalProperties;

        @JsonAnySetter
        public void addAdditionalProperty(String key, Object value) {
            if (additionalProperties == null) additionalProperties = new HashMap<String, Object>();
            additionalProperties.put(key,value);
        }

        public void setAdditionalProperties(Map<String, Object> additionalProperties) {
            this.additionalProperties = additionalProperties;
        }

        @JsonAnyGetter
        public Map<String,Object> getAdditionalProperties() { return additionalProperties; }

        @JsonIgnore
        public String getName() {
           return (String) additionalProperties.get("name");
        }
    }

    static class PropDescBean
    {
        public final static String A_DESC = "That's A!";
        public final static int B_INDEX = 3;

        @JsonPropertyDescription(A_DESC)
        public String a;

        protected int b;

        public String getA() { return a; }

        public void setA(String a) { this.a = a; }

        @JsonProperty(required=true, index=B_INDEX, defaultValue="13")
        public int getB() { return b; }
    }

    @Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @JacksonAnnotation
    @interface A {}

    @Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @JacksonAnnotation
    @interface B {}

    static class DuplicateGetterBean
    {
        @A
        public boolean isBloop() { return true; }

        @B
        public boolean getBloop() { return true; }
    }

    static class DuplicateGetterCreatorBean
    {
        public DuplicateGetterCreatorBean(@JsonProperty("bloop") @A boolean bloop) {}

        public boolean isBloop() { return true; }

        public boolean getBloop() { return true; }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = objectMapper();

    public void testSimple()
    {
        POJOPropertiesCollector coll = collector(MAPPER,
        		Simple.class, true);
        Map<String, POJOPropertyBuilder> props = coll.getPropertyMap();
        assertEquals(1, props.size());
        POJOPropertyBuilder prop = props.get("value");
        assertNotNull(prop);
        assertTrue(prop.hasSetter());
        assertTrue(prop.hasGetter());
        assertTrue(prop.hasField());
    }

    public void testSimpleFieldVisibility()
    {
        // false -> deserialization
        POJOPropertiesCollector coll = collector(MAPPER,
        		SimpleFieldDeser.class, false);
        Map<String, POJOPropertyBuilder> props = coll.getPropertyMap();
        assertEquals(1, props.size());
        POJOPropertyBuilder prop = props.get("values");
        assertNotNull(prop);
        assertFalse(prop.hasSetter());
        assertFalse(prop.hasGetter());
        assertTrue(prop.hasField());
    }

    public void testSimpleGetterVisibility()
    {
        POJOPropertiesCollector coll = collector(MAPPER,
        		SimpleGetterVisibility.class, true);
        Map<String, POJOPropertyBuilder> props = coll.getPropertyMap();
        assertEquals(1, props.size());
        POJOPropertyBuilder prop = props.get("a");
        assertNotNull(prop);
        assertFalse(prop.hasSetter());
        assertTrue(prop.hasGetter());
        assertFalse(prop.hasField());
    }

    // Unit test for verifying that a single @JsonIgnore can remove the
    // whole property, unless explicit property marker exists
    public void testEmpty()
    {
        POJOPropertiesCollector coll = collector(MAPPER,
        		Empty.class, true);
        Map<String, POJOPropertyBuilder> props = coll.getPropertyMap();
        assertEquals(0, props.size());
    }

    // Unit test for verifying handling of 'partial' @JsonIgnore; that is,
    // if there is at least one explicit annotation to indicate property,
    // only parts that are ignored are, well, ignored
    public void testPartialIgnore()
    {
        POJOPropertiesCollector coll = collector(MAPPER,
        		IgnoredSetter.class, true);
        Map<String, POJOPropertyBuilder> props = coll.getPropertyMap();
        assertEquals(1, props.size());
        POJOPropertyBuilder prop = props.get("value");
        assertNotNull(prop);
        assertFalse(prop.hasSetter());
        assertTrue(prop.hasGetter());
        assertTrue(prop.hasField());
    }

    public void testSimpleRenamed()
    {
        POJOPropertiesCollector coll = collector(MAPPER,
        		RenamedProperties.class, true);
        Map<String, POJOPropertyBuilder> props = coll.getPropertyMap();
        assertEquals(1, props.size());
        POJOPropertyBuilder prop = props.get("x");
        assertNotNull(prop);
        assertTrue(prop.hasSetter());
        assertTrue(prop.hasGetter());
        assertTrue(prop.hasField());
    }

    public void testSimpleRenamed2()
    {
        POJOPropertiesCollector coll = collector(MAPPER,
        		RenamedProperties2.class, true);
        Map<String, POJOPropertyBuilder> props = coll.getPropertyMap();
        assertEquals(1, props.size());
        POJOPropertyBuilder prop = props.get("renamed");
        assertNotNull(prop);
        assertTrue(prop.hasSetter());
        assertTrue(prop.hasGetter());
        assertFalse(prop.hasField());
    }

    public void testMergeWithRename()
    {
        POJOPropertiesCollector coll = collector(MAPPER,
        		MergedProperties.class, true);
        Map<String, POJOPropertyBuilder> props = coll.getPropertyMap();
        assertEquals(1, props.size());
        POJOPropertyBuilder prop = props.get("x");
        assertNotNull(prop);
        assertTrue(prop.hasSetter());
        assertFalse(prop.hasGetter());
        assertTrue(prop.hasField());
    }

    public void testSimpleIgnoreAndRename()
    {
        POJOPropertiesCollector coll = collector(MAPPER,
        		IgnoredRenamedSetter.class, true);
        Map<String, POJOPropertyBuilder> props = coll.getPropertyMap();
        assertEquals(1, props.size());
        POJOPropertyBuilder prop = props.get("y");
        assertNotNull(prop);
        assertTrue(prop.hasSetter());
        assertFalse(prop.hasGetter());
        assertFalse(prop.hasField());
    }

    public void testGlobalVisibilityForGetters()
    {
        ObjectMapper m = jsonMapperBuilder()
                .configure(MapperFeature.AUTO_DETECT_GETTERS, false)
                .build();
        POJOPropertiesCollector coll = collector(m, SimpleGetterVisibility.class, true);
        // should be 1, expect that we disabled getter auto-detection, so
        Map<String, POJOPropertyBuilder> props = coll.getPropertyMap();
        assertEquals(0, props.size());
    }

    public void testCollectionOfIgnored()
    {
        POJOPropertiesCollector coll = collector(MAPPER, ImplicitIgnores.class, false);
        // should be 1, due to ignorals
        Map<String, POJOPropertyBuilder> props = coll.getPropertyMap();
        assertEquals(1, props.size());
        // but also have 2 ignored properties
        Collection<String> ign = coll.getIgnoredPropertyNames();
        assertEquals(2, ign.size());
        assertTrue(ign.contains("a"));
        assertTrue(ign.contains("b"));
    }

    public void testSimpleOrderingForDeserialization()
    {
        POJOPropertiesCollector coll = collector(MAPPER, SortedProperties.class, false);
        List<BeanPropertyDefinition> props = coll.getProperties();
        assertEquals(4, props.size());
        assertEquals("a", props.get(0).getName());
        assertEquals("b", props.get(1).getName());
        assertEquals("c", props.get(2).getName());
        assertEquals("d", props.get(3).getName());
    }

    public void testSimpleWithType()
    {
        // first for serialization; should base choice on getter
        POJOPropertiesCollector coll = collector(MAPPER, TypeTestBean.class, true);
        List<BeanPropertyDefinition> props = coll.getProperties();
        assertEquals(1, props.size());
        assertEquals("value", props.get(0).getName());
        AnnotatedMember m = props.get(0).getAccessor();
        assertTrue(m instanceof AnnotatedMethod);
        assertEquals(Integer.class, m.getRawType());

        // then for deserialization; prefer ctor param
        coll = collector(MAPPER, TypeTestBean.class, false);
        props = coll.getProperties();
        assertEquals(1, props.size());
        assertEquals("value", props.get(0).getName());
        m = props.get(0).getMutator();
        assertEquals(AnnotatedParameter.class, m.getClass());
        assertEquals(String.class, m.getRawType());
    }

    public void testInnerClassWithAnnotationsInCreator() throws Exception
    {
        BeanDescription beanDesc;
        // first with serialization
        beanDesc = MAPPER.getSerializationConfig().introspect(MAPPER.constructType(Issue701Bean.class));
        assertNotNull(beanDesc);
        // then with deserialization
        beanDesc = MAPPER.getDeserializationConfig().introspect(MAPPER.constructType(Issue701Bean.class));
        assertNotNull(beanDesc);
    }

    public void testUseAnnotationsFalse() throws Exception
    {
        // note: need a separate mapper, need to reconfigure
        ObjectMapper mapper = jsonMapperBuilder()
                .configure(MapperFeature.USE_ANNOTATIONS, false)
                .build();
        BeanDescription beanDesc = mapper.getSerializationConfig().introspect(mapper.constructType(Jackson703.class));
        assertNotNull(beanDesc);

        Jackson703 bean = new Jackson703();
        String json = mapper.writeValueAsString(bean);
        assertNotNull(json);
    }

    public void testJackson744() throws Exception
    {
        BeanDescription beanDesc = MAPPER.getDeserializationConfig().introspect
                (MAPPER.constructType(Issue744Bean.class));
        assertNotNull(beanDesc);
        AnnotatedMember setter = beanDesc.findAnySetterAccessor();
        assertNotNull(setter);
        assertEquals("addAdditionalProperty", setter.getName());
        assertTrue(setter instanceof AnnotatedMethod);
    }

    // [databind#269]: Support new @JsonPropertyDescription
    public void testPropertyDesc() throws Exception
    {
        // start via deser
        BeanDescription beanDesc = MAPPER.getDeserializationConfig().introspect(MAPPER.constructType(PropDescBean.class));
        _verifyProperty(beanDesc, true, false, "13");
        // and then via ser:
        beanDesc = MAPPER.getSerializationConfig().introspect(MAPPER.constructType(PropDescBean.class));
        _verifyProperty(beanDesc, true, false, "13");
    }

    // [databind#438]: Support @JsonProperty.index
    public void testPropertyIndex() throws Exception
    {
        BeanDescription beanDesc = MAPPER.getDeserializationConfig().introspect(MAPPER.constructType(PropDescBean.class));
        _verifyProperty(beanDesc, false, true, "13");
        beanDesc = MAPPER.getSerializationConfig().introspect(MAPPER.constructType(PropDescBean.class));
        _verifyProperty(beanDesc, false, true, "13");
    }

    public void testDuplicateGetters() throws Exception
    {
        POJOPropertiesCollector coll = collector(MAPPER, DuplicateGetterBean.class, true);
        List<BeanPropertyDefinition> props = coll.getProperties();
        assertEquals(1, props.size());
        BeanPropertyDefinition prop = props.get(0);
        assertEquals("bloop", prop.getName());
        assertTrue(prop.getGetter().hasAnnotation(A.class));
        assertTrue(prop.getGetter().hasAnnotation(B.class));
    }

    public void testDuplicateGettersCreator() throws Exception
    {
        POJOPropertiesCollector coll = collector(MAPPER, DuplicateGetterCreatorBean.class, true);
        List<BeanPropertyDefinition> props = coll.getProperties();
        assertEquals(1, props.size());
        POJOPropertyBuilder prop = (POJOPropertyBuilder) props.get(0);
        assertEquals("bloop", prop.getName());
        // Can't call getGetter or the duplicate will be removed
        assertTrue(prop._getters.value.hasAnnotation(A.class));
        assertNotNull(prop._getters.next);
        assertTrue(prop._getters.next.value.hasAnnotation(A.class));
    }

    private void _verifyProperty(BeanDescription beanDesc,
    		boolean verifyDesc, boolean verifyIndex, String expDefaultValue)
    {
        assertNotNull(beanDesc);
        List<BeanPropertyDefinition> props = beanDesc.findProperties();
        assertEquals(2, props.size());
        for (BeanPropertyDefinition prop : props) {
            String name = prop.getName();
            final PropertyMetadata md = prop.getMetadata();
            if ("a".equals(name)) {
                assertFalse(md.isRequired());
                assertNull(md.getRequired());
                if (verifyDesc) {
                	assertEquals(PropDescBean.A_DESC, md.getDescription());
                }
                if (verifyIndex) {
                	assertNull(md.getIndex());
                }
            } else if ("b".equals(name)) {
                assertTrue(md.isRequired());
                assertEquals(Boolean.TRUE, md.getRequired());
                if (verifyDesc) {
                	assertNull(md.getDescription());
                }
                if (verifyIndex) {
                	assertEquals(Integer.valueOf(PropDescBean.B_INDEX), md.getIndex());
                }
                if (expDefaultValue != null) {
                    assertEquals(expDefaultValue, md.getDefaultValue());
                }
            } else {
                fail("Unrecognized property '"+name+"'");
            }
        }
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    protected POJOPropertiesCollector collector(ObjectMapper m0,
            Class<?> cls, boolean forSerialization)
    {
        BasicClassIntrospector bci = new BasicClassIntrospector();
        // no real difference between serialization, deserialization, at least here
        if (forSerialization) {
            return bci.collectProperties(m0.getSerializationConfig(),
                    m0.constructType(cls), null, true);
        }
        return bci.collectProperties(m0.getDeserializationConfig(),
                m0.constructType(cls), null, false);
    }
}
